package dev.hpopp.sonar.elixir.sensors;

import dev.hpopp.sonar.elixir.language.Elixir;
import dev.hpopp.sonar.elixir.rules.ElixirRule;
import dev.hpopp.sonar.elixir.rules.ElixirRulesDefinition;
import java.io.IOException;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.measures.CoreMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElixirSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(ElixirSensor.class);

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
                .name("Elixir Analyzer")
                .onlyOnLanguage(Elixir.KEY);
    }

    @Override
    public void execute(SensorContext context) {
        FilePredicates predicates = context.fileSystem().predicates();
        Iterable<InputFile> files = context.fileSystem().inputFiles(
                predicates.hasLanguage(Elixir.KEY));

        ElixirParser parser = new ElixirParser();
        var rules = ElixirRule.allRules();

        for (InputFile file : files) {
            LOG.info("Analyzing: {}", file.filename());

            computeMetrics(context, file);

            ElixirAst ast = parser.parse(file.path());
            if (ast == null) {
                LOG.warn("Failed to parse AST: {}", file.filename());
                continue;
            }

            for (ElixirRule rule : rules) {
                rule.analyze(context, file, ast);
            }
        }
    }

    private void computeMetrics(SensorContext context, InputFile file) {
        try {
            String contents = file.contents();
            String[] lines = contents.split("\n", -1);

            int ncloc = 0;
            int commentLines = 0;

            for (String line : lines) {
                String trimmed = line.strip();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("#")) {
                    commentLines++;
                } else {
                    ncloc++;
                }
            }

            context.<Integer>newMeasure()
                    .forMetric(CoreMetrics.NCLOC)
                    .on(file)
                    .withValue(ncloc)
                    .save();

            context.<Integer>newMeasure()
                    .forMetric(CoreMetrics.COMMENT_LINES)
                    .on(file)
                    .withValue(commentLines)
                    .save();

        } catch (IOException e) {
            LOG.warn("Could not read file for metrics: {}", file.filename(), e);
        }
    }
}
