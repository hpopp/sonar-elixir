package dev.hpopp.sonar.elixir.sensors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a parsed Elixir AST node.
 *
 * Elixir AST tuples are {operation, metadata, arguments} where:
 *   - operation is an atom (string) or nested tuple
 *   - metadata contains line/column info
 *   - arguments is a list of child nodes
 *
 * Keyword lists like [do: body] are encoded by the escript as JSON objects
 * {"do": body} and parsed here as nodes with type "keyword_list".
 */
public record ElixirAst(
    String type,
    int line,
    int column,
    List<ElixirAst> children,
    String value
) {

    public static ElixirAst parse(String json) {
        JsonElement root = JsonParser.parseString(json);
        return fromJson(root);
    }

    private static ElixirAst fromJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return new ElixirAst("nil", 0, 0, List.of(), null);
        }

        if (element.isJsonPrimitive()) {
            var prim = element.getAsJsonPrimitive();
            return new ElixirAst("literal", 0, 0, List.of(), prim.getAsString());
        }

        if (element.isJsonObject()) {
            var obj = element.getAsJsonObject();

            if (obj.has("tuple")) {
                var tuple = obj.getAsJsonArray("tuple");
                if (tuple.size() == 3) {
                    String type = extractType(tuple.get(0));
                    int line = extractLine(tuple.get(1));
                    int column = extractColumn(tuple.get(1));
                    List<ElixirAst> children = extractChildren(tuple.get(2));
                    return new ElixirAst(type, line, column, children, null);
                }
            }

            // Keyword list: {"do": body, "else": body, ...}
            return fromKeywordObject(obj);
        }

        if (element.isJsonArray()) {
            var arr = element.getAsJsonArray();
            List<ElixirAst> items = new ArrayList<>();
            for (JsonElement item : arr) {
                items.add(fromJson(item));
            }
            return new ElixirAst("list", 0, 0, items, null);
        }

        return new ElixirAst("unknown", 0, 0, List.of(), null);
    }

    private static ElixirAst fromKeywordObject(JsonObject obj) {
        List<ElixirAst> pairs = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            ElixirAst val = fromJson(entry.getValue());
            pairs.add(new ElixirAst("keyword_pair", val.line(), val.column(), List.of(val),
                entry.getKey()));
        }
        return new ElixirAst("keyword_list", 0, 0, pairs, null);
    }

    private static String extractType(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (element.isJsonObject() && element.getAsJsonObject().has("tuple")) {
            return "nested_call";
        }
        return "unknown";
    }

    private static int extractLine(JsonElement metadata) {
        if (metadata.isJsonObject()) {
            var obj = metadata.getAsJsonObject();
            if (obj.has("line")) {
                return obj.get("line").getAsInt();
            }
        }
        return 0;
    }

    private static int extractColumn(JsonElement metadata) {
        if (metadata.isJsonObject()) {
            var obj = metadata.getAsJsonObject();
            if (obj.has("column")) {
                return obj.get("column").getAsInt();
            }
        }
        return 0;
    }

    private static List<ElixirAst> extractChildren(JsonElement args) {
        List<ElixirAst> children = new ArrayList<>();
        if (args.isJsonArray()) {
            for (JsonElement arg : args.getAsJsonArray()) {
                children.add(fromJson(arg));
            }
        } else if (!args.isJsonNull()) {
            children.add(fromJson(args));
        }
        return children;
    }

    /** Get the value of a keyword pair by key (e.g., "do" from a do-block). */
    public Optional<ElixirAst> keyword(String key) {
        if (!"keyword_list".equals(type)) {
            return Optional.empty();
        }
        return children.stream()
            .filter(c -> "keyword_pair".equals(c.type()) && key.equals(c.value()))
            .map(c -> c.children().isEmpty() ? null : c.children().get(0))
            .findFirst();
    }

    /** Get all top-level expressions in a block body. */
    public List<ElixirAst> blockChildren() {
        if ("__block__".equals(type)) {
            return children;
        }
        return List.of(this);
    }

    /** Recursively find all nodes matching a given type. */
    public List<ElixirAst> findAll(String nodeType) {
        List<ElixirAst> results = new ArrayList<>();
        findAll(nodeType, results);
        return results;
    }

    private void findAll(String nodeType, List<ElixirAst> results) {
        if (this.type.equals(nodeType)) {
            results.add(this);
        }
        for (ElixirAst child : children) {
            child.findAll(nodeType, results);
        }
    }

    /** Walk all nodes depth-first. */
    public void walk(AstVisitor visitor) {
        visitor.visit(this);
        for (ElixirAst child : children) {
            child.walk(visitor);
        }
    }

    @FunctionalInterface
    public interface AstVisitor {
        void visit(ElixirAst node);
    }
}
