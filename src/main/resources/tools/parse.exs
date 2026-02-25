#!/usr/bin/env elixir

# Parses an Elixir source file and emits its AST as JSON to stdout.
#
# Usage: elixir tools/parse.exs path/to/file.ex
#
# The AST uses Elixir's standard {operation, metadata, arguments} tuple format.
# Tuples are encoded as {"tuple": [elem1, elem2, ...]} in JSON since JSON has
# no native tuple type.

defmodule AstToJson do
  def encode(ast), do: do_encode(ast) |> IO.iodata_to_binary()

  defp do_encode(tuple) when is_tuple(tuple) do
    elements = tuple |> Tuple.to_list() |> Enum.map(&do_encode/1) |> Enum.intersperse(",")
    ["{\"tuple\":[", elements, "]}"]
  end

  defp do_encode(list) when is_list(list) do
    if Keyword.keyword?(list) and list != [] do
      pairs = Enum.map(list, fn {k, v} ->
        [?", Atom.to_string(k), "\":", do_encode(v)]
      end) |> Enum.intersperse(",")
      ["{", pairs, "}"]
    else
      items = Enum.map(list, &do_encode/1) |> Enum.intersperse(",")
      ["[", items, "]"]
    end
  end

  defp do_encode(atom) when is_atom(atom) do
    case atom do
      nil -> "null"
      true -> "true"
      false -> "false"
      _ -> [?", Atom.to_string(atom), ?"]
    end
  end

  defp do_encode(binary) when is_binary(binary) do
    escaped = binary
    |> String.replace("\\", "\\\\")
    |> String.replace("\"", "\\\"")
    |> String.replace("\n", "\\n")
    |> String.replace("\r", "\\r")
    |> String.replace("\t", "\\t")
    [?", escaped, ?"]
  end

  defp do_encode(number) when is_integer(number), do: Integer.to_string(number)
  defp do_encode(number) when is_float(number), do: Float.to_string(number)

  defp do_encode(other) do
    [?", inspect(other) |> String.replace("\"", "\\\""), ?"]
  end
end

case System.argv() do
  [file_path] ->
    case File.read(file_path) do
      {:ok, source} ->
        case Code.string_to_quoted(source, columns: true, token_metadata: true) do
          {:ok, ast} ->
            IO.write(AstToJson.encode(ast))

          {:error, {location, message, token}} ->
            line = Keyword.get(location, :line, 0)
            IO.write(:stderr, "Parse error at line #{line}: #{message}#{token}\n")
            System.halt(1)
        end

      {:error, reason} ->
        IO.write(:stderr, "Cannot read file #{file_path}: #{reason}\n")
        System.halt(1)
    end

  _ ->
    IO.write(:stderr, "Usage: elixir parse.exs <file_path>\n")
    System.halt(1)
end
