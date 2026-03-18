package alloy;

import java.util.ArrayList;
import java.util.List;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

public class Runner {

    public A4Solution run(CompModule module, Command cmd) throws Err {
        return TranslateAlloyToKodkod.execute_command(
            new A4Reporter(), module.getAllReachableSigs(), cmd, new A4Options());
    }

    public List<A4Solution> enumerate(CompModule module, Command cmd, int max) throws Err {
        List<A4Solution> sols = new ArrayList<>();
        A4Solution cur = TranslateAlloyToKodkod.execute_command(
            new A4Reporter(), module.getAllReachableSigs(), cmd, new A4Options());
        int n = 0;
        while (cur.satisfiable() && n < max) {
            sols.add(cur);
            n++;
            cur = cur.next();
        }
        return sols;
    }
}
