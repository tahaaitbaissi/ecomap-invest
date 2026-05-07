import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import ProfileRuleEditor from "./ProfileRuleEditor";
import type { ProfileTagOption } from "@/services/api/profileService";

const options: ProfileTagOption[] = [
  {
    tag: "amenity=cafe",
    label: "Cafe",
    group: "Food",
    description: "Coffee shops and casual meeting places.",
    aliases: [],
  },
  {
    tag: "office=company",
    label: "Company office",
    group: "Office",
    description: "General offices and employment centers.",
    aliases: [],
  },
];

describe("ProfileRuleEditor", () => {
  it("adds and removes structured rules", () => {
    const onChange = vi.fn();
    render(
      <ProfileRuleEditor
        title="Drivers"
        description="Things that increase demand."
        rules={[]}
        onChange={onChange}
        options={options}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Add driver" }));
    expect(onChange).toHaveBeenCalledWith([{ tag: "amenity=cafe", weight: 1 }]);
  });

  it("shows inline row errors and selected tag description", () => {
    render(
      <ProfileRuleEditor
        title="Competitors"
        description="Similar businesses."
        rules={[{ tag: "office=company", weight: 2 }]}
        onChange={vi.fn()}
        options={options}
        errors={{ 0: "Weight must be between 0.1 and 1.5." }}
      />,
    );

    expect(screen.getByText("General offices and employment centers.")).toBeTruthy();
    expect(screen.getByText("Weight must be between 0.1 and 1.5.")).toBeTruthy();
  });

  it("does not visually replace unsupported selected tags with the first catalog option", () => {
    render(
      <ProfileRuleEditor
        title="Drivers"
        description="Things that increase demand."
        rules={[{ tag: "amenity=lawyer", weight: 1.2 }]}
        onChange={vi.fn()}
        options={options}
        errors={{ 0: "Choose a supported tag from the catalog." }}
      />,
    );

    expect(screen.getByRole("option", { name: "Unsupported (amenity=lawyer)" })).toBeTruthy();
  });
});
