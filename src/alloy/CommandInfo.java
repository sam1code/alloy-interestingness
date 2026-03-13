package alloy;

public class CommandInfo {
    private final int index;
    private final String description;

    public CommandInfo(int index, String description) {
        this.index = index;
        this.description = description;
    }

    public int getIndex() {
        return index;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return index + ": " + description;
    }
}