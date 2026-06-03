package com.easyops.tools.servicecontrol;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Starts each service with: {@code mvnw -pl libraries/easyops-rbac-client -am install} (internal lib into ~/.m2) then
 * {@code mvnw -f services/&lt;name&gt;/pom.xml spring-boot:run}. A reactor {@code spring-boot:run} runs the plugin on
 * {@code easyops-erp} first (no main class).
 * Log file {@code logs/local-services/&lt;name&gt;.log} is tailed into the UI.
 */
public final class ServiceControlApp extends JFrame {

    private static final String ENV_ROOT = "EASYOPS_ERP_ROOT";
    private static final int LOG_UI_MAX_CHARS = 450_000;
    /** Service name rows in the scroll area (left → right, then next row). */
    private static final int SERVICE_COLUMNS = 2;

    /** Stagger between batch starts so several {@code mvnw} processes do not spawn at once. */
    private static final long BATCH_STAGGER_MS = 1_800L;

    /**
     * Eureka, gateway, RBAC, users, org — typical platform slice (no auth-service here; add manually if needed).
     */
    private static final List<String> BATCH_CORE = List.of(
        "eureka", "api-gateway", "rbac-service", "user-management", "organization-service"
    );

    private static final List<String> BATCH_ACCOUNTING = List.of(
        "accounting-service", "ar-service", "ap-service"
    );

    /** Suggested boot order (any extra modules follow alphabetically). */
    private static final List<String> JVM_PREFERRED_ORDER = List.of(
        "eureka", "api-gateway", "user-management", "auth-service", "rbac-service",
        "organization-service", "notification-service", "monitoring-service",
        "accounting-service", "ar-service", "ap-service", "bank-service", "sales-service",
        "inventory-service", "purchase-service", "crm-service", "hr-service",
        "manufacturing-service", "pharma-service",
        "hospital-service", "hospital-pharmacy-service", "hospital-billing-service",
        "hospital-card-management-service", "hospital-corporate-and-discount-service",
        "hospital-clinical-orders-service", "hospital-scheduling-service"
    );

    private final JTextField rootField = new JTextField(48);
    private final JTextArea logArea = new JTextArea(8, 80);
    private final JPanel servicePanel = new JPanel();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "easyops-service-op");
        t.setDaemon(true);
        return t;
    });

    /** Serializes status polling so timer ticks queue instead of being dropped when a sweep is slow. */
    private final ExecutorService statusExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "easyops-status");
        t.setDaemon(true);
        return t;
    });

    private final Set<String> busyKeys = ConcurrentHashMap.newKeySet();
    private final List<ServiceRow> rows = new ArrayList<>();
    private final AtomicReference<Process> logTailProcess = new AtomicReference<>();
    private final AtomicReference<String> tailedModule = new AtomicReference<>();
    private Timer refreshTimer;

    private ServiceControlApp() {
        super("EasyOps — Service Control (Maven)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Optional<Path> detected = detectRoot();
        detected.ifPresent(p -> rootField.setText(p.toString()));

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        JPanel top = new JPanel(new BorderLayout(8, 4));
        JPanel rootRow = new JPanel(new BorderLayout(8, 0));
        rootRow.add(new JLabel("easyops-erp root:"), BorderLayout.WEST);
        rootRow.add(rootField, BorderLayout.CENTER);
        JButton browse = new JButton("Browse…");
        browse.addActionListener(e -> browseRoot());
        JButton rescan = new JButton("Reload list");
        rescan.addActionListener(e -> rebuildServiceList());
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        east.add(rescan);
        east.add(browse);
        rootRow.add(east, BorderLayout.EAST);
        top.add(rootRow, BorderLayout.NORTH);

        servicePanel.setLayout(new BorderLayout(8, 8));
        servicePanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        JScrollPane serviceScroll = new JScrollPane(servicePanel);
        serviceScroll.getVerticalScrollBar().setUnitIncrement(16);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(880, 320));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, serviceScroll, logScroll);
        split.setResizeWeight(0.38);
        split.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        JLabel hint = new JLabel(
            "<html><body style='width:780px'>"
                + "Each service: <code>mvnw -pl libraries/easyops-rbac-client -am install</code> then "
                + "<code>mvnw -f services/&lt;name&gt;/pom.xml spring-boot:run</code> (profile <code>local</code>). "
                + "Logs: <code>logs/local-services/&lt;name&gt;.log</code>. "
                + "<b>Logs</b> tails the file; <b>Start</b> launches and follows the log. "
                + "Status: green if something is listening on the port from YAML (or PID file if port unknown). "
                + "<b>Stop</b> kills the process on that port (<code>lsof</code> + <code>kill</code>)."
                + "</body></html>");
        hint.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        add(hint, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(1100, 820));
        pack();
        setLocationRelativeTo(null);

        rebuildServiceList();

        refreshTimer = new Timer(1800, e -> refreshStatuses());
        refreshTimer.start();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (refreshTimer != null) {
                    refreshTimer.stop();
                }
                stopLogTail();
                statusExecutor.shutdown();
                executor.shutdown();
            }
        });
    }

    private void browseRoot() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String current = rootField.getText().trim();
        if (!current.isEmpty()) {
            chooser.setCurrentDirectory(Path.of(current).toFile());
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            rootField.setText(chooser.getSelectedFile().getAbsolutePath());
            rebuildServiceList();
        }
    }

    private Optional<Path> validateRoot() {
        String text = rootField.getText().trim();
        if (text.isEmpty()) {
            return Optional.empty();
        }
        Path root = Paths.get(text);
        if (!Files.isDirectory(root) || !Files.isRegularFile(root.resolve("mvnw"))) {
            return Optional.empty();
        }
        return Optional.of(root);
    }

    private void appendLog(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s);
            trimLogUi();
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void trimLogUi() {
        int len = logArea.getDocument().getLength();
        if (len <= LOG_UI_MAX_CHARS) {
            return;
        }
        int excess = len - LOG_UI_MAX_CHARS + 20_000;
        try {
            logArea.replaceRange("", 0, excess);
        } catch (Exception ignored) {
            logArea.setText(logArea.getText().substring(excess));
        }
    }

    private void clearLog() {
        SwingUtilities.invokeLater(() -> logArea.setText(""));
    }

    private void rebuildServiceList() {
        servicePanel.removeAll();
        rows.clear();

        Optional<Path> rootOpt = validateRoot();
        if (rootOpt.isEmpty()) {
            servicePanel.add(new JLabel("Set a valid easyops-erp root (directory containing mvnw)."), BorderLayout.CENTER);
            servicePanel.revalidate();
            servicePanel.repaint();
            return;
        }
        Path root = rootOpt.get();

        List<String> jvmModules = discoverJvmModules(root);
        JLabel header = new JLabel("Spring Boot services (mvnw) — " + jvmModules.size() + " modules");
        header.setFont(header.getFont().deriveFont(Font.BOLD));

        JPanel batchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        batchRow.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        JButton coreBatch = new JButton("Core");
        coreBatch.setToolTipText(
            "Start: eureka, api-gateway, rbac-service, user-management, organization-service");
        styleBatchButton(coreBatch);
        coreBatch.addActionListener(e -> {
            clearLog();
            runJvmStartBatch(root, modulesPresent(BATCH_CORE, jvmModules), "Core");
        });
        JButton accountingBatch = new JButton("Accounting");
        accountingBatch.setToolTipText("Start: accounting-service, ar-service, ap-service");
        styleBatchButton(accountingBatch);
        accountingBatch.addActionListener(e -> {
            clearLog();
            runJvmStartBatch(root, modulesPresent(BATCH_ACCOUNTING, jvmModules), "Accounting");
        });
        List<String> hospitalMods = hospitalModulesFrom(jvmModules);
        JButton hospitalBatch = new JButton("Hospital");
        hospitalBatch.setToolTipText(
            "Start all hospital-* services (" + hospitalMods.size() + " in this checkout)");
        styleBatchButton(hospitalBatch);
        hospitalBatch.addActionListener(e -> {
            clearLog();
            runJvmStartBatch(root, hospitalMods, "Hospital");
        });
        batchRow.add(coreBatch);
        batchRow.add(accountingBatch);
        batchRow.add(hospitalBatch);

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        header.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        batchRow.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        northStack.add(header);
        northStack.add(Box.createVerticalStrut(4));
        northStack.add(batchRow);

        JPanel grid = new JPanel(new GridLayout(0, SERVICE_COLUMNS, 8, 6));
        for (String name : jvmModules) {
            ServiceRow row = new ServiceRow(name);
            rows.add(row);
            grid.add(row.panel);
        }
        servicePanel.add(northStack, BorderLayout.NORTH);
        servicePanel.add(grid, BorderLayout.CENTER);
        servicePanel.revalidate();
        servicePanel.repaint();
        refreshStatuses();
    }

    private static void styleBatchButton(JButton b) {
        Font base = b.getFont();
        b.setFont(base.deriveFont(Font.BOLD, 15f));
        b.setMargin(new Insets(12, 26, 12, 26));
    }

    /** Preserves {@code preferred} order; drops entries not in {@code discovered}. */
    private static List<String> modulesPresent(List<String> preferred, List<String> discovered) {
        Set<String> have = new HashSet<>(discovered);
        List<String> out = new ArrayList<>();
        for (String p : preferred) {
            if (have.contains(p)) {
                out.add(p);
            }
        }
        return out;
    }

    /** All {@code hospital-*} modules in checkout order (same as {@link #discoverJvmModules}). */
    private static List<String> hospitalModulesFrom(List<String> jvmModules) {
        List<String> out = new ArrayList<>();
        for (String m : jvmModules) {
            if (m.startsWith("hospital-")) {
                out.add(m);
            }
        }
        return out;
    }

    /**
     * Starts each module with a stagger so multiple {@code mvnw} JVMs are not forked in the same instant.
     */
    private void runJvmStartBatch(Path root, List<String> modules, String batchName) {
        if (modules.isEmpty()) {
            appendLog("Batch \"" + batchName + "\": no matching services in this checkout.\n");
            return;
        }
        appendLog("\n=== Batch start: " + batchName + " (" + modules.size() + " services, "
            + BATCH_STAGGER_MS + " ms stagger) ===\n");
        for (int i = 0; i < modules.size(); i++) {
            final String mod = modules.get(i);
            final int idx = i;
            executor.submit(() -> {
                try {
                    if (idx > 0) {
                        Thread.sleep(BATCH_STAGGER_MS * (long) idx);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                runJvmStart(root, mod);
            });
        }
    }

    private static List<String> discoverJvmModules(Path root) {
        Path servicesDir = root.resolve("services");
        if (!Files.isDirectory(servicesDir)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        try (var stream = Files.list(servicesDir)) {
            stream.filter(Files::isDirectory)
                .filter(p -> Files.isRegularFile(p.resolve("pom.xml")))
                .forEach(p -> names.add(p.getFileName().toString()));
        } catch (IOException e) {
            return List.of();
        }
        Set<String> seen = new HashSet<>(names);
        List<String> ordered = new ArrayList<>();
        for (String pref : JVM_PREFERRED_ORDER) {
            if (seen.remove(pref)) {
                ordered.add(pref);
            }
        }
        names.sort(Comparator.naturalOrder());
        for (String n : names) {
            if (seen.contains(n)) {
                ordered.add(n);
            }
        }
        return ordered;
    }

    private void refreshStatuses() {
        Optional<Path> rootOpt = validateRoot();
        if (rootOpt.isEmpty()) {
            return;
        }
        Path root = rootOpt.get();
        statusExecutor.submit(() -> {
            for (ServiceRow row : rows) {
                HealthProbe probe = probeServiceHealth(root, row.module);
                SwingUtilities.invokeLater(() -> row.setRunningVisual(probe));
            }
        });
    }

    /**
     * If {@code server.port} is known: up when the port accepts TCP on 127.0.0.1, or the PID in the pid file is alive
     * (covers the gap while the JVM is still starting). If port unknown: {@link #probeJvmRunning} (stale pid cleanup).
     */
    private static HealthProbe probeServiceHealth(Path root, String module) {
        int port = resolveServerPort(root, module);
        String pid = readPidString(root, module);
        if (port > 0) {
            boolean listening = isLocalPortListening(port);
            boolean pidAlive = isPidProcessAlive(pid);
            boolean up = listening || pidAlive;
            return new HealthProbe(up, htmlTip(port, pid, listening, pidAlive, null), rowSubtitle(port, pid));
        }
        try {
            boolean up = probeJvmRunning(root, module);
            pid = readPidString(root, module);
            return new HealthProbe(up, htmlTip(port, pid, false, up, null), rowSubtitle(port, pid));
        } catch (Exception ex) {
            return new HealthProbe(false, htmlTip(port, pid, false, false, ex.getMessage()),
                rowSubtitle(port, pid));
        }
    }

    /** {@code kill -0} without deleting the pid file (used for status only). */
    private static boolean isPidProcessAlive(String pid) {
        if (pid == null || pid.isBlank()) {
            return false;
        }
        try {
            Process p = new ProcessBuilder("/bin/kill", "-0", pid.trim()).redirectErrorStream(true).start();
            return p.waitFor(2, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String rowSubtitle(int port, String pid) {
        String p = port > 0 ? String.valueOf(port) : "?";
        String pids = (pid == null || pid.isBlank()) ? "—" : pid;
        return "Port " + p + " · PID " + pids;
    }

    private static String htmlTip(int port, String pid, boolean listening, boolean pidAlive, String err) {
        String pids = (pid == null || pid.isBlank()) ? "—" : pid;
        String portLine = port > 0 ? String.valueOf(port) : "(not in YAML)";
        String status;
        if (port <= 0) {
            if (err != null) {
                status = "Error: " + err;
            } else if (pidAlive) {
                status = "Running (PID)";
            } else {
                status = "Not running";
            }
        } else if (err != null) {
            status = "Error: " + err;
        } else if (listening) {
            status = "Listening on port";
        } else if (pidAlive) {
            status = "Starting (PID alive, port not open yet)";
        } else {
            status = "Not running";
        }
        return "<html>" + status + "<br>Port: " + portLine + "<br>PID: " + pids + "</html>";
    }

    static String readPidString(Path root, String module) {
        Path f = root.resolve("logs/local-services/pids").resolve(module + ".pid");
        try {
            if (Files.isRegularFile(f)) {
                return Files.readString(f, StandardCharsets.UTF_8).trim();
            }
        } catch (IOException ignored) {
        }
        return "";
    }

    private record HealthProbe(boolean up, String tooltip, String rowSubtitle) {
    }

    /** {@code application-local.yml} overrides {@code application.yml} when present. */
    static int resolveServerPort(Path root, String module) {
        Path res = root.resolve("services").resolve(module).resolve("src/main/resources");
        Path local = res.resolve("application-local.yml");
        if (Files.isRegularFile(local)) {
            int p = readServerPortFromYaml(local);
            if (p > 0) {
                return p;
            }
        }
        Path app = res.resolve("application.yml");
        return readServerPortFromYaml(app);
    }

    /**
     * Reads {@code server.port} from a YAML file (first {@code port:} line inside a top-level {@code server:} block).
     */
    static int readServerPortFromYaml(Path yaml) {
        if (!Files.isRegularFile(yaml)) {
            return -1;
        }
        try {
            List<String> lines = Files.readAllLines(yaml, StandardCharsets.UTF_8);
            boolean inServer = false;
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                char c0 = line.charAt(0);
                if (!Character.isWhitespace(c0)) {
                    inServer = line.stripLeading().startsWith("server:");
                    continue;
                }
                if (inServer) {
                    String s = line.stripLeading();
                    if (s.startsWith("port:")) {
                        String rest = s.substring(5).trim();
                        if (rest.isEmpty()) {
                            continue;
                        }
                        if (rest.charAt(0) == '$' || rest.contains("${")) {
                            return -1;
                        }
                        String digits = rest.split("[^0-9]", 2)[0];
                        if (!digits.isEmpty()) {
                            return Integer.parseInt(digits);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return -1;
        }
        return -1;
    }

    /**
     * Spring Boot binds IPv4 by default; {@link InetAddress#getLoopbackAddress()} is often IPv6 (::1) and fails to
     * connect when nothing listens on IPv6 — use 127.0.0.1 first.
     */
    private static boolean isLocalPortListening(int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 1200);
            return true;
        } catch (IOException e) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(InetAddress.getByName("::1"), port), 800);
                return true;
            } catch (IOException e2) {
                return false;
            }
        }
    }

    /**
     * Kills processes using this port. Uses the same {@code lsof -t -i:PORT} style as
     * {@code scripts/stop-core-spring-services.sh}. Avoid {@code -sTCP:LISTEN}: on macOS it often misses Java/Netty
     * listeners, so nothing was killed.
     */
    static void killListenersOnPort(int port) throws IOException, InterruptedException {
        String lsofPids = "lsof -t -i:" + port + " 2>/dev/null";
        for (int round = 0; round < 3; round++) {
            killPidsFromLsofRound(lsofPids, round == 0);
            Thread.sleep(450);
        }
    }

    private static void killPidsFromLsofRound(String lsofBashSnippet, boolean preferSigTerm) throws IOException, InterruptedException {
        String pids = execCapture(List.of("/bin/bash", "-lc", lsofBashSnippet), 12);
        LinkedHashSet<String> unique = parsePids(pids);
        if (unique.isEmpty()) {
            return;
        }
        String sig = preferSigTerm ? "-15" : "-9";
        for (String pid : unique) {
            new ProcessBuilder("/bin/kill", sig, pid).redirectErrorStream(true).start()
                .waitFor(5, TimeUnit.SECONDS);
        }
    }

    private static LinkedHashSet<String> parsePids(String raw) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String tok : raw.split("\\s+")) {
            String t = tok.trim();
            if (!t.isEmpty() && t.chars().allMatch(Character::isDigit)) {
                out.add(t);
            }
        }
        return out;
    }

    /** Same idea as method 3 in {@code scripts/stop-core-spring-services.sh}. */
    private static void killSpringBootRunByModule(String module) throws IOException, InterruptedException {
        String safe = module.replace("'", "'\"'\"'");
        String script = "pgrep -f '" + safe + ".*spring-boot:run' 2>/dev/null";
        String pids = execCapture(List.of("/bin/bash", "-lc", script), 12);
        for (String pid : parsePids(pids)) {
            new ProcessBuilder("/bin/kill", "-9", pid).redirectErrorStream(true).start()
                .waitFor(5, TimeUnit.SECONDS);
        }
    }

    private static void killPidIfAlive(String pid) throws IOException, InterruptedException {
        if (pid == null || pid.isBlank()) {
            return;
        }
        String p = pid.trim();
        Process pr = new ProcessBuilder("/bin/kill", "-0", p).redirectErrorStream(true).start();
        if (!pr.waitFor(2, TimeUnit.SECONDS) || pr.exitValue() != 0) {
            return;
        }
        new ProcessBuilder("/bin/kill", "-15", p).redirectErrorStream(true).start().waitFor(4, TimeUnit.SECONDS);
        Thread.sleep(200);
        pr = new ProcessBuilder("/bin/kill", "-0", p).redirectErrorStream(true).start();
        if (pr.waitFor(2, TimeUnit.SECONDS) && pr.exitValue() == 0) {
            new ProcessBuilder("/bin/kill", "-9", p).redirectErrorStream(true).start().waitFor(4, TimeUnit.SECONDS);
        }
    }

    private static String execCapture(List<String> command, int timeoutSec) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        p.waitFor(timeoutSec, TimeUnit.SECONDS);
        return out;
    }

    static boolean probeJvmRunning(Path root, String module) throws IOException, InterruptedException {
        Path pidFile = root.resolve("logs/local-services/pids").resolve(module + ".pid");
        if (!Files.isRegularFile(pidFile)) {
            return false;
        }
        String pidStr = Files.readString(pidFile, StandardCharsets.UTF_8).trim();
        if (pidStr.isEmpty()) {
            return false;
        }
        ProcessBuilder pb = new ProcessBuilder("/bin/kill", "-0", pidStr);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean ok = p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0;
        if (!ok && Files.isRegularFile(pidFile)) {
            Files.deleteIfExists(pidFile);
        }
        return ok;
    }

    void stopLogTail() {
        Process prev = logTailProcess.getAndSet(null);
        tailedModule.set(null);
        if (prev != null) {
            prev.destroyForcibly();
        }
    }

    /**
     * Stream new lines from the service log file into the text area ({@code tail -F}).
     */
    void followServiceLog(Path root, String module, boolean clearLogArea) {
        stopLogTail();
        tailedModule.set(module);
        Path logFile = root.resolve("logs/local-services").resolve(module + ".log");
        executor.submit(() -> {
            try {
                Files.createDirectories(logFile.getParent());
                if (!Files.exists(logFile)) {
                    Files.createFile(logFile);
                }
            } catch (IOException e) {
                appendLog("Could not create log file: " + e.getMessage() + "\n");
                return;
            }
            if (clearLogArea) {
                clearLog();
            }
            appendLog("\n──────── " + module + " — " + logFile.toAbsolutePath() + " ────────\n");
            int lp = resolveServerPort(root, module);
            String lpid = readPidString(root, module);
            appendLog("Port: " + (lp > 0 ? lp : "?") + " · PID: " + (lpid.isEmpty() ? "—" : lpid) + "\n");
            Process tailProc = null;
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "tail", "-n", "80", "-F", logFile.toAbsolutePath().toString());
                pb.redirectErrorStream(true);
                tailProc = pb.start();
                logTailProcess.set(tailProc);
                try (var reader = new BufferedReader(
                    new InputStreamReader(tailProc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String L = line + "\n";
                        final Process tp = tailProc;
                        SwingUtilities.invokeLater(() -> {
                            if (tp.equals(logTailProcess.get())) {
                                logArea.append(L);
                                trimLogUi();
                                logArea.setCaretPosition(logArea.getDocument().getLength());
                            }
                        });
                    }
                }
            } catch (Exception ex) {
                if (logTailProcess.get() != null) {
                    appendLog("[log tail ended: " + ex.getMessage() + "]\n");
                }
            } finally {
                if (tailProc != null) {
                    logTailProcess.compareAndSet(tailProc, null);
                }
            }
        });
    }

    void runJvmStart(Path root, String module) {
        String key = "jvm:" + module;
        if (!busyKeys.add(key)) {
            return;
        }
        executor.submit(() -> {
            try {
                Path moduleDir = root.resolve("services").resolve(module);
                if (!Files.isDirectory(moduleDir)) {
                    appendLog("Missing module directory: " + moduleDir + "\n");
                    return;
                }
                Path logDir = root.resolve("logs/local-services");
                Path pidDir = logDir.resolve("pids");
                Files.createDirectories(pidDir);
                Path logFile = logDir.resolve(module + ".log");
                Path pidFile = pidDir.resolve(module + ".pid");
                Path mvnw = root.resolve("mvnw");

                int cfgPort = resolveServerPort(root, module);
                appendLog("\n=== Starting " + module + " (mvnw) ===\n");
                appendLog("Port (application*.yml): " + (cfgPort > 0 ? cfgPort : "unknown") + "\n");
                appendLog("PID file: " + pidFile.toAbsolutePath() + "\n");
                appendLog("Log file: " + logFile.toAbsolutePath() + "\n");

                // 1) Install internal easyops-rbac-client to ~/.m2 (not on Maven Central).
                // 2) spring-boot:run with -f services/<module>/pom.xml only — reactor spring-boot:run hits easyops-erp first.
                String inner = ""
                    + "export SPRING_PROFILES_ACTIVE=local && "
                    + "export EUREKA_INSTANCE_HOSTNAME=localhost && "
                    + "export EUREKA_INSTANCE_PREFER_IP_ADDRESS=false && "
                    + "\"" + mvnw.toAbsolutePath() + "\" -pl libraries/easyops-rbac-client -am install -DskipTests=true && "
                    + "exec \"" + mvnw.toAbsolutePath() + "\" -f \"services/" + module + "/pom.xml\" spring-boot:run "
                    + "-Dspring-boot.run.profiles=local "
                    + "-Dspring-boot.run.jvmArguments=--enable-native-access=ALL-UNNAMED "
                    + "-DskipTests=true "
                    + "-Deureka.instance.hostname=localhost "
                    + "-Deureka.instance.preferIpAddress=false "
                    + ">\"" + logFile.toAbsolutePath() + "\" 2>&1";

                String bash = "( cd \"" + root.toAbsolutePath() + "\" && " + inner + " ) "
                    + "& echo $! > \"" + pidFile.toAbsolutePath() + "\"";

                ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-lc", bash);
                pb.directory(root.toFile());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (var reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        appendLog(line + "\n");
                    }
                }
                p.waitFor(30, TimeUnit.SECONDS);
                appendLog("Launcher shell exit: " + p.exitValue() + " (Maven continues in background if start succeeded)\n");
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                String wrapperPid = readPidString(root, module);
                appendLog("Wrapper PID: " + (wrapperPid.isEmpty() ? "(not in pid file yet — check log)" : wrapperPid) + "\n");
                appendLog("=== End start banner ===\n");

                followServiceLog(root, module, false);
            } catch (Exception ex) {
                appendLog("Error: " + ex.getMessage() + "\n");
            } finally {
                busyKeys.remove(key);
                refreshStatuses();
            }
        });
    }

    void runJvmStop(Path root, String module) {
        String key = "jvm:" + module;
        if (!busyKeys.add(key)) {
            return;
        }
        if (module.equals(tailedModule.get())) {
            stopLogTail();
        }
        executor.submit(() -> {
            try {
                Path pidFile = root.resolve("logs/local-services/pids").resolve(module + ".pid");
                int stopPort = resolveServerPort(root, module);
                appendLog("\n=== Stopping " + module + " ===\n");
                // 1) Port (same as scripts/stop-core-spring-services.sh — lsof -t -i:port, not -sTCP:LISTEN)
                if (stopPort > 0) {
                    appendLog("1) Port " + stopPort + " — lsof -t -i:" + stopPort + "\n");
                    killListenersOnPort(stopPort);
                }
                // 2) PID file (Maven wrapper / subshell)
                if (Files.isRegularFile(pidFile)) {
                    String pidStr = Files.readString(pidFile, StandardCharsets.UTF_8).trim();
                    appendLog("2) PID file → " + pidStr + "\n");
                    killPidIfAlive(pidStr);
                    Files.deleteIfExists(pidFile);
                } else if (stopPort <= 0) {
                    appendLog("No PID file and no port in YAML — trying pgrep only.\n");
                }
                // 3) pgrep spring-boot:run (matches stop-core-spring-services.sh method 3)
                appendLog("3) pgrep -f '" + module + ".*spring-boot:run'\n");
                killSpringBootRunByModule(module);
                appendLog("Done.\n=== End stop ===\n");
            } catch (Exception ex) {
                appendLog("Error: " + ex.getMessage() + "\n");
            } finally {
                busyKeys.remove(key);
                refreshStatuses();
            }
        });
    }

    private static Optional<Path> detectRoot() {
        String env = System.getenv(ENV_ROOT);
        if (env != null && !env.isBlank()) {
            Path p = Paths.get(env.trim());
            if (isEasyOpsRoot(p)) {
                return Optional.of(p);
            }
        }
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 14; i++) {
            if (isEasyOpsRoot(dir)) {
                return Optional.of(dir);
            }
            Path parent = dir.getParent();
            if (parent == null) {
                break;
            }
            dir = parent;
        }
        return Optional.empty();
    }

    private static boolean isEasyOpsRoot(Path dir) {
        return Files.isDirectory(dir)
            && Files.isRegularFile(dir.resolve("mvnw"))
            && Files.isRegularFile(dir.resolve("scripts/start-spring-services.sh"));
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // default L&F
        }
        SwingUtilities.invokeLater(() -> new ServiceControlApp().setVisible(true));
    }

    private final class ServiceRow {
        private final String module;
        private final JLabel statusDot;
        private final JLabel detailLabel;
        private final JButton logsBtn;
        private final JButton action;
        private final JPanel panel;
        private boolean lastRunning;

        ServiceRow(String module) {
            this.module = module;
            this.statusDot = new JLabel("●");
            this.statusDot.setFont(statusDot.getFont().deriveFont(Font.PLAIN, 18f));
            this.statusDot.setForeground(new Color(180, 0, 0));
            this.statusDot.setToolTipText("Stopped");

            JLabel name = new JLabel(module);
            name.setFont(name.getFont().deriveFont(Font.PLAIN, 12f));

            this.detailLabel = new JLabel(" ");
            detailLabel.setFont(detailLabel.getFont().deriveFont(Font.PLAIN, 10f));
            detailLabel.setForeground(new Color(90, 90, 90));

            JPanel nameCol = new JPanel(new BorderLayout(0, 1));
            nameCol.add(name, BorderLayout.NORTH);
            nameCol.add(detailLabel, BorderLayout.SOUTH);
            nameCol.setPreferredSize(new Dimension(200, 38));

            this.logsBtn = new JButton("Logs");
            this.logsBtn.setToolTipText("Show latest log output for this service");
            this.logsBtn.addActionListener(e -> onLogs());

            this.action = new JButton("Start");
            this.action.addActionListener(e -> onStartStop());

            JPanel row = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(2, 4, 2, 6);
            c.gridx = 0;
            c.anchor = GridBagConstraints.NORTH;
            row.add(statusDot, c);
            c.gridx = 1;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            row.add(nameCol, c);
            c.gridx = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            row.add(logsBtn, c);
            c.gridx = 3;
            row.add(action, c);

            this.panel = row;
            this.lastRunning = false;
        }

        void setRunningVisual(HealthProbe probe) {
            this.lastRunning = probe.up();
            statusDot.setForeground(probe.up() ? new Color(0, 140, 40) : new Color(200, 0, 0));
            statusDot.setToolTipText(probe.tooltip());
            detailLabel.setText(probe.rowSubtitle());
            detailLabel.setToolTipText(probe.tooltip());
            action.setText(probe.up() ? "Stop" : "Start");
        }

        private void onLogs() {
            Optional<Path> r = validateRoot();
            if (r.isEmpty()) {
                JOptionPane.showMessageDialog(ServiceControlApp.this,
                    "Set a valid easyops-erp root (directory containing mvnw).",
                    "Invalid root", JOptionPane.WARNING_MESSAGE);
                return;
            }
            followServiceLog(r.get(), module, true);
        }

        private void onStartStop() {
            Optional<Path> r = validateRoot();
            if (r.isEmpty()) {
                JOptionPane.showMessageDialog(ServiceControlApp.this,
                    "Set a valid easyops-erp root (directory containing mvnw).",
                    "Invalid root", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Path rt = r.get();
            if (lastRunning) {
                runJvmStop(rt, module);
            } else {
                clearLog();
                runJvmStart(rt, module);
            }
        }
    }
}
