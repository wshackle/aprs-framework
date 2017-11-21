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
package aprs.framework.optaplanner;

import static aprs.framework.optaplanner.OpDisplayJPanel.showPlan;
import aprs.framework.optaplanner.actionmodel.OpAction;
import aprs.framework.optaplanner.actionmodel.OpActionPlan;
import aprs.framework.optaplanner.actionmodel.OpActionType;
import aprs.framework.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

/**
 * Class for Demonstrating/Testing the use of OptaPlanner to optimize a 
 * plan for picking up a set of parts to put in slots in a kit.
 * 
 * 
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class OptaplannerTest {

    /**
     *Create an example random task to optimize a plan for, run OptaPlanner
     * and display the results.
     * 
     * @param args not used
     */
    public static void main(String[] args) {
        OpActionPlan ap = new OpActionPlan();

        Random rand = new Random();
        
        // Create an initial plan with some set of parts to pickup and drop off.
        List<OpAction> initList = Arrays.asList(
                new OpAction("pickup A3", 5 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A"),
                new OpAction("pickup A3-alt", 5 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A"),
                new OpAction("dropoff A3", 6 + rand.nextDouble(), rand.nextDouble(), OpActionType.DROPOFF, "A"),
                new OpAction("Start", rand.nextDouble(), rand.nextDouble(), OpActionType.START, "START"),
                new OpAction("pickup A1", 1 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A"),
                new OpAction("pickup A1-alt", 1 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A"),
                new OpAction("dropoff A1", 2 + rand.nextDouble(), rand.nextDouble(), OpActionType.DROPOFF, "A"),
                new OpAction("pickup A2", 3 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A"),
                new OpAction("pickup A2-alt", 3 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A"),
                new OpAction("dropoff A2", 4 + rand.nextDouble(), rand.nextDouble(), OpActionType.DROPOFF, "A"),
                new OpAction("pickup B3", 5 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B"),
                new OpAction("pickup B3-alt", 5 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B"),
                new OpAction("dropoff B3", 6 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "B"),
                new OpAction("pickup B1", 1 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B"),
                new OpAction("pickup B1-alt", 1 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B"),
                new OpAction("dropoff B1", 2 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "B"),
                new OpAction("pickup B2", 3 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B"),
                new OpAction("pickup B2-alt", 3 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B"),
                new OpAction("dropoff B2", 4 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "B")
        );
        List<OpAction> shuffledList = new ArrayList<>(initList);
        Collections.shuffle(shuffledList);
        ap.setActions(shuffledList);
        
        // Set the location to return to after the task is complete.
        ap.getEndAction().setLocation(new Point2D.Double(7, 0));
        String apStr = ap.toString();
        System.out.println("apStr = " + apStr);
        ap.initNextActions();
        System.out.println("ap = " + ap);
        
        // Manually get an score for the initial plan just for display.
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore score = calculator.calculateScore(ap);
        
        // Show a window with the initial plan.
        showPlan(ap, "Input : "+score.getSoftScore());
        System.out.println("score = " + score);
        SolverFactory<OpActionPlan> solverFactory = SolverFactory.createFromXmlResource(
                "aprs/framework/optaplanner/actionmodel/actionModelSolverConfig.xml");

        Solver<OpActionPlan> solver = solverFactory.buildSolver();
        
        // Setup callback to have the solver print some status as it runs.
        solver.addEventListener(e -> System.out.println("After " +e.getTimeMillisSpent() + "ms the best score is " + e.getNewBestScore()));
        
        // Run the solver.
        OpActionPlan solvedActionPlan = solver.solve(ap);
        
        // Print the results
        System.out.println("solvedActionPlan = " + solvedActionPlan);
        System.out.println("solvedActionPlan.getActions() = " + solvedActionPlan.getActions());
        score = calculator.calculateScore(solvedActionPlan);
        System.out.println("score = " + score);
        showPlan(solvedActionPlan, "Solution: "+score.getSoftScore());
    }
}