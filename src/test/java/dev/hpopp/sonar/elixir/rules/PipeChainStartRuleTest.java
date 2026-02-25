package dev.hpopp.sonar.elixir.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import org.junit.jupiter.api.Test;

class PipeChainStartRuleTest {

    private final PipeChainStartRule rule = new PipeChainStartRule();

    @Test
    void ruleKey() {
        assertThat(rule.ruleKey()).isEqualTo("S003");
    }

    @Test
    void detectsRemoteCallStart() {
        // String.trim(name) |> String.upcase()
        ElixirAst ast = ElixirAst.parse(remoteCallStartJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).line()).isEqualTo(1);
    }

    @Test
    void detectsLocalCallStart() {
        // foo(bar) |> baz()
        ElixirAst ast = ElixirAst.parse(localCallStartJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).line()).isEqualTo(1);
    }

    @Test
    void allowsVariableStart() {
        // name |> String.trim() |> String.upcase()
        ElixirAst ast = ElixirAst.parse(variableStartJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void allowsModuleAttributeStart() {
        // @name |> String.trim()
        ElixirAst ast = ElixirAst.parse(attrStartJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void allowsLiteralStart() {
        // "hello" |> String.upcase()
        ElixirAst ast = ElixirAst.parse(literalStartJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void allowsListStart() {
        // [a, b, c] |> Enum.map(&to_string/1)
        ElixirAst ast = ElixirAst.parse(listStartJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void nestedPipeReportsOnce() {
        // String.trim(name) |> String.downcase() |> String.upcase()
        ElixirAst ast = ElixirAst.parse(nestedPipeRemoteStartJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
    }

    // String.trim(name) |> String.upcase()
    private static String remoteCallStartJson() {
        return """
                {"tuple":["|>",{"line":1},[\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["String"]]},"trim"]]},\
                {"line":1},[{"tuple":["name",{"line":1},null]}]]},\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["String"]]},"upcase"]]},\
                {"line":1},[]]}\
                ]]}""";
    }

    // foo(bar) |> baz()
    private static String localCallStartJson() {
        return """
                {"tuple":["|>",{"line":1},[\
                {"tuple":["foo",{"line":1},[{"tuple":["bar",{"line":1},null]}]]},\
                {"tuple":["baz",{"line":1},[]]}\
                ]]}""";
    }

    // name |> String.trim() |> String.upcase()
    private static String variableStartJson() {
        return """
                {"tuple":["|>",{"line":1},[\
                {"tuple":["|>",{"line":1},[\
                {"tuple":["name",{"line":1},null]},\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["String"]]},"trim"]]},\
                {"line":1},[]]}\
                ]]},\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["String"]]},"upcase"]]},\
                {"line":1},[]]}\
                ]]}""";
    }

    // @name |> String.trim()
    private static String attrStartJson() {
        return """
                {"tuple":["|>",{"line":1},[\
                {"tuple":["@",{"line":1},[{"tuple":["name",{"line":1},null]}]]},\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["String"]]},"trim"]]},\
                {"line":1},[]]}\
                ]]}""";
    }

    // "hello" |> String.upcase()
    private static String literalStartJson() {
        return """
                {"tuple":["|>",{"line":1},[\
                "hello",\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["String"]]},"upcase"]]},\
                {"line":1},[]]}\
                ]]}""";
    }

    // [a, b, c] |> Enum.map(&to_string/1)
    private static String listStartJson() {
        return """
                {"tuple":["|>",{"line":1},[\
                [{"tuple":["a",{"line":1},null]},\
                {"tuple":["b",{"line":1},null]},\
                {"tuple":["c",{"line":1},null]}],\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["Enum"]]},"map"]]},\
                {"line":1},[]]}\
                ]]}""";
    }

    // String.trim(name) |> String.downcase() |> String.upcase()
    private static String nestedPipeRemoteStartJson() {
        return """
                {"tuple":["|>",{"line":1},[\
                {"tuple":["|>",{"line":1},[\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["String"]]},"trim"]]},\
                {"line":1},[{"tuple":["name",{"line":1},null]}]]},\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["String"]]},"downcase"]]},\
                {"line":1},[]]}\
                ]]},\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["String"]]},"upcase"]]},\
                {"line":1},[]]}\
                ]]}""";
    }
}
