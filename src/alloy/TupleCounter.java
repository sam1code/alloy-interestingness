package alloy;

import edu.mit.csail.sdg.alloy4.SafeList;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4TupleSet;

public class TupleCounter {

    private boolean isBuiltIn(Sig sig) {
        String name = sig.label;
        return name.equals("univ") || name.equals("Int")
            || name.equals("seq/Int") || name.equals("String")
            || name.equals("none") || sig.builtin;
    }

    // Count tuples with no weights
    public int countUserTuples(A4Solution solution) {
        return countUserTuples(solution, new WeightConfig());
    }

    // Count tuples with weights applied
    public int countUserTuples(A4Solution solution, WeightConfig weights) {
        int total = 0;

        SafeList<Sig> sigs = solution.getAllReachableSigs();
        for (Sig sig : sigs) {
            if (isBuiltIn(sig)) continue;

            String sigName = sig.label.replace("this/", "");
            int weight = weights.getWeight(sigName);

            A4TupleSet sigSet = solution.eval(sig);
            total += sigSet.size() * weight;

            for (Field field : sig.getFields()) {
                A4TupleSet fieldSet = solution.eval(field);
                total += fieldSet.size() * weight;
            }
        }

        return total;
    }

    public void printSummary(A4Solution solution, int instanceNumber) {
        System.out.println("--- Instance " + instanceNumber + " ---");

        SafeList<Sig> sigs = solution.getAllReachableSigs();
        for (Sig sig : sigs) {
            if (isBuiltIn(sig)) continue;

            A4TupleSet sigSet = solution.eval(sig);
            System.out.println("  Sig " + sig.label + ": "
                + sigSet.size() + " atoms");

            for (Field field : sig.getFields()) {
                A4TupleSet fieldSet = solution.eval(field);
                System.out.println("    Field " + field.label
                    + ": " + fieldSet.size() + " tuples");
            }
        }
        System.out.println("  Total: " + countUserTuples(solution));
    }
}