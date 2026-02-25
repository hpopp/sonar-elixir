#!/usr/bin/env elixir

# Parses an Elixir source file and emits its AST and syntax tokens as JSON.
#
# Usage: elixir tools/parse.exs path/to/file.ex
#
# Output: {"ast": <ast-json>, "tokens": [<token>, ...]}
#
# Each token: {"type": "keyword|string|comment|annotation|constant",
#              "line": N, "col": N, "end_line": N, "end_col": N}

defmodule AstToJson do
  def encode(ast), do: do_encode(ast) |> IO.iodata_to_binary()

  defp do_encode(tuple) when is_tuple(tuple) do
    elements = tuple |> Tuple.to_list() |> Enum.map(&do_encode/1) |> Enum.intersperse(",")
    ["{\"tuple\":[", elements, "]}"]
  end

  defp do_encode(list) when is_list(list) do
    if Keyword.keyword?(list) and list != [] do
      pairs =
        Enum.map(list, fn {k, v} ->
          [?", Atom.to_string(k), "\":", do_encode(v)]
        end)
        |> Enum.intersperse(",")

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
    escaped =
      binary
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

defmodule Tokenizer do
  @keywords ~w(defmodule defprotocol defimpl defstruct defexception defdelegate
               defguard defguardp defmacro defmacrop defoverridable def defp
               do end fn if else unless case cond with for raise reraise
               try catch rescue after throw import alias require use
               when in and or not quote unquote unquote_splicing
               receive send super)

  @doc_attrs ~w(doc moduledoc typedoc)
  @ann_attrs ~w(spec type typep opaque callback macrocallback behaviour impl
                deprecated since enforce_keys derive)
  @constants ~w(true false nil)

  def tokenize(source) do
    source_lines = String.split(source, "\n")
    comments = extract_comments(source_lines)

    case :elixir.string_to_tokens(String.to_charlist(source), 1, 1, "nofile", []) do
      {:ok, tokens} ->
        elixir_tokens = classify_tokens(tokens, source_lines)
        comments ++ elixir_tokens

      _ ->
        comments
    end
  end

  defp classify_tokens(tokens, source_lines) do
    classify_tokens(tokens, source_lines, nil, [])
  end

  defp classify_tokens([], _source_lines, _prev, acc), do: Enum.reverse(acc)

  defp classify_tokens([token | rest], source_lines, prev, acc) do
    results = classify_token(token, source_lines, prev)
    classify_tokens(rest, source_lines, token, Enum.reverse(results) ++ acc)
  end

  defp extract_comments(lines) do
    lines
    |> Enum.with_index(1)
    |> Enum.flat_map(fn {line, line_num} ->
      case Regex.run(~r/^(\s*)#(.*)$/, line) do
        [_, indent, _rest] ->
          col = String.length(indent) + 1
          end_col = String.length(line) + 1
          [%{type: "comment", line: line_num, col: col, end_line: line_num, end_col: end_col}]

        _ ->
          []
      end
    end)
  end

  defp classify_token({:identifier, {line, col, charlist}, atom_val}, _source_lines, _prev) do
    name = Atom.to_string(atom_val)
    len = if charlist, do: length(charlist), else: String.length(name)

    cond do
      name in @keywords ->
        [%{type: "keyword", line: line, col: col, end_line: line, end_col: col + len}]

      name in @constants ->
        [%{type: "constant", line: line, col: col, end_line: line, end_col: col + len}]

      true ->
        []
    end
  end

  defp classify_token({kw, {line, col, _}}, _source_lines, _prev)
       when kw in [:do, :end, :fn, :catch, :rescue, :after] do
    len = kw |> Atom.to_string() |> String.length()
    [%{type: "keyword", line: line, col: col, end_line: line, end_col: col + len}]
  end

  defp classify_token({:kw_identifier, {line, col, charlist}, _atom_val}, _source_lines, _prev) do
    name = List.to_string(charlist)
    len = String.length(name)

    type = if name in @keywords, do: "keyword", else: "constant"
    [%{type: type, line: line, col: col, end_line: line, end_col: col + len}]
  end

  defp classify_token({:at_op, {line, col, _}, :@}, source_lines, _prev) do
    source_line = Enum.at(source_lines, line - 1, "")
    rest = String.slice(source_line, col..-1//1)

    cond do
      match_attr?(rest, @doc_attrs) ->
        attr = matching_attr(rest, @doc_attrs)

        [
          %{
            type: "structured_comment",
            line: line,
            col: col,
            end_line: line,
            end_col: col + 1 + String.length(attr)
          }
        ]

      match_attr?(rest, @ann_attrs) ->
        attr = matching_attr(rest, @ann_attrs)

        [
          %{
            type: "annotation",
            line: line,
            col: col,
            end_line: line,
            end_col: col + 1 + String.length(attr)
          }
        ]

      true ->
        [%{type: "annotation", line: line, col: col, end_line: line, end_col: col + 1}]
    end
  end

  defp classify_token({:atom, {line, col, _charlist}, atom_val}, _source_lines, _prev) do
    len = atom_val |> Atom.to_string() |> String.length()
    [%{type: "constant", line: line, col: col, end_line: line, end_col: col + len + 1}]
  end

  defp classify_token({:bin_string, {line, col, _}, _parts}, source_lines, prev) do
    type = if doc_attr?(prev), do: "structured_comment", else: "string"
    end_pos = find_string_end(source_lines, line, col, "\"")
    [%{type: type, line: line, col: col, end_line: end_pos.line, end_col: end_pos.col}]
  end

  defp classify_token({:bin_heredoc, {line, col, _}, _parts}, source_lines, prev) do
    type = if doc_attr?(prev), do: "structured_comment", else: "string"
    end_pos = find_heredoc_end(source_lines, line, "\"\"\"")
    [%{type: type, line: line, col: col, end_line: end_pos.line, end_col: end_pos.col}]
  end

  defp classify_token({:bin_heredoc, {line, col, _}, _indent, _parts}, source_lines, prev) do
    type = if doc_attr?(prev), do: "structured_comment", else: "string"
    end_pos = find_heredoc_end(source_lines, line, "\"\"\"")
    [%{type: type, line: line, col: col, end_line: end_pos.line, end_col: end_pos.col}]
  end

  defp classify_token({:list_string, {line, col, _}, _parts}, source_lines, _prev) do
    end_pos = find_string_end(source_lines, line, col, "'")
    [%{type: "string", line: line, col: col, end_line: end_pos.line, end_col: end_pos.col}]
  end

  defp classify_token({:list_heredoc, {line, col, _}, _parts}, source_lines, _prev) do
    end_pos = find_heredoc_end(source_lines, line, "'''")
    [%{type: "string", line: line, col: col, end_line: end_pos.line, end_col: end_pos.col}]
  end

  defp classify_token({:list_heredoc, {line, col, _}, _indent, _parts}, source_lines, _prev) do
    end_pos = find_heredoc_end(source_lines, line, "'''")
    [%{type: "string", line: line, col: col, end_line: end_pos.line, end_col: end_pos.col}]
  end

  defp classify_token(
         {:sigil, {line, col, _}, _, _parts, _modifiers, _delimiter, _},
         source_lines,
         _prev
       ) do
    source_line = Enum.at(source_lines, line - 1, "")
    rest = String.slice(source_line, (col - 1)..-1//1)
    len = String.length(rest)
    [%{type: "string", line: line, col: col, end_line: line, end_col: col + len}]
  end

  defp classify_token({:alias, {line, col, charlist}, _atom}, _source_lines, _prev) do
    len = if charlist, do: length(charlist), else: 0

    if len > 0 do
      [%{type: "constant", line: line, col: col, end_line: line, end_col: col + len}]
    else
      []
    end
  end

  defp classify_token({num_type, {line, col, _}, source_charlist}, _source_lines, _prev)
       when num_type in [:int, :flt] do
    len = if is_list(source_charlist), do: length(source_charlist), else: 1
    [%{type: "constant", line: line, col: col, end_line: line, end_col: col + len}]
  end

  defp classify_token(_, _, _), do: []

  defp doc_attr?({:identifier, _, atom_val}) when is_atom(atom_val) do
    Atom.to_string(atom_val) in @doc_attrs
  end

  defp doc_attr?(_), do: false

  defp match_attr?(rest, attrs), do: Enum.any?(attrs, &String.starts_with?(rest, &1))

  defp matching_attr(rest, attrs) do
    Enum.find(attrs, fn attr -> String.starts_with?(rest, attr) end) || ""
  end

  defp find_string_end(source_lines, start_line, start_col, quote_char) do
    line_str = Enum.at(source_lines, start_line - 1, "")
    rest = String.slice(line_str, start_col..-1//1)

    case find_closing_quote(rest, quote_char, false) do
      {:found, offset} ->
        %{line: start_line, col: start_col + offset + 1}

      :not_found ->
        scan_lines_for_close(source_lines, start_line + 1, quote_char)
    end
  end

  defp find_closing_quote("", _quote, _escaped), do: :not_found

  defp find_closing_quote(<<"\\", _::binary-size(1), rest::binary>>, quote, false) do
    case find_closing_quote(rest, quote, false) do
      {:found, n} -> {:found, n + 2}
      other -> other
    end
  end

  defp find_closing_quote(<<c::utf8, _rest::binary>>, quote, false) when <<c::utf8>> == quote do
    {:found, 1}
  end

  defp find_closing_quote(<<_::utf8, rest::binary>>, quote, _escaped) do
    case find_closing_quote(rest, quote, false) do
      {:found, n} -> {:found, n + 1}
      other -> other
    end
  end

  defp scan_lines_for_close(source_lines, line_num, quote_char) do
    case Enum.at(source_lines, line_num - 1) do
      nil ->
        %{line: line_num - 1, col: 1}

      line_str ->
        case find_closing_quote(line_str, quote_char, false) do
          {:found, offset} -> %{line: line_num, col: offset + 1}
          :not_found -> scan_lines_for_close(source_lines, line_num + 1, quote_char)
        end
    end
  end

  defp find_heredoc_end(source_lines, start_line, delimiter) do
    source_lines
    |> Enum.with_index(1)
    |> Enum.drop(start_line)
    |> Enum.find_value(%{line: start_line, col: 1}, fn {line, idx} ->
      trimmed = String.trim_leading(line)

      if String.starts_with?(trimmed, delimiter) do
        col = String.length(line) - String.length(trimmed) + 1
        %{line: idx, col: col + String.length(delimiter)}
      end
    end)
  end

  def to_json(tokens) do
    items =
      tokens
      |> Enum.sort_by(fn t -> {t.line, t.col} end)
      |> Enum.map(fn t ->
        ~s({"type":"#{t.type}","line":#{t.line},"col":#{t.col},"end_line":#{t.end_line},"end_col":#{t.end_col}})
      end)
      |> Enum.join(",")

    "[#{items}]"
  end
end

case System.argv() do
  [file_path] ->
    case File.read(file_path) do
      {:ok, source} ->
        case Code.string_to_quoted(source, columns: true, token_metadata: true) do
          {:ok, ast} ->
            ast_json = AstToJson.encode(ast)
            tokens_json = source |> Tokenizer.tokenize() |> Tokenizer.to_json()
            IO.write(["{\"ast\":", ast_json, ",\"tokens\":", tokens_json, "}"])

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
