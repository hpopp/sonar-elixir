package dev.hpopp.sonar.elixir.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import org.junit.jupiter.api.Test;

class LargeModuleRuleTest {

    @Test
    void ruleKey() {
        assertThat(new LargeModuleRule().ruleKey()).isEqualTo("S002");
    }

    @Test
    void allowsSmallModule() {
        // Module spanning lines 1-5 (5 lines), threshold 10
        ElixirAst ast = ElixirAst.parse(moduleJson("Small", 1, 3));
        var findings = new LargeModuleRule(10).detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void detectsLargeModule() {
        // defmodule line 1, deepest child line 250, lineCount = 250 - 1 + 2 = 251
        ElixirAst ast = ElixirAst.parse(moduleJson("MyApp.KitchenSink", 1, 250));
        var findings = new LargeModuleRule(200).detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).moduleName()).isEqualTo("MyApp.KitchenSink");
        assertThat(findings.get(0).line()).isEqualTo(1);
        assertThat(findings.get(0).lineCount()).isEqualTo(251);
    }

    @Test
    void detectsModuleAtExactThreshold() {
        // defmodule line 1, deepest child line 10, lineCount = 10 - 1 + 2 = 11
        ElixirAst ast = ElixirAst.parse(moduleJson("Borderline", 1, 10));
        var findings = new LargeModuleRule(10).detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).lineCount()).isEqualTo(11);
    }

    @Test
    void allowsModuleUnderThreshold() {
        // lineCount = 11, threshold = 11
        ElixirAst ast = ElixirAst.parse(moduleJson("JustRight", 1, 10));
        var findings = new LargeModuleRule(11).detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void usesDefaultThreshold() {
        assertThat(LargeModuleRule.DEFAULT_MAX_LINES).isEqualTo(500);
    }

    @Test
    void detectsMultipleLargeModules() {
        // Two modules in a block, both large
        String json = """
                {"tuple":["__block__",[],[\
                %s,\
                %s\
                ]]}""".formatted(
                moduleJsonRaw("First", 1, 100),
                moduleJsonRaw("Second", 110, 220));
        ElixirAst ast = ElixirAst.parse(json);
        var findings = new LargeModuleRule(10).detect(ast);

        assertThat(findings).hasSize(2);
        assertThat(findings.get(0).moduleName()).isEqualTo("First");
        assertThat(findings.get(1).moduleName()).isEqualTo("Second");
    }

    /**
     * Build a defmodule JSON with a function body at the given last line.
     * The module name can be dotted (e.g. "MyApp.Users").
     */
    private static String moduleJson(String name, int startLine, int lastLine) {
        return moduleJsonRaw(name, startLine, lastLine);
    }

    private static String moduleJsonRaw(String name, int startLine, int lastLine) {
        String aliasesJson = aliasesJson(name, startLine);
        return """
                {"tuple":["defmodule",{"line":%d},[\
                %s,\
                {"do":{"tuple":["def",{"line":%d},\
                [{"tuple":["some_func",{"line":%d},null]},\
                {"do":"ok"}]]}}\
                ]]}""".formatted(startLine, aliasesJson, lastLine, lastLine);
    }

    private static String aliasesJson(String name, int line) {
        String[] parts = name.split("\\.");
        StringBuilder sb = new StringBuilder();
        sb.append("{\"tuple\":[\"__aliases__\",{\"line\":").append(line).append("},[");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(parts[i]).append("\"");
        }
        sb.append("]]}");
        return sb.toString();
    }
}
