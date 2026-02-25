package dev.hpopp.sonar.elixir;

import dev.hpopp.sonar.elixir.coverage.CoverageSensor;
import dev.hpopp.sonar.elixir.language.Elixir;
import dev.hpopp.sonar.elixir.language.ElixirQualityProfile;
import dev.hpopp.sonar.elixir.rules.ElixirRulesDefinition;
import dev.hpopp.sonar.elixir.sensors.ElixirSensor;
import org.sonar.api.Plugin;

public class ElixirPlugin implements Plugin {

    @Override
    public void define(Context context) {
        context.addExtensions(
                Elixir.class,
                ElixirQualityProfile.class,
                ElixirRulesDefinition.class,
                ElixirSensor.class,
                CoverageSensor.class);
    }
}
