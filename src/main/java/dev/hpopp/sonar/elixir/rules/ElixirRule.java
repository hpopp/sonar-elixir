package dev.hpopp.sonar.elixir.rules;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import java.util.List;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.fs.InputFile;

/**
 * Base interface for all Elixir analysis rules.
 *
 * Each rule receives the parsed AST and reports issues via the sensor context.
 */
public interface ElixirRule {

    String ruleKey();

    void analyze(SensorContext context, InputFile file, ElixirAst ast);

    static List<ElixirRule> allRules() {
        return List.of(
                new MissingModuledocRule(),
                new PipeChainStartRule(),
                new IoInspectRule());
    }
}
