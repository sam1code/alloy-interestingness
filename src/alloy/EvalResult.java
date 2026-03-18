package alloy;

public class EvalResult {

    public final String strategy;
    public final int found;
    public final int minScore;
    public final int maxScore;
    public final double avgScore;
    public final double avgDiv;
    public final int distinct;

    public EvalResult(String strategy, int found, int minScore, int maxScore,
            double avgScore, double avgDiv, int distinct) {
        this.strategy = strategy;
        this.found = found;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.avgScore = avgScore;
        this.avgDiv = avgDiv;
        this.distinct = distinct;
    }

    @Override
    public String toString() {
        return String.format(
            "%-25s | found=%2d | score=[%2d-%2d] | avgScore=%.1f | avgDiv=%.1f | distinct=%2d",
            strategy, found, minScore, maxScore, avgScore, avgDiv, distinct);
    }
}
