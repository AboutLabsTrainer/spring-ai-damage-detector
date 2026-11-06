// SCAFFOLD: true
package de.aboutlabs.damagedetector.probe;

/** Checks that a dependency can honour a run, before any paid call. */
public interface Probe {

    ProbeResult probe();
}
