"use client";

import { useEffect, useRef } from "react";

interface Props {
  text: string;
  isStreaming: boolean;
  isComplete: boolean;
  error: string | null;
  onStop: () => void;
  onRegenerate: () => void;
}

// Minimal markdown → React elements: **bold**, - bullet, \n paragraph break
function renderMarkdown(raw: string) {
  const lines = raw.split("\n");
  const elements: React.ReactNode[] = [];
  let key = 0;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (line.trim() === "") {
      elements.push(<br key={key++} />);
      continue;
    }
    const isBullet = line.startsWith("- ");
    const content = isBullet ? line.slice(2) : line;
    const parts = content.split(/(\*\*[^*]+\*\*)/g).map((chunk, j) =>
      chunk.startsWith("**") && chunk.endsWith("**") ? (
        <strong key={j}>{chunk.slice(2, -2)}</strong>
      ) : (
        chunk
      )
    );

    if (isBullet) {
      elements.push(
        <div key={key++} style={{ display: "flex", gap: "6px", margin: "2px 0" }}>
          <span style={{ color: "#1a56db", fontWeight: 700, flexShrink: 0, marginTop: "1px" }}>•</span>
          <span>{parts}</span>
        </div>
      );
    } else {
      elements.push(<span key={key++}>{parts}</span>);
    }
  }
  return elements;
}

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).catch(() => {});
}

export default function TypewriterText({
  text,
  isStreaming,
  isComplete,
  error,
  onStop,
  onRegenerate,
}: Props) {
  const bottomRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom while streaming
  useEffect(() => {
    if (isStreaming) bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [text, isStreaming]);

  if (!text && !isStreaming && !error) return null;

  return (
    <div
      style={{
        marginTop: "16px",
        borderTop: "1px solid #f1f5f9",
        paddingTop: "14px",
      }}
    >
      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "10px" }}>
        <div
          style={{
            width: "22px",
            height: "22px",
            borderRadius: "50%",
            background: "linear-gradient(135deg, #7c3aed, #1a56db)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            flexShrink: 0,
          }}
        >
          <span style={{ color: "#fff", fontSize: "11px" }}>✦</span>
        </div>
        <span style={{ fontSize: "11px", fontWeight: 700, color: "#374151" }}>Analyse AI</span>
        {isStreaming && (
          <span
            style={{
              fontSize: "10px",
              color: "#1a56db",
              background: "#eff6ff",
              padding: "2px 8px",
              borderRadius: "20px",
              fontWeight: 600,
            }}
          >
            En cours…
          </span>
        )}
      </div>

      {/* Text body */}
      {error ? (
        <p style={{ fontSize: "12px", color: "#ef4444", lineHeight: 1.6 }}>{error}</p>
      ) : (
        <div
          style={{
            fontSize: "12px",
            color: "#374151",
            lineHeight: 1.7,
            background: "#f8fafc",
            borderRadius: "10px",
            padding: "12px",
            minHeight: "40px",
            maxHeight: "260px",
            overflowY: "auto",
          }}
        >
          {renderMarkdown(text)}
          {/* Blinking cursor */}
          {isStreaming && (
            <span
              style={{
                display: "inline-block",
                width: "2px",
                height: "13px",
                background: "#1a56db",
                marginLeft: "2px",
                verticalAlign: "text-bottom",
                animation: "blink 0.8s step-end infinite",
              }}
            />
          )}
          <div ref={bottomRef} />
        </div>
      )}

      <style>{`@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }`}</style>

      {/* Action buttons */}
      <div style={{ display: "flex", gap: "6px", marginTop: "10px" }}>
        {isStreaming ? (
          <button
            onClick={onStop}
            style={{
              flex: 1,
              padding: "7px",
              borderRadius: "8px",
              border: "1px solid #ef4444",
              background: "#fff",
              color: "#ef4444",
              fontSize: "11px",
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            ■ Arrêter
          </button>
        ) : (
          <>
            <button
              onClick={onRegenerate}
              style={{
                flex: 1,
                padding: "7px",
                borderRadius: "8px",
                border: "1px solid #e2e8f0",
                background: "#fff",
                color: "#374151",
                fontSize: "11px",
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              ↺ Régénérer
            </button>
            {isComplete && (
              <button
                onClick={() => copyToClipboard(text)}
                style={{
                  flex: 1,
                  padding: "7px",
                  borderRadius: "8px",
                  border: "1px solid #e2e8f0",
                  background: "#fff",
                  color: "#374151",
                  fontSize: "11px",
                  fontWeight: 600,
                  cursor: "pointer",
                }}
              >
                ⎘ Copier
              </button>
            )}
          </>
        )}
      </div>
    </div>
  );
}
