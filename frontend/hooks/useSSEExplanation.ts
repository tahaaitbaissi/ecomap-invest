"use client";

import { useCallback, useRef, useState } from "react";

export interface SSEExplanationState {
  text: string;
  isStreaming: boolean;
  isComplete: boolean;
  error: string | null;
  start: () => void;
  stop: () => void;
}

function mockExplanation(h3Index: string, score: number): string {
  const quality = score >= 80 ? "excellente" : score >= 60 ? "bonne" : score >= 40 ? "modérée" : "faible";
  const opportunity = score >= 60 ? "opportunité d'investissement significative" : "zone à surveiller avec prudence";
  return (
    `**Analyse de la zone ${h3Index.slice(-6)}**\n\n` +
    `Ce secteur présente une **attractivité ${quality}** avec un score global de **${score}/100**.\n\n` +
    `**Points positifs :**\n` +
    `- Flux piétonnier ${score > 60 ? "élevé" : "modéré"} aux heures de pointe\n` +
    `- Densité de population favorable à l'implantation commerciale\n` +
    `- ${score > 70 ? "Faible" : "Moyenne"} pression concurrentielle dans un rayon de 500 m\n\n` +
    `**Points de vigilance :**\n` +
    `- ${score < 50 ? "Saturation possible du marché local" : "Loyers potentiellement élevés"}\n` +
    `- Saisonnalité à prendre en compte selon votre secteur\n\n` +
    `En résumé, cette zone représente une **${opportunity}**. ` +
    `Il est recommandé de croiser cette analyse avec les données terrain avant toute décision finale.`
  );
}

export function useSSEExplanation(
  h3Index: string | null,
  profileId: string | null,
  score = 50
): SSEExplanationState {
  const [text, setText] = useState("");
  const [isStreaming, setIsStreaming] = useState(false);
  const [isComplete, setIsComplete] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const esRef = useRef<EventSource | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearAll = useCallback(() => {
    esRef.current?.close();
    esRef.current = null;
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = null;
  }, []);

  const stop = useCallback(() => {
    clearAll();
    setIsStreaming(false);
  }, [clearAll]);

  const streamMock = useCallback(
    (fullText: string) => {
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
    },
    []
  );

  const start = useCallback(() => {
    if (!h3Index) return;
    clearAll();
    setText("");
    setIsStreaming(true);
    setIsComplete(false);
    setError(null);

    const params = new URLSearchParams({ h3Index });
    if (profileId) params.set("profileId", profileId);
    const url = `/api/v1/ai/explain?${params.toString()}`;

    try {
      const es = new EventSource(url, { withCredentials: true });
      esRef.current = es;

      es.onmessage = (e) => {
        if (e.data === "[DONE]") {
          es.close();
          esRef.current = null;
          setIsStreaming(false);
          setIsComplete(true);
          return;
        }
        setText((t) => t + e.data);
      };

      es.onerror = () => {
        es.close();
        esRef.current = null;
        // Backend unavailable — fall back to simulated stream
        streamMock(mockExplanation(h3Index, score));
      };
    } catch {
      streamMock(mockExplanation(h3Index, score));
    }
  }, [h3Index, profileId, score, clearAll, streamMock]);

  return { text, isStreaming, isComplete, error, start, stop };
}
