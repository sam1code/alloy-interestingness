package ui;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4Tuple;
import edu.mit.csail.sdg.translator.A4TupleSet;

public class GraphPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int R = 22; // node circle radius
    private static final Color[] PAL = {
        new Color(100, 160, 230), new Color(100, 200, 140),
        new Color(230, 160, 80),  new Color(200, 100, 130),
        new Color(160, 120, 200), new Color(80, 200, 200)
    };

    private final Map<String, Point> pos = new LinkedHashMap<>();
    private final Map<String, Color> colors = new LinkedHashMap<>();
    private final List<String[]> edges = new ArrayList<>();
    private final Map<String, List<String>> atoms = new LinkedHashMap<>();

    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(480, 400));
    }

    public void loadSolution(A4Solution sol) {
        pos.clear(); colors.clear(); edges.clear(); atoms.clear();

        int ci = 0;
        List<Sig> userSigs = new ArrayList<>();
        for (Sig sig : sol.getAllReachableSigs())
            if (!builtIn(sig)) userSigs.add(sig);

        for (Sig sig : userSigs) {
            String sn = sig.label.replace("this/", "");
            colors.put(sn, PAL[ci++ % PAL.length]);
            List<String> al = new ArrayList<>();
            try {
                for (A4Tuple t : sol.eval(sig)) al.add(t.atom(0));
            } catch (Exception e) { }
            atoms.put(sn, al);
        }

        for (Sig sig : userSigs) {
            for (Field f : sig.getFields()) {
                try {
                    for (A4Tuple t : sol.eval(f))
                        if (t.arity() >= 2) edges.add(new String[]{t.atom(0), t.atom(1), f.label});
                } catch (Exception e) { }
            }
        }
        placeAtoms();
        repaint();
    }

    private void placeAtoms() {
        int w = Math.max(getWidth(), 480);
        int h = Math.max(getHeight(), 400);
        List<String> sigs = new ArrayList<>(atoms.keySet());
        if (sigs.isEmpty()) return;
        int cw = w / sigs.size();

        for (int s = 0; s < sigs.size(); s++) {
            List<String> al = atoms.get(sigs.get(s));
            int x = cw * s + cw / 2;
            for (int i = 0; i < al.size(); i++) {
                // space evenly with padding at top/bottom
            int y = 80 + (int)((h - 120.0) / Math.max(al.size(), 1) * i)
                    + (int)((h - 120.0) / Math.max(al.size() * 2, 1));
                pos.put(al.get(i), new Point(x, y));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        placeAtoms();
        drawBoxes(g2);
        drawEdges(g2);
        drawNodes(g2);
    }

    private void drawBoxes(Graphics2D g2) {
        int pw = getWidth(), ph = getHeight();
        List<String> sigs = new ArrayList<>(atoms.keySet());
        if (sigs.isEmpty()) return;
        int cw = pw / sigs.size();

        for (int s = 0; s < sigs.size(); s++) {
            String sn = sigs.get(s);
            Color c = colors.getOrDefault(sn, Color.LIGHT_GRAY);
            int bx = cw * s + 10, by = 30, bw = cw - 20, bh = ph - 50;

            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 30));
            g2.fillRoundRect(bx, by, bw, bh, 15, 15);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 120));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(bx, by, bw, bh, 15, 15);

            g2.setColor(c.darker());
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(sn, bx + (bw - fm.stringWidth(sn)) / 2, by + 18);
        }
    }

    private void drawEdges(Graphics2D g2) {
        g2.setStroke(new BasicStroke(1.8f));
        for (String[] e : edges) {
            Point from = pos.get(e[0]), to = pos.get(e[1]);
            if (from == null || to == null) continue;
            g2.setColor(new Color(80, 80, 80));
            drawArrow(g2, from.x, from.y, to.x, to.y);
            g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
            g2.setColor(new Color(100, 60, 60));
            g2.drawString(e[2], (from.x + to.x) / 2 + 5, (from.y + to.y) / 2 - 5);
        }
    }

    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        // shorten line so it starts/ends at circle edge, not center
        double ux = dx / len, uy = dy / len;
        int sx = (int)(x1 + ux * R), sy = (int)(y1 + uy * R);
        int ex = (int)(x2 - ux * R), ey = (int)(y2 - uy * R);
        g2.drawLine(sx, sy, ex, ey);

        double a = Math.atan2(ey - sy, ex - sx);
        int sz = 10;
        g2.fillPolygon(
            new int[]{ex, (int)(ex - sz * Math.cos(a - 0.4)), (int)(ex - sz * Math.cos(a + 0.4))},
            new int[]{ey, (int)(ey - sz * Math.sin(a - 0.4)), (int)(ey - sz * Math.sin(a + 0.4))}, 3);
    }

    private void drawNodes(Graphics2D g2) {
        for (Map.Entry<String, Point> e : pos.entrySet()) {
            String atom = e.getKey();
            Point p = e.getValue();

            Color c = Color.LIGHT_GRAY;
            for (Map.Entry<String, List<String>> se : atoms.entrySet()) {
                if (se.getValue().contains(atom)) {
                    c = colors.getOrDefault(se.getKey(), Color.LIGHT_GRAY);
                    break;
                }
            }

            g2.setColor(c);
            g2.fillOval(p.x - R, p.y - R, R * 2, R * 2);
            g2.setColor(c.darker());
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(p.x - R, p.y - R, R * 2, R * 2);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            String lbl = shorten(atom);
            g2.drawString(lbl, p.x - fm.stringWidth(lbl) / 2, p.y + fm.getAscent() / 2 - 1);
        }
    }

    // "Node$0" -> "N0" to fit inside the circle
    private String shorten(String atom) {
        if (atom.contains("$")) {
            String[] p = atom.split("\\$");
            return p[0].substring(0, 1) + p[1];
        }
        return atom.length() > 4 ? atom.substring(0, 4) : atom;
    }

    private boolean builtIn(Sig sig) {
        String n = sig.label;
        return n.equals("univ") || n.equals("Int") || n.equals("seq/Int")
            || n.equals("String") || n.equals("none") || sig.builtin;
    }
}
