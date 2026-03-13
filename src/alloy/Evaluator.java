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

    private final AlloyRunner runner = new AlloyRunner();
    private final TupleCounter counter = new TupleCounter();
    private final DiversitySelector diversitySelector = new DiversitySelector();
    private final MemorySafeDiversityRunner memorySafeRunner =
        new MemorySafeDiversityRunner();

    // Run all strategies and return one EvaluationResult per strategy
    public List<EvaluationResult> evaluateAll(
            CompModule module,
            Command command) throws Err {

        List<EvaluationResult> results = new ArrayList<>();

        // Enumerate all solutions once
        List<A4Solution> all = runner.enumerateSolutions(module, command, 50);

        if (all.isEmpty()) return results;

        // Minimal
        results.add(evaluate("Minimal",
            getSorted(all, "Minimal"), all));

        // Maximal
        results.add(evaluate("Maximal",
            getSorted(all, "Maximal"), all));

        // Diverse (greedy)
        results.add(evaluate("Diverse (greedy)",
            diversitySelector.selectDiverse(all, all.size()), all));

        // Diverse (memory-safe)
        try {
            List<MemorySafeDiversityRunner.DiverseResult> divResults =
                memorySafeRunner.findDiverse(module, command, 10, 100);
            List<A4Solution> divSolutions = new ArrayList<>();
            for (MemorySafeDiversityRunner.DiverseResult r : divResults) {
                divSolutions.add(r.solution);
            }
            results.add(evaluate("Diverse (memory-safe)", divSolutions, all));
        } catch (Exception e) {
            System.err.println("Memory-safe diverse failed: " + e.getMessage());
        }

        return results;
    }

    // Evaluate a list of solutions produced by one strategy
    private EvaluationResult evaluate(
            String strategyName,
            List<A4Solution> solutions,
            List<A4Solution> allSolutions) {

        if (solutions.isEmpty()) {
            return new EvaluationResult(strategyName, 0, 0, 0, 0, 0, 0);
        }

        int minScore = Integer.MAX_VALUE;
        int maxScore = Integer.MIN_VALUE;
        long totalScore = 0;

        for (A4Solution sol : solutions) {
            int score = counter.countUserTuples(sol);
            if (score < minScore) minScore = score;
            if (score > maxScore) maxScore = score;
            totalScore += score;
        }

        double avgScore = (double) totalScore / solutions.size();

        // Average pairwise diversity
        double avgDiversity = computeAverageDiversity(solutions);

        // Count distinct structures
        int distinct = countDistinctStructures(solutions);

        return new EvaluationResult(
            strategyName,
            solutions.size(),
            minScore,
            maxScore,
            avgScore,
            avgDiversity,
            distinct
        );
    }

    // Average pairwise symmetric difference
    private double computeAverageDiversity(List<A4Solution> solutions) {
        if (solutions.size() < 2) return 0;

        long total = 0;
        int pairs = 0;

        for (int i = 0; i < solutions.size(); i++) {
            for (int j = i + 1; j < solutions.size(); j++) {
                total += diversitySelector.symmetricDifference(
                    solutions.get(i), solutions.get(j));
                pairs++;
            }
        }

        return pairs == 0 ? 0 : (double) total / pairs;
    }

    // Count how many structurally distinct instances exist
    // (by their tuple set fingerprint)
    private int countDistinctStructures(List<A4Solution> solutions) {
        Set<String> fingerprints = new HashSet<>();

        for (A4Solution sol : solutions) {
            fingerprints.add(buildFingerprint(sol));
        }

        return fingerprints.size();
    }

    // Build a string fingerprint for a solution
    private String buildFingerprint(A4Solution solution) {
        StringBuilder sb = new StringBuilder();

        try {
            for (Sig sig : solution.getAllReachableSigs()) {
                if (isBuiltIn(sig)) continue;

                String sigName = sig.label.replace("this/", "");
                A4TupleSet sigSet = solution.eval(sig);
                sb.append(sigName).append("=").append(sigSet.size()).append(";");

                for (Field field : sig.getFields()) {
                    A4TupleSet fieldSet = solution.eval(field);
                    sb.append(sigName).append(".")
                      .append(field.label).append("=[");

                    List<String> tuples = new ArrayList<>();
                    for (A4Tuple t : fieldSet) {
                        StringBuilder ts = new StringBuilder();
                        for (int i = 0; i < t.arity(); i++) {
                            if (i > 0) ts.append("->");
                            // Use atom index not name for structure comparison
                            ts.append(normalizeAtom(t.atom(i)));
                        }
                        tuples.add(ts.toString());
                    }
                    java.util.Collections.sort(tuples);
                    sb.append(String.join(",", tuples)).append("];");
                }
            }
        } catch (Exception e) { /* ignore */ }

        return sb.toString();
    }

    // Normalize atom name to a number for structural comparison
    // e.g. "Node$0" -> "0", "Node$2" -> "2"
    private String normalizeAtom(String atom) {
        if (atom.contains("$")) {
            return atom.split("\\$")[1];
        }
        return atom;
    }

    // Sort solutions by strategy
    private List<A4Solution> getSorted(List<A4Solution> all, String strategy) {
        List<A4Solution> sorted = new ArrayList<>(all);
        sorted.sort((a, b) -> {
            int scoreA = counter.countUserTuples(a);
            int scoreB = counter.countUserTuples(b);
            if (strategy.equals("Minimal"))
                return Integer.compare(scoreA, scoreB);
            if (strategy.equals("Maximal"))
                return Integer.compare(scoreB, scoreA);
            return 0;
        });
        return sorted;
    }

    private boolean isBuiltIn(Sig sig) {
        String name = sig.label;
        return name.equals("univ") || name.equals("Int")
            || name.equals("seq/Int") || name.equals("String")
            || name.equals("none") || sig.builtin;
    }
}