package dev.hpopp.sonar.elixir.rules;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

/**
 * Module attribute names should be written in snake_case.
 *
 * Walks the AST looking for @ nodes and checks whether the declared attribute
 * name follows snake_case conventions.
 */
public class ModuleAttributeNameRule implements ElixirRule {

  private static final String KEY = "module_attribute_names";

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
        .message("Rename \"@" + finding.name() + "\" to use snake_case");

      if (finding.line() > 0) {
        location.at(file.selectLine(finding.line()));
      }

      issue.at(location).save();
    }
  }

  List<Finding> detect(ElixirAst ast) {
    List<Finding> findings = new ArrayList<>();
    ast.walk(node -> {
      if (!"@".equals(node.type())) {
        return;
      }
      List<ElixirAst> children = node.children();
      if (children.isEmpty()) {
        return;
      }

      String name = children.get(0).type();
      if (name == null) {
        return;
      }
      if (!SNAKE_CASE.matcher(name).matches()) {
        findings.add(new Finding(name, node.line()));
      }
    });
    return findings;
  }

  record Finding(String name, int line) {}
}
