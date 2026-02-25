package dev.hpopp.sonar.elixir.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import org.junit.jupiter.api.Test;

class MissingModuledocRuleTest {

    private final MissingModuledocRule rule = new MissingModuledocRule();

    @Test
    void ruleKey() {
        assertThat(rule.ruleKey()).isEqualTo("S001");
    }

    @Test
    void detectsMissingModuledoc() {
        ElixirAst ast = ElixirAst.parse(noModuledocJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).moduleName()).isEqualTo("Foo");
        assertThat(findings.get(0).line()).isEqualTo(1);
    }

    @Test
    void noFindingWhenModuledocPresent() {
        ElixirAst ast = ElixirAst.parse(withModuledocJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void noFindingWhenModuledocFalse() {
        ElixirAst ast = ElixirAst.parse(moduledocFalseJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void detectsNestedModuleMissingDoc() {
        ElixirAst ast = ElixirAst.parse(nestedModulesJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).moduleName()).isEqualTo("Foo.Bar");
    }

    @Test
    void skipsTestModules() {
        ElixirAst ast = ElixirAst.parse(testModuleJson());
        var findings = rule.detect(ast);

        assertThat(findings).isEmpty();
    }

    @Test
    void extractsDottedModuleName() {
        ElixirAst ast = ElixirAst.parse(dottedModuleJson());
        var findings = rule.detect(ast);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).moduleName()).isEqualTo("MyApp.Accounts.User");
    }

    // defmodule Foo do\n def bar, do: :ok\nend
    private static String noModuledocJson() {
        return """
                {"tuple":["defmodule",{"line":1,"column":1},\
                [{"tuple":["__aliases__",{"line":1,"column":11},["Foo"]]},\
                {"do":{"tuple":["def",{"line":2,"column":3},\
                [{"tuple":["bar",{"line":2,"column":7},null]},{"do":"ok"}]]}}]]}""";
    }

    // defmodule Foo do\n @moduledoc "Hello"\n def bar, do: :ok\nend
    private static String withModuledocJson() {
        return """
                {"tuple":["defmodule",{"line":1,"column":1},\
                [{"tuple":["__aliases__",{"line":1,"column":11},["Foo"]]},\
                {"do":{"tuple":["__block__",[],\
                [{"tuple":["@",{"line":2,"column":3},\
                [{"tuple":["moduledoc",{"line":2,"column":4},["Hello"]]}]]},\
                {"tuple":["def",{"line":3,"column":3},\
                [{"tuple":["bar",{"line":3,"column":7},null]},{"do":"ok"}]]}]]}}]]}""";
    }

    // defmodule Foo do\n @moduledoc false\n def bar, do: :ok\nend
    private static String moduledocFalseJson() {
        return """
                {"tuple":["defmodule",{"line":1,"column":1},\
                [{"tuple":["__aliases__",{"line":1,"column":11},["Foo"]]},\
                {"do":{"tuple":["__block__",[],\
                [{"tuple":["@",{"line":2,"column":3},\
                [{"tuple":["moduledoc",{"line":2,"column":4},[false]]}]]},\
                {"tuple":["def",{"line":3,"column":3},\
                [{"tuple":["bar",{"line":3,"column":7},null]},{"do":"ok"}]]}]]}}]]}""";
    }

    // defmodule Foo do
    // @moduledoc "Outer"
    // defmodule Bar do
    // def baz, do: :ok
    // end
    // end
    private static String nestedModulesJson() {
        return """
                {"tuple":["defmodule",{"line":1,"column":1},\
                [{"tuple":["__aliases__",{"line":1,"column":11},["Foo"]]},\
                {"do":{"tuple":["__block__",[],\
                [{"tuple":["@",{"line":2,"column":3},\
                [{"tuple":["moduledoc",{"line":2,"column":4},["Outer"]]}]]},\
                {"tuple":["defmodule",{"line":3,"column":3},\
                [{"tuple":["__aliases__",{"line":3,"column":13},["Foo","Bar"]]},\
                {"do":{"tuple":["def",{"line":4,"column":5},\
                [{"tuple":["baz",{"line":4,"column":9},null]},{"do":"ok"}]]}}]]}]]}}]]}""";
    }

    // defmodule MyApp.Accounts.User do\n def foo, do: :ok\nend
    private static String dottedModuleJson() {
        return """
                {"tuple":["defmodule",{"line":1,"column":1},\
                [{"tuple":["__aliases__",{"line":1,"column":11},["MyApp","Accounts","User"]]},\
                {"do":{"tuple":["def",{"line":2,"column":3},\
                [{"tuple":["foo",{"line":2,"column":7},null]},{"do":"ok"}]]}}]]}""";
    }

    // defmodule MyApp.FooTest do\n use ExUnit.Case\nend
    private static String testModuleJson() {
        return """
                {"tuple":["defmodule",{"line":1,"column":1},\
                [{"tuple":["__aliases__",{"line":1,"column":11},["MyApp","FooTest"]]},\
                {"do":{"tuple":["use",{"line":2,"column":3},\
                [{"tuple":["__aliases__",{"line":2,"column":7},["ExUnit","Case"]]}]]}}]]}""";
    }
}
