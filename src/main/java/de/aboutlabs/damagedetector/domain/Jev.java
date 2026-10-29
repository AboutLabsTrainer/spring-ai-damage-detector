package de.aboutlabs.damagedetector.domain;

/** Driven port: asks Jev which defect class an image shows. Only image bytes cross this port, never a file name (C11). */
public interface Jev {

    JevJudgement judge(byte[] image, String mediaType);
}
