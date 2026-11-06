// SCAFFOLD: true
package de.aboutlabs.damagedetector.jev;

import de.aboutlabs.damagedetector.probe.Probe;

/** One attempt at Jev, plus the configuration probe. The stub seam of the retry policy (DDD-36). */
interface AttemptSource extends Probe {

    AttemptResult attempt(byte[] image, String mediaType);
}
