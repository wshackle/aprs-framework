/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import org.optaplanner.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class TestMain {

    public static void main(String[] args) {
// assume SLF4J is bound to logback in the current environment
//        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//        // print logback's internal status
//        StatusPrinter.print(lc);
        OpActionPlan ap = new OpActionPlan();

        Random rand = new Random();
        List<OpAction> initList = Arrays.asList(
                new OpAction("pickup A3", 5 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A"),
                new OpAction("dropoff A3", 6 + rand.nextDouble(), rand.nextDouble(), OpActionType.DROPOFF, "A"),
                new OpAction("Start" + rand.nextDouble(), rand.nextDouble(), 0, OpActionType.START, "START"),
                new OpAction("pickup A1", 1 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A"),
                new OpAction("dropoff A1", 2 + rand.nextDouble(), rand.nextDouble(), OpActionType.DROPOFF, "A"),
                new OpAction("pickup A2", 3 + rand.nextDouble(), rand.nextDouble(), OpActionType.PICKUP, "A"),
                new OpAction("dropoff A2", 4 + rand.nextDouble(), rand.nextDouble(), OpActionType.DROPOFF, "A"),
                new OpAction("pickup B3", 5 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B"),
                new OpAction("dropoff B3", 6 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "B"),
                new OpAction("pickup B1", 1 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B"),
                new OpAction("dropoff B1", 2 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "B"),
                new OpAction("pickup B2", 3 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.PICKUP, "B"),
                new OpAction("dropoff B2", 4 + rand.nextDouble(), 1 + rand.nextDouble(), OpActionType.DROPOFF, "B")
        );
        List<OpAction> shuffledList = new ArrayList<>(initList);
        Collections.shuffle(shuffledList);
        ap.setActions(shuffledList);
        ap.getEndAction().setLocation(new Point2D.Double(7, 0));
        ap.initNextActions();
        showPlan(ap, "shuffled");
        System.out.println("ap = " + ap);
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftBigDecimalScore score = calculator.calculateScore(ap);
        System.out.println("score = " + score);
        SolverFactory<OpActionPlan> solverFactory = SolverFactory.createFromXmlResource(
                "aprs/framework/optaplanner/actionmodel/actionModelSolverConfig.xml");

//        PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory.createFromSolverFactory(solverFactory);
//        PlannerBenchmark plannerBenchmark = benchmarkFactory.buildPlannerBenchmark(ap);
//        plannerBenchmark.benchmark();
        Solver<OpActionPlan> solver = solverFactory.buildSolver();
        long start = System.currentTimeMillis();
        solver.addEventListener(e -> System.out.println(e.getTimeMillisSpent() + ", " + e.getNewBestScore()));
        OpActionPlan solvedActionPlan = solver.solve(ap);
        System.out.println("solvedActionPlan = " + solvedActionPlan);
        System.out.println("solvedActionPlan.getActions() = " + solvedActionPlan.getActions());
        score = calculator.calculateScore(solvedActionPlan);
        System.out.println("score = " + score);
        showPlan(solvedActionPlan, "solved");
    }
}
