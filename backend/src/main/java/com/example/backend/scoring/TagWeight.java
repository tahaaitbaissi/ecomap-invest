package com.example.backend.scoring;

/** Weighted OSM type_tag used in hex scoring and simulation. */
public record TagWeight(String tag, double weight) {}
