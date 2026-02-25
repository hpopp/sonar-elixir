package dev.hpopp.sonar.elixir.sensors;

import java.util.List;

/**
 * Result of parsing an Elixir source file: AST for rules, tokens for
 * highlighting.
 */
public record ParseResult(ElixirAst ast, List<SyntaxToken> tokens) {

    public record SyntaxToken(String type, int line, int col, int endLine, int endCol) {
        // Empty
    }
}
