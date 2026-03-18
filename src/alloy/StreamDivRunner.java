package alloy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4Tuple;
import edu.mit.csail.sdg.translator.A4TupleSet;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

public class StreamDivRunner {

    public static class Result {
        public final A4Solution solution;
        public final int score;
        public final int divFromPrev;

        public Result(A4Solution solution, int score, int divFromPrev) {
            this.solution = solution;
            this.score = score;
            this.divFromPrev = divFromPrev;
        }
    }

    private final Counter counter = new Counter();

    private Set<String> toTupleSet(A4Solution sol) {
        Set<String> tuples = new HashSet<>();
        for (Sig sig : sol.getAllReachableSigs()) {
            if (builtIn(sig)) continue;
            String sn = sig.label.replace("this/", "");
            try {
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
            } catch (Exception e) { }
        }
        return tuples;
    }

    // streams through solutions one-by-one, keeps only k most diverse (never stores all)
    public List<Result> findDiverse(CompModule module, Command cmd, int k, int maxSearch)
            throws Err {
        List<Result> selected = new ArrayList<>();
        List<Set<String>> selSets = new ArrayList<>();

        A4Solution cur = TranslateAlloyToKodkod.execute_command(
            new A4Reporter(), module.getAllReachableSigs(), cmd, new A4Options());

        if (!cur.satisfiable()) return selected;

        int searched = 0;
        while (cur.satisfiable() && searched < maxSearch) {
            Set<String> ts = toTupleSet(cur);
            int score = counter.countUserTuples(cur);

            if (selected.isEmpty()) {
                selected.add(new Result(cur, score, 0));
                selSets.add(ts);
            } else {
                int minDist = Integer.MAX_VALUE;
                for (Set<String> existing : selSets) {
                    int d = symDiff(ts, existing);
                    if (d < minDist) minDist = d;
                }

                if (selected.size() < k) {
                    if (minDist > 0) { // skip exact duplicates
                        selected.add(new Result(cur, score, minDist));
                        selSets.add(ts);
                        System.out.println("  Added diverse instance "
                            + selected.size() + " (dist=" + minDist + ")");
                    }
                } else {
                    // all k slots full — evict the least diverse (but never the seed at 0)
                    int minExisting = Integer.MAX_VALUE;
                    int worstIdx = -1;
                    for (int i = 1; i < selected.size(); i++) {
                        if (selected.get(i).divFromPrev < minExisting) {
                            minExisting = selected.get(i).divFromPrev;
                            worstIdx = i;
                        }
                    }
                    if (worstIdx >= 0 && minDist > minExisting) {
                        selected.set(worstIdx, new Result(cur, score, minDist));
                        selSets.set(worstIdx, ts);
                        System.out.println("  Replaced instance " + worstIdx
                            + " with more diverse one (dist=" + minDist + ")");
                    }
                }
            }
            searched++;
            cur = cur.next();
        }
        System.out.println("Searched " + searched + " solutions, kept "
            + selected.size() + " diverse instances.");
        return selected;
    }

    private int symDiff(Set<String> a, Set<String> b) {
        Set<String> diff = new HashSet<>(a);
        diff.addAll(b);
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        diff.removeAll(inter);
        return diff.size();
    }

    private boolean builtIn(Sig sig) {
        String n = sig.label;
        return n.equals("univ") || n.equals("Int") || n.equals("seq/Int")
            || n.equals("String") || n.equals("none") || sig.builtin;
    }
}
