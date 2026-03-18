package alloy;

import java.util.HashMap;
import java.util.Map;

public class Weights {

    private final Map<String, Integer> map = new HashMap<>();

    public void set(String sig, int w) { map.put(sig, w); }

    public int get(String sig) { return map.getOrDefault(sig, 1); }

    public void reset() { map.clear(); }

    public Map<String, Integer> getAll() { return new HashMap<>(map); }
}
