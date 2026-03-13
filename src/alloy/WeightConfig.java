package alloy;

import java.util.HashMap;
import java.util.Map;

public class WeightConfig {

    // sig name -> weight multiplier (default 1)
    private final Map<String, Integer> weights = new HashMap<>();

    public void setWeight(String sigName, int weight) {
        weights.put(sigName, weight);
    }

    public int getWeight(String sigName) {
        return weights.getOrDefault(sigName, 1);
    }

    public void reset() {
        weights.clear();
    }

    public Map<String, Integer> getAllWeights() {
        return new HashMap<>(weights);
    }
}