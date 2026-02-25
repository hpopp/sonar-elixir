package dev.hpopp.sonar.elixir.rules;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

/**
 * S201: Credentials should not be hardcoded.
 *
 * Two detection strategies:
 * 1. Key-name matching — flags sensitive keys (password, secret, token, etc.)
 * assigned to string literals in config keyword lists and module attributes.
 * 2. Prefix matching — flags any string literal whose value starts with a known
 * credential prefix (Stripe, AWS, GitHub, Slack, SendGrid).
 */
public class HardcodedSecretRule implements ElixirRule {

    private static final String KEY = "S201";

    static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwd", "pass",
            "secret", "secret_key", "secret_key_base",
            "token", "auth_token", "access_token",
            "api_key", "api_secret", "apikey",
            "private_key", "signing_key", "encryption_key",
            "credentials", "credential");

    static final Map<String, String> SECRET_PREFIXES = Map.ofEntries(
            Map.entry("sk_live_", "Stripe secret key"),
            Map.entry("sk_test_", "Stripe test secret key"),
            Map.entry("pk_live_", "Stripe publishable key"),
            Map.entry("pk_test_", "Stripe test publishable key"),
            Map.entry("rk_live_", "Stripe restricted key"),
            Map.entry("rk_test_", "Stripe test restricted key"),
            Map.entry("AKIA", "AWS access key"),
            Map.entry("ASIA", "AWS temporary access key"),
            Map.entry("ghp_", "GitHub personal access token"),
            Map.entry("gho_", "GitHub OAuth access token"),
            Map.entry("ghs_", "GitHub server-to-server token"),
            Map.entry("ghr_", "GitHub refresh token"),
            Map.entry("github_pat_", "GitHub fine-grained PAT"),
            Map.entry("glpat-", "GitLab personal/project/group access token"),
            Map.entry("gloas-", "GitLab OAuth application secret"),
            Map.entry("gldt-", "GitLab deploy token"),
            Map.entry("glrt-", "GitLab runner authentication token"),
            Map.entry("glcbt-", "GitLab CI/CD job token"),
            Map.entry("glptt-", "GitLab trigger token"),
            Map.entry("glagent-", "GitLab agent token"),
            Map.entry("xoxb-", "Slack bot token"),
            Map.entry("xoxp-", "Slack user token"),
            Map.entry("xoxs-", "Slack session token"),
            Map.entry("SG.", "SendGrid API key"));

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
                    .message(finding.message());

            if (finding.line > 0) {
                location.at(file.selectLine(finding.line));
            }

            issue.at(location).save();
        }
    }

    List<Finding> detect(ElixirAst ast) {
        List<Finding> findings = new ArrayList<>();
        ast.walk(node -> {
            if ("keyword_pair".equals(node.type())) {
                checkKeywordPair(node, findings);
            } else if ("@".equals(node.type())) {
                checkModuleAttribute(node, findings);
            } else if ("literal".equals(node.type())) {
                checkSecretPrefix(node, findings);
            }
        });
        return findings;
    }

    private void checkKeywordPair(ElixirAst node, List<Finding> findings) {
        String keyName = node.value();
        if (keyName == null || !SENSITIVE_KEYS.contains(keyName)) {
            return;
        }
        if (node.children().isEmpty()) {
            return;
        }
        ElixirAst value = node.children().get(0);
        if (isStringLiteral(value)) {
            int line = value.line() > 0 ? value.line() : node.line();
            findings.add(new Finding("\"" + keyName + "\" contains a hardcoded credential", line));
        }
    }

    private void checkModuleAttribute(ElixirAst node, List<Finding> findings) {
        if (node.children().isEmpty()) {
            return;
        }
        ElixirAst attr = node.children().get(0);
        String attrName = attr.type();
        if (!SENSITIVE_KEYS.contains(attrName)) {
            return;
        }
        if (attr.children().isEmpty()) {
            return;
        }
        ElixirAst value = attr.children().get(0);
        if (isStringLiteral(value)) {
            int line = node.line() > 0 ? node.line() : attr.line();
            findings.add(new Finding("\"" + attrName + "\" contains a hardcoded credential", line));
        }
    }

    private void checkSecretPrefix(ElixirAst node, List<Finding> findings) {
        String val = node.value();
        if (val == null || val.isEmpty()) {
            return;
        }
        for (var entry : SECRET_PREFIXES.entrySet()) {
            if (val.startsWith(entry.getKey())) {
                findings.add(new Finding(
                        "String looks like a " + entry.getValue() + " (prefix: " + entry.getKey() + ")",
                        node.line()));
                return;
            }
        }
    }

    private boolean isStringLiteral(ElixirAst node) {
        return "literal".equals(node.type()) && node.value() != null && !node.value().isEmpty();
    }

    record Finding(String message, int line) {
    }
}
