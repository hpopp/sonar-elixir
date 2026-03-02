package dev.hpopp.sonar.elixir;

import dev.hpopp.sonar.elixir.coverage.CoverageSensor;
import dev.hpopp.sonar.elixir.language.Elixir;
import dev.hpopp.sonar.elixir.language.ElixirQualityProfile;
import dev.hpopp.sonar.elixir.rules.ElixirRulesDefinition;
import dev.hpopp.sonar.elixir.sensors.ElixirSensor;
import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;

public class ElixirPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtensions(
      Elixir.class,
      ElixirQualityProfile.class,
      ElixirRulesDefinition.class,
      ElixirSensor.class,
      CoverageSensor.class
    );

    context.addExtension(
      PropertyDefinition.builder(Elixir.FILE_SUFFIXES_KEY)
        .name("File Suffixes")
        .description("List of suffixes for Elixir files to analyze.")
        .category(Elixir.NAME)
        .defaultValue(Elixir.FILE_SUFFIXES_DEFAULT)
        .onConfigScopes(PropertyDefinition.ConfigScope.PROJECT)
        .multiValues(true)
        .build()
    );
  }
}
