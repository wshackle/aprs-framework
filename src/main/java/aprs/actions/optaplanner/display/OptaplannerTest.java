/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copyright/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain.
 * 
 * This software is experimental. NIST assumes no responsibility whatsoever 
 * for its use by other parties, and makes no guarantees, expressed or 
 * implied, about its quality, reliability, or any other characteristic. 
 * We would appreciate acknowledgement if the software is used. 
 * This software can be redistributed and/or modified freely provided 
 * that any derivative works bear some notice that they are derived from it, 
 * and any modified versions bear some notice that they have been modified.
 * 
 *  See http://www.copyright.gov/title17/92chap1.html#105
 * 
 */
package aprs.actions.optaplanner.display;

import aprs.actions.optaplanner.actionmodel.OpAction;
import aprs.actions.optaplanner.actionmodel.OpActionInterface;
import aprs.actions.optaplanner.actionmodel.OpActionMoveListFactory;
import aprs.actions.optaplanner.actionmodel.OpActionPlan;
import aprs.actions.optaplanner.actionmodel.OpActionType;
import static aprs.actions.optaplanner.actionmodel.OpActionType.DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.PICKUP;
import aprs.actions.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
import aprs.misc.Utils;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;

/**
 * Class for Demonstrating/Testing the use of OptaPlanner to optimize a plan for
 * picking up a set of parts to put in slots in a kit.
 *
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("guieffect")
public class OptaplannerTest {

    /**
     * Create an example random task to optimize a plan for, run OptaPlanner and
     * display the results.
     *
     * @param args not used
     * @throws java.io.IOException can't create temporary files
     */
    public static void main(String[] args) throws IOException {

        long seed = 2001;
        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            switch (arg) {
                case "--seed":
                    seed = Long.parseLong(args[i + 1]);
                    i++;
                    break;
            }
        }
        OpActionPlan ap = new OpActionPlan();

        Random rand;
        if (seed > 0) {
            rand = new Random();
        } else {
            rand = new Random(seed);
        }

        OpAction.setAllowedPartTypes("A", "B", "C");
        // Create an initial plan with some set of parts to pickup and drop off.
        List<OpAction> initList = Arrays.asList(
                new OpAction("pickup A3", 5 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A", false),
                new OpAction("pickup A3-alt", 5 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A", false),
                new OpAction("dropoff A3", 6 + rand.nextDouble(), rand.nextDouble(), OpActionType.DROPOFF, "A", true),
                new OpAction("Start", rand.nextDouble(), rand.nextDouble(), OpActionType.START, "START", true),
                new OpAction("pickup A1", 1 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A", false),
                new OpAction("pickup A1-alt", 1 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A", false),
                new OpAction("dropoff A1", 2 + rand.nextDouble(), rand.nextDouble(), OpActionType.DROPOFF, "A", true),
                new OpAction("pickup A2", 3 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A", false),
                new OpAction("pickup A2-alt", 3 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A", false),
                new OpAction("dropoff A2", 4 + rand.nextDouble(), rand.nextDouble(), OpActionType.DROPOFF, "A", true),
                new OpAction("pickup B3", 5 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B", false),
                new OpAction("pickup B3-alt", 5 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B", false),
                new OpAction("dropoff B3", 6 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "B", true),
                new OpAction("pickup B1", 1 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B", false),
                new OpAction("pickup B1-alt", 1 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B", false),
                new OpAction("dropoff B1", 2 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "B", true),
                new OpAction("pickup B2", 3 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B", false),
                new OpAction("pickup B2-alt", 3 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B", false),
                new OpAction("dropoff B2", 4 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "B", true),
                new OpAction("DROPOFF C3", 5 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "C", false),
                new OpAction("DROPOFF C3-alt", 5 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "C", false),
                new OpAction("PICKUP C3", 6 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "C", true),
                new OpAction("DROPOFF C1", 1 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "C", false),
                new OpAction("DROPOFF C1-alt", 1 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "C", false),
                new OpAction("PICKUP C1", 2 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "C", true),
                new OpAction("DROPOFF C2", 3 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "C", false),
                new OpAction("DROPOFF C2-alt", 3 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "C", false),
                new OpAction("PICKUP C2", 4 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "C", true)
        );
        String partType = "A";
        printInfo(initList, "A");
        printInfo(initList, "B");
        printInfo(initList, "C");
        List<OpAction> shuffledList = new ArrayList<>(initList);
        ap.setAccelleration(0.1);
        ap.setMaxSpeed(0.25);
        ap.setStartEndMaxSpeed(1.0);
//        Collections.shuffle(shuffledList);
        ap.setActions(shuffledList);
        ap.setUseDistForCost(true);
        ap.setUseStartEndCost(false);

        // Set the location to return to after the task is complete.
        ap.getEndAction().setLocation(new Point2D.Double(7, 0));
        String apStr = ap.computeString();
        System.out.println("apStr = " + apStr);
        ap.initNextActions();
        System.out.println("ap = " + ap.computeString());

        // Manually get an score for the initial plan just for display.
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore score = calculator.calculateScore(ap);

        // Show a window with the initial plan.
//        showPlan(ap, "Input : " + score.getSoftScore(),JFrame.EXIT_ON_CLOSE);
        System.out.println("score = " + score);
        SolverFactory<OpActionPlan> solverFactory = OpActionPlan.createSolverFactory();

        Solver<OpActionPlan> solver = solverFactory.buildSolver();

        ap.saveActionList(File.createTempFile("Optaplanner_Test_input_" + seed + "_" + Utils.runTimeToString(System.currentTimeMillis()), ".csv"));
//        OpActionPlan apOut = solver.solve(ap);

        ScoreManager<OpActionPlan> scoreManager = ScoreManager.create(solverFactory);
        
//Score score = guiScoreDirector.updateScore(solution);
//        final ScoreDirector<OpActionPlan> scoreDirector
//                = solver.getScoreDirectorFactory().buildScoreDirector();
//        scoreDirector.setWorkingSolution(ap);

        OpActionMoveListFactory moveFactory = new OpActionMoveListFactory();
        List<AbstractMove<OpActionPlan>> moveList
                = moveFactory
                        .createMoveList(ap);
        @SuppressWarnings("rawtypes")
        Score scoreFromDrl
                = scoreManager.updateScore(ap);

        System.out.println("scoreFromDrl = " + scoreFromDrl);
        long total = 0;
        final List<OpAction> apActions = ap.getActions();
        if (null == apActions) {
            throw new NullPointerException("ap.getActions() returned null");
        }
        for (int i = 0; i < apActions.size(); i++) {
            OpActionInterface apI = apActions.get(i);
            if (apI instanceof OpAction) {
                OpAction act = (OpAction) apI;
                final OpActionInterface actNext = act.getNext();
                if (!act.isFake()
                        && actNext != null
                        && !actNext.isFake()
                        && act.getLocation() != null
                        && actNext.getLocation() != null) {
                    final long distToNextLong = act.getDistToNextLong();
                    total += distToNextLong;
                }
            }
        }
        System.out.println("total = " + total);
        solver.addEventListener(e -> System.out.println("After " + e.getTimeMillisSpent() + "ms the best score is " + e.getNewBestScore()));

        List<OpAction> apActionsCopy = new ArrayList<>(apActions);
        System.out.println("apActionsCopy = " + apActionsCopy);

        // Run the solver.
        long t0 = System.currentTimeMillis();
        OpActionPlan solvedActionPlan = solver.solve(ap);

        long t1 = System.currentTimeMillis();
        long solveTime = t1-t0;
        System.out.println("solveTime = " + solveTime);
        HardSoftLongScore solvedActionPlanScore = calculator.calculateScore(solvedActionPlan);

        ap.saveActionList(File.createTempFile("Optaplanner_Test_solvedActionPlan_" + seed + "_" + Utils.runTimeToString(System.currentTimeMillis()), ".csv"));

        System.out.println("bestPlan = " + solvedActionPlan.computeString());
        System.out.println("bestPlan.getActions() = " + solvedActionPlan.getActions());
        System.out.println("bestScore = " + solvedActionPlanScore);
        aprs.actions.optaplanner.display.OptiplannerDisplayJFrame.showPlan(ap, solvedActionPlan, "Solution: " + solvedActionPlanScore.getSoftScore(), JFrame.EXIT_ON_CLOSE);
    }

    private static void printInfo(List<OpAction> initList, String partType) {
        int optionalPickups = 0;
        int requiredPickups = 0;
        int optionalDropoffs = 0;
        int requiredDropoffs = 0;
        for (int i = 0; i < initList.size(); i++) {
            OpAction a = initList.get(i);
            if (!a.getPartType().equals(partType)) {
                continue;
            }
            if (a.getOpActionType() == PICKUP) {
                if (a.isRequired()) {
                    requiredPickups++;
                } else {
                    optionalPickups++;
                }
            } else if (a.getOpActionType() == DROPOFF) {
                if (a.isRequired()) {
                    requiredDropoffs++;
                } else {
                    optionalDropoffs++;
                }
            }
        }
        System.out.println("partType = " + partType);
        System.out.println("requiredDropoffs = " + requiredDropoffs);
        System.out.println("optionalDropoffs = " + optionalDropoffs);
        System.out.println("requiredPickups = " + requiredPickups);
        System.out.println("optionalPickups = " + optionalPickups);
    }
}
