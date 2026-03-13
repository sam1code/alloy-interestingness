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

public class AlloyRunner {

    public A4Solution executeOnce(CompModule module, Command command) throws Err {
        A4Reporter reporter = new A4Reporter();
        A4Options options = new A4Options();
        return TranslateAlloyToKodkod.execute_command(
            reporter,
            module.getAllReachableSigs(),
            command,
            options
        );
    }

    public List<A4Solution> enumerateSolutions(CompModule module, Command command, int maxSolutions) throws Err {
        A4Reporter reporter = new A4Reporter();
        A4Options options = new A4Options();
        List<A4Solution> solutions = new ArrayList<>();

        A4Solution current = TranslateAlloyToKodkod.execute_command(
            reporter,
            module.getAllReachableSigs(),
            command,
            options
        );

        int count = 0;
        while (current.satisfiable() && count < maxSolutions) {
            solutions.add(current);
            count++;
            current = current.next();
        }

        return solutions;
    }
}