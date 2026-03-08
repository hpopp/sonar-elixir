package dev.hpopp.sonar.elixir.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import org.junit.jupiter.api.Test;

class ModuleNameRuleTest {

  private final ModuleNameRule rule = new ModuleNameRule();

  @Test
  void ruleKey() {
    assertThat(rule.ruleKey()).isEqualTo("module_names");
  }

  @Test
  void detectsUnderscoreInModuleName() {
    // defmodule MyApp.Web_searchController do ... end
    ElixirAst ast = ElixirAst.parse(underscoreModuleJson());
    var findings = rule.detect(ast);

    assertThat(findings).hasSize(1);
    assertThat(findings.get(0).moduleName()).isEqualTo(
      "MyApp.Web_searchController"
    );
    assertThat(findings.get(0).line()).isEqualTo(1);
  }

  @Test
  void allowsPascalCaseModule() {
    // defmodule MyApp.WebSearchController do ... end
    ElixirAst ast = ElixirAst.parse(pascalCaseModuleJson());
    var findings = rule.detect(ast);

    assertThat(findings).isEmpty();
  }

  @Test
  void allowsSingleWordModule() {
    // defmodule Foo do ... end
    ElixirAst ast = ElixirAst.parse(singleWordModuleJson());
    var findings = rule.detect(ast);

    assertThat(findings).isEmpty();
  }

  @Test
  void detectsLowercaseSegment() {
    // defmodule MyApp.foo do ... end
    ElixirAst ast = ElixirAst.parse(lowercaseSegmentJson());
    var findings = rule.detect(ast);

    assertThat(findings).hasSize(1);
    assertThat(findings.get(0).moduleName()).isEqualTo("MyApp.foo");
  }

  @Test
  void allowsDottedPascalCase() {
    // defmodule MyApp.Accounts.User do ... end
    ElixirAst ast = ElixirAst.parse(dottedPascalCaseJson());
    var findings = rule.detect(ast);

    assertThat(findings).isEmpty();
  }

  @Test
  void reportsOncePerModule() {
    // defmodule My_App.Web_search do ... end
    ElixirAst ast = ElixirAst.parse(multipleBadSegmentsJson());
    var findings = rule.detect(ast);

    assertThat(findings).hasSize(1);
    assertThat(findings.get(0).moduleName()).isEqualTo("My_App.Web_search");
  }

  @Test
  void allowsAcronymModule() {
    // defmodule MyApp.HTTP do ... end
    ElixirAst ast = ElixirAst.parse(acronymModuleJson());
    var findings = rule.detect(ast);

    assertThat(findings).isEmpty();
  }

  // defmodule MyApp.Web_searchController do def foo, do: :ok end
  private static String underscoreModuleJson() {
    return """
    {"tuple":["defmodule",{"line":1,"column":1},\
    [{"tuple":["__aliases__",{"line":1,"column":11},\
    ["MyApp","Web_searchController"]]},\
    {"do":{"tuple":["def",{"line":2,"column":3},\
    [{"tuple":["foo",{"line":2,"column":7},null]},{"do":"ok"}]]}}]]}""";
  }

  // defmodule MyApp.WebSearchController do def foo, do: :ok end
  private static String pascalCaseModuleJson() {
    return """
    {"tuple":["defmodule",{"line":1,"column":1},\
    [{"tuple":["__aliases__",{"line":1,"column":11},\
    ["MyApp","WebSearchController"]]},\
    {"do":{"tuple":["def",{"line":2,"column":3},\
    [{"tuple":["foo",{"line":2,"column":7},null]},{"do":"ok"}]]}}]]}""";
  }

  // defmodule Foo do def bar, do: :ok end
  private static String singleWordModuleJson() {
    return """
    {"tuple":["defmodule",{"line":1,"column":1},\
    [{"tuple":["__aliases__",{"line":1,"column":11},["Foo"]]},\
    {"do":{"tuple":["def",{"line":2,"column":3},\
    [{"tuple":["bar",{"line":2,"column":7},null]},{"do":"ok"}]]}}]]}""";
  }

  // defmodule MyApp.foo do def bar, do: :ok end
  private static String lowercaseSegmentJson() {
    return """
    {"tuple":["defmodule",{"line":1,"column":1},\
    [{"tuple":["__aliases__",{"line":1,"column":11},\
    ["MyApp","foo"]]},\
    {"do":{"tuple":["def",{"line":2,"column":3},\
    [{"tuple":["bar",{"line":2,"column":7},null]},{"do":"ok"}]]}}]]}""";
  }

  // defmodule MyApp.Accounts.User do def foo, do: :ok end
  private static String dottedPascalCaseJson() {
    return """
    {"tuple":["defmodule",{"line":1,"column":1},\
    [{"tuple":["__aliases__",{"line":1,"column":11},\
    ["MyApp","Accounts","User"]]},\
    {"do":{"tuple":["def",{"line":2,"column":3},\
    [{"tuple":["foo",{"line":2,"column":7},null]},{"do":"ok"}]]}}]]}""";
  }

  // defmodule My_App.Web_search do def foo, do: :ok end
  private static String multipleBadSegmentsJson() {
    return """
    {"tuple":["defmodule",{"line":1,"column":1},\
    [{"tuple":["__aliases__",{"line":1,"column":11},\
    ["My_App","Web_search"]]},\
    {"do":{"tuple":["def",{"line":2,"column":3},\
    [{"tuple":["foo",{"line":2,"column":7},null]},{"do":"ok"}]]}}]]}""";
  }

  // defmodule MyApp.HTTP do def foo, do: :ok end
  private static String acronymModuleJson() {
    return """
    {"tuple":["defmodule",{"line":1,"column":1},\
    [{"tuple":["__aliases__",{"line":1,"column":11},\
    ["MyApp","HTTP"]]},\
    {"do":{"tuple":["def",{"line":2,"column":3},\
    [{"tuple":["foo",{"line":2,"column":7},null]},{"do":"ok"}]]}}]]}""";
  }
}
