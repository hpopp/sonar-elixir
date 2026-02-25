package dev.hpopp.sonar.elixir.rules;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

/**
 * S007: Pipe chains should start with a raw value.
 *
 * Flags pipe chains whose leftmost expression is a function call rather than
 * a variable, literal, or data structure. Starting pipes with a raw value
 * makes the data flow easier to follow.
 */
public class PipeChainStartRule implements ElixirRule {

    private static final String KEY = "S003";

    private static final Set<String> EXCLUDED_TYPES = Set.of(
            "@", "for", "with", "if", "unless", "case", "cond",
            "fn", "receive", "try", "quote", "unquote", "raise",
            "reraise", "throw", "super", "import", "require", "alias", "use",
            "list", "literal", "nil", "keyword_list", "keyword_pair", "unknown");

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
                    .message("Pipe chain should start with a raw value");

            if (finding.line() > 0) {
                location.at(file.selectLine(finding.line()));
            }

            issue.at(location).save();
        }
    }

    List<Finding> detect(ElixirAst ast) {
        List<Finding> findings = new ArrayList<>();
        for (ElixirAst pipe : ast.findAll("|>")) {
            ElixirAst left = pipeChainStart(pipe);
            if (left != null && isFunctionCall(left)) {
                int line = left.line() > 0 ? left.line() : pipe.line();
                findings.add(new Finding(line));
            }
        }
        return findings;
    }

    /**
     * Returns the leftmost expression of a pipe chain, or null if this pipe
     * node is not the outermost pipe (its left child is another pipe).
     */
    private ElixirAst pipeChainStart(ElixirAst pipe) {
        List<ElixirAst> children = pipe.children();
        if (children.size() < 2) {
            return null;
        }
        ElixirAst left = children.get(0);
        if ("|>".equals(left.type())) {
            return null;
        }
        return left;
    }

    private boolean isFunctionCall(ElixirAst node) {
        if ("nested_call".equals(node.type())) {
            return true;
        }

        String type = node.type();
        if (node.children().isEmpty()) {
            return false;
        }
        if (EXCLUDED_TYPES.contains(type)) {
            return false;
        }
        if (type.startsWith("__") || type.startsWith("sigil_")) {
            return false;
        }

        return type.matches("[a-z_][a-zA-Z0-9_!?]*");
    }

    record Finding(int line) {
    }
}
