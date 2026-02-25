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
 * S005: Modules should have a @moduledoc attribute.
 *
 * Walks the AST looking for defmodule nodes and checks whether the module body
 * contains a @moduledoc. Modules that set @moduledoc false are considered
 * compliant (intentional suppression).
 */
public class MissingModuledocRule implements ElixirRule {

    private static final String KEY = "S005";

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
                    .message("Add @moduledoc to " + finding.moduleName());

            if (finding.line() > 0) {
                location.at(file.selectLine(finding.line()));
            }

            issue.at(location).save();
        }
    }

    /** Detect modules missing @moduledoc, returning findings with location info. */
    List<Finding> detect(ElixirAst ast) {
        List<Finding> findings = new ArrayList<>();
        for (ElixirAst defmodule : ast.findAll("defmodule")) {
            String moduleName = extractModuleName(defmodule);
            if (moduleName.endsWith("Test")) {
                continue;
            }
            if (!hasModuledoc(defmodule)) {
                findings.add(new Finding(moduleName, defmodule.line()));
            }
        }
        return findings;
    }

    private boolean hasModuledoc(ElixirAst defmodule) {
        ElixirAst body = getModuleBody(defmodule);
        if (body == null) {
            return false;
        }

        for (ElixirAst expr : body.blockChildren()) {
            if ("@".equals(expr.type())) {
                for (ElixirAst attr : expr.children()) {
                    if ("moduledoc".equals(attr.type())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Navigate defmodule -> children[1] (keyword list) -> "do" keyword -> body.
     */
    private ElixirAst getModuleBody(ElixirAst defmodule) {
        List<ElixirAst> children = defmodule.children();
        if (children.size() < 2) {
            return null;
        }

        ElixirAst kwList = children.get(1);
        return kwList.keyword("do").orElse(null);
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

    record Finding(String moduleName, int line) {
    }
}
