package dev.hpopp.sonar.elixir.rules;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

/**
 * S006: Modules should not have too many lines.
 *
 * Finds defmodule nodes in the AST, computes line span from the defmodule
 * declaration to the deepest descendant, and reports when it exceeds the
 * threshold.
 */
public class LargeModuleRule implements ElixirRule {

    private static final String KEY = "S002";
    static final int DEFAULT_MAX_LINES = 500;

    private final int maxLines;

    public LargeModuleRule() {
        this(DEFAULT_MAX_LINES);
    }

    public LargeModuleRule(int maxLines) {
        this.maxLines = maxLines;
    }

    @Override
    public String ruleKey() {
        return KEY;
    }

    @Override
    public void analyze(SensorContext context, InputFile file, ElixirAst ast) {
        for (Finding finding : detect(ast)) {
            RuleKey ruleKey = RuleKey.of(ElixirRulesDefinition.REPOSITORY_KEY, KEY);
            NewIssue issue = context.newIssue().forRule(ruleKey);
            NewIssueLocation location = issue.newLocation()
                    .on(file)
                    .message(finding.moduleName() + " has " + finding.lineCount()
                            + " lines (max " + maxLines + ")");

            if (finding.line() > 0) {
                location.at(file.selectLine(finding.line()));
            }

            issue.at(location).save();
        }
    }

    List<Finding> detect(ElixirAst ast) {
        List<Finding> findings = new ArrayList<>();
        for (ElixirAst defmodule : ast.findAll("defmodule")) {
            int startLine = defmodule.line();
            int endLine = maxLine(defmodule);
            if (startLine <= 0 || endLine <= 0) {
                continue;
            }
            int lineCount = endLine - startLine + 2;
            if (lineCount > maxLines) {
                String name = extractModuleName(defmodule);
                findings.add(new Finding(name, startLine, lineCount));
            }
        }
        return findings;
    }

    private int maxLine(ElixirAst node) {
        int max = node.line();
        for (ElixirAst child : node.children()) {
            max = Math.max(max, maxLine(child));
        }
        return max;
    }

    private String extractModuleName(ElixirAst defmodule) {
        List<ElixirAst> children = defmodule.children();
        if (children.isEmpty()) {
            return "Module";
        }

        ElixirAst aliases = children.get(0);
        if ("__aliases__".equals(aliases.type())) {
            List<String> parts = aliases.children().stream()
                    .filter(c -> c.value() != null)
                    .map(ElixirAst::value)
                    .toList();
            if (!parts.isEmpty()) {
                return String.join(".", parts);
            }
        }
        return "Module";
    }

    record Finding(String moduleName, int line, int lineCount) {
    }
}
