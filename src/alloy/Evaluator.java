package alloy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4Tuple;
import edu.mit.csail.sdg.translator.A4TupleSet;

public class Evaluator {

    private final Runner runner = new Runner();
    private final Counter counter = new Counter();
    private final DivPicker divPicker = new DivPicker();
    private final StreamDivRunner streamDiv = new StreamDivRunner();

    public List<EvalResult> evaluateAll(CompModule module, Command cmd) throws Err {
        List<EvalResult> results = new ArrayList<>();
        List<A4Solution> all = runner.enumerate(module, cmd, 50);
        if (all.isEmpty()) return results;

        results.add(eval("Minimal", getSorted(all, "Minimal"), all));
        results.add(eval("Maximal", getSorted(all, "Maximal"), all));
        results.add(eval("Diverse (greedy)", divPicker.selectDiverse(all, all.size()), all));

        try {
            List<StreamDivRunner.Result> divResults = streamDiv.findDiverse(module, cmd, 10, 100);
            List<A4Solution> divSols = new ArrayList<>();
            for (StreamDivRunner.Result r : divResults) divSols.add(r.solution);
            results.add(eval("Diverse (memory-safe)", divSols, all));
        } catch (Exception e) {
            System.err.println("Memory-safe diverse failed: " + e.getMessage());
        }
        return results;
    }

    private EvalResult eval(String name, List<A4Solution> sols, List<A4Solution> all) {
        if (sols.isEmpty()) return new EvalResult(name, 0, 0, 0, 0, 0, 0);

        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        long total = 0;
        for (A4Solution s : sols) {
            int sc = counter.countUserTuples(s);
            if (sc < min) min = sc;
            if (sc > max) max = sc;
            total += sc;
        }
        double avg = (double) total / sols.size();
        double avgDiv = avgDiversity(sols);
        int distinct = countDistinct(sols);
        return new EvalResult(name, sols.size(), min, max, avg, avgDiv, distinct);
    }

    // O(n^2) pairwise — fine for <=50 solutions
    private double avgDiversity(List<A4Solution> sols) {
        if (sols.size() < 2) return 0;
        long total = 0;
        int pairs = 0;
        for (int i = 0; i < sols.size(); i++) {
            for (int j = i + 1; j < sols.size(); j++) {
                total += divPicker.symDiff(sols.get(i), sols.get(j));
                pairs++;
            }
        }
        return pairs == 0 ? 0 : (double) total / pairs;
    }

    private int countDistinct(List<A4Solution> sols) {
        Set<String> fps = new HashSet<>();
        for (A4Solution s : sols) fps.add(fingerprint(s));
        return fps.size();
    }

    private String fingerprint(A4Solution sol) {
        StringBuilder sb = new StringBuilder();
        try {
            for (Sig sig : sol.getAllReachableSigs()) {
                if (builtIn(sig)) continue;
                String sn = sig.label.replace("this/", "");
                A4TupleSet sigSet = sol.eval(sig);
                sb.append(sn).append("=").append(sigSet.size()).append(";");
                for (Field f : sig.getFields()) {
                    A4TupleSet fs = sol.eval(f);
                    sb.append(sn).append(".").append(f.label).append("=[");
                    List<String> tuples = new ArrayList<>();
                    for (A4Tuple t : fs) {
                        StringBuilder ts = new StringBuilder();
                        for (int i = 0; i < t.arity(); i++) {
                            if (i > 0) ts.append("->");
                            ts.append(normAtom(t.atom(i)));
                        }
                        tuples.add(ts.toString());
                    }
                    java.util.Collections.sort(tuples); // canonical order
                    sb.append(String.join(",", tuples)).append("];");
                }
            }
        } catch (Exception e) { }
        return sb.toString();
    }

    // "Node$0" -> "0" so isomorphic instances get the same fingerprint
    private String normAtom(String atom) {
        if (atom.contains("$")) return atom.split("\\$")[1];
        return atom;
    }

    private List<A4Solution> getSorted(List<A4Solution> all, String strategy) {
        List<A4Solution> sorted = new ArrayList<>(all);
        sorted.sort((a, b) -> {
            int sa = counter.countUserTuples(a);
            int sb = counter.countUserTuples(b);
            if (strategy.equals("Minimal")) return Integer.compare(sa, sb);
            if (strategy.equals("Maximal")) return Integer.compare(sb, sa);
            return 0;
        });
        return sorted;
    }

    private boolean builtIn(Sig sig) {
        String n = sig.label;
        return n.equals("univ") || n.equals("Int") || n.equals("seq/Int")
            || n.equals("String") || n.equals("none") || sig.builtin;
    }
}
