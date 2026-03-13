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

public class MemorySafeDiversityRunner {

    public static class DiverseResult {
        public final A4Solution solution;
        public final int score;
        public final int diversityFromPrevious;

        public DiverseResult(A4Solution solution, int score, int diversityFromPrevious) {
            this.solution = solution;
            this.score = score;
            this.diversityFromPrevious = diversityFromPrevious;
        }
    }

    private final TupleCounter counter = new TupleCounter();
    private final DiversitySelector diversitySelector = new DiversitySelector();

    // Convert a solution to a set of tuple strings
    private Set<String> toTupleSet(A4Solution solution) {
        Set<String> tuples = new HashSet<>();
        for (Sig sig : solution.getAllReachableSigs()) {
            if (isBuiltIn(sig)) continue;
            String sigName = sig.label.replace("this/", "");

            try {
                A4TupleSet sigSet = solution.eval(sig);
                for (A4Tuple t : sigSet) {
                    tuples.add(sigName + ":" + t.atom(0));
                }

                for (Field field : sig.getFields()) {
                    A4TupleSet fieldSet = solution.eval(field);
                    for (A4Tuple t : fieldSet) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(sigName).append(".")
                          .append(field.label).append(":");
                        for (int i = 0; i < t.arity(); i++) {
                            if (i > 0) sb.append("->");
                            sb.append(t.atom(i));
                        }
                        tuples.add(sb.toString());
                    }
                }
            } catch (Exception e) { /* ignore */ }
        }
        return tuples;
    }

    // Find k diverse solutions memory-safely
    // We enumerate solutions one by one, keeping only
    // the k most diverse — never storing everything
    public List<DiverseResult> findDiverse(
            CompModule module,
            Command command,
            int k,
            int maxSearch) throws Err {

        List<DiverseResult> selected = new ArrayList<>();
        List<Set<String>> selectedTupleSets = new ArrayList<>();

        A4Reporter reporter = new A4Reporter();
        A4Options options = new A4Options();

        A4Solution current = TranslateAlloyToKodkod.execute_command(
            reporter,
            module.getAllReachableSigs(),
            command,
            options
        );

        if (!current.satisfiable()) return selected;

        int searched = 0;

        while (current.satisfiable() && searched < maxSearch) {
            Set<String> tupleSet = toTupleSet(current);
            int score = counter.countUserTuples(current);

            if (selected.isEmpty()) {
                // Always take the first one
                selected.add(new DiverseResult(current, score, 0));
                selectedTupleSets.add(tupleSet);
            } else {
                // Compute minimum distance to any already-selected solution
                int minDist = Integer.MAX_VALUE;
                for (Set<String> existingSet : selectedTupleSets) {
                    int dist = symmetricDifference(tupleSet, existingSet);
                    if (dist < minDist) minDist = dist;
                }

                if (selected.size() < k) {
                    // Still filling slots — take if sufficiently different
                    if (minDist > 0) {
                        selected.add(new DiverseResult(current, score, minDist));
                        selectedTupleSets.add(tupleSet);
                        System.out.println("  Added diverse instance "
                            + selected.size() + " (dist=" + minDist + ")");
                    }
                } else {
                    // All slots filled — replace least diverse if this is better
                    int minExistingDiversity = Integer.MAX_VALUE;
                    int worstIndex = -1;

                    for (int i = 1; i < selected.size(); i++) {
                        if (selected.get(i).diversityFromPrevious
                                < minExistingDiversity) {
                            minExistingDiversity =
                                selected.get(i).diversityFromPrevious;
                            worstIndex = i;
                        }
                    }

                    if (worstIndex >= 0 && minDist > minExistingDiversity) {
                        selected.set(worstIndex,
                            new DiverseResult(current, score, minDist));
                        selectedTupleSets.set(worstIndex, tupleSet);
                        System.out.println("  Replaced instance " + worstIndex
                            + " with more diverse one (dist=" + minDist + ")");
                    }
                }
            }

            searched++;
            current = current.next();
        }

        System.out.println("Searched " + searched + " solutions, kept "
            + selected.size() + " diverse instances.");
        return selected;
    }

    private int symmetricDifference(Set<String> a, Set<String> b) {
        Set<String> diff = new HashSet<>(a);
        diff.addAll(b);
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        diff.removeAll(intersection);
        return diff.size();
    }

    private boolean isBuiltIn(Sig sig) {
        String name = sig.label;
        return name.equals("univ") || name.equals("Int")
            || name.equals("seq/Int") || name.equals("String")
            || name.equals("none") || sig.builtin;
    }
}