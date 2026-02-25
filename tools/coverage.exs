#!/usr/bin/env elixir

# Converts Elixir cover data to SonarQube's generic coverage XML format.
#
# Usage:
#   mix test --cover
#   elixir tools/coverage.exs [--input cover/Elixir.*.coverdata] [--output cover/sonar-coverage.xml]
#
# The output file can be imported via sonar.coverageReportPaths in sonar-project.properties.

defmodule SonarCoverage do
  def run(args) do
    {opts, _rest} = parse_args(args)

    input_pattern = opts[:input] || "cover/*.coverdata"
    output_path = opts[:output] || "cover/sonar-coverage.xml"

    coverdata_files = Path.wildcard(input_pattern)

    if coverdata_files == [] do
      IO.puts(:stderr, "No coverdata files found matching: #{input_pattern}")
      IO.puts(:stderr, "Run `mix test --cover` first.")
      System.halt(1)
    end

    Path.wildcard("_build/*/lib/*/ebin")
    |> Enum.each(&Code.prepend_path/1)

    :cover.start()

    for file <- coverdata_files do
      case :cover.import(String.to_charlist(file)) do
        :ok -> :ok
        {:error, reason} ->
          IO.puts(:stderr, "Warning: failed to import #{file}: #{inspect(reason)}")
      end
    end

    modules = :cover.imported_modules()

    file_coverages =
      modules
      |> Enum.map(&analyze_module/1)
      |> Enum.reject(&is_nil/1)

    xml = build_xml(file_coverages)
    File.mkdir_p!(Path.dirname(output_path))
    File.write!(output_path, xml)

    total_lines = Enum.reduce(file_coverages, 0, fn {_, lines}, acc -> acc + length(lines) end)
    covered = Enum.reduce(file_coverages, 0, fn {_, lines}, acc ->
      acc + Enum.count(lines, fn {_, count} -> count > 0 end)
    end)

    pct = if total_lines > 0, do: Float.round(covered / total_lines * 100, 1), else: 0.0
    IO.puts("Coverage: #{covered}/#{total_lines} lines (#{pct}%)")
    IO.puts("Written to: #{output_path}")

    :cover.stop()
  end

  defp analyze_module(module) do
    case :cover.analyse(module, :coverage, :line) do
      {:ok, line_data} ->
        source_file = find_source_file(module)

        if source_file do
          lines =
            line_data
            |> Enum.map(fn {{_mod, line}, {hits, _misses}} ->
              {line, hits}
            end)
            |> Enum.reject(fn {line, _} -> line == 0 end)
            |> Enum.sort_by(&elem(&1, 0))

          {source_file, lines}
        else
          nil
        end

      {:error, _} ->
        nil
    end
  end

  defp find_source_file(module) do
    case module.module_info(:compile)[:source] do
      nil -> nil
      source ->
        path = List.to_string(source)
        make_relative(path)
    end
  rescue
    _ -> nil
  end

  defp make_relative(path) do
    cwd = File.cwd!()

    if String.starts_with?(path, cwd) do
      String.trim_leading(path, cwd <> "/")
    else
      path
    end
  end

  defp build_xml(file_coverages) do
    files_xml =
      file_coverages
      |> Enum.map(fn {path, lines} ->
        lines_xml =
          lines
          |> Enum.map(fn {line, hits} ->
            covered = if hits > 0, do: "true", else: "false"
            ~s(    <lineToCover lineNumber="#{line}" covered="#{covered}"/>)
          end)
          |> Enum.join("\n")

        ~s(  <file path="#{escape_xml(path)}">\n#{lines_xml}\n  </file>)
      end)
      |> Enum.join("\n")

    ~s(<?xml version="1.0" encoding="UTF-8"?>\n<coverage version="1">\n#{files_xml}\n</coverage>\n)
  end

  defp escape_xml(str) do
    str
    |> String.replace("&", "&amp;")
    |> String.replace("<", "&lt;")
    |> String.replace(">", "&gt;")
    |> String.replace("\"", "&quot;")
  end

  defp parse_args(args) do
    {opts, rest, _} =
      OptionParser.parse(args, strict: [input: :string, output: :string])

    {opts, rest}
  end
end

SonarCoverage.run(System.argv())
