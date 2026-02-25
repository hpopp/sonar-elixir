package dev.hpopp.sonar.elixir.sensors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ElixirAstTest {

    // JSON from: defmodule Foo do\n  def bar, do: :ok\nend
    private static final String NO_MODULEDOC_JSON =
        """
        {"tuple":["defmodule",{"line":1,"column":1},\
        [{"tuple":["__aliases__",{"line":1,"column":11},["Foo"]]},\
        {"do":{"tuple":["def",{"line":2,"column":3},\
        [{"tuple":["bar",{"line":2,"column":7},null]},{"do":"ok"}]]}}]]}""";

    // JSON from: defmodule Foo do\n  @moduledoc "Hello"\n  def bar, do: :ok\nend
    private static final String WITH_MODULEDOC_JSON =
        """
        {"tuple":["defmodule",{"line":1,"column":1},\
        [{"tuple":["__aliases__",{"line":1,"column":11},["Foo"]]},\
        {"do":{"tuple":["__block__",[],\
        [{"tuple":["@",{"line":2,"column":3},\
        [{"tuple":["moduledoc",{"line":2,"column":4},["Hello"]]}]]},\
        {"tuple":["def",{"line":3,"column":3},\
        [{"tuple":["bar",{"line":3,"column":7},null]},{"do":"ok"}]]}]]}}]]}""";

    @Test
    void parsesDefmoduleType() {
        ElixirAst ast = ElixirAst.parse(NO_MODULEDOC_JSON);
        assertThat(ast.type()).isEqualTo("defmodule");
    }

    @Test
    void parsesLineAndColumn() {
        ElixirAst ast = ElixirAst.parse(NO_MODULEDOC_JSON);
        assertThat(ast.line()).isEqualTo(1);
        assertThat(ast.column()).isEqualTo(1);
    }

    @Test
    void parsesModuleAlias() {
        ElixirAst ast = ElixirAst.parse(NO_MODULEDOC_JSON);
        ElixirAst aliases = ast.children().get(0);
        assertThat(aliases.type()).isEqualTo("__aliases__");
        assertThat(aliases.children()).hasSize(1);
        assertThat(aliases.children().get(0).value()).isEqualTo("Foo");
    }

    @Test
    void parsesDoBlockAsKeywordList() {
        ElixirAst ast = ElixirAst.parse(NO_MODULEDOC_JSON);
        ElixirAst kwList = ast.children().get(1);
        assertThat(kwList.type()).isEqualTo("keyword_list");

        var doBody = kwList.keyword("do");
        assertThat(doBody).isPresent();
        assertThat(doBody.get().type()).isEqualTo("def");
    }

    @Test
    void findsDefmoduleNodes() {
        ElixirAst ast = ElixirAst.parse(WITH_MODULEDOC_JSON);
        var modules = ast.findAll("defmodule");
        assertThat(modules).hasSize(1);
    }

    @Test
    void findsModuledocInBlock() {
        ElixirAst ast = ElixirAst.parse(WITH_MODULEDOC_JSON);
        var atNodes = ast.findAll("@");
        assertThat(atNodes).hasSize(1);

        ElixirAst attr = atNodes.get(0).children().get(0);
        assertThat(attr.type()).isEqualTo("moduledoc");
    }

    @Test
    void blockChildrenReturnsListForBlock() {
        ElixirAst ast = ElixirAst.parse(WITH_MODULEDOC_JSON);
        ElixirAst doBody = ast.children().get(1).keyword("do").orElseThrow();
        assertThat(doBody.type()).isEqualTo("__block__");
        assertThat(doBody.blockChildren()).hasSize(2);
    }

    @Test
    void blockChildrenReturnsSingletonForNonBlock() {
        ElixirAst ast = ElixirAst.parse(NO_MODULEDOC_JSON);
        ElixirAst doBody = ast.children().get(1).keyword("do").orElseThrow();
        assertThat(doBody.type()).isEqualTo("def");
        assertThat(doBody.blockChildren()).hasSize(1);
        assertThat(doBody.blockChildren().get(0)).isSameAs(doBody);
    }

    @Test
    void parsesLiteralValues() {
        ElixirAst ast = ElixirAst.parse("42");
        assertThat(ast.type()).isEqualTo("literal");
        assertThat(ast.value()).isEqualTo("42");
    }

    @Test
    void parsesNullAsNil() {
        ElixirAst ast = ElixirAst.parse("null");
        assertThat(ast.type()).isEqualTo("nil");
    }
}
