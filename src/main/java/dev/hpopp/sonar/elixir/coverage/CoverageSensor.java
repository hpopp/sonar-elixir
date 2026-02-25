package dev.hpopp.sonar.elixir.coverage;

import dev.hpopp.sonar.elixir.language.Elixir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.CoreMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Reads the coverage XML produced by tools/coverage.exs and reports
 * EXECUTABLE_LINES_DATA so SonarQube's coverage denominator matches
 * what Elixir's :cover module considers executable.
 *
 * The actual line-level coverage import is handled by SonarQube's built-in
 * Generic Coverage sensor via sonar.coverageReportPaths.
 */
public class CoverageSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageSensor.class);

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
                .name("Elixir Coverage")
                .onlyOnLanguage(Elixir.KEY);
    }

    @Override
    public void execute(SensorContext context) {
        Configuration config = context.config();
        String[] reportPaths = config.getStringArray("sonar.coverageReportPaths");

        if (reportPaths.length == 0) {
            LOG.info("No sonar.coverageReportPaths configured, skipping executable lines");
            return;
        }

        Map<String, Set<Integer>> executableLines = new HashMap<>();

        for (String reportPath : reportPaths) {
            Path path = context.fileSystem().baseDir().toPath().resolve(reportPath);
            if (!Files.exists(path)) {
                LOG.warn("Coverage report not found: {}", path);
                continue;
            }

            try {
                parseCoverageReport(path, executableLines);
            } catch (Exception e) {
                LOG.warn("Failed to parse coverage report {}: {}", path, e.getMessage());
            }
        }

        FilePredicates predicates = context.fileSystem().predicates();

        for (Map.Entry<String, Set<Integer>> entry : executableLines.entrySet()) {
            InputFile file = context.fileSystem().inputFile(
                predicates.hasRelativePath(entry.getKey()));

            if (file == null) {
                continue;
            }

            String data = entry.getValue().stream()
                .sorted()
                .map(line -> line + "=1")
                .collect(Collectors.joining(";"));

            context.<String>newMeasure()
                .forMetric(CoreMetrics.EXECUTABLE_LINES_DATA)
                .on(file)
                .withValue(data)
                .save();
        }

        LOG.info("Reported executable lines for {} files", executableLines.size());
    }

    private void parseCoverageReport(Path path, Map<String, Set<Integer>> result)
            throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(path.toFile());

        NodeList fileNodes = doc.getElementsByTagName("file");
        for (int i = 0; i < fileNodes.getLength(); i++) {
            Element fileEl = (Element) fileNodes.item(i);
            String filePath = fileEl.getAttribute("path");

            Set<Integer> lines = result.computeIfAbsent(filePath, k -> new HashSet<>());

            NodeList lineNodes = fileEl.getElementsByTagName("lineToCover");
            for (int j = 0; j < lineNodes.getLength(); j++) {
                Element lineEl = (Element) lineNodes.item(j);
                int lineNum = Integer.parseInt(lineEl.getAttribute("lineNumber"));
                lines.add(lineNum);
            }
        }
    }
}
