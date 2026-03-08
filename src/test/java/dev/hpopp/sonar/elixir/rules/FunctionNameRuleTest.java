package dev.hpopp.sonar.elixir.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import org.junit.jupiter.api.Test;

class FunctionNameRuleTest {

    private final FunctionNameRule rule = new FunctionNameRule();

    @Test
    void ruleKey() {
        assertThat(rule.ruleKey()).isEqualTo("function_names");
    }

    @Test
    void detectsCamelCaseFunctionName() {
        // def handleIncomingMessage(msg), do: :ok
        ElixirAst ast = ElixirAst.parse(camelCaseDefJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).name()).isEqualTo("handleIncomingMessage");
        assertThat(findings.get(0).line()).isEqualTo(1);
    }

    @Test
    void allowsSnakeCaseFunctionName() {
        // def handle_incoming_message(msg), do: :ok
        ElixirAst ast = ElixirAst.parse(snakeCaseDefJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void allowsPredicateFunction() {
        // def valid?(value), do: true
        ElixirAst ast = ElixirAst.parse(predicateDefJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void allowsBangFunction() {
        // def save!(record), do: record
        ElixirAst ast = ElixirAst.parse(bangDefJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void allowsDunderName() {
        // def __using__(opts), do: opts
        ElixirAst ast = ElixirAst.parse(dunderDefJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void detectsCamelCaseInDefp() {
        // defp processData(data), do: data
        ElixirAst ast = ElixirAst.parse(camelCaseDefpJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).name()).isEqualTo("processData");
    }

    @Test
    void detectsCamelCaseInDefmacro() {
        // defmacro myMacro(arg), do: arg
        ElixirAst ast = ElixirAst.parse(camelCaseDefmacroJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).name()).isEqualTo("myMacro");
    }

    @Test
    void detectsCamelCaseInDefguard() {
        // defguard isPositive(x) when x > 0
        ElixirAst ast = ElixirAst.parse(camelCaseDefguardJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).name()).isEqualTo("isPositive");
    }

    @Test
    void detectsMultipleViolations() {
        ElixirAst ast = ElixirAst.parse(multipleViolationsJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(2);
    }

    @Test
    void allowsUnderscoreOnlyName() {
        // def _private_helper(x), do: x
        ElixirAst ast = ElixirAst.parse(underscorePrefixJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    // def handleIncomingMessage(msg), do: :ok
    private static String camelCaseDefJson() {
        return """
                {"tuple":["def",{"line":1,"column":1},\
                [{"tuple":["handleIncomingMessage",{"line":1,"column":5},\
                [{"tuple":["msg",{"line":1,"column":30},null]}]]},\
                {"do":"ok"}]]}""";
    }

    // def handle_incoming_message(msg), do: :ok
    private static String snakeCaseDefJson() {
        return """
                {"tuple":["def",{"line":1,"column":1},\
                [{"tuple":["handle_incoming_message",{"line":1,"column":5},\
                [{"tuple":["msg",{"line":1,"column":30},null]}]]},\
                {"do":"ok"}]]}""";
    }

    // def valid?(value), do: true
    private static String predicateDefJson() {
        return """
                {"tuple":["def",{"line":1,"column":1},\
                [{"tuple":["valid?",{"line":1,"column":5},\
                [{"tuple":["value",{"line":1,"column":12},null]}]]},\
                {"do":true}]]}""";
    }

    // def save!(record), do: record
    private static String bangDefJson() {
        return """
                {"tuple":["def",{"line":1,"column":1},\
                [{"tuple":["save!",{"line":1,"column":5},\
                [{"tuple":["record",{"line":1,"column":11},null]}]]},\
                {"do":{"tuple":["record",{"line":1,"column":25},null]}}]]}""";
    }

    // def __using__(opts), do: opts
    private static String dunderDefJson() {
        return """
                {"tuple":["def",{"line":1,"column":1},\
                [{"tuple":["__using__",{"line":1,"column":5},\
                [{"tuple":["opts",{"line":1,"column":15},null]}]]},\
                {"do":{"tuple":["opts",{"line":1,"column":25},null]}}]]}""";
    }

    // defp processData(data), do: data
    private static String camelCaseDefpJson() {
        return """
                {"tuple":["defp",{"line":2,"column":1},\
                [{"tuple":["processData",{"line":2,"column":6},\
                [{"tuple":["data",{"line":2,"column":18},null]}]]},\
                {"do":{"tuple":["data",{"line":2,"column":28},null]}}]]}""";
    }

    // defmacro myMacro(arg), do: arg
    private static String camelCaseDefmacroJson() {
        return """
                {"tuple":["defmacro",{"line":3,"column":1},\
                [{"tuple":["myMacro",{"line":3,"column":10},\
                [{"tuple":["arg",{"line":3,"column":18},null]}]]},\
                {"do":{"tuple":["arg",{"line":3,"column":28},null]}}]]}""";
    }

    // defguard isPositive(x) when x > 0
    private static String camelCaseDefguardJson() {
        return """
                {"tuple":["defguard",{"line":4,"column":1},\
                [{"tuple":["when",{"line":4,"column":10},\
                [{"tuple":["isPositive",{"line":4,"column":10},\
                [{"tuple":["x",{"line":4,"column":21},null]}]]},\
                {"tuple":[">",{"line":4,"column":30},\
                [{"tuple":["x",{"line":4,"column":28},null]},0]]}]]}]]}""";
    }

    // def handleFoo(x), do: x \n def processBar(y), do: y
    private static String multipleViolationsJson() {
        return """
                {"tuple":["__block__",[],[\
                {"tuple":["def",{"line":1,"column":1},\
                [{"tuple":["handleFoo",{"line":1,"column":5},\
                [{"tuple":["x",{"line":1,"column":15},null]}]]},\
                {"do":{"tuple":["x",{"line":1,"column":23},null]}}]]},\
                {"tuple":["def",{"line":2,"column":1},\
                [{"tuple":["processBar",{"line":2,"column":5},\
                [{"tuple":["y",{"line":2,"column":16},null]}]]},\
                {"do":{"tuple":["y",{"line":2,"column":24},null]}}]]}\
                ]]}""";
    }

    // def _private_helper(x), do: x
    private static String underscorePrefixJson() {
        return """
                {"tuple":["def",{"line":1,"column":1},\
                [{"tuple":["_private_helper",{"line":1,"column":5},\
                [{"tuple":["x",{"line":1,"column":21},null]}]]},\
                {"do":{"tuple":["x",{"line":1,"column":29},null]}}]]}""";
    }
}
