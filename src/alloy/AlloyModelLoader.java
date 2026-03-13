package alloy;

import java.util.ArrayList;
import java.util.List;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;

public class AlloyModelLoader {

    public CompModule loadModel(String modelPath) throws Err {
        A4Reporter reporter = new A4Reporter();
        return CompUtil.parseEverything_fromFile(reporter, null, modelPath);
    }

    public List<CommandInfo> listCommands(CompModule module) {
        List<CommandInfo> result = new ArrayList<>();
        List<Command> commands = module.getAllCommands();

        for (int i = 0; i < commands.size(); i++) {
            result.add(new CommandInfo(i, commands.get(i).toString()));
        }

        return result;
    }

    public Command getCommand(CompModule module, int commandIndex) {
        List<Command> commands = module.getAllCommands();

        if (commandIndex < 0 || commandIndex >= commands.size()) {
            throw new IllegalArgumentException(
                "Invalid command index: " + commandIndex + 
                ". Found " + commands.size() + " commands."
            );
        }

        return commands.get(commandIndex);
    }
}