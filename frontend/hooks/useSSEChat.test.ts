import { describe, expect, it } from "vitest";
import { parseEventDataBlock } from "./useSSEChat";

describe("useSSEChat.parseEventDataBlock", () => {
  it("joins multiple data lines with newline", () => {
    const payload = parseEventDataBlock(["data: hello", "data: world"]);
    expect(payload).toBe("hello\nworld");
  });

  it("preserves leading spaces inside tokens", () => {
    const payload = parseEventDataBlock(["data: Hello", "data:  world"]);
    expect(payload).toBe("Hello\n world");
  });

  it("returns empty for blocks without data:", () => {
    const payload = parseEventDataBlock(["event: message", "id: 1"]);
    expect(payload).toBe("");
  });

  it("keeps [DONE] unchanged", () => {
    const payload = parseEventDataBlock(["data: [DONE]"]);
    expect(payload).toBe("[DONE]");
  });
});

