package dev.hpopp.sonar.elixir.language;

import dev.hpopp.sonar.elixir.rules.ElixirRulesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

public class ElixirQualityProfile implements BuiltInQualityProfilesDefinition {

    @Override
    public void define(Context context) {
        NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(
                "Elixir Way", Elixir.KEY);
        profile.setDefault(true);

        for (String ruleKey : ElixirRulesDefinition.defaultProfileKeys()) {
            profile.activateRule(ElixirRulesDefinition.REPOSITORY_KEY, ruleKey);
        }

        profile.done();
    }
}
