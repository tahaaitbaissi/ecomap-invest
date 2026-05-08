"use client";

import { getToken } from "@/lib/auth";
import { useCallback, useEffect, useRef, useState } from "react";

export interface ChatMessage {
  id: string;
  role: "system" | "user" | "assistant";
  content: string;
}

export interface SSEChatState {
  messages: ChatMessage[];
  isStreaming: boolean;
  error: string | null;
  send: (content: string) => void;
  stop: () => void;
  injectAssistantMessage: (content: string) => void;
  injectSystemMessage: (content: string) => void;
  clear: () => void;
}

export function parseEventDataBlock(blockLines: readonly string[]): string {
  const dataLines = blockLines.filter((l) => l.startsWith("data:"));
  if (dataLines.length === 0) return "";
  return dataLines
    .map((l) => {
      // We consume raw bytes from `fetch()` (not `EventSource`), so we must preserve
      // the payload exactly as emitted by the server, including leading spaces.
      return l.slice(5);
    })
    .join("\n");
}

function mkId() {
  return crypto.randomUUID();
}

export function useSSEChat(params: {
  conversationId: string | null;
  profileId: string | null;
  selectedHexIndex: string | null;
  viewportCellCount?: number;
}): SSEChatState {
  const { conversationId, profileId, selectedHexIndex, viewportCellCount } = params;

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const abortRef = useRef<AbortController | null>(null);

  const stop = useCallback(() => {
    abortRef.current?.abort();
    abortRef.current = null;
    setIsStreaming(false);
  }, []);

  const clear = useCallback(() => {
    stop();
    setMessages([]);
    setError(null);
  }, [stop]);

  const injectAssistantMessage = useCallback((content: string) => {
    setMessages((m) => [...m, { id: mkId(), role: "assistant", content }]);
  }, []);

  const injectSystemMessage = useCallback((content: string) => {
    setMessages((m) => [...m, { id: mkId(), role: "system", content }]);
  }, []);

  const send = useCallback(
    (content: string) => {
      if (!conversationId || !profileId) {
        setError("Sélectionnez un profil commercial pour utiliser l'assistant.");
        return;
      }
      if (!selectedHexIndex) {
        setError("Sélectionnez un hexagone pour démarrer.");
        return;
      }
      const token = getToken();
      if (!token) {
        setError("Connectez-vous pour utiliser l'assistant.");
        return;
      }

      stop();
      setError(null);
      setIsStreaming(true);

      const userMsg: ChatMessage = { id: mkId(), role: "user", content };
      const assistantId = mkId();
      setMessages((m) => [...m, userMsg, { id: assistantId, role: "assistant", content: "" }]);

      const url = `/api/v1/ai/chat/stream`;
      const body = {
        conversationId,
        profileId,
        selectedHexIndex,
        viewportCellCount: viewportCellCount != null && viewportCellCount >= 1 ? viewportCellCount : undefined,
        message: content,
      };

      const ac = new AbortController();
      abortRef.current = ac;

      void (async () => {
        try {
          const res = await fetch(url, {
            method: "POST",
            headers: {
              Authorization: `Bearer ${token}`,
              Accept: "text/event-stream",
              "Content-Type": "application/json",
            },
            body: JSON.stringify(body),
            signal: ac.signal,
          });

          if (!res.ok) {
            const msg =
              res.status === 401 || res.status === 403
                ? "Session expirée ou accès refusé — reconnectez-vous."
                : `Erreur serveur (${res.status}).`;
            setError(msg);
            setIsStreaming(false);
            return;
          }

          const reader = res.body?.getReader();
          if (!reader) {
            setError("Flux de réponse vide.");
            setIsStreaming(false);
            return;
          }

          const decoder = new TextDecoder();
          let buffer = "";

          readLoop: while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            const parts = buffer.split(/\r?\n\r?\n/);
            buffer = parts.pop() ?? "";
            for (const block of parts) {
              const lines = block.split(/\r?\n/).filter(Boolean);
              const payload = parseEventDataBlock(lines);
              if (payload === "[DONE]") {
                break readLoop;
              }
              if (payload.length > 0) {
                setMessages((m) =>
                  m.map((msg) =>
                    msg.id === assistantId ? { ...msg, content: msg.content + payload } : msg,
                  ),
                );
              }
            }
          }

          if (buffer.trim()) {
            const lines = buffer.split(/\r?\n/).filter(Boolean);
            const payload = parseEventDataBlock(lines);
            if (payload.length > 0 && payload !== "[DONE]") {
              setMessages((m) =>
                m.map((msg) =>
                  msg.id === assistantId ? { ...msg, content: msg.content + payload } : msg,
                ),
              );
            }
          }

          setIsStreaming(false);
        } catch (e) {
          if ((e as Error).name === "AbortError") {
            setIsStreaming(false);
            return;
          }
          setError("Flux interrompu — vérifiez le réseau ou l'orchestrator.");
          setIsStreaming(false);
        }
      })();
    },
    [conversationId, profileId, selectedHexIndex, viewportCellCount, stop],
  );

  useEffect(
    () => () => {
      stop();
    },
    [stop],
  );

  return {
    messages,
    isStreaming,
    error,
    send,
    stop,
    injectAssistantMessage,
    injectSystemMessage,
    clear,
  };
}

