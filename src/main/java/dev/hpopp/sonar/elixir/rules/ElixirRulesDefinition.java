package dev.hpopp.sonar.elixir.rules;

import dev.hpopp.sonar.elixir.language.Elixir;
import java.util.List;
import org.sonar.api.server.rule.RulesDefinition;

public class ElixirRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "elixir";
  public static final String REPOSITORY_NAME = "Elixir Analyzer";

  private static final List<RuleDefinition> RULES = List.of(
    new RuleDefinition(
      "function_names",
      "Function names should be written in snake_case",
      "Rename this function to use snake_case",
      "CODE_SMELL",
      "MINOR",
      true
    ),
    new RuleDefinition(
      "hardcoded_secret",
      "Credentials should not be hardcoded",
      "Use environment variables or runtime configuration for secrets",
      "VULNERABILITY",
      "BLOCKER",
      true
    ),
    new RuleDefinition(
      "io_inspect",
      "IO.inspect calls should be removed",
      "Remove debugging IO.inspect calls before committing",
      "CODE_SMELL",
      "MAJOR",
      true
    ),
    new RuleDefinition(
      "large_module",
      "Modules should not have too many lines",
      "Split large modules into smaller, focused modules",
      "CODE_SMELL",
      "MINOR",
      false
    ),
    new RuleDefinition(
      "module_names",
      "Module names should be written in PascalCase",
      "Rename this module to use PascalCase",
      "CODE_SMELL",
      "MINOR",
      true
    ),
    new RuleDefinition(
      "missing_moduledoc",
      "Modules should have a @moduledoc attribute",
      "Add @moduledoc to document this module's purpose",
      "CODE_SMELL",
      "MINOR",
      true
    ),
    new RuleDefinition(
      "module_attribute_names",
      "Module attribute names should be written in snake_case",
      "Rename this module attribute to use snake_case",
      "CODE_SMELL",
      "MINOR",
      true
    ),
    new RuleDefinition(
      "pipe_chain_start",
      "Pipe chains should start with a raw value",
      "Use a variable or literal as the first element of a pipe chain",
      "CODE_SMELL",
      "MINOR",
      false
    )
  );

  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(REPOSITORY_KEY, Elixir.KEY)
      .setName(REPOSITORY_NAME);

    for (RuleDefinition rule : RULES) {
      NewRule newRule = repository
        .createRule(rule.key())
        .setName(rule.name())
        .setHtmlDescription(loadRuleDescription(rule.key()))
        .setType(org.sonar.api.rules.RuleType.valueOf(rule.type()))
        .setSeverity(rule.severity());

      newRule.setDebtRemediationFunction(
        newRule.debtRemediationFunctions().constantPerIssue("5min")
      );
    }

    repository.done();
  }

  public static List<String> ruleKeys() {
    return RULES.stream().map(RuleDefinition::key).toList();
  }

  public static List<String> defaultProfileKeys() {
    return RULES.stream()
      .filter(RuleDefinition::defaultProfile)
      .map(RuleDefinition::key)
      .toList();
  }

  private String loadRuleDescription(String ruleKey) {
    // Descriptions loaded from
    // src/main/resources/org/sonar/l10n/elixir/rules/<key>.html
    var stream = getClass().getResourceAsStream(
      "/org/sonar/l10n/elixir/rules/" + ruleKey + ".html"
    );
    if (stream == null) {
      return "<p>No description available.</p>";
    }
    try (stream) {
      return new String(stream.readAllBytes());
    } catch (Exception e) {
      return "<p>No description available.</p>";
    }
  }

  private record RuleDefinition(
    String key,
    String name,
    String description,
    String type,
    String severity,
    boolean defaultProfile
  ) {
    // Empty
  }
}
