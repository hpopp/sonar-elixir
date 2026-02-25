package dev.hpopp.sonar.elixir.sensors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses Elixir source files by shelling out to the escript in tools/parse.exs.
 *
 * The escript calls Code.string_to_quoted!/2 and emits the AST as JSON.
 */
public class ElixirParser {

    private static final Logger LOG = LoggerFactory.getLogger(ElixirParser.class);
    private static final int TIMEOUT_SECONDS = 30;

    private final String elixirPath;
    private Path cachedScript;

    public ElixirParser(String elixirPath) {
        this.elixirPath = elixirPath;
    }

    public ElixirParser() {
        this("elixir");
    }

    public ElixirAst parse(Path file) {
        try {
            Path scriptPath = locateScript();
            ProcessBuilder pb = new ProcessBuilder(
                    elixirPath, scriptPath.toString(), file.toAbsolutePath().toString());
            pb.redirectErrorStream(false);

            Process process = pb.start();
            String stdout;
            String stderr;

            try (
                    var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    var errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                stdout = reader.lines().reduce("", (a, b) -> a + b);
                stderr = errReader.lines().reduce("", (a, b) -> a + "\n" + b).trim();
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOG.error("Elixir parser timed out for file: {}", file);
                return null;
            }

            if (process.exitValue() != 0) {
                LOG.error("Elixir parser failed for {}: {}", file, stderr);
                return null;
            }

            return ElixirAst.parse(stdout);

        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to run Elixir parser for {}: {}", file, e.getMessage());
            return null;
        }
    }

    private Path locateScript() throws IOException {
        if (cachedScript != null && Files.exists(cachedScript)) {
            return cachedScript;
        }

        InputStream resource = getClass().getClassLoader().getResourceAsStream("tools/parse.exs");
        if (resource != null) {
            Path tmp = Files.createTempFile("sonar-elixir-parse", ".exs");
            tmp.toFile().deleteOnExit();
            Files.copy(resource, tmp, StandardCopyOption.REPLACE_EXISTING);
            resource.close();
            cachedScript = tmp;
            return tmp;
        }

        return Path.of("tools", "parse.exs");
    }
}
