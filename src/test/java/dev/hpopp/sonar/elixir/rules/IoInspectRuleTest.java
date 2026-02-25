package dev.hpopp.sonar.elixir.rules;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hpopp.sonar.elixir.sensors.ElixirAst;
import org.junit.jupiter.api.Test;

class IoInspectRuleTest {

    private final IoInspectRule rule = new IoInspectRule();

    @Test
    void ruleKey() {
        assertThat(rule.ruleKey()).isEqualTo("S004");
    }

    @Test
    void detectsIoInspect() {
        // IO.inspect(value)
        ElixirAst ast = ElixirAst.parse(ioInspectJson());
        var lines = rule.detect(ast);

        assertThat(lines).containsExactly(5);
    }

    @Test
    void allowsOtherIoCalls() {
        // IO.puts("hello")
        ElixirAst ast = ElixirAst.parse(ioPutsJson());
        var lines = rule.detect(ast);

        assertThat(lines).isEmpty();
    }

    @Test
    void allowsNonIoInspect() {
        // MyModule.inspect(value)
        ElixirAst ast = ElixirAst.parse(otherModuleInspectJson());
        var lines = rule.detect(ast);

        assertThat(lines).isEmpty();
    }

    @Test
    void detectsMultipleOccurrences() {
        // Two IO.inspect calls in a block
        ElixirAst ast = ElixirAst.parse(multipleInspectJson());
        var lines = rule.detect(ast);

        assertThat(lines).hasSize(2);
    }

    @Test
    void detectsCaptureForm() {
        // &IO.inspect/1
        ElixirAst ast = ElixirAst.parse(captureInspectJson());
        var lines = rule.detect(ast);

        assertThat(lines).containsExactly(1);
    }

    @Test
    void detectsInspectWithOptions() {
        // IO.inspect(data, label: "debug")
        ElixirAst ast = ElixirAst.parse(inspectWithOptionsJson());
        var lines = rule.detect(ast);

        assertThat(lines).containsExactly(1);
    }

    // IO.inspect(value) on line 5
    private static String ioInspectJson() {
        return """
                {"tuple":[{"tuple":[".",{"line":5},\
                [{"tuple":["__aliases__",{"line":5},["IO"]]},"inspect"]]},\
                {"line":5},[{"tuple":["value",{"line":5},null]}]]}""";
    }

    // IO.puts("hello")
    private static String ioPutsJson() {
        return """
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["IO"]]},"puts"]]},\
                {"line":1},["hello"]]}""";
    }

    // MyModule.inspect(value)
    private static String otherModuleInspectJson() {
        return """
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["MyModule"]]},"inspect"]]},\
                {"line":1},[{"tuple":["value",{"line":1},null]}]]}""";
    }

    // Two IO.inspect calls
    private static String multipleInspectJson() {
        return """
                {"tuple":["__block__",[],[\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["IO"]]},"inspect"]]},\
                {"line":1},[{"tuple":["a",{"line":1},null]}]]},\
                {"tuple":[{"tuple":[".",{"line":3},\
                [{"tuple":["__aliases__",{"line":3},["IO"]]},"inspect"]]},\
                {"line":3},[{"tuple":["b",{"line":3},null]}]]}\
                ]]}""";
    }

    // &IO.inspect/1
    private static String captureInspectJson() {
        return """
                {"tuple":["&",{"line":1},[\
                {"tuple":["/",{"line":1},[\
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["IO"]]},"inspect"]]},\
                {"line":1},[]]},\
                1]]}]]}""";
    }

    // IO.inspect(data, label: "debug")
    private static String inspectWithOptionsJson() {
        return """
                {"tuple":[{"tuple":[".",{"line":1},\
                [{"tuple":["__aliases__",{"line":1},["IO"]]},"inspect"]]},\
                {"line":1},\
                [{"tuple":["data",{"line":1},null]},{"label":"debug"}]]}""";
    }
}
