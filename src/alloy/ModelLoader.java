package alloy;

import java.util.ArrayList;
import java.util.List;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;

public class ModelLoader {

    public CompModule load(String path) throws Err {
        return CompUtil.parseEverything_fromFile(new A4Reporter(), null, path);
    }

    public List<CmdInfo> listCommands(CompModule module) {
        List<CmdInfo> out = new ArrayList<>();
        List<Command> cmds = module.getAllCommands();
        for (int i = 0; i < cmds.size(); i++) {
            out.add(new CmdInfo(i, cmds.get(i).toString()));
        }
        return out;
    }

    public Command getCommand(CompModule module, int idx) {
        List<Command> cmds = module.getAllCommands();
        if (idx < 0 || idx >= cmds.size()) {
            throw new IllegalArgumentException(
                "Invalid command index: " + idx + ". Found " + cmds.size() + " commands.");
        }
        return cmds.get(idx);
    }
}
