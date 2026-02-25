package dev.hpopp.sonar.elixir.coverage;

import dev.hpopp.sonar.elixir.language.Elixir;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Imports coverage data from the generic coverage XML format.
 *
 * Users generate this file by running `mix sonar.coverage` which reads
 * .coverdata files produced by `mix test --cover` and writes
 * sonar-coverage.xml in SonarQube's generic coverage format.
 *
 * The actual import of the generic format is handled by SonarQube core
 * via the sonar.coverageReportPaths property. This sensor exists as a
 * placeholder for any Elixir-specific coverage processing if needed.
 */
public class CoverageSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageSensor.class);

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
                .name("Elixir Coverage")
                .onlyOnLanguage(Elixir.KEY);
    }

    @Override
    public void execute(SensorContext context) {
        LOG.info("Elixir coverage import is handled via sonar.coverageReportPaths");
    }
}
