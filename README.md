[![CI](https://github.com/hpopp/sonar-elixir/actions/workflows/ci.yml/badge.svg)](https://github.com/hpopp/sonar-elixir/actions/workflows/ci.yml)
[![Version](https://img.shields.io/badge/version-0.1.0-orange.svg)](https://github.com/hpopp/sonar-elixir/commits/main)
[![License](https://img.shields.io/github/license/hpopp/sonar-elixir)](LICENSE)
[![Last Updated](https://img.shields.io/github/last-commit/hpopp/sonar-elixir.svg)](https://github.com/hpopp/sonar-elixir/commits/main)

# sonar-elixir

SonarQube plugin for static analysis of Elixir projects.

> [!NOTE]
> This plugin is in early development. Only a subset of rules are currently implemented. Feedback, bug reports, and contributions are welcome.

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

### Code Smells

| Key  | Name             | Description                               | Severity | Default |
| ---- | ---------------- | ----------------------------------------- | -------- | :-----: |
| S001 | MissingModuledoc | Modules should have `@moduledoc`          | Minor    |    ✓    |
| S002 | LargeModule      | Modules should not have too many lines    | Minor    |         |
| S003 | PipeChainStart   | Pipe chains should start with a raw value | Minor    |         |
| S004 | IoInspect        | `IO.inspect` calls should be removed      | Major    |    ✓    |

### Vulnerabilities

| Key  | Name            | Description                         | Severity | Default |
| ---- | --------------- | ----------------------------------- | -------- | :-----: |
| S201 | HardcodedSecret | Credentials should not be hardcoded | Blocker  |    ✓    |

Rules marked with ✓ in **Default** are active in the built-in "Elixir Way" quality profile. All rules can be individually enabled or disabled in SonarQube's quality profile settings.

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
