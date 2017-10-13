/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework.optaplanner;

import aprs.framework.optaplanner.actionmodel.Action;
import aprs.framework.optaplanner.actionmodel.ActionPlan;
import aprs.framework.optaplanner.actionmodel.ActionType;
import aprs.framework.optaplanner.actionmodel.score.EasyActionPlanScoreCalculator;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.optaplanner.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Main {

    public static void main(String[] args) {
// assume SLF4J is bound to logback in the current environment
//        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//        // print logback's internal status
//        StatusPrinter.print(lc);
        ActionPlan ap = new ActionPlan();

        List<Action> initList = Arrays.asList(
                new Action("pickup A3", 5, 0, ActionType.PICKUP, "A"),
                new Action("dropoff A3", 6, 0, ActionType.DROPOFF, "A"),
                new Action("Start", 0, 0, ActionType.START, "START"),
                new Action("pickup A1", 1, 0, ActionType.PICKUP, "A"),
                new Action("dropoff A1", 2, 0, ActionType.DROPOFF, "A"),
                new Action("pickup A2", 3, 0, ActionType.PICKUP, "A"),
                new Action("dropoff A2", 4, 0, ActionType.DROPOFF, "A"),
                new Action("pickup B3", 5, 1, ActionType.PICKUP, "B"),
                new Action("dropoff B3", 6, 1, ActionType.DROPOFF, "B"),
                new Action("pickup B1", 1, 1, ActionType.PICKUP, "B"),
                new Action("dropoff B1", 2, 1, ActionType.DROPOFF, "B"),
                new Action("pickup B2", 3, 1, ActionType.PICKUP, "B"),
                new Action("dropoff B2", 4, 1, ActionType.DROPOFF, "B")
        );
        List<Action> shuffledList = new ArrayList<>(initList);
        Collections.shuffle(shuffledList);
        ap.setActions(shuffledList);
        ap.getEndAction().setLocation(new Point2D.Double(7, 0));
        ap.initNextActions();
        System.out.println("ap = " + ap);
        EasyActionPlanScoreCalculator calculator = new EasyActionPlanScoreCalculator();
        HardSoftBigDecimalScore score = calculator.calculateScore(ap);
        System.out.println("score = " + score);
        SolverFactory<ActionPlan> solverFactory = SolverFactory.createFromXmlResource(
                "aprs/framework/optaplanner/actionmodel/actionModelSolverConfig.xml");
        Solver<ActionPlan> solver = solverFactory.buildSolver();
        long start = System.currentTimeMillis();
        solver.addEventListener(e -> System.out.println(e.getTimeMillisSpent() + ", " + e.getNewBestScore()));
        ActionPlan solvedActionPlan = solver.solve(ap);
        System.out.println("solvedActionPlan = " + solvedActionPlan);
        System.out.println("solvedActionPlan.getActions() = " + solvedActionPlan.getActions());
        score = calculator.calculateScore(solvedActionPlan);
        System.out.println("score = " + score);
    }
}
