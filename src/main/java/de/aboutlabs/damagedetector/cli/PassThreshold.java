package de.aboutlabs.damagedetector.cli;

import de.aboutlabs.damagedetector.domain.PassPolicy;
import de.aboutlabs.damagedetector.probe.Probe;
import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.util.List;
import java.util.Optional;

/** The raw detector.pass-threshold text, so that "abc" is a refusal and not a binding failure (DDD-32). Pure value. */
public final class PassThreshold implements Probe {

    private static final String PLAIN_NUMBER = "[0-9]+(\\.[0-9]+)?";

    private final String raw;
    private final Optional<PassPolicy> policy;

    private PassThreshold(String raw, Optional<PassPolicy> policy) {
        this.raw = raw;
        this.policy = policy;
    }

    public static PassThreshold of(String raw) {
        String text = raw == null ? "" : raw;
        return new PassThreshold(text, policyOf(text));
    }

    public PassPolicy policy() {
        return policy.orElseThrow(() -> new IllegalStateException("Pass threshold was refused: " + raw));
    }

    @Override
    public ProbeResult probe() {
        if (policy.isPresent()) {
            return new ProbeResult.Ok();
        }
        return new ProbeResult.Refused(ProbeResult.Reason.THRESHOLD_INVALID, List.of(raw));
    }

    private static Optional<PassPolicy> policyOf(String text) {
        if (!text.matches(PLAIN_NUMBER)) {
            return Optional.empty();
        }
        double value = Double.parseDouble(text);
        return value <= 1.0 ? Optional.of(new PassPolicy(value)) : Optional.empty();
    }
}
