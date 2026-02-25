package dev.hpopp.sonar.elixir.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import org.junit.jupiter.api.Test;

class HardcodedSecretRuleTest {

    private final HardcodedSecretRule rule = new HardcodedSecretRule();

    @Test
    void ruleKey() {
        assertThat(rule.ruleKey()).isEqualTo("S201");
    }

    @Test
    void detectsHardcodedSecretInConfig() {
        // config :my_app, secret_key_base: "abc123"
        ElixirAst ast = ElixirAst.parse(configSecretJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).message()).contains("secret_key_base");
    }

    @Test
    void detectsHardcodedPasswordInRepoConfig() {
        // config :my_app, MyApp.Repo, password: "hunter2", username: "admin"
        ElixirAst ast = ElixirAst.parse(repoConfigJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).message()).contains("password");
    }

    @Test
    void allowsSystemGetEnv() {
        // config :my_app, secret_key_base: System.get_env("SECRET")
        ElixirAst ast = ElixirAst.parse(systemGetEnvJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void detectsModuleAttributeSecret() {
        // defmodule MyApp do
        //   @api_key "sk_live_abc"
        // end
        ElixirAst ast = ElixirAst.parse(moduleAttrSecretJson());
        var findings = rule.detect(ast);

        assertThat(findings)
                .anyMatch(f -> f.message().contains("api_key"))
                .anyMatch(f -> f.message().contains("Stripe secret key"));
    }

    @Test
    void allowsNonSensitiveKeys() {
        // config :my_app, username: "admin", port: "5432"
        ElixirAst ast = ElixirAst.parse(nonSensitiveConfigJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void allowsNonSensitiveModuleAttributes() {
        // @timeout 5000
        ElixirAst ast = ElixirAst.parse(nonSensitiveAttrJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void detectsMultipleSecrets() {
        // config :my_app, secret_key_base: "abc", password: "hunter2"
        ElixirAst ast = ElixirAst.parse(multipleSecretsJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void detectsStripePrefix() {
        // "sk_live_abc123def456" anywhere in source
        ElixirAst ast = ElixirAst.parse(stripePrefixJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).message()).contains("Stripe secret key");
    }

    @Test
    void detectsAwsPrefix() {
        // "AKIAIOSFODNN7EXAMPLE"
        ElixirAst ast = ElixirAst.parse(awsPrefixJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).message()).contains("AWS access key");
    }

    @Test
    void detectsGithubPrefix() {
        // "ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        ElixirAst ast = ElixirAst.parse(githubPrefixJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).message()).contains("GitHub personal access token");
    }

    @Test
    void detectsSlackPrefix() {
        // "xoxb-1234-5678-abcdef"
        ElixirAst ast = ElixirAst.parse(slackPrefixJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).message()).contains("Slack bot token");
    }

    @Test
    void detectsGitlabPrefix() {
        // "glpat-xxxxxxxxxxxxxxxxxxxx"
        ElixirAst ast = ElixirAst.parse(gitlabPrefixJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).message()).contains("GitLab personal/project/group access token");
    }

    @Test
    void allowsNormalStrings() {
        // "hello world"
        ElixirAst ast = ElixirAst.parse(normalStringJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    // config :my_app, secret_key_base: "abc123"
    private static String configSecretJson() {
        return """
                {"tuple":["config",{"line":1},\
                ["my_app",{"secret_key_base":"abc123"}]]}""";
    }

    // config :my_app, MyApp.Repo, password: "hunter2", username: "admin"
    private static String repoConfigJson() {
        return """
                {"tuple":["config",{"line":1},\
                ["my_app",\
                {"tuple":["__aliases__",{"line":1},["MyApp","Repo"]]},\
                {"password":"hunter2","username":"admin"}]]}""";
    }

    // config :my_app, secret_key_base: System.get_env("SECRET")
    private static String systemGetEnvJson() {
        return """
                {"tuple":["config",{"line":1},\
                ["my_app",{"secret_key_base":\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["System"]]},"get_env"]]},\
                {"line":1},["SECRET"]]}}]]}""";
    }

    // defmodule MyApp do @api_key "sk_live_abc" end
    private static String moduleAttrSecretJson() {
        return """
                {"tuple":["defmodule",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["MyApp"]]},\
                {"do":{"tuple":["@",{"line":2},\
                [{"tuple":["api_key",{"line":2},["sk_live_abc"]]}]]}}]]}""";
    }

    // config :my_app, username: "admin", port: "5432"
    private static String nonSensitiveConfigJson() {
        return """
                {"tuple":["config",{"line":1},\
                ["my_app",{"username":"admin","port":"5432"}]]}""";
    }

    // @timeout 5000
    private static String nonSensitiveAttrJson() {
        return """
                {"tuple":["@",{"line":1},\
                [{"tuple":["timeout",{"line":1},[5000]]}]]}""";
    }

    // config :my_app, secret_key_base: "abc", password: "hunter2"
    private static String multipleSecretsJson() {
        return """
                {"tuple":["config",{"line":1},\
                ["my_app",{"secret_key_base":"abc","password":"hunter2"}]]}""";
    }

    // @stripe_key "sk_live_abc123def456"
    private static String stripePrefixJson() {
        return """
                {"tuple":["@",{"line":1},\
                [{"tuple":["stripe_key",{"line":1},["sk_live_abc123def456"]]}]]}""";
    }

    // @aws_key "AKIAIOSFODNN7EXAMPLE"
    private static String awsPrefixJson() {
        return """
                {"tuple":["@",{"line":1},\
                [{"tuple":["aws_key",{"line":1},["AKIAIOSFODNN7EXAMPLE"]]}]]}""";
    }

    // @gh_token "ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
    private static String githubPrefixJson() {
        return """
                {"tuple":["@",{"line":1},\
                [{"tuple":["gh_token",{"line":1},\
                ["ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"]]}]]}""";
    }

    // @slack_token "xoxb-1234-5678-abcdef"
    private static String slackPrefixJson() {
        return """
                {"tuple":["@",{"line":1},\
                [{"tuple":["slack_token",{"line":1},["xoxb-1234-5678-abcdef"]]}]]}""";
    }

    // @gl_token "glpat-xxxxxxxxxxxxxxxxxxxx"
    private static String gitlabPrefixJson() {
        return """
                {"tuple":["@",{"line":1},\
                [{"tuple":["gl_token",{"line":1},\
                ["glpat-xxxxxxxxxxxxxxxxxxxx"]]}]]}""";
    }

    // "hello world"
    private static String normalStringJson() {
        return "\"hello world\"";
    }
}
