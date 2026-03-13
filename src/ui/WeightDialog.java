package ui;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import alloy.WeightConfig;

public class WeightDialog extends JDialog {

    private final WeightConfig weightConfig;
    private final Map<String, JSpinner> spinners = new HashMap<>();
    private boolean confirmed = false;

    public WeightDialog(Frame parent, List<String> sigNames,
                        WeightConfig existingConfig) {
        super(parent, "Set Weights", true);
        this.weightConfig = existingConfig;

        setSize(400, 120 + sigNames.size() * 55);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // Instructions
        JLabel info = new JLabel(
            "<html><b>Set weight for each sig.</b><br>"
            + "Higher weight = counts more in the score.<br>"
            + "0 = ignore this sig completely.</html>"
        );
        info.setBorder(BorderFactory.createEmptyBorder(12, 15, 8, 15));
        add(info, BorderLayout.NORTH);

        // Spinner grid
        JPanel grid = new JPanel();
        grid.setLayout(new GridLayout(sigNames.size(), 2, 10, 10));
        grid.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        for (String sigName : sigNames) {
            int currentWeight = existingConfig.getWeight(sigName);

            JLabel label = new JLabel(sigName + ":");
            label.setFont(new Font("SansSerif", Font.BOLD, 14));

            JSpinner spinner = new JSpinner(
                new SpinnerNumberModel(currentWeight, 0, 10, 1)
            );
            spinner.setPreferredSize(new Dimension(100, 35));
            spinner.setFont(new Font("SansSerif", Font.PLAIN, 14));

            // Make the text field inside the spinner larger
            JComponent editor = spinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
                tf.setFont(new Font("SansSerif", Font.PLAIN, 14));
                tf.setColumns(4);
            }

            spinners.put(sigName, spinner);
            grid.add(label);
            grid.add(spinner);
        }
        add(grid, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton resetBtn = new JButton("Reset to 1");
        JButton cancelBtn = new JButton("Cancel");
        JButton okBtn = new JButton("Apply");
        okBtn.setBackground(new Color(70, 130, 180));
        okBtn.setForeground(Color.WHITE);
        okBtn.setOpaque(true);

        resetBtn.addActionListener(e -> {
            for (JSpinner s : spinners.values()) {
                s.setValue(1);
            }
        });

        okBtn.addActionListener(e -> {
            for (Map.Entry<String, JSpinner> entry : spinners.entrySet()) {
                weightConfig.setWeight(entry.getKey(),
                    (int) entry.getValue().getValue());
            }
            confirmed = true;
            dispose();
        });

        cancelBtn.addActionListener(e -> dispose());

        buttons.add(resetBtn);
        buttons.add(cancelBtn);
        buttons.add(okBtn);
        add(buttons, BorderLayout.SOUTH);
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}