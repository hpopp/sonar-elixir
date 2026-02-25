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
 * S009: Calls to IO.inspect/1 should be removed before committing.
 *
 * IO.inspect is a debugging tool that should not remain in production code.
 * Walks the AST looking for remote calls to IO.inspect.
 */
public class IoInspectRule implements ElixirRule {

    private static final String KEY = "S004";

    @Override
    public String ruleKey() {
        return KEY;
    }

    @Override
    public void analyze(SensorContext context, InputFile file, ElixirAst ast) {
        for (int line : detect(ast)) {
            RuleKey ruleKey = RuleKey.of(ElixirRulesDefinition.REPOSITORY_KEY, KEY);
            NewIssue issue = context.newIssue().forRule(ruleKey);
            NewIssueLocation location = issue.newLocation()
                    .on(file)
                    .message("Remove this IO.inspect call");

            if (line > 0) {
                location.at(file.selectLine(line));
            }

            issue.at(location).save();
        }
    }

    List<Integer> detect(ElixirAst ast) {
        List<Integer> lines = new ArrayList<>();
        ast.walk(node -> {
            if (!"nested_call".equals(node.type())) {
                return;
            }
            List<ElixirAst> children = node.children();
            if (children.isEmpty()) {
                return;
            }
            ElixirAst dot = children.get(0);
            if (!".".equals(dot.type())) {
                return;
            }
            List<ElixirAst> dotChildren = dot.children();
            if (dotChildren.size() < 2) {
                return;
            }
            if (isIoAlias(dotChildren.get(0)) && "inspect".equals(dotChildren.get(1).value())) {
                lines.add(node.line());
            }
        });
        return lines;
    }

    private boolean isIoAlias(ElixirAst node) {
        if (!"__aliases__".equals(node.type())) {
            return false;
        }
        List<ElixirAst> parts = node.children();
        return parts.size() == 1
                && "IO".equals(parts.get(0).value());
    }
}
