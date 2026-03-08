package dev.hpopp.sonar.elixir.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import org.junit.jupiter.api.Test;

class ModuleAttributeNameRuleTest {

    private final ModuleAttributeNameRule rule = new ModuleAttributeNameRule();

    @Test
    void ruleKey() {
        assertThat(rule.ruleKey()).isEqualTo("module_attribute_names");
    }

    @Test
    void detectsCamelCaseAttribute() {
        // @inboxName "incoming"
        ElixirAst ast = ElixirAst.parse(camelCaseAttrJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).name()).isEqualTo("inboxName");
        assertThat(findings.get(0).line()).isEqualTo(2);
    }

    @Test
    void allowsSnakeCaseAttribute() {
        // @inbox_name "incoming"
        ElixirAst ast = ElixirAst.parse(snakeCaseAttrJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void allowsModuledoc() {
        // @moduledoc "Hello"
        ElixirAst ast = ElixirAst.parse(moduledocJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void allowsDoc() {
        // @doc "Does things"
        ElixirAst ast = ElixirAst.parse(docJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void detectsMultipleViolations() {
        ElixirAst ast = ElixirAst.parse(multipleViolationsJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(2);
    }

    @Test
    void allowsSingleWordAttribute() {
        // @timeout 5000
        ElixirAst ast = ElixirAst.parse(singleWordAttrJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void detectsPascalCaseAttribute() {
        // @MaxRetries 3
        ElixirAst ast = ElixirAst.parse(pascalCaseAttrJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).name()).isEqualTo("MaxRetries");
    }

    // @inboxName "incoming"
    private static String camelCaseAttrJson() {
        return """
                {"tuple":["@",{"line":2,"column":3},\
                [{"tuple":["inboxName",{"line":2,"column":4},["incoming"]]}]]}""";
    }

    // @inbox_name "incoming"
    private static String snakeCaseAttrJson() {
        return """
                {"tuple":["@",{"line":2,"column":3},\
                [{"tuple":["inbox_name",{"line":2,"column":4},["incoming"]]}]]}""";
    }

    // @moduledoc "Hello"
    private static String moduledocJson() {
        return """
                {"tuple":["@",{"line":2,"column":3},\
                [{"tuple":["moduledoc",{"line":2,"column":4},["Hello"]]}]]}""";
    }

    // @doc "Does things"
    private static String docJson() {
        return """
                {"tuple":["@",{"line":2,"column":3},\
                [{"tuple":["doc",{"line":2,"column":4},["Does things"]]}]]}""";
    }

    // @inboxName "incoming" \n @maxRetries 3
    private static String multipleViolationsJson() {
        return """
                {"tuple":["__block__",[],[\
                {"tuple":["@",{"line":1,"column":3},\
                [{"tuple":["inboxName",{"line":1,"column":4},["incoming"]]}]]},\
                {"tuple":["@",{"line":2,"column":3},\
                [{"tuple":["maxRetries",{"line":2,"column":4},[3]]}]]}\
                ]]}""";
    }

    // @timeout 5000
    private static String singleWordAttrJson() {
        return """
                {"tuple":["@",{"line":1,"column":3},\
                [{"tuple":["timeout",{"line":1,"column":4},[5000]]}]]}""";
    }

    // @MaxRetries 3
    private static String pascalCaseAttrJson() {
        return """
                {"tuple":["@",{"line":1,"column":3},\
                [{"tuple":["MaxRetries",{"line":1,"column":4},[3]]}]]}""";
    }
}
