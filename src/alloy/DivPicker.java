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

public class DivPicker {

    private Set<String> toTupleSet(A4Solution sol) {
        Set<String> tuples = new HashSet<>();
        for (Sig sig : sol.getAllReachableSigs()) {
            if (builtIn(sig)) continue;
            String sn = sig.label.replace("this/", "");

            // encode atoms as "SigName:atom0" and fields as "SigName.field:a0->a1"
            A4TupleSet sigSet = sol.eval(sig);
            for (A4Tuple t : sigSet)
                tuples.add(sn + ":" + t.atom(0));

            for (Field f : sig.getFields()) {
                A4TupleSet fs = sol.eval(f);
                for (A4Tuple t : fs) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(sn).append(".").append(f.label).append(":");
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

    // |A △ B| via union minus intersection
    public int symDiff(A4Solution a, A4Solution b) {
        Set<String> sa = toTupleSet(a);
        Set<String> sb = toTupleSet(b);
        Set<String> diff = new HashSet<>(sa);
        diff.addAll(sb);
        Set<String> inter = new HashSet<>(sa);
        inter.retainAll(sb);
        diff.removeAll(inter);
        return diff.size();
    }

    public List<A4Solution> selectDiverse(List<A4Solution> pool, int k) {
        List<A4Solution> picked = new ArrayList<>();
        if (pool.isEmpty()) return picked;

        picked.add(pool.get(0)); // seed

        // greedy: always pick the candidate farthest from all already-picked
        while (picked.size() < k && picked.size() < pool.size()) {
            A4Solution best = null;
            int bestScore = -1;
            for (A4Solution c : pool) {
                if (picked.contains(c)) continue;
                int minDist = Integer.MAX_VALUE;
                for (A4Solution s : picked) {
                    int d = symDiff(c, s);
                    if (d < minDist) minDist = d;
                }
                if (minDist > bestScore) {
                    bestScore = minDist;
                    best = c;
                }
            }
            if (best != null) picked.add(best);
        }
        return picked;
    }

    private boolean builtIn(Sig sig) {
        String n = sig.label;
        return n.equals("univ") || n.equals("Int") || n.equals("seq/Int")
            || n.equals("String") || n.equals("none") || sig.builtin;
    }
}
