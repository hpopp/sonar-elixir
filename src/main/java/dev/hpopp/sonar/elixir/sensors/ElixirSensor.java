package dev.hpopp.sonar.elixir.sensors;

import dev.hpopp.sonar.elixir.language.Elixir;
import dev.hpopp.sonar.elixir.rules.ElixirRule;
import dev.hpopp.sonar.elixir.rules.ElixirRulesDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.measures.CoreMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElixirSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(ElixirSensor.class);

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
            .name("Elixir Analyzer")
            .onlyOnLanguage(Elixir.KEY);
    }

    @Override
    public void execute(SensorContext context) {
        FilePredicates predicates = context.fileSystem().predicates();
        List<InputFile> files = new ArrayList<>();
        context.fileSystem().inputFiles(predicates.hasLanguage(Elixir.KEY))
            .forEach(files::add);

        if (files.isEmpty()) {
            return;
        }

        ElixirParser parser = new ElixirParser();

        int threads = Math.min(files.size(), Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        Map<InputFile, ParseResult> results = new ConcurrentHashMap<>();

        List<Future<?>> futures = new ArrayList<>();
        for (InputFile file : files) {
            futures.add(executor.submit(() -> {
                ParseResult result = parser.parse(file.path());
                if (result != null) {
                    results.put(file, result);
                } else {
                    LOG.warn("Failed to parse AST: {}", file.filename());
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                LOG.warn("Parse task failed: {}", e.getMessage());
            }
        }
        executor.shutdown();

        var rules = ElixirRule.allRules();

        for (InputFile file : files) {
            LOG.info("Analyzing: {}", file.filename());
            computeMetrics(context, file);

            ParseResult result = results.get(file);
            if (result == null) {
                continue;
            }

            applyHighlighting(context, file, result.tokens());

            for (ElixirRule rule : rules) {
                rule.analyze(context, file, result.ast());
            }
        }
    }

    private void computeMetrics(SensorContext context, InputFile file) {
        try {
            String contents = file.contents();
            String[] lines = contents.split("\n", -1);

            int ncloc = 0;
            int commentLines = 0;

            for (String line : lines) {
                String trimmed = line.strip();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("#")) {
                    commentLines++;
                } else {
                    ncloc++;
                }
            }

            context.<Integer>newMeasure()
                .forMetric(CoreMetrics.NCLOC)
                .on(file)
                .withValue(ncloc)
                .save();

            context.<Integer>newMeasure()
                .forMetric(CoreMetrics.COMMENT_LINES)
                .on(file)
                .withValue(commentLines)
                .save();

        } catch (IOException e) {
            LOG.warn("Could not read file for metrics: {}", file.filename(), e);
        }
    }

    private void applyHighlighting(SensorContext context, InputFile file,
                                   List<ParseResult.SyntaxToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        try {
            NewHighlighting highlighting = context.newHighlighting().onFile(file);
            int totalLines = file.lines();

            for (ParseResult.SyntaxToken token : tokens) {
                TypeOfText textType = mapTokenType(token.type());
                if (textType == null || token.line() < 1 || token.endLine() < 1) {
                    continue;
                }
                if (token.line() > totalLines || token.endLine() > totalLines) {
                    continue;
                }
                if (token.col() < 1 || token.endCol() < 1) {
                    continue;
                }

                try {
                    highlighting.highlight(
                        file.newRange(token.line(), token.col() - 1,
                                      token.endLine(), token.endCol() - 1),
                        textType);
                } catch (Exception e) {
                    // Range might be invalid if source changed since parse
                }
            }

            highlighting.save();
        } catch (Exception e) {
            LOG.warn("Failed to apply highlighting for {}: {}", file.filename(), e.getMessage());
        }
    }

    private TypeOfText mapTokenType(String type) {
        return switch (type) {
            case "keyword" -> TypeOfText.KEYWORD;
            case "string" -> TypeOfText.STRING;
            case "comment" -> TypeOfText.COMMENT;
            case "structured_comment" -> TypeOfText.STRUCTURED_COMMENT;
            case "annotation" -> TypeOfText.ANNOTATION;
            case "constant" -> TypeOfText.CONSTANT;
            default -> null;
        };
    }
}
