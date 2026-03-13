package core;

import java.util.List;

import alloy.AlloyModelLoader;
import alloy.AlloyRunner;
import alloy.CommandInfo;
import alloy.TupleCounter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.translator.A4Solution;

public class Main {

    public static void main(String[] args) {
        try {
            Config config = new Config("models/graph.als", 0);

            AlloyModelLoader loader = new AlloyModelLoader();
            AlloyRunner runner = new AlloyRunner();
            TupleCounter counter = new TupleCounter();

            CompModule module = loader.loadModel(config.getModelPath());

            System.out.println("=== MODEL LOADED ===");
            System.out.println("File: " + config.getModelPath());

            List<CommandInfo> commands = loader.listCommands(module);
            System.out.println("\n=== COMMANDS FOUND ===");
            for (CommandInfo info : commands) {
                System.out.println(info);
            }

            Command command = loader.getCommand(module, config.getCommandIndex());

            System.out.println("\n=== EXECUTING COMMAND ===");
            System.out.println(command);

            List<A4Solution> solutions = runner.enumerateSolutions(module, command, 50);

            System.out.println("\n=== ALL INSTANCES ===");
            for (int i = 0; i < solutions.size(); i++) {
                counter.printSummary(solutions.get(i), i + 1);
            }

            // Find smallest and largest
            int minScore = Integer.MAX_VALUE;
            int maxScore = Integer.MIN_VALUE;
            int minIndex = 0;
            int maxIndex = 0;

            for (int i = 0; i < solutions.size(); i++) {
                int score = counter.countUserTuples(solutions.get(i));
                if (score < minScore) { minScore = score; minIndex = i; }
                if (score > maxScore) { maxScore = score; maxIndex = i; }
            }

            System.out.println("\n=== MOST INTERESTING ===");
            System.out.println("Smallest instance (index " + (minIndex+1) + ", score=" + minScore + "):");
            counter.printSummary(solutions.get(minIndex), minIndex + 1);

            System.out.println("Largest instance (index " + (maxIndex+1) + ", score=" + maxScore + "):");
            counter.printSummary(solutions.get(maxIndex), maxIndex + 1);

            System.out.println("\nTotal solutions found: " + solutions.size());

        } catch (Err e) {
            System.err.println("Alloy error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("General error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}