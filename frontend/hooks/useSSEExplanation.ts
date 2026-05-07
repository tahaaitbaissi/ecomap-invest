"use client";

import { getToken } from "@/lib/auth";
import { useCallback, useEffect, useRef, useState } from "react";

export interface SSEExplanationState {
  text: string;
  isStreaming: boolean;
  isComplete: boolean;
  error: string | null;
  start: () => void;
  stop: () => void;
}

const MOCK_ALLOWED =
  process.env.NODE_ENV !== "production" &&
  process.env.NEXT_PUBLIC_EXPLAIN_MOCK_FALLBACK === "1";

function mockExplanation(h3Index: string, score: number): string {
  const quality = score >= 80 ? "excellente" : score >= 60 ? "bonne" : score >= 40 ? "modérée" : "faible";
  const opportunity =
    score >= 60 ? "opportunité d'investissement potentielle à confirmer" : "zone à analyser avec prudence";
  return (
    `**(Mode démo — backend indisponible)** Analyse de la zone ${h3Index.slice(-6)}\n\n` +
    `Ce secteur présente une **attractivité ${quality}** avec un score global de **${score}/100**.\n\n` +
    `**Points positifs et vigilance**\n\n` +
    `En résumé, cette zone correspond à une **${opportunity}** — activez un LLM configuré ou retirez NEXT_PUBLIC_EXPLAIN_MOCK_FALLBACK pour des faits métier réels.\n`
  );
}

function parseEventDataBlock(blockLines: readonly string[]): string {
  const dataLines = blockLines.filter((l) => l.startsWith("data:"));
  if (dataLines.length === 0) {
    return "";
  }
  return dataLines
    .map((l) => {
      const rest = l.slice(5);
      return rest.startsWith(" ") ? rest.slice(1) : rest;
    })
    .join("\n");
}

export function useSSEExplanation(
  h3Index: string | null,
  profileId: string | null,
  score = 50,
  viewportCellCount?: number,
): SSEExplanationState {
  const [text, setText] = useState("");
  const [isStreaming, setIsStreaming] = useState(false);
  const [isComplete, setIsComplete] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const abortRef = useRef<AbortController | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearAll = useCallback(() => {
    abortRef.current?.abort();
    abortRef.current = null;
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = null;
  }, []);

  const stop = useCallback(() => {
    clearAll();
    setIsStreaming(false);
  }, [clearAll]);

  const streamMock = useCallback((fullText: string) => {
    const words = fullText.split(" ");
    let i = 0;
    setText("");
    setIsStreaming(true);
    setIsComplete(false);
    setError(null);

    const tick = () => {
      if (i >= words.length) {
        setIsStreaming(false);
        setIsComplete(true);
        return;
      }
      const chunk = (i === 0 ? "" : " ") + words[i++];
      setText((t) => t + chunk);
      timerRef.current = setTimeout(tick, 35 + Math.random() * 45);
    };
    tick();
  }, []);

  const start = useCallback(() => {
    if (!h3Index || !profileId) return;
    clearAll();
    setText("");
    setIsStreaming(true);
    setIsComplete(false);
    setError(null);

    const token = getToken();
    if (!token) {
      setError("Connectez-vous pour obtenir l'explication du score.");
      setIsStreaming(false);
      return;
    }

    const params = new URLSearchParams({ h3Index, profileId });
    if (viewportCellCount != null && viewportCellCount >= 1) {
      params.set("viewportCellCount", String(viewportCellCount));
    }
    const url = `/api/v1/ai/explain?${params.toString()}`;

    const ac = new AbortController();
    abortRef.current = ac;

    void (async () => {
      try {
        const res = await fetch(url, {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
            Accept: "text/event-stream",
          },
          signal: ac.signal,
        });

        if (!res.ok) {
          const msg =
            res.status === 401 || res.status === 403
              ? "Session expirée ou accès refusé — reconnectez-vous."
              : `Erreur serveur (${res.status}).`;
          setError(msg);
          setIsStreaming(false);
          if (MOCK_ALLOWED) {
            streamMock(mockExplanation(h3Index, score));
          }
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
        let sawDone = false;

        readLoop: while (true) {
          const { done, value } = await reader.read();
          if (done) {
            break;
          }
          buffer += decoder.decode(value, { stream: true });
          const parts = buffer.split(/\r?\n\r?\n/);
          buffer = parts.pop() ?? "";
          for (const block of parts) {
            const lines = block.split(/\r?\n/).filter(Boolean);
            const payload = parseEventDataBlock(lines);
            if (payload === "[DONE]") {
              sawDone = true;
              break readLoop;
            }
            if (payload.length > 0) {
              setText((t) => t + payload);
            }
          }
        }

        if (!sawDone && buffer.trim()) {
          const lines = buffer.split(/\r?\n/).filter(Boolean);
          const payload = parseEventDataBlock(lines);
          if (payload === "[DONE]") {
            sawDone = true;
          } else if (payload.length > 0) {
            setText((t) => t + payload);
          }
        }

        setIsStreaming(false);
        setIsComplete(true);
      } catch (e) {
        if ((e as Error).name === "AbortError") {
          setIsStreaming(false);
          return;
        }
        setError("Flux interrompu — vérifiez le réseau ou Ollama.");
        setIsStreaming(false);
        if (MOCK_ALLOWED) {
          streamMock(mockExplanation(h3Index, score));
        }
      }
    })();
  }, [h3Index, profileId, score, viewportCellCount, clearAll, streamMock]);

  useEffect(
    () => () => {
      clearAll();
    },
    [clearAll],
  );

  return { text, isStreaming, isComplete, error, start, stop };
}
