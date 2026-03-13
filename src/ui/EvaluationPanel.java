package ui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

import alloy.AlloyModelLoader;
import alloy.CommandInfo;
import alloy.EvaluationResult;
import alloy.Evaluator;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;

public class EvaluationPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final AlloyModelLoader loader = new AlloyModelLoader();
    private final Evaluator evaluator = new Evaluator();

    private JLabel fileLabel;
    private JComboBox<String> commandDropdown;
    private JButton runButton;
    private JPanel resultsPanel;

    private CompModule loadedModule = null;

    public EvaluationPanel() {
        setLayout(new BorderLayout(0, 0));
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildResultsPanel(), BorderLayout.CENTER);
    }

    private JPanel buildTopPanel() {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton browseButton = new JButton("Browse .als file");
        fileLabel = new JLabel("No file selected");
        fileLabel.setForeground(Color.GRAY);

        browseButton.addActionListener(e -> onBrowse());
        row1.add(browseButton);
        row1.add(fileLabel);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        commandDropdown = new JComboBox<>();
        commandDropdown.setPreferredSize(new Dimension(280, 28));

        runButton = new JButton("Run Evaluation");
        runButton.setEnabled(false);
        runButton.addActionListener(e -> onRunEvaluation());

        row2.add(new JLabel("Command:"));
        row2.add(commandDropdown);
        row2.add(Box.createHorizontalStrut(10));
        row2.add(runButton);

        JLabel info = new JLabel(
            "  Runs all strategies automatically and compares them."
        );
        info.setForeground(Color.GRAY);
        info.setFont(new Font("SansSerif", Font.ITALIC, 12));

        outer.add(row1);
        outer.add(Box.createVerticalStrut(8));
        outer.add(row2);
        outer.add(Box.createVerticalStrut(4));
        outer.add(info);
        return outer;
    }

    private JPanel buildResultsPanel() {
        resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder(
            "Evaluation Results"));
        JLabel placeholder = new JLabel(
            "Load a .als file and click Run Evaluation to compare all strategies.",
            SwingConstants.CENTER
        );
        placeholder.setForeground(Color.GRAY);
        resultsPanel.add(placeholder, BorderLayout.CENTER);
        return resultsPanel;
    }

    private void onBrowse() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(
            SwingUtilities.getWindowAncestor(this));
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
            runButton.setEnabled(true);
            showMessage("Model loaded. " + commands.size()
                + " command(s) found.");
        } catch (Err ex) {
            showMessage("Error: " + ex.getMessage());
        }
    }

    private void onRunEvaluation() {
        if (loadedModule == null) return;

        int commandIndex = commandDropdown.getSelectedIndex();
        showMessage("Running all strategies... please wait.");
        runButton.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<EvaluationResult> evalResults;
            String errorMessage;

            @Override
            protected Void doInBackground() {
                try {
                    Command command = loader.getCommand(
                        loadedModule, commandIndex);
                    evalResults = evaluator.evaluateAll(loadedModule, command);
                } catch (Exception ex) {
                    errorMessage = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                if (errorMessage != null) {
                    showMessage("Error: " + errorMessage);
                    return;
                }
                showEvaluationResults(evalResults);
            }
        };
        worker.execute();
    }

    private void showEvaluationResults(List<EvaluationResult> results) {
        resultsPanel.removeAll();
        resultsPanel.setBorder(BorderFactory.createTitledBorder(
            "Evaluation Results"));

        if (results.isEmpty()) {
            resultsPanel.add(new JLabel("No results.",
                SwingConstants.CENTER));
            resultsPanel.revalidate();
            resultsPanel.repaint();
            return;
        }

        String[] columns = {
            "Strategy", "Found", "Min Score", "Max Score",
            "Avg Score", "Avg Diversity", "Distinct Structures"
        };

        Object[][] data = new Object[results.size()][7];
        for (int i = 0; i < results.size(); i++) {
            EvaluationResult r = results.get(i);
            data[i][0] = r.strategyName;
            data[i][1] = r.instancesFound;
            data[i][2] = r.minScore;
            data[i][3] = r.maxScore;
            data[i][4] = String.format("%.1f", r.averageScore);
            data[i][5] = String.format("%.1f", r.averageDiversity);
            data[i][6] = r.totalDistinctStructures;
        }

        JTable table = new JTable(data, columns) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table.setRowHeight(35);
        table.setFont(new Font("Monospaced", Font.PLAIN, 13));
        table.getTableHeader().setFont(
            new Font("SansSerif", Font.BOLD, 13));

        // Highlight best value in each column
        highlightBestValues(table, results);

        JScrollPane scrollPane = new JScrollPane(table);
        resultsPanel.add(scrollPane, BorderLayout.CENTER);

        // Summary text below table
        JTextArea summary = new JTextArea();
        summary.setFont(new Font("Monospaced", Font.PLAIN, 12));
        summary.setEditable(false);
        summary.setBackground(new Color(245, 245, 245));
        summary.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        summary.setText(buildSummaryText(results));

        JScrollPane summaryScroll = new JScrollPane(summary);
        summaryScroll.setPreferredSize(new Dimension(900, 120));
        summaryScroll.setBorder(BorderFactory.createTitledBorder("Summary"));
        resultsPanel.add(summaryScroll, BorderLayout.SOUTH);

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    // Highlight the best cell in each numeric column
    private void highlightBestValues(JTable table,
                                      List<EvaluationResult> results) {
        // Best distinct structures = highest
        int bestDistinctRow = 0;
        int bestDistinct = results.get(0).totalDistinctStructures;
        for (int i = 1; i < results.size(); i++) {
            if (results.get(i).totalDistinctStructures > bestDistinct) {
                bestDistinct = results.get(i).totalDistinctStructures;
                bestDistinctRow = i;
            }
        }

        // Best diversity = highest
        int bestDivRow = 0;
        double bestDiv = results.get(0).averageDiversity;
        for (int i = 1; i < results.size(); i++) {
            if (results.get(i).averageDiversity > bestDiv) {
                bestDiv = results.get(i).averageDiversity;
                bestDivRow = i;
            }
        }

        final int finalBestDistinctRow = bestDistinctRow;
        final int finalBestDivRow = bestDivRow;

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int col) {
                Component c = super.getTableCellRendererComponent(
                    t, value, isSelected, hasFocus, row, col);

                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);

                if (!isSelected) {
                    if (col == 6 && row == finalBestDistinctRow) {
                        c.setBackground(new Color(180, 230, 180));
                        c.setForeground(new Color(0, 100, 0));
                        ((JLabel) c).setFont(
                            new Font("Monospaced", Font.BOLD, 13));
                    } else if (col == 5 && row == finalBestDivRow) {
                        c.setBackground(new Color(180, 210, 255));
                        c.setForeground(new Color(0, 0, 150));
                        ((JLabel) c).setFont(
                            new Font("Monospaced", Font.BOLD, 13));
                    }
                }
                return c;
            }
        });
    }

    private String buildSummaryText(List<EvaluationResult> results) {
        StringBuilder sb = new StringBuilder();

        // Find best strategy by distinct structures
        EvaluationResult bestDistinct = results.get(0);
        EvaluationResult bestDiversity = results.get(0);

        for (EvaluationResult r : results) {
            if (r.totalDistinctStructures >
                    bestDistinct.totalDistinctStructures) {
                bestDistinct = r;
            }
            if (r.averageDiversity > bestDiversity.averageDiversity) {
                bestDiversity = r;
            }
        }

        sb.append("Most distinct structures: ")
          .append(bestDistinct.strategyName)
          .append(" (").append(bestDistinct.totalDistinctStructures)
          .append(" unique structures)\n");

        sb.append("Most diverse (avg pairwise distance): ")
          .append(bestDiversity.strategyName)
          .append(" (avg=")
          .append(String.format("%.1f", bestDiversity.averageDiversity))
          .append(")\n");

        sb.append("\nAll strategies:\n");
        for (EvaluationResult r : results) {
            sb.append("  ").append(r.toString()).append("\n");
        }

        return sb.toString();
    }

    private void showMessage(String msg) {
        resultsPanel.removeAll();
        resultsPanel.setBorder(BorderFactory.createTitledBorder(
            "Evaluation Results"));
        JLabel label = new JLabel(msg, SwingConstants.CENTER);
        label.setForeground(Color.GRAY);
        resultsPanel.add(label, BorderLayout.CENTER);
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }
}