package dev.hpopp.sonar.elixir.rules;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;

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
      new FunctionNameRule(),
      new HardcodedSecretRule(),
      new IoInspectRule(),
      new LargeModuleRule(),
      new MissingModuledocRule(),
      new ModuleAttributeNameRule(),
      new ModuleNameRule(),
      new PipeChainStartRule()
    );
  }
}
