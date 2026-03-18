package alloy;

import java.util.ArrayList;
import java.util.List;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

public class ScopeRunner {

    public static class Result {
        public final int scope;
        public final A4Solution solution;
        public final int score;

        public Result(int scope, A4Solution solution, int score) {
            this.scope = scope;
            this.solution = solution;
            this.score = score;
        }
    }

    private final Counter counter = new Counter();

    // grows search budget gradually: round N checks N solutions, tracks best per level
    public List<Result> run(CompModule module, Command cmd,
            int minScope, int maxScope, String strategy) throws Err {
        List<Result> results = new ArrayList<>();

        A4Solution first = TranslateAlloyToKodkod.execute_command(
            new A4Reporter(), module.getAllReachableSigs(), cmd, new A4Options());

        if (!first.satisfiable()) {
            System.out.println("No solutions found at all.");
            return results;
        }

        A4Solution cur = first;
        int checked = 0;
        int curScope = minScope;
        A4Solution bestSol = null;
        int bestScore = strategy.contains("Minimal") ? Integer.MAX_VALUE : Integer.MIN_VALUE;

        while (cur.satisfiable() && curScope <= maxScope) {
            int score = counter.countUserTuples(cur);
            checked++;

            boolean better = false;
            if (strategy.contains("Minimal") && score < bestScore) better = true;
            else if (strategy.contains("Maximal") && score > bestScore) better = true;
            else if (strategy.contains("Diverse")) better = true;

            if (better || bestSol == null) {
                bestScore = score;
                bestSol = cur;
            }

            if (checked >= curScope && curScope >= minScope) { // checkpoint
                System.out.println("Scope level " + curScope + ": checked "
                    + checked + " solutions, best score=" + bestScore);
                results.add(new Result(curScope, bestSol, bestScore));
                curScope++;
            }
            cur = cur.next();
        }
        return results;
    }
}
