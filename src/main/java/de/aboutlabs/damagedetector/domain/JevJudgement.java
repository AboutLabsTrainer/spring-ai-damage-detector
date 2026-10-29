package de.aboutlabs.damagedetector.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What Jev answered for one image: the chosen class, the probability per class, its confidence and the call cost.
 * The class label is a suspicion, not a fact (the spike measured only 20/40 exact-class hits).
 */
public record JevJudgement(String choice, Map<String, Double> probabilities, double confidence, double cost) {

    public static final String GOOD = "GOOD";

    public JevJudgement {
        probabilities = Collections.unmodifiableMap(new LinkedHashMap<>(probabilities));
    }

    public boolean choseGood() {
        return GOOD.equals(choice);
    }

    public double probabilityOfGood() {
        return probabilityOf(GOOD);
    }

    public double probabilityOfChoice() {
        return probabilityOf(choice);
    }

    private double probabilityOf(String defectClass) {
        return probabilities.getOrDefault(defectClass, 0.0);
    }
}
