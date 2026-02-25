[![CI](https://github.com/hpopp/sonar-elixir/actions/workflows/ci.yml/badge.svg)](https://github.com/hpopp/sonar-elixir/actions/workflows/ci.yml)
[![Version](https://img.shields.io/badge/version-0.1.0-orange.svg)](https://github.com/hpopp/sonar-elixir/commits/main)
[![License](https://img.shields.io/github/license/hpopp/sonar-elixir)](LICENSE)
[![Last Updated](https://img.shields.io/github/last-commit/hpopp/sonar-elixir.svg)](https://github.com/hpopp/sonar-elixir/commits/main)

# sonar-elixir

SonarQube plugin for static analysis of Elixir projects.

## Features

- **Static analysis** -- Credo-inspired rules covering code smells, security vulnerabilities, and reliability bugs
- **Metrics** -- Lines of code, comment lines, and executable line tracking
- **Test coverage** -- Converts Elixir's `.coverdata` to SonarQube's generic coverage format
- **Syntax highlighting** -- Keywords, strings, atoms, numbers, doc attributes, and module attributes colored in the SonarQube code viewer

## Installation

**Compatibility:** SonarQube 9.9+ (Community Edition or higher).

1. Download the latest `sonar-elixir-plugin-x.y.z.jar` from [Releases](https://github.com/hpopp/sonar-elixir/releases).

2. Copy the JAR to your SonarQube installation's plugin directory.

```shell
cp sonar-elixir-plugin-0.1.0.jar $SONARQUBE_HOME/extensions/plugins/
```

3. Restart SonarQube.

**Docker users** can volume-mount the JAR instead. See the included `docker-compose.yml` for an example.

> **Note:** Elixir must be installed on the machine running `sonar-scanner`. The plugin shells out to the Elixir runtime for AST parsing and tokenization.

## Project Setup

Create a `sonar-project.properties` file in your project root:

```properties
sonar.projectKey=my-elixir-app
sonar.projectName=My Elixir App
sonar.sources=lib
sonar.tests=test
sonar.sourceEncoding=UTF-8
sonar.host.url=http://localhost:9000
sonar.elixir.file.suffixes=.ex,.exs
```

Then run the scanner:

```shell
sonar-scanner
```

### Properties

| Property                     | Description                          | Default    |
| ---------------------------- | ------------------------------------ | ---------- |
| `sonar.elixir.file.suffixes` | File extensions recognized as Elixir | `.ex,.exs` |
| `sonar.coverageReportPaths`  | Path to the generated coverage XML   | _(none)_   |

## Coverage

Elixir's built-in `:cover` tool produces `.coverdata` files. The included `tools/coverage.exs` script converts these to SonarQube's
[generic coverage](https://docs.sonarsource.com/sonarqube/latest/analyzing-source-code/test-coverage/generic-test-data/) XML format.

1. Run tests with coverage export.

```shell
mix test --cover --export-coverage default
```

2. Convert to SonarQube XML.

```shell
elixir tools/coverage.exs
```

This reads `cover/default.coverdata` and writes `cover/sonar-coverage.xml`.

3. Add the coverage path to your `sonar-project.properties`:

```properties
sonar.coverageReportPaths=cover/sonar-coverage.xml
```

4. Run `sonar-scanner` as usual.

## CI Integration

A typical CI pipeline needs Elixir (for tests and coverage conversion) and `sonar-scanner` (Java-based). The full sequence:

```yaml
# Example GitHub Actions steps
- name: Install dependencies
  run: mix deps.get

- name: Run tests with coverage
  run: mix test --cover --export-coverage default

- name: Convert coverage to SonarQube format
  run: elixir tools/coverage.exs

- name: Run SonarQube scanner
  run: sonar-scanner
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
```

The `tools/coverage.exs` script is included in the plugin JAR and also available in this repository under `tools/`. Copy it into your project or reference it from a local clone.

## Rules

The default quality profile enables all rules. Rules without a checkmark are defined and will be implemented in future releases.

### Code Smells

| Key  | Name               | Description                                             | Severity | Impl |
| ---- | ------------------ | ------------------------------------------------------- | -------- | :--: |
| S001 | FunctionComplexity | Function cognitive complexity should not be too high    | Major    |      |
| S002 | LongFunction       | Functions should not have too many lines                | Major    |      |
| S003 | TooManyParameters  | Functions should not have too many parameters           | Major    |      |
| S004 | NestingDepth       | Control flow statements should not be nested too deeply | Major    |      |
| S005 | MissingModuledoc   | Modules should have `@moduledoc`                        | Minor    |  ✓   |
| S006 | LargeModule        | Modules should not have too many lines                  | Major    |      |
| S007 | PipeChainStart     | Pipe chains should start with a raw value               | Minor    |      |
| S008 | SingleClauseWith   | `with` statements should have more than one clause      | Minor    |      |

### Vulnerabilities

| Key  | Name                  | Description                                                      | Severity | Impl |
| ---- | --------------------- | ---------------------------------------------------------------- | -------- | :--: |
| S101 | HardcodedSecret       | Credentials should not be hardcoded                              | Blocker  |      |
| S102 | SQLInjection          | SQL queries should not be built using string interpolation       | Critical |      |
| S103 | AtomFromUserInput     | `String.to_atom` should not be called on user input              | Critical |      |
| S104 | UnsafeDeserialization | `:erlang.binary_to_term` should not be used with untrusted input | Critical |      |
| S105 | InsecureHttpClient    | HTTP requests should use HTTPS                                   | Major    |      |
| S106 | WeakCrypto            | Weak cryptographic algorithms should not be used                 | Critical |      |

### Bugs

| Key  | Name                    | Description                                              | Severity | Impl |
| ---- | ----------------------- | -------------------------------------------------------- | -------- | :--: |
| S201 | UnhandledErrorTuple     | Error tuples should be pattern matched, not ignored      | Major    |      |
| S202 | BareRescue              | `rescue` clauses should specify exception types          | Major    |      |
| S203 | GenServerCallInCallback | `GenServer.call` should not be used inside `handle_call` | Critical |      |

## Contributing

### Prerequisites

- Java 17+
- Maven
- Elixir 1.14+

### Build

```shell
mvn package
```

The plugin JAR is written to `target/sonar-elixir-plugin-0.1.0.jar`.

### Test

```shell
mvn test
```

### Local SonarQube

The included `docker-compose.yml` runs a SonarQube instance with the plugin mounted:

```shell
docker compose up
```

SonarQube will be available at `http://localhost:9000` (default credentials: `admin` / `admin`).

## License

Copyright (c) 2026 Henry Popp

This project is MIT licensed. See the [LICENSE](LICENSE) for details.
