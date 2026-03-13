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

    public static class ScopeResult {
        public final int scope;
        public final A4Solution solution;
        public final int score;

        public ScopeResult(int scope, A4Solution solution, int score) {
            this.scope = scope;
            this.solution = solution;
            this.score = score;
        }
    }

    private final TupleCounter counter = new TupleCounter();

    // Instead of overriding the Alloy scope (which causes errors),
    // we use scope as the number of solutions to check per round.
    // Round 1: check 1 solution
    // Round 2: check 2 solutions
    // Round 3: check 3 solutions ... up to maxScope
    // This grows the search budget gradually — memory safe.
    public List<ScopeResult> runAcrossScopes(
            CompModule module,
            Command command,
            int minScope,
            int maxScope,
            String strategy) throws Err {

        List<ScopeResult> results = new ArrayList<>();

        A4Reporter reporter = new A4Reporter();
        A4Options options = new A4Options();

        // Run the solver once
        A4Solution first = TranslateAlloyToKodkod.execute_command(
            reporter,
            module.getAllReachableSigs(),
            command,
            options
        );

        if (!first.satisfiable()) {
            System.out.println("No solutions found at all.");
            return results;
        }

        // Walk through solutions, keeping track of best at each scope level
        A4Solution current = first;
        int totalChecked = 0;
        int currentScope = minScope;

        A4Solution bestSoFar = null;
        int bestScoreSoFar = strategy.contains("Minimal")
            ? Integer.MAX_VALUE : Integer.MIN_VALUE;

        while (current.satisfiable() && currentScope <= maxScope) {
            int score = counter.countUserTuples(current);
            totalChecked++;

            // Track best for this strategy
            boolean better = false;
            if (strategy.contains("Minimal") && score < bestScoreSoFar) {
                better = true;
            } else if (strategy.contains("Maximal") && score > bestScoreSoFar) {
                better = true;
            } else if (strategy.contains("Diverse")) {
                better = true; // always take new one for diverse
            }

            if (better || bestSoFar == null) {
                bestScoreSoFar = score;
                bestSoFar = current;
            }

            // Every time we hit a scope checkpoint, record best so far
            if (totalChecked >= currentScope && currentScope >= minScope) {
                System.out.println("Scope level " + currentScope
                    + ": checked " + totalChecked
                    + " solutions, best score=" + bestScoreSoFar);
                results.add(new ScopeResult(currentScope, bestSoFar, bestScoreSoFar));
                currentScope++;
            }

            current = current.next();
        }

        return results;
    }
}