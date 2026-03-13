package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4Tuple;
import edu.mit.csail.sdg.translator.A4TupleSet;

public class GraphPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    // Position of each atom on screen
    private final Map<String, Point> atomPositions = new LinkedHashMap<>();

    // Color per sig
    private final Map<String, Color> sigColors = new LinkedHashMap<>();

    // Edges to draw: [fromAtom, toAtom, label]
    private final List<String[]> edges = new ArrayList<>();

    // Sig name -> list of atom names
    private final Map<String, List<String>> sigAtoms = new LinkedHashMap<>();

    private static final int NODE_RADIUS = 22;
    private static final Color[] PALETTE = {
        new Color(100, 160, 230),
        new Color(100, 200, 140),
        new Color(230, 160, 80),
        new Color(200, 100, 130),
        new Color(160, 120, 200),
        new Color(80, 200, 200)
    };

    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(480, 400));
    }

    public void loadSolution(A4Solution solution) {
        atomPositions.clear();
        sigColors.clear();
        edges.clear();
        sigAtoms.clear();

        // Collect sigs and atoms
        int colorIndex = 0;
        List<Sig> userSigs = new ArrayList<>();
        for (Sig sig : solution.getAllReachableSigs()) {
            if (isBuiltIn(sig)) continue;
            userSigs.add(sig);
        }

        // Assign colors and collect atoms per sig
        for (Sig sig : userSigs) {
            String sigName = sig.label.replace("this/", "");
            Color color = PALETTE[colorIndex % PALETTE.length];
            sigColors.put(sigName, color);
            colorIndex++;

            List<String> atoms = new ArrayList<>();
            try {
                A4TupleSet sigSet = solution.eval(sig);
                for (A4Tuple t : sigSet) {
                    atoms.add(t.atom(0));
                }
            } catch (Exception e) { /* ignore */ }
            sigAtoms.put(sigName, atoms);
        }

        // Collect edges from fields
        for (Sig sig : userSigs) {
            for (Field field : sig.getFields()) {
                try {
                    A4TupleSet fieldSet = solution.eval(field);
                    for (A4Tuple t : fieldSet) {
                        if (t.arity() >= 2) {
                            edges.add(new String[]{
                                t.atom(0), t.atom(1), field.label
                            });
                        }
                    }
                } catch (Exception e) { /* ignore */ }
            }
        }

        // Layout: arrange each sig's atoms in a group
        layoutAtoms();
        repaint();
    }

    private void layoutAtoms() {
        int panelWidth = Math.max(getWidth(), 480);
        int panelHeight = Math.max(getHeight(), 400);

        List<String> sigNames = new ArrayList<>(sigAtoms.keySet());
        int numSigs = sigNames.size();
        if (numSigs == 0) return;

        // Divide panel into columns per sig
        int colWidth = panelWidth / numSigs;

        for (int s = 0; s < numSigs; s++) {
            String sigName = sigNames.get(s);
            List<String> atoms = sigAtoms.get(sigName);

            int x = colWidth * s + colWidth / 2;
            int numAtoms = atoms.size();

            for (int i = 0; i < numAtoms; i++) {
                int y = 80 + (int)((panelHeight - 120.0) / Math.max(numAtoms, 1) * i)
                        + (int)((panelHeight - 120.0) / Math.max(numAtoms * 2, 1));
                atomPositions.put(atoms.get(i), new Point(x, y));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Relayout in case panel was resized
        layoutAtoms();

        // Draw sig group boxes
        drawSigBoxes(g2);

        // Draw edges
        drawEdges(g2);

        // Draw nodes
        drawNodes(g2);
    }

    private void drawSigBoxes(Graphics2D g2) {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        List<String> sigNames = new ArrayList<>(sigAtoms.keySet());
        int numSigs = sigNames.size();
        if (numSigs == 0) return;

        int colWidth = panelWidth / numSigs;

        for (int s = 0; s < numSigs; s++) {
            String sigName = sigNames.get(s);
            Color color = sigColors.getOrDefault(sigName, Color.LIGHT_GRAY);

            int boxX = colWidth * s + 10;
            int boxY = 30;
            int boxW = colWidth - 20;
            int boxH = panelHeight - 50;

            // Draw light background box
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
            g2.fillRoundRect(boxX, boxY, boxW, boxH, 15, 15);

            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(boxX, boxY, boxW, boxH, 15, 15);

            // Sig label at top of box
            g2.setColor(color.darker());
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int labelX = boxX + (boxW - fm.stringWidth(sigName)) / 2;
            g2.drawString(sigName, labelX, boxY + 18);
        }
    }

    private void drawEdges(Graphics2D g2) {
        g2.setStroke(new BasicStroke(1.8f));

        for (String[] edge : edges) {
            String from = edge[0];
            String to = edge[1];
            String label = edge[2];

            Point pFrom = atomPositions.get(from);
            Point pTo = atomPositions.get(to);
            if (pFrom == null || pTo == null) continue;

            // Draw arrow
            g2.setColor(new Color(80, 80, 80));
            drawArrow(g2, pFrom.x, pFrom.y, pTo.x, pTo.y);

            // Draw label at midpoint
            int mx = (pFrom.x + pTo.x) / 2;
            int my = (pFrom.y + pTo.y) / 2;
            g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
            g2.setColor(new Color(100, 60, 60));
            g2.drawString(label, mx + 5, my - 5);
        }
    }

    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;

        // Shorten line to edge of circles
        double ux = dx / len;
        double uy = dy / len;
        int startX = (int)(x1 + ux * NODE_RADIUS);
        int startY = (int)(y1 + uy * NODE_RADIUS);
        int endX = (int)(x2 - ux * NODE_RADIUS);
        int endY = (int)(y2 - uy * NODE_RADIUS);

        g2.drawLine(startX, startY, endX, endY);

        // Arrowhead
        double angle = Math.atan2(endY - startY, endX - startX);
        int arrowSize = 10;
        int ax1 = (int)(endX - arrowSize * Math.cos(angle - 0.4));
        int ay1 = (int)(endY - arrowSize * Math.sin(angle - 0.4));
        int ax2 = (int)(endX - arrowSize * Math.cos(angle + 0.4));
        int ay2 = (int)(endY - arrowSize * Math.sin(angle + 0.4));

        g2.fillPolygon(new int[]{endX, ax1, ax2}, new int[]{endY, ay1, ay2}, 3);
    }

    private void drawNodes(Graphics2D g2) {
        for (Map.Entry<String, Point> entry : atomPositions.entrySet()) {
            String atom = entry.getKey();
            Point p = entry.getValue();

            // Find sig color for this atom
            Color color = Color.LIGHT_GRAY;
            for (Map.Entry<String, List<String>> sigEntry : sigAtoms.entrySet()) {
                if (sigEntry.getValue().contains(atom)) {
                    color = sigColors.getOrDefault(sigEntry.getKey(), Color.LIGHT_GRAY);
                    break;
                }
            }

            // Draw circle
            g2.setColor(color);
            g2.fillOval(p.x - NODE_RADIUS, p.y - NODE_RADIUS,
                        NODE_RADIUS * 2, NODE_RADIUS * 2);

            g2.setColor(color.darker());
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(p.x - NODE_RADIUS, p.y - NODE_RADIUS,
                        NODE_RADIUS * 2, NODE_RADIUS * 2);

            // Draw atom label
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();

            // Shorten label: Node$0 -> N0
            String shortLabel = shortenAtom(atom);
            int lx = p.x - fm.stringWidth(shortLabel) / 2;
            int ly = p.y + fm.getAscent() / 2 - 1;
            g2.drawString(shortLabel, lx, ly);
        }
    }

    private String shortenAtom(String atom) {
        // "Node$0" -> "N0", "Edge$1" -> "E1"
        if (atom.contains("$")) {
            String[] parts = atom.split("\\$");
            return parts[0].substring(0, 1) + parts[1];
        }
        return atom.length() > 4 ? atom.substring(0, 4) : atom;
    }

    private boolean isBuiltIn(Sig sig) {
        String name = sig.label;
        return name.equals("univ") || name.equals("Int")
            || name.equals("seq/Int") || name.equals("String")
            || name.equals("none") || sig.builtin;
    }
}