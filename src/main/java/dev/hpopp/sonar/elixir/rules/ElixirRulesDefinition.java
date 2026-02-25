package dev.hpopp.sonar.elixir.rules;

import dev.hpopp.sonar.elixir.language.Elixir;
import java.util.List;
import org.sonar.api.server.rule.RulesDefinition;

public class ElixirRulesDefinition implements RulesDefinition {

        public static final String REPOSITORY_KEY = "elixir";
        public static final String REPOSITORY_NAME = "Elixir Analyzer";

        private static final List<RuleDefinition> RULES = List.of(
                        // Code smells
                        new RuleDefinition("S001", "FunctionComplexity",
                                        "Function cognitive complexity should not be too high",
                                        "CODE_SMELL", "MAJOR"),
                        new RuleDefinition("S002", "LongFunction",
                                        "Functions should not have too many lines",
                                        "CODE_SMELL", "MAJOR"),
                        new RuleDefinition("S003", "TooManyParameters",
                                        "Functions should not have too many parameters",
                                        "CODE_SMELL", "MAJOR"),
                        new RuleDefinition("S004", "NestingDepth",
                                        "Control flow statements should not be nested too deeply",
                                        "CODE_SMELL", "MAJOR"),
                        new RuleDefinition("S005", "MissingModuledoc",
                                        "Modules should have @moduledoc",
                                        "CODE_SMELL", "MINOR"),
                        new RuleDefinition("S006", "LargeModule",
                                        "Modules should not have too many lines",
                                        "CODE_SMELL", "MAJOR"),
                        new RuleDefinition("S007", "PipeChainStart",
                                        "Pipe chains should start with a raw value",
                                        "CODE_SMELL", "MINOR"),
                        new RuleDefinition("S008", "SingleClauseWith",
                                        "with statements should have more than one clause",
                                        "CODE_SMELL", "MINOR"),
                        new RuleDefinition("S009", "IoInspect",
                                        "IO.inspect calls should be removed",
                                        "CODE_SMELL", "MAJOR"),

                        // Security
                        new RuleDefinition("S101", "HardcodedSecret",
                                        "Credentials should not be hardcoded",
                                        "VULNERABILITY", "BLOCKER"),
                        new RuleDefinition("S102", "SQLInjection",
                                        "SQL queries should not be built using string interpolation",
                                        "VULNERABILITY", "CRITICAL"),
                        new RuleDefinition("S103", "AtomFromUserInput",
                                        "String.to_atom should not be called on user input",
                                        "VULNERABILITY", "CRITICAL"),
                        new RuleDefinition("S104", "UnsafeDeserialization",
                                        ":erlang.binary_to_term should not be used with untrusted input",
                                        "VULNERABILITY", "CRITICAL"),
                        new RuleDefinition("S105", "InsecureHttpClient",
                                        "HTTP requests should use HTTPS",
                                        "VULNERABILITY", "MAJOR"),
                        new RuleDefinition("S106", "WeakCrypto",
                                        "Weak cryptographic algorithms should not be used",
                                        "VULNERABILITY", "CRITICAL"),

                        // Reliability
                        new RuleDefinition("S201", "UnhandledErrorTuple",
                                        "Error tuples should be pattern matched, not ignored",
                                        "BUG", "MAJOR"),
                        new RuleDefinition("S202", "BareRescue",
                                        "rescue clauses should specify exception types",
                                        "BUG", "MAJOR"),
                        new RuleDefinition("S203", "GenServerCallInCallback",
                                        "GenServer.call should not be used inside handle_call",
                                        "BUG", "CRITICAL"));

        @Override
        public void define(Context context) {
                NewRepository repository = context.createRepository(REPOSITORY_KEY, Elixir.KEY)
                                .setName(REPOSITORY_NAME);

                for (RuleDefinition rule : RULES) {
                        NewRule newRule = repository.createRule(rule.key())
                                        .setName(rule.name())
                                        .setHtmlDescription(loadRuleDescription(rule.key()))
                                        .setType(org.sonar.api.rules.RuleType.valueOf(rule.type()))
                                        .setSeverity(rule.severity());

                        newRule.setDebtRemediationFunction(
                                        newRule.debtRemediationFunctions().constantPerIssue("5min"));
                }

                repository.done();
        }

        public static List<String> ruleKeys() {
                return RULES.stream().map(RuleDefinition::key).toList();
        }

        private String loadRuleDescription(String ruleKey) {
                // Descriptions loaded from
                // src/main/resources/org/sonar/l10n/elixir/rules/<key>.html
                var stream = getClass().getResourceAsStream(
                                "/org/sonar/l10n/elixir/rules/" + ruleKey + ".html");
                if (stream == null) {
                        return "<p>No description available.</p>";
                }
                try (stream) {
                        return new String(stream.readAllBytes());
                } catch (Exception e) {
                        return "<p>No description available.</p>";
                }
        }

        private record RuleDefinition(
                        String key, String name, String description, String type, String severity) {
        }
}
