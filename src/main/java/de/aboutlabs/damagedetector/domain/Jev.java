package de.aboutlabs.damagedetector.domain;

/** Driven port: asks Jev which defect class an image shows. Only image bytes cross this port, never a file name (C11). */
public interface Jev {

    /** Never throws for a Jev failure: the outcome says whether the image was judged or every allowed attempt failed. */
    JevOutcome judge(byte[] image, String mediaType);
}
