package de.aboutlabs.damagedetector.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Universe-bound assertion (Mandate 8). The universe names port-exposed observables; every entry without an
 * expectation must stay unchanged, so an unplanned change fails closed.
 */
public final class StateDelta {

    /** What must hold for one universe entry between the two snapshots. */
    public interface Expectation {

        boolean holds(Object before, Object after);

        String describe();
    }

    private StateDelta() {
    }

    public static void assertStateDelta(Map<String, ?> before, Map<String, ?> after, Set<String> universe,
                                        Map<String, Expectation> expected) {
        List<String> violations = new ArrayList<>();
        expected.keySet().stream()
            .filter(key -> !universe.contains(key))
            .forEach(key -> violations.add(key + ": expected a change outside the declared universe"));
        for (String key : new TreeSet<>(universe)) {
            if (!before.containsKey(key) || !after.containsKey(key)) {
                violations.add(key + ": not observable in the snapshots");
                continue;
            }
            Expectation expectation = expected.getOrDefault(key, unchanged());
            if (!expectation.holds(before.get(key), after.get(key))) {
                violations.add(key + ": expected " + expectation.describe() + " but went from <" + before.get(key)
                    + "> to <" + after.get(key) + ">");
            }
        }
        if (!violations.isEmpty()) {
            throw new AssertionError("State delta violated:\n  " + String.join("\n  ", violations));
        }
    }

    public static Expectation unchanged() {
        return expectation("unchanged", Objects::equals);
    }

    public static Expectation setTo(Object value) {
        return expectation("set to <" + value + ">", (before, after) -> Objects.equals(after, value));
    }

    /** Every element of a collection observable satisfies the rule. */
    public static Expectation everyEntry(String description, Predicate<String> rule) {
        return expectation("every entry " + description, (before, after) -> entriesOf(after).stream().allMatch(rule));
    }

    private static List<String> entriesOf(Object collection) {
        return ((java.util.Collection<?>) collection).stream().map(String::valueOf).toList();
    }

    private static Expectation expectation(String description, java.util.function.BiPredicate<Object, Object> rule) {
        return new Expectation() {
            @Override
            public boolean holds(Object before, Object after) {
                return rule.test(before, after);
            }

            @Override
            public String describe() {
                return description;
            }
        };
    }
}
