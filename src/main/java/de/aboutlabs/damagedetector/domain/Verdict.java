package de.aboutlabs.damagedetector.domain;

/** Outcome of inspecting one part. Only PASS lets a part through; REVIEW means a human must decide. */
public enum Verdict {
    PASS,
    SCRAP,
    REVIEW
}
