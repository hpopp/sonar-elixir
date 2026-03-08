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
 * Module names should be written in PascalCase.
 *
 * Walks the AST looking for defmodule nodes and checks whether each segment
 * of the module name follows PascalCase conventions (no underscores, starts
 * with an uppercase letter).
 */
public class ModuleNameRule implements ElixirRule {

  private static final String KEY = "module_names";

  private static final Pattern PASCAL_CASE = Pattern.compile(
    "^[A-Z][a-zA-Z0-9]*$"
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
        .message("Rename \"" + finding.moduleName() + "\" to use PascalCase");

      if (finding.line() > 0) {
        location.at(file.selectLine(finding.line()));
      }

      issue.at(location).save();
    }
  }

  List<Finding> detect(ElixirAst ast) {
    List<Finding> findings = new ArrayList<>();
    for (ElixirAst defmodule : ast.findAll("defmodule")) {
      List<ElixirAst> children = defmodule.children();
      if (children.isEmpty()) {
        continue;
      }

      ElixirAst aliases = children.get(0);
      if (!"__aliases__".equals(aliases.type())) {
        continue;
      }

      List<String> parts = aliases
        .children()
        .stream()
        .filter(c -> c.value() != null)
        .map(ElixirAst::value)
        .toList();

      for (String part : parts) {
        if (!PASCAL_CASE.matcher(part).matches()) {
          String fullName = String.join(".", parts);
          findings.add(new Finding(fullName, defmodule.line()));
          break;
        }
      }
    }
    return findings;
  }

  record Finding(String moduleName, int line) {}
}
