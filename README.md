# sonar-elixir

> SonarQube plugin for static analysis of Elixir projects.

[![CI](https://github.com/hpopp/sonar-elixir/actions/workflows/ci.yml/badge.svg)](https://github.com/hpopp/sonar-elixir/actions/workflows/ci.yml)
[![Version](https://img.shields.io/badge/version-0.1.0-orange.svg)](https://github.com/hpopp/sonar-elixir/commits/main)
[![License](https://img.shields.io/github/license/hpopp/sonar-elixir)](LICENSE)
[![Last Updated](https://img.shields.io/github/last-commit/hpopp/sonar-elixir.svg)](https://github.com/hpopp/sonar-elixir/commits/main)

> [!NOTE]
> This plugin is in early development. Only a subset of rules are currently implemented.
> Feedback, bug reports, and contributions are welcome.

## Features

- **Static analysis** -- Credo-inspired rules covering code smells, security vulnerabilities, and reliability bugs.
- **Metrics** -- Lines of code and comment line counts.
- **Test coverage** -- Imports coverage via the [`sonarqube`](https://github.com/hpopp/mix-sonarqube) Hex package.
- **Syntax highlighting** -- Elixir-aware highlighting in the SonarQube code viewer.

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

# Include if using mix-sonarqube for coverage reporting.
sonar.coverageReportPaths=cover/sonar-coverage.xml
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

Coverage is handled by the [`sonarqube`](https://github.com/hpopp/mix-sonarqube) Hex package. See its README for installation and setup instructions.

## CI Integration

A typical CI pipeline needs Elixir (for tests and coverage) and `sonar-scanner` (Java-based). The full sequence:

```yaml
# Example GitHub Actions steps
- name: Install dependencies
  run: mix deps.get

- name: Run tests with coverage
  run: mix sonarqube.coverage

- name: Run SonarQube scanner
  run: sonar-scanner
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
```

## Rules

### Code Smells

| Key                      | Description                                    | Severity | Default |
| ------------------------ | ---------------------------------------------- | -------- | :-----: |
| `function_names`         | Function names should be in snake_case         | Minor    |    ✓    |
| `io_inspect`             | `IO.inspect` calls should be removed           | Major    |    ✓    |
| `large_module`           | Modules should not have too many lines         | Minor    |         |
| `missing_moduledoc`      | Modules should have `@moduledoc`               | Minor    |    ✓    |
| `module_attribute_names` | Module attribute names should be in snake_case | Minor    |    ✓    |
| `module_names`           | Module names should be in PascalCase           | Minor    |    ✓    |
| `pipe_chain_start`       | Pipe chains should start with a raw value      | Minor    |         |

### Vulnerabilities

| Key                | Description                         | Severity | Default |
| ------------------ | ----------------------------------- | -------- | :-----: |
| `hardcoded_secret` | Credentials should not be hardcoded | Blocker  |    ✓    |

Rules marked with ✓ in **Default** are active in the built-in "Elixir Way" quality profile. All rules can be individually enabled or disabled in SonarQube's quality profile settings.

## Contributing

### Prerequisites

- Java 17+
- Maven
- Elixir 1.15+

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
