package alloy;

import edu.mit.csail.sdg.alloy4.SafeList;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4TupleSet;

public class Counter {

    private boolean builtIn(Sig sig) {
        String n = sig.label;
        return n.equals("univ") || n.equals("Int") || n.equals("seq/Int")
            || n.equals("String") || n.equals("none") || sig.builtin;
    }

    public int countUserTuples(A4Solution sol) {
        return countUserTuples(sol, new Weights());
    }

    public int countUserTuples(A4Solution sol, Weights w) {
        int total = 0;
        SafeList<Sig> sigs = sol.getAllReachableSigs();
        for (Sig sig : sigs) {
            if (builtIn(sig)) continue;
            String name = sig.label.replace("this/", ""); // alloy prefixes user sigs
            int wt = w.get(name);
            total += sol.eval(sig).size() * wt;
            for (Field f : sig.getFields())
                total += sol.eval(f).size() * wt;
        }
        return total;
    }

    public void printSummary(A4Solution sol, int num) {
        System.out.println("--- Instance " + num + " ---");
        SafeList<Sig> sigs = sol.getAllReachableSigs();
        for (Sig sig : sigs) {
            if (builtIn(sig)) continue;
            A4TupleSet sigSet = sol.eval(sig);
            System.out.println("  Sig " + sig.label + ": " + sigSet.size() + " atoms");
            for (Field f : sig.getFields()) {
                A4TupleSet fs = sol.eval(f);
                System.out.println("    Field " + f.label + ": " + fs.size() + " tuples");
            }
        }
        System.out.println("  Total: " + countUserTuples(sol));
    }
}
