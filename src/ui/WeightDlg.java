package ui;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import alloy.Weights;

public class WeightDlg extends JDialog {

    private final Weights weights;
    private final Map<String, JSpinner> spinners = new HashMap<>();
    private boolean confirmed = false;

    public WeightDlg(Frame parent, List<String> sigNames, Weights existing) {
        super(parent, "Set Weights", true);
        this.weights = existing;

        setSize(400, 120 + sigNames.size() * 55);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JLabel info = new JLabel(
            "<html><b>Set weight for each sig.</b><br>"
            + "Higher weight = counts more in the score.<br>"
            + "0 = ignore this sig completely.</html>");
        info.setBorder(BorderFactory.createEmptyBorder(12, 15, 8, 15));
        add(info, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(sigNames.size(), 2, 10, 10));
        grid.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        for (String sn : sigNames) {
            int cur = existing.get(sn);
            JLabel lbl = new JLabel(sn + ":");
            lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
            JSpinner sp = new JSpinner(new SpinnerNumberModel(cur, 0, 10, 1));
            sp.setPreferredSize(new Dimension(100, 35));
            sp.setFont(new Font("SansSerif", Font.PLAIN, 14));
            JComponent ed = sp.getEditor();
            if (ed instanceof JSpinner.DefaultEditor) {
                JTextField tf = ((JSpinner.DefaultEditor) ed).getTextField();
                tf.setFont(new Font("SansSerif", Font.PLAIN, 14));
                tf.setColumns(4);
            }
            spinners.put(sn, sp);
            grid.add(lbl);
            grid.add(sp);
        }
        add(grid, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton resetBtn = new JButton("Reset to 1");
        JButton cancelBtn = new JButton("Cancel");
        JButton okBtn = new JButton("Apply");
        okBtn.setBackground(new Color(70, 130, 180));
        okBtn.setForeground(Color.WHITE);
        okBtn.setOpaque(true);

        resetBtn.addActionListener(e -> spinners.values().forEach(s -> s.setValue(1)));
        okBtn.addActionListener(e -> {
            for (Map.Entry<String, JSpinner> en : spinners.entrySet())
                weights.set(en.getKey(), (int) en.getValue().getValue());
            confirmed = true;
            dispose();
        });
        cancelBtn.addActionListener(e -> dispose());

        btns.add(resetBtn);
        btns.add(cancelBtn);
        btns.add(okBtn);
        add(btns, BorderLayout.SOUTH);
    }

    public boolean isConfirmed() { return confirmed; }
}
