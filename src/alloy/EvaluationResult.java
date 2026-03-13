package alloy;

public class EvaluationResult {

    public final String strategyName;
    public final int instancesFound;
    public final int minScore;
    public final int maxScore;
    public final double averageScore;
    public final double averageDiversity;
    public final int totalDistinctStructures;

    public EvaluationResult(
            String strategyName,
            int instancesFound,
            int minScore,
            int maxScore,
            double averageScore,
            double averageDiversity,
            int totalDistinctStructures) {

        this.strategyName = strategyName;
        this.instancesFound = instancesFound;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.averageScore = averageScore;
        this.averageDiversity = averageDiversity;
        this.totalDistinctStructures = totalDistinctStructures;
    }

    @Override
    public String toString() {
        return String.format(
            "%-25s | found=%2d | score=[%2d-%2d] | avgScore=%.1f"
            + " | avgDiv=%.1f | distinct=%2d",
            strategyName, instancesFound,
            minScore, maxScore,
            averageScore, averageDiversity,
            totalDistinctStructures
        );
    }
}