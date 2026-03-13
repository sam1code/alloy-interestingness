package alloy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4Tuple;
import edu.mit.csail.sdg.translator.A4TupleSet;

public class DiversitySelector {

    // Convert a solution into a set of strings describing its tuples
    // e.g. "Edge.from:Edge$0->Node$1"
    private Set<String> toTupleSet(A4Solution solution) {
        Set<String> tuples = new HashSet<>();

        for (Sig sig : solution.getAllReachableSigs()) {
            if (isBuiltIn(sig)) continue;

            String sigName = sig.label.replace("this/", "");

            // atoms
            A4TupleSet sigSet = solution.eval(sig);
            for (A4Tuple t : sigSet) {
                tuples.add(sigName + ":" + t.atom(0));
            }

            // fields
            for (Field field : sig.getFields()) {
                A4TupleSet fieldSet = solution.eval(field);
                for (A4Tuple t : fieldSet) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(sigName).append(".").append(field.label).append(":");
                    for (int i = 0; i < t.arity(); i++) {
                        if (i > 0) sb.append("->");
                        sb.append(t.atom(i));
                    }
                    tuples.add(sb.toString());
                }
            }
        }

        return tuples;
    }

    // Measure how different two solutions are
    // Higher = more different
    public int symmetricDifference(A4Solution a, A4Solution b) {
        Set<String> setA = toTupleSet(a);
        Set<String> setB = toTupleSet(b);

        Set<String> diff = new HashSet<>(setA);
        diff.addAll(setB);

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        diff.removeAll(intersection);
        return diff.size();
    }

    // Pick k diverse solutions greedily from a pool
    // Start with first solution, then keep picking
    // the one most different from all already selected
    public List<A4Solution> selectDiverse(List<A4Solution> pool, int k) {
        List<A4Solution> selected = new ArrayList<>();
        if (pool.isEmpty()) return selected;

        // Start with the first one
        selected.add(pool.get(0));

        while (selected.size() < k && selected.size() < pool.size()) {
            A4Solution bestCandidate = null;
            int bestScore = -1;

            for (A4Solution candidate : pool) {
                if (selected.contains(candidate)) continue;

                // Score = minimum distance to any already-selected solution
                int minDist = Integer.MAX_VALUE;
                for (A4Solution s : selected) {
                    int dist = symmetricDifference(candidate, s);
                    if (dist < minDist) minDist = dist;
                }

                if (minDist > bestScore) {
                    bestScore = minDist;
                    bestCandidate = candidate;
                }
            }

            if (bestCandidate != null) {
                selected.add(bestCandidate);
            }
        }

        return selected;
    }

    private boolean isBuiltIn(Sig sig) {
        String name = sig.label;
        return name.equals("univ") || name.equals("Int")
            || name.equals("seq/Int") || name.equals("String")
            || name.equals("none") || sig.builtin;
    }
}