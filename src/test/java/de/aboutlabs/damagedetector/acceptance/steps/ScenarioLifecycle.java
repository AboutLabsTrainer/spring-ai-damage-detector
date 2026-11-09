package de.aboutlabs.damagedetector.acceptance.steps;

import de.aboutlabs.damagedetector.acceptance.support.DetectorWorld;
import io.cucumber.java.After;
import io.cucumber.java.Before;

public class ScenarioLifecycle {

    private final DetectorWorld world;

    public ScenarioLifecycle(DetectorWorld world) {
        this.world = world;
    }

    @Before
    public void open() {
        world.open();
    }

    @Before("@driving_adapter")
    public void useTheRealProcess() {
        world.run().useTheRealProcess();
    }

    @After
    public void close() {
        world.close();
    }
}
