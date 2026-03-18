package alloy;

public class CmdInfo {

    private final int idx;
    private final String desc;

    public CmdInfo(int idx, String desc) {
        this.idx = idx;
        this.desc = desc;
    }

    public int getIndex() { return idx; }

    public String getDescription() { return desc; }

    @Override
    public String toString() { return idx + ": " + desc; }
}
