package ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

import alloy.ModelLoader;
import alloy.Runner;
import alloy.CmdInfo;
import alloy.DivPicker;
import alloy.StreamDivRunner;
import alloy.ScopeRunner;
import alloy.Counter;
import alloy.Weights;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4Tuple;
import edu.mit.csail.sdg.translator.A4TupleSet;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

public class MainWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private JLabel fileLbl;
    private JComboBox<String> cmdBox;
    private JComboBox<String> stratBox;
    private JButton runBtn;
    private JPanel resPanel;
    private JCheckBox scopeChk;
    private JSpinner minSpin;
    private JSpinner maxSpin;

    private CompModule module = null;
    private List<A4Solution> lastSols = null;
    private Weights weights = new Weights();

    private final ModelLoader loader = new ModelLoader();
    private final Runner runner = new Runner();
    private final Counter counter = new Counter();
    private final DivPicker divPicker = new DivPicker();
    private final ScopeRunner scopeRunner = new ScopeRunner();
    private final StreamDivRunner streamDiv = new StreamDivRunner();

    public MainWindow() {
        setTitle("Alloy Interestingness Explorer");
        setSize(900, 700);
        setMinimumSize(new Dimension(800, 550));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        JTabbedPane tabs = new JTabbedPane();

        JPanel expTab = new JPanel(new BorderLayout());
        expTab.add(buildTopPanel(), BorderLayout.NORTH);
        expTab.add(buildResPanel(), BorderLayout.CENTER);
        tabs.addTab("Explorer", expTab);
        tabs.addTab("Evaluation", new EvalPanel());

        add(tabs, BorderLayout.CENTER);
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

        JButton wBtn = new JButton("Set Weights");
        wBtn.addActionListener(e -> onSetWeights());

        row1.add(browseBtn);
        row1.add(fileLbl);
        row1.add(Box.createHorizontalStrut(20));
        row1.add(wBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cmdBox = new JComboBox<>();
        cmdBox.setPreferredSize(new Dimension(280, 28));

        stratBox = new JComboBox<>();
        stratBox.addItem("Minimal (fewest tuples)");
        stratBox.addItem("Maximal (most tuples)");
        stratBox.addItem("Diverse");
        stratBox.addItem("Diverse (memory-safe)");
        stratBox.setPreferredSize(new Dimension(220, 28));

        runBtn = new JButton("Run");
        runBtn.setPreferredSize(new Dimension(80, 28));
        runBtn.setEnabled(false);
        runBtn.addActionListener(e -> onRun());

        row2.add(new JLabel("Command:"));
        row2.add(cmdBox);
        row2.add(Box.createHorizontalStrut(10));
        row2.add(new JLabel("Strategy:"));
        row2.add(stratBox);
        row2.add(Box.createHorizontalStrut(10));
        row2.add(runBtn);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        scopeChk = new JCheckBox("Scope Growing");
        minSpin = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        minSpin.setPreferredSize(new Dimension(55, 28));
        maxSpin = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        maxSpin.setPreferredSize(new Dimension(55, 28));

        row3.add(scopeChk);
        row3.add(new JLabel("Min scope:"));
        row3.add(minSpin);
        row3.add(new JLabel("Max scope:"));
        row3.add(maxSpin);

        outer.add(row1);
        outer.add(Box.createVerticalStrut(8));
        outer.add(row2);
        outer.add(Box.createVerticalStrut(8));
        outer.add(row3);
        return outer;
    }

    private JPanel buildResPanel() {
        resPanel = new JPanel(new BorderLayout());
        resPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        JLabel ph = new JLabel("Load a .als file and press Run to see results.", SwingConstants.CENTER);
        ph.setForeground(Color.GRAY);
        resPanel.add(ph, BorderLayout.CENTER);
        return resPanel;
    }

    private void onBrowse() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        String path = fc.getSelectedFile().getAbsolutePath();
        fileLbl.setText(fc.getSelectedFile().getName());
        fileLbl.setForeground(Color.BLACK);

        try {
            module = loader.load(path);
            List<CmdInfo> cmds = loader.listCommands(module);
            cmdBox.removeAllItems();
            for (CmdInfo c : cmds)
                cmdBox.addItem(c.getDescription());
            weights.reset();
            runBtn.setEnabled(true);
            showMsg("Model loaded. " + cmds.size() + " command(s) found.");
        } catch (Err ex) {
            showErr("Failed to load model: " + ex.getMessage());
        }
    }

    private void onSetWeights() {
        if (module == null) {
            JOptionPane.showMessageDialog(this, "Please load a .als file first.");
            return;
        }
        try {
            // need a quick solve just to discover available sig names
            Command cmd = loader.getCommand(module, cmdBox.getSelectedIndex());
            A4Solution sol = TranslateAlloyToKodkod.execute_command(
                    new A4Reporter(), module.getAllReachableSigs(), cmd, new A4Options());
            if (!sol.satisfiable()) {
                JOptionPane.showMessageDialog(this, "No solution found to get sigs from.");
                return;
            }
            List<String> sigNames = getUserSigNames(sol);
            WeightDlg dlg = new WeightDlg(this, sigNames, weights);
            dlg.setVisible(true);
            if (dlg.isConfirmed()) {
                StringBuilder sb = new StringBuilder("Weights set: ");
                for (String s : sigNames)
                    sb.append(s).append("=").append(weights.get(s)).append(" ");
                showMsg(sb.toString());
            }
        } catch (Exception ex) {
            showErr("Error getting sigs: " + ex.getMessage());
        }
    }

    private void onRun() {
        if (module == null)
            return;
        int cmdIdx = cmdBox.getSelectedIndex();
        String strat = (String) stratBox.getSelectedItem();
        boolean useScope = scopeChk.isSelected();

        showMsg("Running... please wait.");
        runBtn.setEnabled(false);

        if (useScope) {
            int lo = (int) minSpin.getValue();
            int hi = (int) maxSpin.getValue();

            SwingWorker<Void, Void> w = new SwingWorker<>() {
                List<ScopeRunner.Result> scopeRes;
                String err;

                @Override
                protected Void doInBackground() {
                    long t0 = System.currentTimeMillis();
                    try {
                        Command cmd = loader.getCommand(module, cmdIdx);
                        scopeRes = scopeRunner.run(module, cmd, lo, hi, strat);
                    } catch (Exception ex) {
                        err = ex.getMessage();
                    }
                    System.out.println("Time: " + (System.currentTimeMillis() - t0) + " ms");
                    return null;
                }

                @Override
                protected void done() {
                    runBtn.setEnabled(true);
                    if (err != null) {
                        showErr("Error: " + err);
                        return;
                    }
                    showScopeResults(scopeRes, strat);
                }
            };
            w.execute();
        } else {
            SwingWorker<Void, Void> w = new SwingWorker<>() {
                List<A4Solution> sols;
                List<StreamDivRunner.Result> divRes;
                String err;

                @Override
                protected Void doInBackground() {
                    long t0 = System.currentTimeMillis();
                    try {
                        Command cmd = loader.getCommand(module, cmdIdx);
                        if (strat.contains("memory-safe"))
                            divRes = streamDiv.findDiverse(module, cmd, 10, 100);
                        else
                            sols = runner.enumerate(module, cmd, 50);
                    } catch (Exception ex) {
                        err = ex.getMessage();
                    }
                    // debug stats
                    long t1 = System.currentTimeMillis();
                    System.out.println("TIME: " + strat + " = " + (t1 - t0) + " ms");
                    Runtime rt = Runtime.getRuntime();
                    long mb = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
                    System.out.println("MEMORY: " + strat + " = " + mb + " MB");
                    return null;
                }

                @Override
                protected void done() {
                    runBtn.setEnabled(true);
                    if (err != null) {
                        showErr("Error: " + err);
                        return;
                    }
                    if (strat.contains("memory-safe"))
                        showStreamResults(divRes);
                    else {
                        lastSols = sols;
                        showResults(sols, strat);
                    }
                }
            };
            w.execute();
        }
    }

    private void showResults(List<A4Solution> sols, String strat) {
        resPanel.removeAll();
        resPanel.setBorder(BorderFactory.createTitledBorder("Results"));

        if (sols.isEmpty()) {
            resPanel.add(new JLabel("No solutions found.", SwingConstants.CENTER));
            resPanel.revalidate();
            resPanel.repaint();
            return;
        }

        if (strat.contains("Diverse"))
            sols = divPicker.selectDiverse(sols, sols.size());

        List<String> sigNames = getUserSigNames(sols.get(0));
        boolean isDiv = strat.contains("Diverse");
        int extra = isDiv ? 2 : 1; // diverse adds a Div.Score column
        int cols = 2 + sigNames.size() + extra;

        String[] hdr = new String[cols];
        hdr[0] = "#";
        hdr[1] = "Score";
        for (int i = 0; i < sigNames.size(); i++)
            hdr[2 + i] = sigNames.get(i);
        if (isDiv)
            hdr[cols - 2] = "Div.Score";
        hdr[cols - 1] = "View";

        Object[][] data = new Object[sols.size()][cols];
        for (int i = 0; i < sols.size(); i++) {
            A4Solution s = sols.get(i);
            data[i][0] = i + 1;
            data[i][1] = counter.countUserTuples(s, weights);
            for (int j = 0; j < sigNames.size(); j++)
                data[i][2 + j] = getSigCount(s, sigNames.get(j));
            if (isDiv)
                data[i][cols - 2] = i == 0 ? "-" : divPicker.symDiff(sols.get(i - 1), s);
            data[i][cols - 1] = "View";
        }

        if (!isDiv)
            sortData(data, strat);
        buildTable(sols, data, hdr, cols, sols.size() + " solutions found. Strategy: " + strat);
    }

    private void showScopeResults(List<ScopeRunner.Result> res, String strat) {
        resPanel.removeAll();
        resPanel.setBorder(BorderFactory.createTitledBorder("Results — Scope Growing"));

        if (res.isEmpty()) {
            resPanel.add(new JLabel("No solutions found across any scope.", SwingConstants.CENTER));
            resPanel.revalidate();
            resPanel.repaint();
            return;
        }

        List<String> sigNames = getUserSigNames(res.get(0).solution);
        int cols = 3 + sigNames.size() + 1;

        String[] hdr = new String[cols];
        hdr[0] = "#";
        hdr[1] = "Scope";
        hdr[2] = "Score";
        for (int i = 0; i < sigNames.size(); i++)
            hdr[3 + i] = sigNames.get(i);
        hdr[cols - 1] = "View";

        Object[][] data = new Object[res.size()][cols];
        List<A4Solution> sols = new ArrayList<>();
        for (int i = 0; i < res.size(); i++) {
            ScopeRunner.Result r = res.get(i);
            sols.add(r.solution);
            data[i][0] = i + 1;
            data[i][1] = r.scope;
            data[i][2] = r.score;
            for (int j = 0; j < sigNames.size(); j++)
                data[i][3 + j] = getSigCount(r.solution, sigNames.get(j));
            data[i][cols - 1] = "View";
        }

        buildTable(sols, data, hdr, cols,
                res.size() + " scope levels searched. Strategy: " + strat);
    }

    private void showStreamResults(List<StreamDivRunner.Result> res) {
        resPanel.removeAll();
        resPanel.setBorder(BorderFactory.createTitledBorder("Results — Diverse (Memory-Safe)"));

        if (res.isEmpty()) {
            resPanel.add(new JLabel("No diverse solutions found.", SwingConstants.CENTER));
            resPanel.revalidate();
            resPanel.repaint();
            return;
        }

        List<String> sigNames = getUserSigNames(res.get(0).solution);
        int cols = 2 + sigNames.size() + 2;

        String[] hdr = new String[cols];
        hdr[0] = "#";
        hdr[1] = "Score";
        for (int i = 0; i < sigNames.size(); i++)
            hdr[2 + i] = sigNames.get(i);
        hdr[cols - 2] = "Div.Score";
        hdr[cols - 1] = "View";

        Object[][] data = new Object[res.size()][cols];
        List<A4Solution> sols = new ArrayList<>();
        for (int i = 0; i < res.size(); i++) {
            StreamDivRunner.Result r = res.get(i);
            sols.add(r.solution);
            data[i][0] = i + 1;
            data[i][1] = r.score;
            for (int j = 0; j < sigNames.size(); j++)
                data[i][2 + j] = getSigCount(r.solution, sigNames.get(j));
            data[i][cols - 2] = i == 0 ? "-" : r.divFromPrev;
            data[i][cols - 1] = "View";
        }

        buildTable(sols, data, hdr, cols,
                res.size() + " diverse instances found (memory-safe).");
    }

    private void buildTable(List<A4Solution> sols, Object[][] data,
            String[] hdr, int cols, String summary) {
        DefaultTableModel model = new DefaultTableModel(data, hdr) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == cols - 1;
            }
        };

        JTable tbl = new JTable(model);
        tbl.setRowHeight(30);
        tbl.setFont(new Font("Monospaced", Font.PLAIN, 13));
        tbl.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        tbl.getColumn("View").setCellRenderer(new BtnRenderer());
        tbl.getColumn("View").setCellEditor(new BtnEditor(sols));
        tbl.getColumn("View").setMaxWidth(80);

        resPanel.add(new JScrollPane(tbl), BorderLayout.CENTER);
        JLabel lbl = new JLabel("  " + summary);
        lbl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        resPanel.add(lbl, BorderLayout.SOUTH);
        resPanel.revalidate();
        resPanel.repaint();
    }

    private void openDetail(A4Solution sol, int num) {
        JDialog dlg = new JDialog(this, "Instance " + num, false);
        dlg.setSize(900, 560);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        GraphPanel gp = new GraphPanel();
        gp.loadSolution(sol);
        gp.setBorder(BorderFactory.createTitledBorder("Graph"));

        JTextArea txt = new JTextArea();
        txt.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txt.setEditable(false);
        txt.setText(detailText(sol, num));

        JScrollPane txtScroll = new JScrollPane(txt);
        txtScroll.setBorder(BorderFactory.createTitledBorder("Details"));
        txtScroll.setPreferredSize(new Dimension(280, 500));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gp, txtScroll);
        split.setDividerLocation(580);
        split.setResizeWeight(0.7);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));

        JButton simBtn = new JButton("Find Similar");
        simBtn.setToolTipText("Find instances structurally similar to this one");
        simBtn.addActionListener(e -> {
            dlg.dispose();
            findRelated(sol, true);
        });

        JButton diffBtn = new JButton("Find Different");
        diffBtn.setToolTipText("Find instances structurally different from this one");
        diffBtn.addActionListener(e -> {
            dlg.dispose();
            findRelated(sol, false);
        });

        btns.add(simBtn);
        btns.add(diffBtn);

        dlg.add(split, BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void findRelated(A4Solution ref, boolean similar) {
        if (module == null)
            return;
        int cmdIdx = cmdBox.getSelectedIndex();
        showMsg("Finding " + (similar ? "similar" : "different") + " instances... please wait.");
        runBtn.setEnabled(false);

        SwingWorker<Void, Void> w = new SwingWorker<>() {
            List<A4Solution> res;
            String err;

            @Override
            protected Void doInBackground() {
                try {
                    Command cmd = loader.getCommand(module, cmdIdx);
                    List<A4Solution> all = runner.enumerate(module, cmd, 50);
                    res = new ArrayList<>(all);
                    // ascending dist = most similar first, descending = most different
                    res.sort((a, b) -> {
                        int dA = divPicker.symDiff(ref, a);
                        int dB = divPicker.symDiff(ref, b);
                        return similar ? Integer.compare(dA, dB) : Integer.compare(dB, dA);
                    });
                    if (res.size() > 10)
                        res = res.subList(0, 10); // top 10
                } catch (Exception ex) {
                    err = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                runBtn.setEnabled(true);
                if (err != null) {
                    showErr("Error: " + err);
                    return;
                }
                showRelatedResults(res, ref, similar ? "Similar" : "Different");
            }
        };
        w.execute();
    }

    private void showRelatedResults(List<A4Solution> res, A4Solution ref, String mode) {
        resPanel.removeAll();
        resPanel.setBorder(BorderFactory.createTitledBorder("Results — Find " + mode));

        if (res.isEmpty()) {
            resPanel.add(new JLabel("No results found.", SwingConstants.CENTER));
            resPanel.revalidate();
            resPanel.repaint();
            return;
        }

        List<String> sigNames = getUserSigNames(res.get(0));
        int cols = 2 + sigNames.size() + 2;

        String[] hdr = new String[cols];
        hdr[0] = "#";
        hdr[1] = "Score";
        for (int i = 0; i < sigNames.size(); i++)
            hdr[2 + i] = sigNames.get(i);
        hdr[cols - 2] = "Distance";
        hdr[cols - 1] = "View";

        Object[][] data = new Object[res.size()][cols];
        for (int i = 0; i < res.size(); i++) {
            A4Solution s = res.get(i);
            data[i][0] = i + 1;
            data[i][1] = counter.countUserTuples(s, weights);
            for (int j = 0; j < sigNames.size(); j++)
                data[i][2 + j] = getSigCount(s, sigNames.get(j));
            data[i][cols - 2] = divPicker.symDiff(ref, s);
            data[i][cols - 1] = "View";
        }

        buildTable(res, data, hdr, cols, res.size() + " instances found. Mode: Find " + mode);
    }

    private String detailText(A4Solution sol, int num) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Instance ").append(num).append(" ===\n\n");
        try {
            for (Sig sig : sol.getAllReachableSigs()) {
                if (builtIn(sig))
                    continue;
                A4TupleSet sigSet = sol.eval(sig);
                String sn = sig.label.replace("this/", "");
                sb.append("Sig ").append(sn).append(" (").append(sigSet.size())
                        .append(" atoms, weight=").append(weights.get(sn)).append(")\n");
                for (A4Tuple t : sigSet)
                    sb.append("  ").append(t.atom(0)).append("\n");
                for (Field f : sig.getFields()) {
                    A4TupleSet fs = sol.eval(f);
                    sb.append("  Field ").append(f.label).append(" (").append(fs.size()).append(" tuples)\n");
                    for (A4Tuple t : fs) {
                        sb.append("    ");
                        for (int i = 0; i < t.arity(); i++) {
                            if (i > 0)
                                sb.append(" -> ");
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
        sb.append("\nWeighted score: ").append(counter.countUserTuples(sol, weights));
        return sb.toString();
    }

    private List<String> getUserSigNames(A4Solution sol) {
        List<String> names = new ArrayList<>();
        for (Sig sig : sol.getAllReachableSigs()) {
            if (builtIn(sig))
                continue;
            names.add(sig.label.replace("this/", ""));
        }
        return names;
    }

    private int getSigCount(A4Solution sol, String name) {
        try {
            for (Sig sig : sol.getAllReachableSigs())
                if (sig.label.equals("this/" + name))
                    return sol.eval(sig).size();
        } catch (Exception e) {
        }
        return 0;
    }

    private boolean builtIn(Sig sig) {
        String n = sig.label;
        return n.equals("univ") || n.equals("Int") || n.equals("seq/Int")
                || n.equals("String") || n.equals("none") || sig.builtin;
    }

    private void sortData(Object[][] data, String strat) {
        java.util.Arrays.sort(data, (a, b) -> {
            int sa = (int) a[1], sb = (int) b[1];
            if (strat.contains("Minimal"))
                return Integer.compare(sa, sb);
            if (strat.contains("Maximal"))
                return Integer.compare(sb, sa);
            return 0;
        });
    }

    private void showMsg(String msg) {
        resPanel.removeAll();
        resPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        JLabel lbl = new JLabel(msg, SwingConstants.CENTER);
        lbl.setForeground(Color.GRAY);
        resPanel.add(lbl, BorderLayout.CENTER);
        resPanel.revalidate();
        resPanel.repaint();
    }

    private void showErr(String msg) {
        resPanel.removeAll();
        resPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        JLabel lbl = new JLabel(msg, SwingConstants.CENTER);
        lbl.setForeground(Color.RED);
        resPanel.add(lbl, BorderLayout.CENTER);
        resPanel.revalidate();
        resPanel.repaint();
    }

    class BtnRenderer extends JButton implements TableCellRenderer {
        BtnRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            setText("View");
            return this;
        }
    }

    class BtnEditor extends DefaultCellEditor {
        private final List<A4Solution> sols;
        private int row;

        BtnEditor(List<A4Solution> sols) {
            super(new JCheckBox()); // DefaultCellEditor needs a component; we replace it below
            this.sols = sols;
            JButton b = new JButton("View");
            b.addActionListener(e -> {
                fireEditingStopped();
                openDetail(sols.get(row), row + 1);
            });
            editorComponent = b;
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val, boolean sel, int r, int c) {
            row = (int) t.getValueAt(r, 0) - 1;
            return editorComponent;
        }

        @Override
        public Object getCellEditorValue() {
            return "View";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
