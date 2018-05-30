/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copywrite/Warranty Disclaimer
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

import static aprs.actions.optaplanner.display.OpDisplayJPanel.showPlan;
import aprs.actions.optaplanner.actionmodel.OpAction;
import aprs.actions.optaplanner.actionmodel.OpActionPlan;
import aprs.actions.optaplanner.actionmodel.OpActionType;
import aprs.actions.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

/**
 * Class for Demonstrating/Testing the use of OptaPlanner to optimize a plan for
 * picking up a set of parts to put in slots in a kit.
 *
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class OptaplannerTest {

    /**
     * Create an example random task to optimize a plan for, run OptaPlanner and
     * display the results.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        OpActionPlan ap = new OpActionPlan();
        
        Random rand = new Random();

        // Create an initial plan with some set of parts to pickup and drop off.
        List<OpAction> initList = Arrays.asList(
                new OpAction("pickup A3", 5 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A",false),
                new OpAction("pickup A3-alt", 5 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A",false),
                new OpAction("dropoff A3", 6 + rand.nextDouble(), rand.nextDouble(), OpActionType.DROPOFF, "A",true),
                new OpAction("Start", rand.nextDouble(), rand.nextDouble(), OpActionType.START, "START",true),
                new OpAction("pickup A1", 1 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A",false),
                new OpAction("pickup A1-alt", 1 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A",false),
                new OpAction("dropoff A1", 2 + rand.nextDouble(), rand.nextDouble(), OpActionType.DROPOFF, "A",true),
                new OpAction("pickup A2", 3 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A",false),
                new OpAction("pickup A2-alt", 3 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A",false),
                new OpAction("dropoff A2", 4 + rand.nextDouble(), rand.nextDouble(), OpActionType.DROPOFF, "A",true),
                new OpAction("pickup B3", 5 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B",false),
                new OpAction("pickup B3-alt", 5 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B",false),
                new OpAction("dropoff B3", 6 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "B",true),
                new OpAction("pickup B1", 1 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B",false),
                new OpAction("pickup B1-alt", 1 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B",false),
                new OpAction("dropoff B1", 2 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "B",true),
                new OpAction("pickup B2", 3 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B",false),
                new OpAction("pickup B2-alt", 3 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B",false),
                new OpAction("dropoff B2", 4 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "B",true),
                
                
                new OpAction("DROPOFF C3", 5 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "C",false),
                new OpAction("DROPOFF C3-alt", 5 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "C",false),
                new OpAction("PICKUP C3", 6 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "C",true),
                new OpAction("DROPOFF C1", 1 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "C",false),
                new OpAction("DROPOFF C1-alt", 1 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "C",false),
                new OpAction("PICKUP C1", 2 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "C",true),
                new OpAction("DROPOFF C2", 3 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "C",false),
                new OpAction("DROPOFF C2-alt", 3 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "C",false),
                new OpAction("PICKUP C2", 4 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "C",true)
        );
        List<OpAction> shuffledList = new ArrayList<>(initList);
        ap.setAccelleration(0.1);
        ap.setMaxSpeed(0.25);
        ap.setStartEndMaxSpeed(1.0);
        Collections.shuffle(shuffledList);
        ap.setActions(shuffledList);

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
        showPlan(ap, "Input : " + score.getSoftScore(),JFrame.EXIT_ON_CLOSE);
        System.out.println("score = " + score);
        SolverFactory<OpActionPlan> solverFactory = SolverFactory.createFromXmlResource(
                "aprs/framework/optaplanner/actionmodel/actionModelSolverConfig.xml");
        
        Solver<OpActionPlan> solver = solverFactory.buildSolver();

        // Setup callback to have the solver print some status as it runs.
        solver.addEventListener(e -> System.out.println("After " + e.getTimeMillisSpent() + "ms the best score is " + e.getNewBestScore()));

        // Run the solver.
        long t0 = System.currentTimeMillis();
        OpActionPlan solvedActionPlan = solver.solve(ap);
        
        long t1 = System.currentTimeMillis();
        HardSoftLongScore bestScore = calculator.calculateScore(solvedActionPlan);
        OpActionPlan bestPlan = solvedActionPlan;
        for (int i = 0; i < 20; i++) {
            Collections.shuffle(shuffledList);
            ap.setActions(shuffledList);
            solvedActionPlan = solver.solve(ap);
            score = calculator.calculateScore(solvedActionPlan);
            System.out.println("score = " + score);
            if(score.getHardScore() > bestScore.getHardScore() 
                    || (score.getHardScore() == bestScore.getHardScore() && score.getSoftScore() > bestScore.getSoftScore())) {
                bestScore = score;
                bestPlan = solvedActionPlan;
            }
        }

        long t2 = System.currentTimeMillis();
        System.out.println("(t1-t0) = " + (t1-t0));
        System.out.println("(t2-t0) = " + (t2-t0));
        
        // Print the results
        System.out.println("bestPlan = " + bestPlan.computeString());
        System.out.println("bestPlan.getActions() = " + bestPlan.getActions());
//        score = calculator.calculateScore(bestPlan);
        System.out.println("bestScore = " + bestScore);
        showPlan(bestPlan, "Solution: " + bestScore.getSoftScore(),JFrame.EXIT_ON_CLOSE);
    }
}
