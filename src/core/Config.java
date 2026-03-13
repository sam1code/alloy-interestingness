package core;

public class Config {
    private final String modelPath;
    private final int commandIndex;

    public Config(String modelPath, int commandIndex) {
        this.modelPath = modelPath;
        this.commandIndex = commandIndex;
    }

    public String getModelPath() {
        return modelPath;
    }

    public int getCommandIndex() {
        return commandIndex;
    }
}