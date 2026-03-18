package core;

import java.util.List;

import alloy.ModelLoader;
import alloy.Runner;
import alloy.CmdInfo;
import alloy.Counter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Solution;

public class Main {

    public static void main(String[] args) {
        try {
            Config cfg = new Config("models/graph.als", 0);
            ModelLoader loader = new ModelLoader();
            Runner runner = new Runner();
            Counter counter = new Counter();

            CompModule module = loader.load(cfg.getModelPath());

            System.out.println("=== MODEL LOADED ===");
            System.out.println("File: " + cfg.getModelPath());

            List<CmdInfo> cmds = loader.listCommands(module);
            System.out.println("\n=== COMMANDS FOUND ===");
            for (CmdInfo c : cmds) System.out.println(c);

            Command cmd = loader.getCommand(module, cfg.getCommandIndex());
            System.out.println("\n=== EXECUTING COMMAND ===");
            System.out.println(cmd);

            List<A4Solution> sols = runner.enumerate(module, cmd, 50);

            System.out.println("\n=== ALL INSTANCES ===");
            for (int i = 0; i < sols.size(); i++)
                counter.printSummary(sols.get(i), i + 1);

            int minScore = Integer.MAX_VALUE, maxScore = Integer.MIN_VALUE;
            int minIdx = 0, maxIdx = 0;
            for (int i = 0; i < sols.size(); i++) {
                int sc = counter.countUserTuples(sols.get(i));
                if (sc < minScore) { minScore = sc; minIdx = i; }
                if (sc > maxScore) { maxScore = sc; maxIdx = i; }
            }

            System.out.println("\n=== MOST INTERESTING ===");
            System.out.println("Smallest instance (index " + (minIdx+1) + ", score=" + minScore + "):");
            counter.printSummary(sols.get(minIdx), minIdx + 1);
            System.out.println("Largest instance (index " + (maxIdx+1) + ", score=" + maxScore + "):");
            counter.printSummary(sols.get(maxIdx), maxIdx + 1);
            System.out.println("\nTotal solutions found: " + sols.size());

        } catch (Err e) {
            System.err.println("Alloy error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("General error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
