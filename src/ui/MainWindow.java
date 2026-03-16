package ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.*;

import alloy.AlloyModelLoader;
import alloy.AlloyRunner;
import alloy.CommandInfo;
import alloy.DiversitySelector;
import alloy.MemorySafeDiversityRunner;
import alloy.ScopeRunner;
import alloy.TupleCounter;
import alloy.WeightConfig;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4Tuple;
import edu.mit.csail.sdg.translator.A4TupleSet;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

public class MainWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private JLabel fileLabel;
    private JComboBox<String> commandDropdown;
    private JComboBox<String> strategyDropdown;
    private JButton runButton;
    private JPanel resultsPanel;
    private JCheckBox scopeGrowingCheck;
    private JSpinner minScopeSpinner;
    private JSpinner maxScopeSpinner;

    private CompModule loadedModule = null;
    private List<A4Solution> lastSolutions = null;
    private WeightConfig weightConfig = new WeightConfig();

    private final AlloyModelLoader loader = new AlloyModelLoader();
    private final AlloyRunner runner = new AlloyRunner();
    private final TupleCounter counter = new TupleCounter();
    private final DiversitySelector diversitySelector = new DiversitySelector();
    private final ScopeRunner scopeRunner = new ScopeRunner();
    private final MemorySafeDiversityRunner memorySafeRunner =
        new MemorySafeDiversityRunner();

    public MainWindow() {
        setTitle("Alloy Interestingness Explorer");
        setSize(900, 700);
        setMinimumSize(new Dimension(800, 550));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        // Main tabbed pane
        JTabbedPane tabs = new JTabbedPane();

        // Tab 1: Explorer
        JPanel explorerTab = new JPanel(new BorderLayout());
        explorerTab.add(buildTopPanel(), BorderLayout.NORTH);
        explorerTab.add(buildResultsPanel(), BorderLayout.CENTER);
        tabs.addTab("Explorer", explorerTab);

        // Tab 2: Evaluation
        tabs.addTab("Evaluation", new EvaluationPanel());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildTopPanel() {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Row 1: file + weights button
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton browseButton = new JButton("Browse .als file");
        fileLabel = new JLabel("No file selected");
        fileLabel.setForeground(Color.GRAY);
        browseButton.addActionListener(e -> onBrowse());

        JButton weightsButton = new JButton("Set Weights");
        weightsButton.addActionListener(e -> onSetWeights());

        row1.add(browseButton);
        row1.add(fileLabel);
        row1.add(Box.createHorizontalStrut(20));
        row1.add(weightsButton);

        // Row 2: command + strategy + run
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        commandDropdown = new JComboBox<>();
        commandDropdown.setPreferredSize(new Dimension(280, 28));

        strategyDropdown = new JComboBox<>();
        strategyDropdown.addItem("Minimal (fewest tuples)");
        strategyDropdown.addItem("Maximal (most tuples)");
        strategyDropdown.addItem("Diverse");
        strategyDropdown.addItem("Diverse (memory-safe)");
        strategyDropdown.setPreferredSize(new Dimension(220, 28));

        runButton = new JButton("Run");
        runButton.setPreferredSize(new Dimension(80, 28));
        runButton.setEnabled(false);
        runButton.addActionListener(e -> onRun());

        row2.add(new JLabel("Command:"));
        row2.add(commandDropdown);
        row2.add(Box.createHorizontalStrut(10));
        row2.add(new JLabel("Strategy:"));
        row2.add(strategyDropdown);
        row2.add(Box.createHorizontalStrut(10));
        row2.add(runButton);

        // Row 3: scope growing
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        scopeGrowingCheck = new JCheckBox("Scope Growing");
        minScopeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        minScopeSpinner.setPreferredSize(new Dimension(55, 28));
        maxScopeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        maxScopeSpinner.setPreferredSize(new Dimension(55, 28));

        row3.add(scopeGrowingCheck);
        row3.add(new JLabel("Min scope:"));
        row3.add(minScopeSpinner);
        row3.add(new JLabel("Max scope:"));
        row3.add(maxScopeSpinner);

        outer.add(row1);
        outer.add(Box.createVerticalStrut(8));
        outer.add(row2);
        outer.add(Box.createVerticalStrut(8));
        outer.add(row3);
        return outer;
    }

    private JPanel buildResultsPanel() {
        resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        JLabel placeholder = new JLabel(
            "Load a .als file and press Run to see results.",
            SwingConstants.CENTER
        );
        placeholder.setForeground(Color.GRAY);
        resultsPanel.add(placeholder, BorderLayout.CENTER);
        return resultsPanel;
    }

    private void onBrowse() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        String path = chooser.getSelectedFile().getAbsolutePath();
        fileLabel.setText(chooser.getSelectedFile().getName());
        fileLabel.setForeground(Color.BLACK);

        try {
            loadedModule = loader.loadModel(path);
            List<CommandInfo> commands = loader.listCommands(loadedModule);
            commandDropdown.removeAllItems();
            for (CommandInfo cmd : commands) {
                commandDropdown.addItem(cmd.getDescription());
            }
            weightConfig.reset();
            runButton.setEnabled(true);
            showMessage("Model loaded. " + commands.size() + " command(s) found.");
        } catch (Err ex) {
            showError("Failed to load model: " + ex.getMessage());
        }
    }

    private void onSetWeights() {
        if (loadedModule == null) {
            JOptionPane.showMessageDialog(this,
                "Please load a .als file first.");
            return;
        }

        // Get sig names from first solution
        try {
            Command command = loader.getCommand(loadedModule,
                commandDropdown.getSelectedIndex());
            A4Reporter rep = new A4Reporter();
            A4Options opts = new A4Options();
            A4Solution sol = TranslateAlloyToKodkod.execute_command(
                rep, loadedModule.getAllReachableSigs(), command, opts);

            if (!sol.satisfiable()) {
                JOptionPane.showMessageDialog(this, "No solution found to get sigs from.");
                return;
            }

            List<String> sigNames = getUserSigNames(sol);
            WeightDialog dialog = new WeightDialog(this, sigNames, weightConfig);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                StringBuilder sb = new StringBuilder("Weights set: ");
                for (String s : sigNames) {
                    sb.append(s).append("=")
                      .append(weightConfig.getWeight(s)).append(" ");
                }
                showMessage(sb.toString());
            }

        } catch (Exception ex) {
            showError("Error getting sigs: " + ex.getMessage());
        }
    }

    private void onRun() {
        if (loadedModule == null) return;

        int commandIndex = commandDropdown.getSelectedIndex();
        String strategy = (String) strategyDropdown.getSelectedItem();
        boolean useScopeGrowing = scopeGrowingCheck.isSelected();

        showMessage("Running... please wait.");
        runButton.setEnabled(false);

        if (useScopeGrowing) {
            int minScope = (int) minScopeSpinner.getValue();
            int maxScope = (int) maxScopeSpinner.getValue();

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                List<ScopeRunner.ScopeResult> scopeResults;
                String errorMessage;

                @Override
                protected Void doInBackground() {
                	long start = System.currentTimeMillis();
                    try {
                        Command command = loader.getCommand(
                            loadedModule, commandIndex);
                        scopeResults = scopeRunner.runAcrossScopes(
                            loadedModule, command, minScope, maxScope, strategy);
                    } catch (Exception ex) {
                        errorMessage = ex.getMessage();
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("Time: " + (end - start) + " ms");
                    return null;
                }

                @Override
                protected void done() {
                    runButton.setEnabled(true);
                    if (errorMessage != null) {
                        showError("Error: " + errorMessage);
                        return;
                    }
                    showScopeResults(scopeResults, strategy);
                }
            };
            worker.execute();

        } else {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                List<A4Solution> solutions;
                List<MemorySafeDiversityRunner.DiverseResult> diverseResults;
                String errorMessage;

                @Override
                protected Void doInBackground() {
                    try {
                        Command command = loader.getCommand(
                            loadedModule, commandIndex);
                        if (strategy.contains("memory-safe")) {
                            diverseResults = memorySafeRunner.findDiverse(
                                loadedModule, command, 10, 100);
                        } else {
                            solutions = runner.enumerateSolutions(
                                loadedModule, command, 50);
                        }
                    } catch (Exception ex) {
                        errorMessage = ex.getMessage();
                    }
                    return null;
                }

                @Override
                protected void done() {
                    runButton.setEnabled(true);
                    if (errorMessage != null) {
                        showError("Error: " + errorMessage);
                        return;
                    }
                    if (strategy.contains("memory-safe")) {
                        showMemorySafeResults(diverseResults);
                    } else {
                        lastSolutions = solutions;
                        showResults(solutions, strategy);
                    }
                }
            };
            worker.execute();
        }
    }

    // -------------------------------------------------------
    // Normal results
    // -------------------------------------------------------
    private void showResults(List<A4Solution> solutions, String strategy) {
        resultsPanel.removeAll();
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Results"));

        if (solutions.isEmpty()) {
            resultsPanel.add(new JLabel("No solutions found.",
                SwingConstants.CENTER));
            resultsPanel.revalidate();
            resultsPanel.repaint();
            return;
        }

        if (strategy.contains("Diverse")) {
            solutions = diversitySelector.selectDiverse(
                solutions, solutions.size());
        }

        List<String> sigNames = getUserSigNames(solutions.get(0));
        boolean isDiverse = strategy.contains("Diverse");
        int extraCols = isDiverse ? 2 : 1;
        int totalCols = 2 + sigNames.size() + extraCols;

        String[] columns = new String[totalCols];
        columns[0] = "#";
        columns[1] = "Score";
        for (int i = 0; i < sigNames.size(); i++) {
            columns[2 + i] = sigNames.get(i);
        }
        if (isDiverse) columns[totalCols - 2] = "Div.Score";
        columns[totalCols - 1] = "View";

        Object[][] data = new Object[solutions.size()][totalCols];
        for (int i = 0; i < solutions.size(); i++) {
            A4Solution sol = solutions.get(i);
            data[i][0] = i + 1;
            data[i][1] = counter.countUserTuples(sol, weightConfig);
            for (int j = 0; j < sigNames.size(); j++) {
                data[i][2 + j] = getSigCount(sol, sigNames.get(j));
            }
            if (isDiverse) {
                data[i][totalCols - 2] = i == 0 ? "-"
                    : diversitySelector.symmetricDifference(
                        solutions.get(i - 1), sol);
            }
            data[i][totalCols - 1] = "View";
        }

        if (!isDiverse) sortData(data, strategy);

        buildAndShowTable(solutions, data, columns, totalCols,
            solutions.size() + " solutions found. Strategy: " + strategy);
    }

    // -------------------------------------------------------
    // Scope growing results
    // -------------------------------------------------------
    private void showScopeResults(List<ScopeRunner.ScopeResult> scopeResults,
                                   String strategy) {
        resultsPanel.removeAll();
        resultsPanel.setBorder(BorderFactory.createTitledBorder(
            "Results — Scope Growing"));

        if (scopeResults.isEmpty()) {
            resultsPanel.add(new JLabel(
                "No solutions found across any scope.",
                SwingConstants.CENTER));
            resultsPanel.revalidate();
            resultsPanel.repaint();
            return;
        }

        List<String> sigNames = getUserSigNames(scopeResults.get(0).solution);
        int totalCols = 3 + sigNames.size() + 1;

        String[] columns = new String[totalCols];
        columns[0] = "#";
        columns[1] = "Scope";
        columns[2] = "Score";
        for (int i = 0; i < sigNames.size(); i++) {
            columns[3 + i] = sigNames.get(i);
        }
        columns[totalCols - 1] = "View";

        Object[][] data = new Object[scopeResults.size()][totalCols];
        List<A4Solution> solutions = new ArrayList<>();

        for (int i = 0; i < scopeResults.size(); i++) {
            ScopeRunner.ScopeResult r = scopeResults.get(i);
            solutions.add(r.solution);
            data[i][0] = i + 1;
            data[i][1] = r.scope;
            data[i][2] = r.score;
            for (int j = 0; j < sigNames.size(); j++) {
                data[i][3 + j] = getSigCount(r.solution, sigNames.get(j));
            }
            data[i][totalCols - 1] = "View";
        }

        buildAndShowTable(solutions, data, columns, totalCols,
            scopeResults.size() + " scope levels searched. Strategy: "
            + strategy);
    }

    // -------------------------------------------------------
    // Memory-safe diverse results
    // -------------------------------------------------------
    private void showMemorySafeResults(
            List<MemorySafeDiversityRunner.DiverseResult> results) {

        resultsPanel.removeAll();
        resultsPanel.setBorder(BorderFactory.createTitledBorder(
            "Results — Diverse (Memory-Safe)"));

        if (results.isEmpty()) {
            resultsPanel.add(new JLabel("No diverse solutions found.",
                SwingConstants.CENTER));
            resultsPanel.revalidate();
            resultsPanel.repaint();
            return;
        }

        List<String> sigNames = getUserSigNames(results.get(0).solution);
        int totalCols = 2 + sigNames.size() + 2;

        String[] columns = new String[totalCols];
        columns[0] = "#";
        columns[1] = "Score";
        for (int i = 0; i < sigNames.size(); i++) {
            columns[2 + i] = sigNames.get(i);
        }
        columns[totalCols - 2] = "Div.Score";
        columns[totalCols - 1] = "View";

        Object[][] data = new Object[results.size()][totalCols];
        List<A4Solution> solutions = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            MemorySafeDiversityRunner.DiverseResult r = results.get(i);
            solutions.add(r.solution);
            data[i][0] = i + 1;
            data[i][1] = r.score;
            for (int j = 0; j < sigNames.size(); j++) {
                data[i][2 + j] = getSigCount(r.solution, sigNames.get(j));
            }
            data[i][totalCols - 2] = i == 0 ? "-" : r.diversityFromPrevious;
            data[i][totalCols - 1] = "View";
        }

        buildAndShowTable(solutions, data, columns, totalCols,
            results.size() + " diverse instances found (memory-safe).");
    }

    // -------------------------------------------------------
    // Shared table builder
    // -------------------------------------------------------
    private void buildAndShowTable(List<A4Solution> solutions,
                                    Object[][] data,
                                    String[] columns,
                                    int totalCols,
                                    String summaryText) {
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == totalCols - 1;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Monospaced", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));

        table.getColumn("View").setCellRenderer(new ButtonRenderer());
        table.getColumn("View").setCellEditor(new ButtonEditor(solutions));
        table.getColumn("View").setMaxWidth(80);

        JScrollPane scrollPane = new JScrollPane(table);
        resultsPanel.add(scrollPane, BorderLayout.CENTER);

        JLabel summary = new JLabel("  " + summaryText);
        summary.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        resultsPanel.add(summary, BorderLayout.SOUTH);

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    // -------------------------------------------------------
    // Detail window with graph + text + find similar/different
    // -------------------------------------------------------
    private void openDetailWindow(A4Solution solution, int instanceNumber) {
        JDialog dialog = new JDialog(this, "Instance " + instanceNumber, false);
        dialog.setSize(900, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // Graph panel
        GraphPanel graphPanel = new GraphPanel();
        graphPanel.loadSolution(solution);
        graphPanel.setBorder(BorderFactory.createTitledBorder("Graph"));

        // Text panel
        JTextArea text = new JTextArea();
        text.setFont(new Font("Monospaced", Font.PLAIN, 12));
        text.setEditable(false);
        text.setText(buildDetailText(solution, instanceNumber));

        JScrollPane textScroll = new JScrollPane(text);
        textScroll.setBorder(BorderFactory.createTitledBorder("Details"));
        textScroll.setPreferredSize(new Dimension(280, 500));

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, graphPanel, textScroll);
        splitPane.setDividerLocation(580);
        splitPane.setResizeWeight(0.7);

        // Bottom buttons: Find Similar / Find Different
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));

        JButton similarBtn = new JButton("Find Similar");
        similarBtn.setToolTipText(
            "Find instances structurally similar to this one");
        similarBtn.addActionListener(e -> {
            dialog.dispose();
            findSimilarTo(solution);
        });

        JButton differentBtn = new JButton("Find Different");
        differentBtn.setToolTipText(
            "Find instances structurally different from this one");
        differentBtn.addActionListener(e -> {
            dialog.dispose();
            findDifferentFrom(solution);
        });

        buttonPanel.add(similarBtn);
        buttonPanel.add(differentBtn);

        dialog.add(splitPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // Find instances similar to a reference solution
    private void findSimilarTo(A4Solution reference) {
        if (loadedModule == null) return;

        int commandIndex = commandDropdown.getSelectedIndex();
        showMessage("Finding similar instances... please wait.");
        runButton.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<A4Solution> results;
            String errorMessage;

            @Override
            protected Void doInBackground() {
                try {
                    Command command = loader.getCommand(
                        loadedModule, commandIndex);
                    List<A4Solution> all = runner.enumerateSolutions(
                        loadedModule, command, 50);

                    // Sort by similarity to reference (ascending diff = most similar)
                    results = new ArrayList<>(all);
                    results.sort((a, b) -> {
                        int dA = diversitySelector.symmetricDifference(
                            reference, a);
                        int dB = diversitySelector.symmetricDifference(
                            reference, b);
                        return Integer.compare(dA, dB);
                    });

                    // Keep top 10 most similar
                    if (results.size() > 10) {
                        results = results.subList(0, 10);
                    }
                } catch (Exception ex) {
                    errorMessage = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                if (errorMessage != null) {
                    showError("Error: " + errorMessage);
                    return;
                }
                showSimilarResults(results, reference, "Similar");
            }
        };
        worker.execute();
    }

    // Find instances different from a reference solution
    private void findDifferentFrom(A4Solution reference) {
        if (loadedModule == null) return;

        int commandIndex = commandDropdown.getSelectedIndex();
        showMessage("Finding different instances... please wait.");
        runButton.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<A4Solution> results;
            String errorMessage;

            @Override
            protected Void doInBackground() {
                try {
                    Command command = loader.getCommand(
                        loadedModule, commandIndex);
                    List<A4Solution> all = runner.enumerateSolutions(
                        loadedModule, command, 50);

                    // Sort by difference from reference (descending diff = most different)
                    results = new ArrayList<>(all);
                    results.sort((a, b) -> {
                        int dA = diversitySelector.symmetricDifference(
                            reference, a);
                        int dB = diversitySelector.symmetricDifference(
                            reference, b);
                        return Integer.compare(dB, dA);
                    });

                    // Keep top 10 most different
                    if (results.size() > 10) {
                        results = results.subList(0, 10);
                    }
                } catch (Exception ex) {
                    errorMessage = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                if (errorMessage != null) {
                    showError("Error: " + errorMessage);
                    return;
                }
                showSimilarResults(results, reference, "Different");
            }
        };
        worker.execute();
    }

    // Show similar or different results with a "distance" column
    private void showSimilarResults(List<A4Solution> results,
                                     A4Solution reference,
                                     String mode) {
        resultsPanel.removeAll();
        resultsPanel.setBorder(BorderFactory.createTitledBorder(
            "Results — Find " + mode));

        if (results.isEmpty()) {
            resultsPanel.add(new JLabel("No results found.",
                SwingConstants.CENTER));
            resultsPanel.revalidate();
            resultsPanel.repaint();
            return;
        }

        List<String> sigNames = getUserSigNames(results.get(0));
        int totalCols = 2 + sigNames.size() + 2; // # | Score | sigs | Dist | View

        String[] columns = new String[totalCols];
        columns[0] = "#";
        columns[1] = "Score";
        for (int i = 0; i < sigNames.size(); i++) {
            columns[2 + i] = sigNames.get(i);
        }
        columns[totalCols - 2] = "Distance";
        columns[totalCols - 1] = "View";

        Object[][] data = new Object[results.size()][totalCols];
        for (int i = 0; i < results.size(); i++) {
            A4Solution sol = results.get(i);
            data[i][0] = i + 1;
            data[i][1] = counter.countUserTuples(sol, weightConfig);
            for (int j = 0; j < sigNames.size(); j++) {
                data[i][2 + j] = getSigCount(sol, sigNames.get(j));
            }
            data[i][totalCols - 2] = diversitySelector.symmetricDifference(
                reference, sol);
            data[i][totalCols - 1] = "View";
        }

        buildAndShowTable(results, data, columns, totalCols,
            results.size() + " instances found. Mode: Find " + mode);
    }

    private String buildDetailText(A4Solution solution, int instanceNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Instance ").append(instanceNumber).append(" ===\n\n");

        try {
            for (Sig sig : solution.getAllReachableSigs()) {
                if (isBuiltIn(sig)) continue;

                A4TupleSet sigSet = solution.eval(sig);
                String sigName = sig.label.replace("this/", "");
                int weight = weightConfig.getWeight(sigName);

                sb.append("Sig ").append(sigName)
                  .append(" (").append(sigSet.size()).append(" atoms")
                  .append(", weight=").append(weight).append(")\n");

                for (A4Tuple t : sigSet) {
                    sb.append("  ").append(t.atom(0)).append("\n");
                }

                for (Field field : sig.getFields()) {
                    A4TupleSet fieldSet = solution.eval(field);
                    sb.append("  Field ").append(field.label)
                      .append(" (").append(fieldSet.size())
                      .append(" tuples)\n");
                    for (A4Tuple t : fieldSet) {
                        sb.append("    ");
                        for (int i = 0; i < t.arity(); i++) {
                            if (i > 0) sb.append(" -> ");
                            sb.append(t.atom(i));
                        }
                        sb.append("\n");
                    }
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            sb.append("Error: ").append(e.getMessage());
        }

        sb.append("\nWeighted score: ")
          .append(counter.countUserTuples(solution, weightConfig));
        return sb.toString();
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    private List<String> getUserSigNames(A4Solution solution) {
        List<String> names = new ArrayList<>();
        for (Sig sig : solution.getAllReachableSigs()) {
            if (isBuiltIn(sig)) continue;
            names.add(sig.label.replace("this/", ""));
        }
        return names;
    }

    private int getSigCount(A4Solution solution, String sigName) {
        try {
            for (Sig sig : solution.getAllReachableSigs()) {
                if (sig.label.equals("this/" + sigName)) {
                    return solution.eval(sig).size();
                }
            }
        } catch (Exception e) { /* ignore */ }
        return 0;
    }

    private boolean isBuiltIn(Sig sig) {
        String name = sig.label;
        return name.equals("univ") || name.equals("Int")
            || name.equals("seq/Int") || name.equals("String")
            || name.equals("none") || sig.builtin;
    }

    private void sortData(Object[][] data, String strategy) {
        java.util.Arrays.sort(data, (a, b) -> {
            int scoreA = (int) a[1];
            int scoreB = (int) b[1];
            if (strategy.contains("Minimal"))
                return Integer.compare(scoreA, scoreB);
            if (strategy.contains("Maximal"))
                return Integer.compare(scoreB, scoreA);
            return 0;
        });
    }

    private void showMessage(String msg) {
        resultsPanel.removeAll();
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        JLabel label = new JLabel(msg, SwingConstants.CENTER);
        label.setForeground(Color.GRAY);
        resultsPanel.add(label, BorderLayout.CENTER);
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private void showError(String msg) {
        resultsPanel.removeAll();
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        JLabel label = new JLabel(msg, SwingConstants.CENTER);
        label.setForeground(Color.RED);
        resultsPanel.add(label, BorderLayout.CENTER);
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    // -------------------------------------------------------
    // Button renderer and editor
    // -------------------------------------------------------
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            setText("View");
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private final List<A4Solution> solutions;
        private int clickedRow;

        public ButtonEditor(List<A4Solution> solutions) {
            super(new JCheckBox());
            this.solutions = solutions;
            JButton button = new JButton("View");
            button.addActionListener(e -> {
                fireEditingStopped();
                openDetailWindow(solutions.get(clickedRow), clickedRow + 1);
            });
            editorComponent = button;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {
            clickedRow = (int) table.getValueAt(row, 0) - 1;
            return editorComponent;
        }

        @Override
        public Object getCellEditorValue() { return "View"; }
    }

    public static void main(String[] args) {
    	SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}