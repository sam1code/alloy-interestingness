package core;

public class Config {

    private final String modelPath;
    private final int cmdIndex;

    public Config(String modelPath, int cmdIndex) {
        this.modelPath = modelPath;
        this.cmdIndex = cmdIndex;
    }

    public String getModelPath() { return modelPath; }

    public int getCommandIndex() { return cmdIndex; }
}
