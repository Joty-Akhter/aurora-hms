package com.easyops.dbversioning;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Executable entry point for running Liquibase migrations packaged inside this
 * JAR. The bundled changelog lives at {@code changelog/master-changelog.xml} on
 * the classpath; everything else (DB host, credentials, contexts, ...) is
 * supplied at runtime via CLI args, system properties, or environment
 * variables.
 *
 * <p>Resolution order for every setting is:
 * <ol>
 *   <li>CLI argument ({@code --url=...} or {@code --url ...})</li>
 *   <li>System property ({@code -Ddb.url=...})</li>
 *   <li>Environment variable ({@code DB_URL})</li>
 *   <li>Built-in default</li>
 * </ol>
 *
 * <p>Examples:
 * <pre>
 *   java -jar easyops-database-versioning.jar update \
 *        --url=jdbc:postgresql://prod-host:5432/easyops \
 *        --username=easyops --password=secret
 *
 *   DB_URL=jdbc:postgresql://staging:5432/easyops \
 *   DB_USERNAME=easyops DB_PASSWORD=secret \
 *     java -jar easyops-database-versioning.jar status
 * </pre>
 */
public final class MigrationRunner {

    private static final String DEFAULT_CHANGELOG = "changelog/master-changelog.xml";
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/easyops";
    private static final String DEFAULT_USERNAME = "easyops";
    private static final String DEFAULT_PASSWORD = "easyops123";

    private MigrationRunner() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (HelpRequested h) {
            printHelp();
            System.exit(0);
        } catch (Exception e) {
            System.err.println();
            System.err.println("Migration FAILED: " + e.getMessage());
            if (Boolean.parseBoolean(System.getProperty("db.debug", "false"))) {
                e.printStackTrace(System.err);
            } else {
                System.err.println("(re-run with -Ddb.debug=true for the full stack trace)");
            }
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        Map<String, String> opts = parseArgs(args);
        if (opts.containsKey("help") || opts.containsKey("h")) {
            throw new HelpRequested();
        }

        String command = opts.getOrDefault("command", "update").toLowerCase(Locale.ROOT);

        String url = resolve(opts, "url", "DB_URL", "db.url", DEFAULT_URL);
        String username = resolve(opts, "username", "DB_USERNAME", "db.username", DEFAULT_USERNAME);
        String password = resolve(opts, "password", "DB_PASSWORD", "db.password", DEFAULT_PASSWORD);
        String changelog = resolve(opts, "changelog", "DB_CHANGELOG", "db.changelog", DEFAULT_CHANGELOG);
        String contexts = resolve(opts, "contexts", "DB_CONTEXTS", "db.contexts", "");
        String labels = resolve(opts, "labels", "DB_LABELS", "db.labels", "");
        String defaultSchema = resolve(opts, "default-schema", "DB_DEFAULT_SCHEMA", "db.defaultSchema", "");
        String liquibaseSchema = resolve(opts, "liquibase-schema", "DB_LIQUIBASE_SCHEMA", "db.liquibaseSchema", "");

        printBanner(command, url, username, changelog, contexts, labels, defaultSchema);

        Class.forName("org.postgresql.Driver");

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            if (!defaultSchema.isEmpty()) {
                database.setDefaultSchemaName(defaultSchema);
            }
            if (!liquibaseSchema.isEmpty()) {
                database.setLiquibaseSchemaName(liquibaseSchema);
            }

            try (Liquibase liquibase = new Liquibase(changelog,
                    new ClassLoaderResourceAccessor(MigrationRunner.class.getClassLoader()),
                    database)) {

                Contexts ctxs = contexts.isEmpty() ? new Contexts() : new Contexts(contexts);
                LabelExpression lbls = labels.isEmpty() ? new LabelExpression() : new LabelExpression(labels);
                PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

                switch (command) {
                    case "update":
                        liquibase.update(ctxs, lbls);
                        break;
                    case "update-sql":
                    case "updatesql":
                        liquibase.update(ctxs, lbls, out);
                        break;
                    case "status":
                        liquibase.reportStatus(true, ctxs, out);
                        break;
                    case "history":
                        liquibase.reportStatus(true, ctxs, out);
                        break;
                    case "validate":
                        liquibase.validate();
                        out.println("Validation succeeded.");
                        break;
                    case "tag": {
                        String tag = required(opts, "tag");
                        liquibase.tag(tag);
                        out.println("Tagged current state as: " + tag);
                        break;
                    }
                    case "rollback": {
                        String tag = required(opts, "tag");
                        liquibase.rollback(tag, ctxs);
                        break;
                    }
                    case "rollback-count":
                    case "rollbackcount": {
                        int count = Integer.parseInt(required(opts, "count"));
                        liquibase.rollback(count, contexts);
                        break;
                    }
                    case "drop-all":
                    case "dropall":
                        liquibase.dropAll();
                        break;
                    case "clear-checksums":
                    case "clearchecksums":
                        liquibase.clearCheckSums();
                        out.println("Cleared all checksums.");
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown command: " + command
                                + " (expected one of: update, update-sql, status, history, validate, "
                                + "tag, rollback, rollback-count, drop-all, clear-checksums)");
                }
                out.flush();
            }
        }

        System.out.println();
        System.out.println("Migration command '" + command + "' completed successfully.");
    }

    private static void printBanner(String command, String url, String username,
                                    String changelog, String contexts, String labels,
                                    String defaultSchema) {
        System.out.println("==============================================");
        System.out.println(" EasyOps ERP - Database Versioning Runner");
        System.out.println("==============================================");
        System.out.println(" Command         : " + command);
        System.out.println(" JDBC URL        : " + url);
        System.out.println(" Username        : " + username);
        System.out.println(" Changelog       : " + changelog);
        System.out.println(" Contexts        : " + (contexts.isEmpty() ? "(all)" : contexts));
        System.out.println(" Labels          : " + (labels.isEmpty() ? "(all)" : labels));
        if (!defaultSchema.isEmpty()) {
            System.out.println(" Default schema  : " + defaultSchema);
        }
        System.out.println("==============================================");
    }

    private static void printHelp() {
        String help = """
                EasyOps ERP - Database Versioning Runner

                Usage:
                  java -jar easyops-database-versioning.jar [COMMAND] [OPTIONS]

                Commands:
                  update              Apply all pending changesets (default)
                  update-sql          Print the SQL that 'update' would run, without executing it
                  status              Show pending changesets
                  history             Same as status (alias)
                  validate            Validate the bundled changelog
                  tag --tag NAME      Tag current DB state with NAME
                  rollback --tag NAME Roll back to the given tag
                  rollback-count --count N
                                      Roll back the last N changesets
                  drop-all            Drop ALL database objects (DANGEROUS)
                  clear-checksums     Clear stored changelog checksums

                Options (CLI / -Dproperty / ENV):
                  --url            -Ddb.url             DB_URL
                  --username       -Ddb.username        DB_USERNAME
                  --password       -Ddb.password        DB_PASSWORD
                  --contexts       -Ddb.contexts        DB_CONTEXTS
                  --labels         -Ddb.labels          DB_LABELS
                  --changelog      -Ddb.changelog       DB_CHANGELOG
                  --default-schema -Ddb.defaultSchema   DB_DEFAULT_SCHEMA
                  --liquibase-schema -Ddb.liquibaseSchema DB_LIQUIBASE_SCHEMA
                  --tag, --count   (command-specific)
                  -Ddb.debug=true  Print full stack traces on failure

                Resolution order: CLI > -D system property > environment variable > default.

                Examples:
                  # Apply migrations to a remote DB
                  java -jar easyops-database-versioning.jar update \\
                       --url=jdbc:postgresql://db.prod:5432/easyops \\
                       --username=easyops --password=********

                  # Same, via env vars
                  DB_URL=jdbc:postgresql://db.prod:5432/easyops \\
                  DB_USERNAME=easyops DB_PASSWORD=secret \\
                    java -jar easyops-database-versioning.jar update

                  # Dry run: show SQL only
                  java -jar easyops-database-versioning.jar update-sql --url=... --username=... --password=...
                """;
        System.out.println(help);
    }

    /**
     * Very small argument parser. Recognizes:
     *   --key value
     *   --key=value
     *   --flag         (boolean flag, value="true")
     * The first non-flag positional token is taken as the command name.
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a == null || a.isEmpty()) {
                continue;
            }
            if (a.startsWith("--")) {
                String body = a.substring(2);
                int eq = body.indexOf('=');
                if (eq > 0) {
                    map.put(body.substring(0, eq), body.substring(eq + 1));
                } else if (i + 1 < args.length && !args[i + 1].startsWith("--")
                        && !isCommandWord(args[i + 1], map)) {
                    map.put(body, args[++i]);
                } else {
                    map.put(body, "true");
                }
            } else if (!map.containsKey("command")) {
                map.put("command", a);
            } else {
                map.put("_extra_" + i, a);
            }
        }
        return map;
    }

    private static boolean isCommandWord(String token, Map<String, String> map) {
        // Heuristic: the first bare word is always treated as the command, never
        // as the value of a preceding --flag. Subsequent bare words could only
        // be values, so allow them.
        return !map.containsKey("command")
                && switch (token.toLowerCase(Locale.ROOT)) {
                    case "update", "update-sql", "updatesql", "status", "history",
                         "validate", "tag", "rollback", "rollback-count",
                         "rollbackcount", "drop-all", "dropall",
                         "clear-checksums", "clearchecksums" -> true;
                    default -> false;
                };
    }

    private static String resolve(Map<String, String> args, String argKey,
                                  String envKey, String sysKey, String defaultValue) {
        String v = args.get(argKey);
        if (isPresent(v)) {
            return v;
        }
        v = System.getProperty(sysKey);
        if (isPresent(v)) {
            return v;
        }
        v = System.getenv(envKey);
        if (isPresent(v)) {
            return v;
        }
        return defaultValue;
    }

    private static boolean isPresent(String v) {
        return v != null && !v.isEmpty();
    }

    private static String required(Map<String, String> opts, String key) {
        String v = opts.get(key);
        if (!isPresent(v)) {
            throw new IllegalArgumentException("Missing required option: --" + key);
        }
        return v;
    }

    /** Internal sentinel to short-circuit out of {@link #run(String[])} on --help. */
    private static final class HelpRequested extends RuntimeException {
        @SuppressWarnings("unused")
        private static final Map<String, String> SUPPRESS = new HashMap<>();
    }
}
