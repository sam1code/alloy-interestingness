package ui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

import alloy.ModelLoader;
import alloy.CmdInfo;
import alloy.EvalResult;
import alloy.Evaluator;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;

public class EvalPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ModelLoader loader = new ModelLoader();
    private final Evaluator evaluator = new Evaluator();

    private JLabel fileLbl;
    private JComboBox<String> cmdBox;
    private JButton runBtn;
    private JPanel resPanel;

    private CompModule module = null;

    public EvalPanel() {
        setLayout(new BorderLayout(0, 0));
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildResPanel(), BorderLayout.CENTER);
    }

    private JPanel buildTopPanel() {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton browseBtn = new JButton("Browse .als file");
        fileLbl = new JLabel("No file selected");
        fileLbl.setForeground(Color.GRAY);
        browseBtn.addActionListener(e -> onBrowse());
        row1.add(browseBtn);
        row1.add(fileLbl);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cmdBox = new JComboBox<>();
        cmdBox.setPreferredSize(new Dimension(280, 28));
        runBtn = new JButton("Run Evaluation");
        runBtn.setEnabled(false);
        runBtn.addActionListener(e -> onRun());
        row2.add(new JLabel("Command:"));
        row2.add(cmdBox);
        row2.add(Box.createHorizontalStrut(10));
        row2.add(runBtn);

        JLabel info = new JLabel("  Runs all strategies automatically and compares them.");
        info.setForeground(Color.GRAY);
        info.setFont(new Font("SansSerif", Font.ITALIC, 12));

        outer.add(row1);
        outer.add(Box.createVerticalStrut(8));
        outer.add(row2);
        outer.add(Box.createVerticalStrut(4));
        outer.add(info);
        return outer;
    }

    private JPanel buildResPanel() {
        resPanel = new JPanel(new BorderLayout());
        resPanel.setBorder(BorderFactory.createTitledBorder("Evaluation Results"));
        JLabel ph = new JLabel(
            "Load a .als file and click Run Evaluation to compare all strategies.",
            SwingConstants.CENTER);
        ph.setForeground(Color.GRAY);
        resPanel.add(ph, BorderLayout.CENTER);
        return resPanel;
    }

    private void onBrowse() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(SwingUtilities.getWindowAncestor(this)) != JFileChooser.APPROVE_OPTION)
            return;

        String path = fc.getSelectedFile().getAbsolutePath();
        fileLbl.setText(fc.getSelectedFile().getName());
        fileLbl.setForeground(Color.BLACK);

        try {
            module = loader.load(path);
            List<CmdInfo> cmds = loader.listCommands(module);
            cmdBox.removeAllItems();
            for (CmdInfo c : cmds) cmdBox.addItem(c.getDescription());
            runBtn.setEnabled(true);
            showMsg("Model loaded. " + cmds.size() + " command(s) found.");
        } catch (Err ex) {
            showMsg("Error: " + ex.getMessage());
        }
    }

    private void onRun() {
        if (module == null) return;
        int idx = cmdBox.getSelectedIndex();
        showMsg("Running all strategies... please wait.");
        runBtn.setEnabled(false);

        SwingWorker<Void, Void> w = new SwingWorker<>() {
            List<EvalResult> results;
            String err;

            @Override
            protected Void doInBackground() {
                try {
                    Command cmd = loader.getCommand(module, idx);
                    results = evaluator.evaluateAll(module, cmd);
                } catch (Exception ex) { err = ex.getMessage(); }
                return null;
            }

            @Override
            protected void done() {
                runBtn.setEnabled(true);
                if (err != null) { showMsg("Error: " + err); return; }
                showResults(results);
            }
        };
        w.execute();
    }

    private void showResults(List<EvalResult> results) {
        resPanel.removeAll();
        resPanel.setBorder(BorderFactory.createTitledBorder("Evaluation Results"));

        if (results.isEmpty()) {
            resPanel.add(new JLabel("No results.", SwingConstants.CENTER));
            resPanel.revalidate(); resPanel.repaint();
            return;
        }

        String[] cols = {"Strategy", "Found", "Min Score", "Max Score",
            "Avg Score", "Avg Diversity", "Distinct Structures"};

        Object[][] data = new Object[results.size()][7];
        for (int i = 0; i < results.size(); i++) {
            EvalResult r = results.get(i);
            data[i][0] = r.strategy;
            data[i][1] = r.found;
            data[i][2] = r.minScore;
            data[i][3] = r.maxScore;
            data[i][4] = String.format("%.1f", r.avgScore);
            data[i][5] = String.format("%.1f", r.avgDiv);
            data[i][6] = r.distinct;
        }

        JTable tbl = new JTable(data, cols) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tbl.setRowHeight(35);
        tbl.setFont(new Font("Monospaced", Font.PLAIN, 13));
        tbl.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));

        highlightBest(tbl, results);

        resPanel.add(new JScrollPane(tbl), BorderLayout.CENTER);

        JTextArea summary = new JTextArea();
        summary.setFont(new Font("Monospaced", Font.PLAIN, 12));
        summary.setEditable(false);
        summary.setBackground(new Color(245, 245, 245));
        summary.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        summary.setText(summaryText(results));

        JScrollPane sp = new JScrollPane(summary);
        sp.setPreferredSize(new Dimension(900, 120));
        sp.setBorder(BorderFactory.createTitledBorder("Summary"));
        resPanel.add(sp, BorderLayout.SOUTH);

        resPanel.revalidate(); resPanel.repaint();
    }

    private void highlightBest(JTable tbl, List<EvalResult> results) {
        int bestDistRow = 0, bestDivRow = 0;
        int bestDist = results.get(0).distinct;
        double bestDiv = results.get(0).avgDiv;

        for (int i = 1; i < results.size(); i++) {
            if (results.get(i).distinct > bestDist) {
                bestDist = results.get(i).distinct;
                bestDistRow = i;
            }
            if (results.get(i).avgDiv > bestDiv) {
                bestDiv = results.get(i).avgDiv;
                bestDivRow = i;
            }
        }

        final int dRow = bestDistRow, vRow = bestDivRow;

        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
                if (!sel) {
                    if (col == 6 && row == dRow) { // "Distinct Structures" col
                        c.setBackground(new Color(180, 230, 180));
                        c.setForeground(new Color(0, 100, 0));
                        ((JLabel) c).setFont(new Font("Monospaced", Font.BOLD, 13));
                    } else if (col == 5 && row == vRow) { // "Avg Diversity" col
                        c.setBackground(new Color(180, 210, 255));
                        c.setForeground(new Color(0, 0, 150));
                        ((JLabel) c).setFont(new Font("Monospaced", Font.BOLD, 13));
                    }
                }
                return c;
            }
        });
    }

    private String summaryText(List<EvalResult> results) {
        StringBuilder sb = new StringBuilder();
        EvalResult bestDist = results.get(0), bestDiv = results.get(0);
        for (EvalResult r : results) {
            if (r.distinct > bestDist.distinct) bestDist = r;
            if (r.avgDiv > bestDiv.avgDiv) bestDiv = r;
        }
        sb.append("Most distinct structures: ").append(bestDist.strategy)
          .append(" (").append(bestDist.distinct).append(" unique structures)\n");
        sb.append("Most diverse (avg pairwise distance): ").append(bestDiv.strategy)
          .append(" (avg=").append(String.format("%.1f", bestDiv.avgDiv)).append(")\n");
        sb.append("\nAll strategies:\n");
        for (EvalResult r : results)
            sb.append("  ").append(r.toString()).append("\n");
        return sb.toString();
    }

    private void showMsg(String msg) {
        resPanel.removeAll();
        resPanel.setBorder(BorderFactory.createTitledBorder("Evaluation Results"));
        JLabel lbl = new JLabel(msg, SwingConstants.CENTER);
        lbl.setForeground(Color.GRAY);
        resPanel.add(lbl, BorderLayout.CENTER);
        resPanel.revalidate(); resPanel.repaint();
    }
}
