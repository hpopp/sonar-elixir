package dev.hpopp.sonar.elixir.rules;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

/**
 * S005: Function, macro, and guard names should be written in snake_case.
 *
 * Walks the AST looking for def, defp, defmacro, defmacrop, defguard, and
 * defguardp nodes and checks whether the declared name follows snake_case
 * conventions.
 */
public class FunctionNameRule implements ElixirRule {

  private static final String KEY = "function_names";

  private static final Set<String> DEFINITION_TYPES = Set.of(
    "def",
    "defp",
    "defmacro",
    "defmacrop",
    "defguard",
    "defguardp"
  );

  private static final Pattern SNAKE_CASE = Pattern.compile(
    "^[a-z_][a-z0-9_]*[?!]?$"
  );

  @Override
  public String ruleKey() {
    return KEY;
  }

  @Override
  public void analyze(SensorContext context, InputFile file, ElixirAst ast) {
    for (Finding finding : detect(ast)) {
      RuleKey ruleKey = RuleKey.of(ElixirRulesDefinition.REPOSITORY_KEY, KEY);
      NewIssue issue = context.newIssue().forRule(ruleKey);
      NewIssueLocation location = issue
        .newLocation()
        .on(file)
        .message("Rename \"" + finding.name() + "\" to use snake_case");

      if (finding.line() > 0) {
        location.at(file.selectLine(finding.line()));
      }

      issue.at(location).save();
    }
  }

  List<Finding> detect(ElixirAst ast) {
    List<Finding> findings = new ArrayList<>();
    ast.walk(node -> {
      if (!DEFINITION_TYPES.contains(node.type())) {
        return;
      }
      List<ElixirAst> children = node.children();
      if (children.isEmpty()) {
        return;
      }

      ElixirAst head = children.get(0);
      if ("when".equals(head.type()) && !head.children().isEmpty()) {
        head = head.children().get(0);
      }
      String name = head.type();
      if (name == null || isDunderName(name)) {
        return;
      }
      if (!SNAKE_CASE.matcher(name).matches()) {
        findings.add(new Finding(name, node.line()));
      }
    });
    return findings;
  }

  /** Names surrounded by double underscores are Elixir internals (e.g. __using__). */
  private boolean isDunderName(String name) {
    return name.startsWith("__") && name.endsWith("__");
  }

  record Finding(String name, int line) {}
}
