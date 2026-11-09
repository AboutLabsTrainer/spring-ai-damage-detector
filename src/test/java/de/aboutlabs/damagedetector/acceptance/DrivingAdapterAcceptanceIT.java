package de.aboutlabs.damagedetector.acceptance;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Runs the @driving_adapter scenarios against the repackaged jar, so it belongs to the integration-test phase.
 * The tag filter comes from the pom. A suite that finds no scenario fails.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/transistor-damage-detection/acceptance")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "de.aboutlabs.damagedetector.acceptance")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
class DrivingAdapterAcceptanceIT {
}
