/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.actions.optaplanner.actionmodel;

import aprs.actions.executor.ActionType;
import static aprs.actions.optaplanner.actionmodel.OpActionType.DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.END;
import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_PICKUP;
import static aprs.actions.optaplanner.actionmodel.OpActionType.PICKUP;
import static aprs.actions.optaplanner.actionmodel.OpActionType.START;
import aprs.actions.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
import static aprs.misc.AprsCommonLogger.println;
import aprs.misc.Utils;
import aprs.system.AprsSystem;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.factory.Lists;
import org.optaplanner.benchmark.api.PlannerBenchmarkFactory;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactProperty;

import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("WeakerAccess")
@PlanningSolution(solutionCloner = OpActionPlanCloner.class)
public class OpActionPlan {

    private final static AtomicInteger idCounter = new AtomicInteger(1);

    static int newActionPlanId() {
        return idCounter.incrementAndGet();
    }

    static public SolverFactory<OpActionPlan> createSolverFactory() {
        return SolverFactory.createFromXmlResource(
                "aprs/actions/optaplanner/actionmodel/actionModelSolverConfig.xml");
    }

    static public PlannerBenchmarkFactory createBenchmarkFactory() {
        return PlannerBenchmarkFactory.createFromXmlResource(
                "aprs/actions/optaplanner/actionmodel/benchmark.xml");
    }

    @ProblemFactProperty
    private final OpEndAction endAction = new OpEndAction();

    public OpEndAction getEndAction() {
        return endAction;
    }

    private volatile int exhaustiveSearchScored = 0;
    private volatile int comboSearchScored = 0;

    private List<OpAction> findPossiblesNotInSet(OpAction action, Set<Integer> set) {
        List<OpActionInterface> possibles = action.getPossibleNextActions();
        List<OpAction> possiblesNotInSet = new ArrayList<>();
        boolean fake_found = false;
        for (int i = 0; i < possibles.size(); i++) {
            OpActionInterface possible = possibles.get(i);
            if (possible instanceof OpAction) {
                OpAction possibleAction = (OpAction) possible;
                if (!set.contains(possibleAction.getOrigId())) {
                    boolean isFake = possibleAction.getOpActionType() == FAKE_DROPOFF || possibleAction.getOpActionType() == FAKE_PICKUP;
                    if (!isFake || !fake_found) {
                        possiblesNotInSet.add(possibleAction);
                    }
                    if (isFake) {
                        fake_found = true;
                    }
                }
            }
        }
        return possiblesNotInSet;
    }

    private OpActionPlan simpleExhaustiveSearch(OpAction action, OpActionPlan bestPlan, Set<Integer> set, HardSoftLongScore score, int needed) {

        set.add(action.getOrigId());
        try {
            List<OpAction> possiblesNotInSet = findPossiblesNotInSet(action, set);
            if (!possiblesNotInSet.isEmpty()) {
                needed *= possiblesNotInSet.size();
                for (int i = 0; i < possiblesNotInSet.size(); i++) {
                    OpAction possibleAction = possiblesNotInSet.get(i);
                    action.setNext(possibleAction);
                    OpActionPlan planForAct2 = simpleExhaustiveSearch(possibleAction, bestPlan, set, score, needed);
                    if (planForAct2 != null && planForAct2 != this && planForAct2 != bestPlan) {
                        if (planForAct2.getScore().compareTo(score) > 0) {
                            bestPlan = planForAct2;
                            score = planForAct2.getScore();
                        }
                    }
                }
            } else {
                if (!action.checkNextAction(getEndAction())) {
                    System.out.println("nothing left and can't end");
                }
                action.setNext(getEndAction());
//            List<OpAction> orderedList = this.getOrderedList(true);
//            System.out.println("orderedList = " + orderedList);
                EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
                HardSoftLongScore newscore = calculator.calculateScore(this);
                exhaustiveSearchScored++;
                System.out.println("exhaustiveSearchScored = " + exhaustiveSearchScored + ", needed=" + needed);
                if (newscore.compareTo(score) > 0) {
                    OpActionPlan clone = new OpActionPlanCloner().cloneSolution(this);
                    score = newscore;
                    bestPlan = clone;
                    bestPlan.setScore(score);
                }
            }
        } finally {
            set.remove(action.getOrigId());
        }
        return bestPlan;
    }

    public OpActionPlan simpleExhaustiveSearch() {
        OpActionPlan clone = new OpActionPlanCloner().cloneSolution(this);
        List<OpAction> cloneActions = clone.actions;
        OpAction start = clone.findStartAction();
        if (null == start) {
            throw new NullPointerException("clone.findStartAction() returned null : clone=" + clone);
        }
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore cloneScore = calculator.calculateScore(clone);
        return clone.simpleExhaustiveSearch(start, clone, new TreeSet<>(), cloneScore, 1);
    }

    private OpActionPlan greedySearch(OpAction action, OpActionPlan bestPlan, Set<Integer> set, HardSoftLongScore score, int needed) {

        set.add(action.getOrigId());
        try {
            List<OpAction> possiblesNotInSet = findPossiblesNotInSet(action, set);
            if (!possiblesNotInSet.isEmpty()) {
                needed *= possiblesNotInSet.size();
                OpAction minPossibleAction = null;
                double minCostPossible = Double.POSITIVE_INFINITY;
                for (int i = 0; i < possiblesNotInSet.size(); i++) {
                    OpAction possibleAction = possiblesNotInSet.get(i);
                    double cost;
                    if (action.getOpActionType() == FAKE_PICKUP
                            || action.getOpActionType() == FAKE_DROPOFF
                            || possibleAction.getOpActionType() == FAKE_PICKUP
                            || possibleAction.getOpActionType() == FAKE_DROPOFF) {
                        cost = 0;
                    } else {
                        cost = action.costOfNext(possibleAction, this);
                    }
                    if (cost < minCostPossible) {
                        minPossibleAction = possibleAction;
                        minCostPossible = cost;
                    }
                }
                if (null == minPossibleAction) {
                    throw new NullPointerException("minPosssibleAction == null: possiblesNotInSet=" + possiblesNotInSet);
                }
                action.setNext(minPossibleAction);
                OpActionPlan planForAct2 = greedySearch(minPossibleAction, bestPlan, set, score, needed);
                if (planForAct2 != null && planForAct2 != this && planForAct2 != bestPlan) {
                    if (planForAct2.getScore().compareTo(score) > 0) {
                        bestPlan = planForAct2;
                        score = planForAct2.getScore();
                    }
                }
            } else {
                if (!action.checkNextAction(getEndAction())) {
                    System.out.println("nothing left and can't end");
                }
                action.setNext(getEndAction());
//            List<OpAction> orderedList = this.getOrderedList(true);
//            System.out.println("orderedList = " + orderedList);
                EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
                HardSoftLongScore newscore = calculator.calculateScore(this);
                exhaustiveSearchScored++;
                System.out.println("exhaustiveSearchScored = " + exhaustiveSearchScored + ", needed=" + needed);
                if (newscore.compareTo(score) > 0) {
                    OpActionPlan clone = new OpActionPlanCloner().cloneSolution(this);
                    score = newscore;
                    bestPlan = clone;
                    bestPlan.setScore(score);
                }
            }
        } finally {
            set.remove(action.getOrigId());
        }
        return bestPlan;
    }

    public OpActionPlan greedySearch() {
        OpActionPlan clone = new OpActionPlanCloner().cloneSolution(this);
        clone.checkActionList();
        OpActionPlan clone2 = new OpActionPlanCloner().cloneSolution(this);
        clone2.checkActionList();
        List<OpAction> actions = clone.actions;
        OpAction start = clone.findStartAction();
        if (null == start) {
            throw new NullPointerException("clone.findStartAction() returned null : clone = " + clone);
        }
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore score = calculator.calculateScore(clone);
        return clone.greedySearch(start, clone2, new TreeSet<>(), score, 1);
    }

    private volatile int comboSearchSkipped = 0;

    @SuppressWarnings({"unchecked"})
    private OpActionPlan comboSearch(OpAction action, OpActionPlan bestPlan, Set<Integer> set, HardSoftLongScore score, double costSoFar, double minRemainingCost, int requiredPickups, int availPickups, int requiredDrops, int availDrops) {

        set.add(action.getOrigId());
        try {
            List<OpAction> possiblesNotInSet = findPossiblesNotInSet(action, set);

            Collections.sort(possiblesNotInSet,
                    Comparators.chain(
                            Comparators.byBooleanFunction((OpAction a) -> !a.isRequired()),
                            Comparators.byDoubleFunction((OpAction a) -> action.costOfNext(a, this))));
            final double expectedCost = -1000.0 * (costSoFar + minRemainingCost);
            final long expectedCostLong = (long) expectedCost;
            if (!possiblesNotInSet.isEmpty()) {
                if (action.getOpActionType() == PICKUP) {
                    if (requiredPickups > availDrops) {
                        return bestPlan;
                    }
                    availPickups--;
                    if (action.isRequired()) {
                        requiredPickups--;
                    }
                } else if (action.getOpActionType() == DROPOFF) {
                    availDrops--;
                    if (action.isRequired()) {
                        requiredDrops--;
                    }
                    if (requiredDrops > availPickups) {
                        return bestPlan;
                    }
                }
                long diffScore = score.getSoftScore() - expectedCostLong;
                double ratio = diffScore / expectedCost;
                if (diffScore > 0 || ratio < 0.05) {
                    comboSearchSkipped++;
                    return bestPlan;
                }
                double minCost = findMinActCost(action, bestPlan);
                for (int i = 0; i < possiblesNotInSet.size(); i++) {
                    OpAction possibleAction = possiblesNotInSet.get(i);
                    action.setNext(possibleAction);
                    double cost = action.costOfNext(possibleAction, bestPlan);
                    OpActionPlan planForAct2 = comboSearch(possibleAction, bestPlan, set, score, costSoFar + cost, minRemainingCost - minCost, requiredPickups, availPickups, requiredDrops, availDrops);
                    if (planForAct2 != this && planForAct2 != bestPlan) {
                        if (planForAct2.getScore().compareTo(score) > 0) {
                            bestPlan = planForAct2;
                            bestPlan.checkActionList();
                            score = planForAct2.getScore();
                            if (score.getSoftScore() > expectedCostLong) {
                                return bestPlan;
                            }
                        }
                    }
                }
            } else {
                if (!action.checkNextAction(getEndAction())) {
                    return bestPlan;
                }
                action.setNext(getEndAction());
                this.checkActionList();
//            List<OpAction> orderedList = this.getOrderedList(true);
//            System.out.println("orderedList = " + orderedList);
                EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
                HardSoftLongScore newscore = calculator.calculateScore(this);
                comboSearchScored++;
//                System.out.println("comboSearchScored = " + comboSearchScored);
//                System.out.println("costSoFar = " + costSoFar);
//                System.out.println("minRemainingCost = " + minRemainingCost);
//                System.out.println("score = " + score);
//                System.out.println("newscore = " + newscore);
                if (newscore.compareTo(score) > 0) {
                    OpActionPlan clone = new OpActionPlanCloner().cloneSolution(this);
                    clone.checkActionList();
                    score = newscore;
                    bestPlan = clone;
                    bestPlan.setScore(score);
                }
            }
        } finally {
            set.remove(action.getOrigId());
        }
        return bestPlan;
    }

    public OpActionPlan comboSearch() {
        this.checkActionList();
        comboSearchScored = 0;
        comboSearchSkipped = 0;
        OpActionPlan startPlan = greedySearch();
        OpActionPlan clone = new OpActionPlanCloner().cloneSolution(startPlan);
        clone.checkActionList();
        startPlan.checkActionList();
        List<OpAction> actions = startPlan.actions;
        if (null == actions) {
            throw new NullPointerException("startPlan.actions == null : startPlan =" + startPlan);
        }
        if (actions.isEmpty()) {
            throw new RuntimeException("startPlan.actions.isEmpty() : startPlan=" + startPlan);
        }
        OpAction start = startPlan.findStartAction();
        if (null == start) {
            throw new NullPointerException("startPlan.findStartAction() returned null : startPlan =" + startPlan);
        }
        double minRemainingCost = 0;
        double totalCost = 0;
        int requiredPickups = 0, availPickups = 0, requiredDrops = 0, availDrops = 0;
        for (OpAction act : actions) {
            double minActCost = findMinActCost(act, startPlan);
            double cost = act.cost(startPlan);
            totalCost += cost;
            if (cost < minActCost) {
                System.out.println("act = " + act);
                System.out.println("cost = " + cost);
                System.out.println("minActost = " + minActCost);
                double minActCost2 = findMinActCost(act, startPlan);
                double cost2 = act.cost(startPlan);
            }
            minRemainingCost += minActCost;
            if (act.getOpActionType() == PICKUP) {
                availPickups++;
                if (act.isRequired()) {
                    requiredPickups++;
                }
            } else if (act.getOpActionType() == DROPOFF) {
                availDrops++;
                if (act.isRequired()) {
                    requiredDrops++;
                }
            }
        }
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore startPlanScore = calculator.calculateScore(startPlan);
        if (minRemainingCost > -1000.0 * startPlanScore.getSoftScore()) {
            System.out.println("score = " + startPlanScore);
            System.out.println("minRemainingCost = " + minRemainingCost);
            System.out.println("totalCost = " + totalCost);
        }

        return startPlan.comboSearch(start, clone, new TreeSet<>(), startPlanScore, 0, minRemainingCost, requiredPickups, availPickups, requiredDrops, availDrops);
    }

    private double findMinActCost(OpAction act, OpActionPlan startPlan) {
        if (act.getOpActionType() == FAKE_DROPOFF || act.getOpActionType() == FAKE_PICKUP) {
            return 0;
        }
        List<OpActionInterface> possibles = act.getPossibleNextActions();
        double minActCost = Double.POSITIVE_INFINITY;
        for (OpActionInterface p : possibles) {
            if (p instanceof OpAction) {
                OpAction possibleNextAct = (OpAction) p;
                if (possibleNextAct.getOpActionType() == FAKE_DROPOFF || possibleNextAct.getOpActionType() == FAKE_PICKUP) {
                    return 0;
                }
                double cost = act.costOfNext(possibleNextAct, startPlan);
                if (cost < minActCost) {
                    minActCost = cost;
                }
            }
        }
        return minActCost;
    }

    public OpActionPlan clonePlan() {
        return new OpActionPlanCloner().cloneSolution(this);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, Object>[] createMapsArray() {
        final List<OpAction> actions1 = this.getActions();
        if (null == actions1) {
            return new Map[0];
        }
        Map<String, Object>[] scoreVals = new Map[actions1.size()];
        for (int i = 0; i < scoreVals.length; i++) {
            OpAction actI = actions1.get(i);
            scoreVals[i] = createIMap(i, actI, this);
        }
        return scoreVals;
    }

    public void printDiffs(PrintStream ps, OpActionPlan otherPlan) {
        List<Map<String, Object>[]> mapArray
                = createMapsArrayDiffList(otherPlan);
        printMapsArrayDiffList(ps, mapArray);
        ps.println();
        ps.println("this.getMoves().size() = " + this.getMoves().size());
        ps.println("otherPlan.getMoves().size() = " + otherPlan.getMoves().size());
        for (int i = 0; i < this.getMoves().size() || i < otherPlan.getMoves().size(); i++) {
            AbstractMove<OpActionPlan> thisMoveI = (i < this.getMoves().size()) ? this.getMoves().get(i) : null;
            AbstractMove<OpActionPlan> otherMoveI = (i < otherPlan.getMoves().size()) ? otherPlan.getMoves().get(i) : null;
            if (thisMoveI != otherMoveI) {
                if (null != thisMoveI) {
                    System.out.println(i + ": thisMoveI = " + thisMoveI);
                }
                if (null != otherMoveI) {
                    System.out.println(i + ": otherMoveI = " + otherMoveI);
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Map<String, Object>[]> createMapsArrayDiffList(OpActionPlan otherPlan) {
        final List<OpAction> thisActions = this.getActions();
        if (null == thisActions) {
            return Collections.emptyList();
        }
        final List<OpAction> otherPlanActions = otherPlan.getActions();
        if (null == otherPlanActions) {
            return Collections.emptyList();
        }
        List<Map<String, Object>[]> ret = new ArrayList<>();
        for (int i = 0; i < Math.min(thisActions.size(), otherPlanActions.size()); i++) {
            OpAction actI = thisActions.get(i);
            OpAction otherActI = otherPlanActions.get(i);
            double costActI = actI.cost(this);
            double costOtherActI = otherActI.cost(otherPlan);
            double costDiff = costActI - costOtherActI;
            if (Math.abs(costDiff) > 0.01) {
                Map<String, Object>[] mapArray = new Map[2];
                mapArray[0] = createIMap(i, actI, this);
                mapArray[1] = createIMap(i, otherActI, otherPlan);
                ret.add(mapArray);
            }
        }
        return ret;
    }

    public static void printMapsArrayDiffList(PrintStream ps, List<Map<String, Object>[]> list) {
        for (Map<String, Object>[] mapArray : list) {
            Map<String, Object> map0 = mapArray[0];
            Map<String, Object> map1 = mapArray[1];
            for (String key : map0.keySet()) {
                ps.println(key + ": \t" + map0.get(key) + " \t" + map1.get(key));
            }
            ps.println();
        }
    }

    private Map<String, Object> createIMap(int i, OpAction actI, OpActionPlan opActionPlan) {
        List<OpAction> actIToScordList = actI.previousScoreList();
        List<String> actIToScordNameList = new ArrayList<>();
        List<Integer> actIToScoreIndexList = new ArrayList<>();
        List<Double> actICostList = new ArrayList<>();
        final List<OpAction> actions1 = opActionPlan.getActions();
        if (null == actions1) {
            return Collections.emptyMap();
        }
        for (int j = 0; j < actIToScordList.size(); j++) {
            OpAction actIToScore = actIToScordList.get(j);
            int index = actions1.indexOf(actIToScore);
            actIToScoreIndexList.add(index);
            double actICost = actIToScore.cost(this);
            actICostList.add(actICost);
            actIToScordNameList.add(actIToScore.getName());
        }
        List<OpAction> nextList = actI.nextsToEffectiveNextList();
        List<String> nextNameList = new ArrayList<>();
        List<Integer> nextIndexList = new ArrayList<>();
        List<Double> nextCostList = new ArrayList<>();
        for (int j = 0; j < nextList.size(); j++) {
            OpAction next = nextList.get(j);
            nextNameList.add(next.getName());
            int nextIndex = actions1.indexOf(next);
            nextIndexList.add(nextIndex);
            double nextCost = next.cost(this);
            nextCostList.add(nextCost);
        }
        Map<String, Object> map = new TreeMap<>();
        map.put("i", i);
        map.put("actI", actI.getName());
        final OpActionInterface actINext = actI.getNext();
        if(null != actINext) {
            map.put("actI.getNext()", actINext.getName());
        } else {
             map.put("actI.getNext()", "null");
        }
        map.put("cost", actI.cost(this));
        map.put("actIToScoreIndexList", actIToScoreIndexList);
        map.put("nextIndexList", nextIndexList);
        map.put("actIToScoreNameList", actIToScordNameList);
        map.put("nextNameList", nextNameList);
        map.put("actICostList", actICostList);
        return map;
    }

    private final List<AbstractMove<OpActionPlan>> moves = new ArrayList<>();

    public void addMove(AbstractMove<OpActionPlan> move) {
        moves.add(move);
    }

    public List<AbstractMove<OpActionPlan>> getMoves() {
        return moves;
    }

    public OpActionPlan cloneAndShufflePlan() {
        List<OpAction> actions = this.getActions();
        if (null == actions) {
            throw new RuntimeException("inPlan.getActions() returned null : this=" + this);
        }
        if (actions.isEmpty()) {
            throw new RuntimeException("inPlan.getActions().isEmpty() : this=" + this);
        }
        List<OpAction> newActionsList = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            OpAction origAction = actions.get(i);
            if (origAction.getOpActionType() != OpActionType.FAKE_DROPOFF && origAction.getOpActionType() != OpActionType.FAKE_PICKUP) {
                OpAction newAction = new OpAction(
                        origAction.getName(),
                        origAction.getLocation().x,
                        origAction.getLocation().y,
                        origAction.getOpActionType(),
                        origAction.getPartType(),
                        origAction.isRequired()
                );
                newActionsList.add(newAction);
            }
        }
        OpActionPlan newPlan = new OpActionPlan();
        newPlan.setAccelleration(this.getAccelleration());
        newPlan.setDebug(this.isDebug());
        newPlan.setMaxSpeed(this.getMaxSpeed());
        newPlan.setStartEndMaxSpeed(this.getStartEndMaxSpeed());
        newPlan.getEndAction().getLocation().x = this.getEndAction().getLocation().x;
        newPlan.getEndAction().getLocation().y = this.getEndAction().getLocation().y;
        newPlan.setUseDistForCost(this.isUseDistForCost());
        newPlan.setUseStartEndCost(this.isUseStartEndCost());
        Collections.shuffle(newActionsList);
        newPlan.setActions(newActionsList);
        newPlan.initNextActions();
        return newPlan;
    }

    @ProblemFactCollectionProperty
    private List<OpEndAction> endActions = Collections.singletonList(endAction);

    @ValueRangeProvider(id = "endActions")
    public List<OpEndAction> getEndActions() {
        return endActions;
    }

    public void setEndActions(List<OpEndAction> endActions) {
        this.endActions = endActions;
    }

    @PlanningEntityCollectionProperty
    private @Nullable
    List<OpAction> actions;

    public @Nullable
    List<OpAction> getActions() {
        return actions;
    }

    public static void checkActionsList(@Nullable List<OpAction> actionsToCheck) {
        if (null != actionsToCheck) {
            IdentityHashMap<OpActionInterface, OpActionInterface> inverseEntityMap = new IdentityHashMap<>();
            for (OpAction opAction : actionsToCheck) {
                OpActionInterface nxtAction = opAction.getNext();
                if (null != nxtAction) {
                    OpActionInterface old = inverseEntityMap.put(nxtAction, opAction);
                    if (null != old) {
                        throw new IllegalStateException("opAction=" + opAction + ",nxtAction=" + nxtAction + ",old=" + old);
                    }
                }
            }
        }
    }

    private volatile StackTraceElement lastCheckActionListTrace @Nullable []  = null;

    public void checkActionList() {
        List<OpAction> orderedActions = this.getOrderedList(true);
        List<OpAction> notInBaseList = this.notInBaseList(orderedActions);
        if (!notInBaseList.isEmpty()) {
            System.err.println("orderedActions=" + orderedActions);
            System.err.println("lastCheckActionListTrace=" + Utils.traceToString(lastCheckActionListTrace));
            throw new IllegalStateException("notInBaseList = " + notInBaseList);
        }
        List<OpAction> notInOrderedList = this.notInOrderedList(orderedActions);
        if (!notInOrderedList.isEmpty()) {
            System.err.println("orderedActions=" + orderedActions);
            System.err.println("lastCheckActionListTrace=" + Utils.traceToString(lastCheckActionListTrace));
            throw new IllegalStateException("notInOrderedList = " + notInOrderedList);
        }
        checkActionsList(getActions());
        lastCheckActionListTrace = Thread.currentThread().getStackTrace();
    }

    private static final String CSV_HEADERS[] = {
        "Index", "Id", "OrigId", "Name", "Type", "PartType", "ExecutorType", "ExecutorArgs", "X", "Y", "Cost", "Required", "Skipped", "NextId", "NextOrigId", "PossibleNextIds", "PossibleNextOrigIds"
    };

    private static @Nullable
    Object[] propRecord(String propName, Object propValue) {
        return new Object[]{
            -1, -1, -1, propName + "=" + propValue, SET_PROPERTY_PROPNAME, null, null, null, Double.NaN, Double.NaN, Double.NaN, true, false, Collections.emptyList()
        };
    }

    private static final String SET_PROPERTY_PROPNAME = "SET_PROPERTY";

    public static boolean isSkippedAction(OpAction action, @Nullable OpActionInterface prevAction) {
        if (null != prevAction && prevAction.getNext() != action) {
            throw new IllegalArgumentException("prevAction.getNext() != action : action=" + action + ",prevAction=" + prevAction);
        }
        final OpActionInterface actionNext = action.getNext();
        if (action.getOpActionType() == FAKE_DROPOFF || action.getOpActionType() == FAKE_PICKUP) {
            return true;
        } else if (action.getOpActionType() == PICKUP && null != actionNext && actionNext.getOpActionType() == FAKE_DROPOFF) {
            return true;
        } else if (null == prevAction || prevAction.getOpActionType() == FAKE_PICKUP) {
            if (action.getOpActionType() == START) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private @Nullable
    Object[] actionRecord(int index, OpActionInterface actionInterface, boolean skipped) {
        final OpActionInterface next = actionInterface.getNext();
        int nextId = (next != null) ? next.getId() : -1;
        int nextOrigId = (next != null) ? next.getOrigId() : -1;
        ActionType executorType = null;
        String executorArgs = null;
        if (actionInterface instanceof OpAction) {
            OpAction action = (OpAction) actionInterface;
            executorType = action.getExecutorActionType();
            executorArgs = Arrays.toString(action.getExecutorArgs());
        }
        return new Object[]{
            index, actionInterface.getId(), actionInterface.getOrigId(), actionInterface.getName(), actionInterface.getOpActionType(), actionInterface.getPartType(), executorType, executorArgs, actionInterface.getLocation().x, actionInterface.getLocation().y, actionInterface.cost(this), actionInterface.isRequired(), skipped, nextId, nextOrigId, actionInterface.getPossibleNextIds(), actionInterface.getPossibleNextOrigIds()
        };
    }

    public List<OpAction> notInOrderedList() {
        List<OpAction> orderedActions = this.getOrderedList(true);
        return notInOrderedList(orderedActions);
    }

    public List<OpAction> notInOrderedList(List<OpAction> orderedActions) {
        final List<OpAction> actionsFinal = actions;
        if (null == actionsFinal) {
            throw new NullPointerException("actions");
        }
        List<OpAction> ret = new ArrayList<>();
        for (int i = 0; i < actionsFinal.size(); i++) {
            OpAction actionI = actionsFinal.get(i);
            boolean found = false;
            for (int j = 0; j < orderedActions.size(); j++) {
                OpAction actionJ = orderedActions.get(j);
                if (actionJ == actionI) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                ret.add(actionI);
            }
        }
        return ret;
    }

    public List<OpAction> notInBaseList() {
        List<OpAction> orderedActions = this.getOrderedList(true);
        return notInBaseList(orderedActions);
    }

    private List<OpAction> notInBaseList(List<OpAction> orderedActions) {
        final List<OpAction> actionsFinal = actions;
        if (null == actionsFinal) {
            throw new NullPointerException("actions");
        }
        List<OpAction> ret = new ArrayList<>();
        for (int i = 0; i < orderedActions.size(); i++) {
            OpAction actionI = orderedActions.get(i);
            boolean found = false;
            for (int j = 0; j < actionsFinal.size(); j++) {
                OpAction actionJ = actionsFinal.get(j);
                if (actionJ == actionI) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                ret.add(actionI);
            }
        }
        return ret;
    }

    @SuppressWarnings("nullness")
    static private void printPropRecord(CSVPrinter printer, String propName, @Nullable Object propValue) throws IOException {
        if (propValue != null) {
            printer.printRecord(propRecord(propName, propValue));
        }
    }

    @SuppressWarnings("nullness")
    private void printActionRecord(CSVPrinter printer, int index, OpActionInterface actionInterface, boolean skipped) throws IOException {
        printer.printRecord(actionRecord(index, actionInterface, skipped));
    }

    private static final File recentActionListsFile = new File(System.getProperty("user.home"), ".recentActionListsFile");

    private static final List<File> recentActionListFiles = new ArrayList<>();

    private static volatile boolean recentActionsFileListRead = false;

    private static synchronized void readRecentActionListFile() throws IOException {
        try {
            if (recentActionListsFile.exists()) {
                
                List<File> newRecentActionListFiles = new ArrayList<>();
                try (BufferedReader br = new BufferedReader(new FileReader(recentActionListsFile))) {
                    String line = br.readLine();
                    while (line != null) {
                        File f = new File(line);
                        if (f.exists() && f.canRead()) {
                            newRecentActionListFiles.add(f);
                        }
                        line = br.readLine();
                    }
                }
                Collections.sort(newRecentActionListFiles, Comparators.byLongFunction(File::lastModified));
                if (newRecentActionListFiles.size() > 12) {
                    while (newRecentActionListFiles.size() > 12) {
                        newRecentActionListFiles.remove(0);
                    }
                    try (PrintWriter pw = new PrintWriter(new FileWriter(recentActionListsFile))) {
                        for (int i = 0; i < recentActionListFiles.size(); i++) {
                            File f = recentActionListFiles.get(i);
                            pw.println(f.getCanonicalPath());
                        }
                    }
                }
                recentActionListFiles.clear();
                recentActionListFiles.addAll(newRecentActionListFiles);
            }
        } finally {
            recentActionsFileListRead = true;
        }
    }

    public static synchronized List<File> getRecentActionListFiles() {
        if (!recentActionsFileListRead) {
            try {
                readRecentActionListFile();
            } catch (IOException ex) {
                Logger.getLogger(OpActionPlan.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                recentActionsFileListRead = true;
            }
        }
        return java.util.Collections.unmodifiableList(new ArrayList<>(recentActionListFiles));
                //recentActionListFiles;
    }

    private void addRecentActionListFile(File f) throws IOException {
        if (!recentActionsFileListRead) {
            try {
                readRecentActionListFile();
            } finally {
                recentActionsFileListRead = true;
            }
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(recentActionListsFile, true))) {
            pw.println(f.getCanonicalPath());
        }
        recentActionListFiles.add(f);
    }

    public void saveActionList(File f) throws IOException {
        addRecentActionListFile(f);
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(f), CSVFormat.DEFAULT.withHeader(CSV_HEADERS))) {

            printPropRecord(printer, USE_DIST_FOR_COST_PROPNAME, useDistForCost);
            printPropRecord(printer, USE_START_END_COST_PROPNAME, useStartEndCost);
            printPropRecord(printer, DEBUG_PROPNAME, debug);
            printPropRecord(printer, "score", score);
            printPropRecord(printer, MAX_SPEED_PROPNAME, maxSpeed);
            printPropRecord(printer, START_END_MAX_SPEED_PROPNAME, startEndMaxSpeed);
            printPropRecord(printer, ACCELLERATION_PROPNAME, accelleration);
            List<OpAction> orderedActions = this.getOrderedList(true);
            OpActionInterface prevAction = null;
            for (int i = 0; i < orderedActions.size(); i++) {
                final OpAction action = orderedActions.get(i);
                boolean skipped = isSkippedAction(action, prevAction);
                prevAction = action;
                printActionRecord(printer, i, action, skipped);
            }
            printActionRecord(printer, orderedActions.size(), endAction, false);
        }
    }

    public static OpActionPlan loadActionList(File f) throws IOException {
        OpActionPlan plan = new OpActionPlan();
        plan.privateLoadActionList(f);
        return plan;
    }

    private void privateLoadActionList(File file) throws IOException {
        final List<OpAction> actionsFinal;
        if (null == actions) {
            actionsFinal = new ArrayList<>();
            actions = actionsFinal;
        } else {
            actionsFinal = actions;
        }
        actionsFinal.clear();
        String fileScore = "";
//        Map<Integer, OpActionInterface> idActionMap = new HashMap<>();
//        Map<Integer, OpActionInterface> origIdActionMap = new HashMap<>();
//        Map<OpAction, Integer> idNextMap = new IdentityHashMap<>();
//        Map<OpAction, Integer> origIdNextMap = new IdentityHashMap<>();
        int recordCount = 0;
        try (CSVParser parser = new CSVParser(new FileReader(file), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                recordCount++;
                String type = record.get("Type");
                try {
                    if (type.equals(SET_PROPERTY_PROPNAME)) {
                        String name = record.get("Name");
                        int eindex = name.indexOf('=');
                        String vname = name.substring(0, eindex);
                        String vval = name.substring(eindex + 1);
                        switch (vname) {
                            case USE_DIST_FOR_COST_PROPNAME:
                                useDistForCost = Boolean.parseBoolean(vval);
                                break;

                            case USE_START_END_COST_PROPNAME:
                                useStartEndCost = Boolean.parseBoolean(vval);
                                break;

                            case DEBUG_PROPNAME:
                                debug = Boolean.parseBoolean(vval);
                                break;

                            case MAX_SPEED_PROPNAME:
                                maxSpeed = Double.parseDouble(vval);
                                break;

                            case START_END_MAX_SPEED_PROPNAME:
                                startEndMaxSpeed = Double.parseDouble(vval);
                                break;
                            case ACCELLERATION_PROPNAME:
                                accelleration = Double.parseDouble(vval);
                                break;

                            case SCORE_PROPNAME:
                                fileScore = vval;
                                break;
                        }
                        continue;
                    } else if (!type.equals("END")) {
                        // String name, double x, double y, OpActionType opActionType, String partType, boolean required
                        String name = record.get("Name");
                        double x = Double.parseDouble(record.get("X"));
                        double y = Double.parseDouble(record.get("Y"));
                        boolean required = Boolean.parseBoolean(record.get("Required"));
                        int id = Integer.parseInt(record.get("Id"));
                        OpAction action;
                        final String origIdString = record.get("OrigId");
                        if (null != origIdString && origIdString.length() > 0) {
                            int origId = Integer.parseInt(origIdString);
                            action = new OpAction(origId, name, x, y, OpActionType.valueOf(type), record.get("PartType"), required);
//                        origIdActionMap.put(origId, action);
                        } else {
                            action = new OpAction(name, x, y, OpActionType.valueOf(type), record.get("PartType"), required);
                        }
//                    idActionMap.put(id, action);
//                    final String nextIdString = record.get("NextId");
//                    if (null != nextIdString && nextIdString.length() > 0) {
//                        int nextId = Integer.parseInt(nextIdString);
//                        idNextMap.put(action, nextId);
//                    }
//                    final String nextOrigIdString = record.get("NextOrigId");
//                    if (null != nextIdString && nextIdString.length() > 0) {
//                        int nextOrigId = Integer.parseInt(nextIdString);
//                        origIdNextMap.put(action, nextOrigId);
//                    }
                        actionsFinal.add(action);
                    } else if (type.equals("END")) {
                        if (null == endAction) {
                            throw new NullPointerException("endAction");
                        }
                        double x = Double.parseDouble(record.get("X"));
                        double y = Double.parseDouble(record.get("Y"));
//                    int id = Integer.parseInt(record.get("Id"));
//                    final String origIdString = record.get("OrigId");
//                    if (null != origIdString && origIdString.length() > 0) {
//                        int origId = Integer.parseInt(origIdString);
//                        origIdActionMap.put(origId, endAction);
//                    }
//                    idActionMap.put(id, endAction);
                        endAction.setLocation(new Point2D.Double(x, y));
                    }
                } catch (Exception e) {
                    Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "recordCount=" + recordCount + ",type=" + type + ",record=" + record, e);
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(AprsSystem.class.getName()).log(Level.SEVERE, "file=" + file, e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
        List<OpActionInterface> actionsWithEnd = new ArrayList<>(actionsFinal);
        actionsWithEnd.add(endAction);
        for (int i = 0; i < actionsFinal.size(); i++) {
            OpAction actionI = actionsFinal.get(i);
            OpActionInterface next;
            if (i < actionsFinal.size() - 1) {
                next = actionsFinal.get(i + 1);
            } else {
                next = endAction;
            }
            actionI.addPossibleNextActions(actionsWithEnd);
            actionI.setNext(next);
        }
//        initNextActions();
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore calculatedScore = calculator.calculateScore(this);
        if (null == score) {
            score = calculatedScore;
        }
        if (!fileScore.equals(calculatedScore.toString())) {
            throw new RuntimeException("fileScore = " + fileScore + ", calculatedScore = " + score);
        }
    }
    private static final String ACCELLERATION_PROPNAME = "accelleration";
    private static final String START_END_MAX_SPEED_PROPNAME = "startEndMaxSpeed";
    private static final String MAX_SPEED_PROPNAME = "maxSpeed";
    private static final String DEBUG_PROPNAME = "debug";
    private static final String USE_START_END_COST_PROPNAME = "useStartEndCost";
    private static final String USE_DIST_FOR_COST_PROPNAME = "useDistForCost";
    private static final String SCORE_PROPNAME = "score";
    private boolean useDistForCost = true;

    /**
     * Get the value of useDistForCost
     *
     * @return the value of useDistForCost
     */
    public boolean isUseDistForCost() {
        return useDistForCost;
    }

    /**
     * Set the value of useDistForCost
     *
     * @param useDistForCost new value of useDistForCost
     */
    public void setUseDistForCost(boolean useDistForCost) {
        this.useDistForCost = useDistForCost;
    }

    private boolean useStartEndCost = true;

    /**
     * Get the value of useStartEndCost
     *
     * @return the value of useStartEndCost
     */
    public boolean isUseStartEndCost() {
        return useStartEndCost;
    }

    /**
     * Set the value of useStartEndCost
     *
     * @param useStartEndCost new value of useStartEndCost
     */
    public void setUseStartEndCost(boolean useStartEndCost) {
        this.useStartEndCost = useStartEndCost;
    }

    private boolean debug;

    /**
     * Get the value of debug
     *
     * @return the value of debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Set the value of debug
     *
     * @param debug new value of debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setActions(List<OpAction> actions) {
        this.actions = actions;
    }

    @SuppressWarnings("unused")
    public void initNextActions() {
        final List<OpAction> actionsFinal = actions;

        if (null == actionsFinal) {
            throw new IllegalStateException("actions not initialized");
        }

        List<OpAction> tmpActions = new ArrayList<>();
        for (int i = 0; i < actionsFinal.size(); i++) {
            OpAction actionI = actionsFinal.get(i);
            actionI.getPossibleNextActions().clear();
            actionI.clearNext();
            if (actionI.getOpActionType() != FAKE_PICKUP && actionI.getOpActionType() != FAKE_DROPOFF) {
                tmpActions.add(actionI);
            }
        }
        List<OpAction> origActions = new ArrayList<>(actionsFinal);

        if (debug) {
            println("origActions = " + origActions);
        }
        for (OpAction act : tmpActions) {
            if (act.getOpActionType() == FAKE_PICKUP || act.getOpActionType() == FAKE_DROPOFF) {
                throw new IllegalStateException("input list should not have fake actions : act =" + act + ",\norigActions=" + origActions);
            }
        }
        MutableMultimap<String, OpAction> multimapWithList
                = Lists.mutable.ofAll(tmpActions)
                        .select(a -> a.getOpActionType() == PICKUP || a.getOpActionType() == DROPOFF)
                        .groupBy(OpAction::getPartType);
        for (String partType : multimapWithList.keySet()) {
            MutableCollection<OpAction> thisPartActions = multimapWithList.get(partType);
            MutableCollection<OpAction> pickupThisPartActions = thisPartActions.select(a -> a.getOpActionType() == PICKUP);
            long pickupCount = pickupThisPartActions.size();
            MutableCollection<OpAction> dropoffThisPartActions = thisPartActions.select(a -> a.getOpActionType() == DROPOFF);
            long dropoffCount = dropoffThisPartActions.size();
            if (true) {
                println("partType = " + partType);
                println("dropoffCount = " + dropoffCount);
                println("pickupCount = " + pickupCount);
                println("thisPartActions = " + thisPartActions);
                println("pickupThisPartActions = " + pickupThisPartActions);
                println("dropoffThisPartActions = " + dropoffThisPartActions);
                println("multimapWithList = " + multimapWithList);
            }
            OpAction.AddFakeInfo addFakeInfo = new OpAction.AddFakeInfo(pickupThisPartActions, dropoffThisPartActions);
            if (dropoffCount < pickupCount) {
                for (long j = dropoffCount; j < pickupCount; j++) {
                    tmpActions.add(new OpAction("fake_dropoff_" + partType + "_" + j, 0, 0, FAKE_DROPOFF, partType, addFakeInfo));
                }
            } else if (pickupCount < dropoffCount) {
                for (long j = pickupCount; j < dropoffCount; j++) {
                    tmpActions.add(new OpAction("fake_pickup_" + partType + "_" + j, 0, 0, FAKE_PICKUP, partType, addFakeInfo));
                }
            }
        }
        List<OpActionInterface> unusedActions = new ArrayList<>(tmpActions);
        unusedActions.addAll(endActions);
        List<OpActionInterface> allActions = new ArrayList<>(unusedActions);
        OpAction startAction = findStartAction();
        for (OpAction act : tmpActions) {
            act.addPossibleNextActions(allActions);
            if (act.getPossibleNextActions().isEmpty()) {
                System.out.println("");
                System.out.flush();
                System.err.println("act = " + act);
                System.err.println("act.getPartType() = " + act.getPartType());
                System.err.println("act.addFakeInfo = " + act.addFakeInfo);
                System.err.println("tmpActions = " + tmpActions);
                println("multimapWithList = " + multimapWithList);
                throw new IllegalStateException("action has no possible next action=" + act + ", act.getPartType()=" + act.getPartType() + ",\norigActions=" + origActions + ",\nallActions=" + allActions + ",\nmultimapWithList=" + multimapWithList);
            }
        }
        for (OpAction act : tmpActions) {
            boolean actRequired = act.isRequired();
            act.getPossibleNextActions().sort(new Comparator<OpActionInterface>() {
                @Override
                public int compare(OpActionInterface o1, OpActionInterface o2) {
                    return Integer.compare(o1.getPriority(actRequired), o2.getPriority(actRequired));
                }
            });
        }
        List<OpAction> newActions = new ArrayList<>();
        if (null != startAction) {
            newActions.add(startAction);
            unusedActions.remove(startAction);
        }
        OpAction lastStartAction = startAction;
        OpActionInterface lastNextAction = null;
        while (startAction != null) {
            OpAction action = startAction;
            lastStartAction = startAction;
            startAction = null;
            for (OpActionInterface nxtAction : action.getPossibleNextActions()) {
                lastNextAction = nxtAction;
                if (unusedActions.contains(nxtAction)) {
                    unusedActions.remove(nxtAction);

                    action.setNext(nxtAction);
                    if (nxtAction.getOpActionType() != END && nxtAction instanceof OpAction) {
                        newActions.add((OpAction) nxtAction);
                        startAction = (OpAction) nxtAction;
                    } else if (debug) {
                        println("Ending with action =" + action);
                        println("nxtAction = " + nxtAction);
                    }
                    break;
                }
            }
            if (action.getNext() == null) {
                throw new IllegalStateException("action has null next:action=" + action + ",\norigActions=" + origActions + ",\nnewActions=" + newActions + ",\naction.getPossibleNextActions()" + action.getPossibleNextActions() + ",\nunusedActions=" + unusedActions);
            }
        }
        if (debug) {
            println("origActions = " + origActions);
            println("newActions.size() = " + newActions.size());
            for (int i = 0; i < newActions.size(); i++) {
                OpAction act = newActions.get(i);
                if (act.getNext() == null) {
                    throw new IllegalStateException("action has null next:i=" + i + ",act=" + act + ",\norigActions=" + tmpActions + ",\nnewActions=" + newActions);
                }
                if (act.getPossibleNextActions() == null || act.getPossibleNextActions().isEmpty()) {
                    throw new IllegalStateException("action has no possibleNextAction :" + act);
                }
                println("i=" + i + ", " + act + ".getPossibleNextActions() = "
                        + act.getPossibleNextActions()
                                .stream()
                                .map(OpActionInterface::getName)
                                .collect(Collectors.joining(",")));
            }
        }
        actions = Collections.unmodifiableList(new ArrayList<>(newActions));
        if (debug && unusedActions.size() > 0) {
            println("unusedActions = " + unusedActions);
            println("actions = " + actionsFinal);
        }

        if (debug) {
            List<OpAction> effectiveOrderedList = getEffectiveOrderedList(true);
            println("effectiveOrderedList = " + effectiveOrderedList);
        }
    }

    private @Nullable
    HardSoftLongScore score;

    @PlanningScore
    public HardSoftLongScore getScore() {
        if (null == score) {
            HardSoftLongScore defaultScore = HardSoftLongScore.of(Long.MIN_VALUE, Long.MIN_VALUE);
            score = defaultScore;
            return defaultScore;
        }
        return score;
    }

    public void setScore(HardSoftLongScore score) {
        this.score = score;
    }

    private volatile String asString = "";

    @Override
    public String toString() {
        return asString;
    }

    private double maxSpeed = 1.0;

    /**
     * Get the value of maxSpeed
     *
     * @return the value of maxSpeed
     */
    public double getMaxSpeed() {
        return maxSpeed;
    }

    /**
     * Set the value of maxSpeed
     *
     * @param maxSpeed new value of maxSpeed
     */
    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    private double startEndMaxSpeed = 2 * maxSpeed;

    /**
     * Get the value of startEndMaxSpeed
     *
     * @return the value of startEndMaxSpeed
     */
    public double getStartEndMaxSpeed() {
        return startEndMaxSpeed;
    }

    /**
     * Set the value of startEndMaxSpeed
     *
     * @param startEndMaxSpeed new value of startEndMaxSpeed
     */
    public void setStartEndMaxSpeed(double startEndMaxSpeed) {
        this.startEndMaxSpeed = startEndMaxSpeed;
    }

    private double accelleration = 1.0;

    /**
     * Get the value of accelleration
     *
     * @return the value of accelleration
     */
    public double getAccelleration() {
        return accelleration;
    }

    /**
     * Set the value of accelleration
     *
     * @param accelleration new value of accelleration
     */
    public void setAccelleration(double accelleration) {
        this.accelleration = accelleration;
    }

    public String computeString() {
        double totalCost = 0;
        OpAction startAction = findStartAction();
        StringBuilder sb = new StringBuilder();
        OpActionInterface tmp = startAction;
        Set<String> visited = new HashSet<>();
        List<OpActionInterface> notInList = new ArrayList<>();
        List<OpAction> localActionsList = getActions();
        if (null != localActionsList) {
            int maxCount = localActionsList.size();
            int count = 0;
            while (tmp != null && tmp.getOpActionType() != END) {
                count++;
                if (count >= maxCount) {
                    break;
                }
                if (tmp != startAction) {
                    sb.append(" -> ");
                }
                if (tmp instanceof OpAction) {
                    boolean inList = false;
                    for (OpAction action : localActionsList) {
                        if (action == tmp) {
                            inList = true;
                            break;
                        }
                    }
                    if (!inList) {
                        notInList.add(tmp);
                    }
                    OpAction actionTmp = (OpAction) tmp;
                    visited.add(actionTmp.getName());
                    if (actionTmp.getNext() == null) {
                        sb.append(" -> ");
                        tmp = null;
                        break;
                    }
                    totalCost += actionTmp.cost(this);
                    sb.append(actionTmp.getName());
                    OpActionInterface effNext = actionTmp.effectiveNext(true);
                    if (null != effNext && effNext != actionTmp.getNext()) {
                        sb.append("(effectiveNext=");
                        sb.append(effNext.getName());
                        sb.append(")");
                    }
                    sb.append(String.format("(%.2f)", actionTmp.distance(true)));
                } else {
                    sb.append(tmp.getOpActionType());
                }
                tmp = tmp.getNext();
            }

            sb.append(": ");
            sb.append(totalCost);
            if (notInList.size() > 0) {
                sb.append(" notInList(").append(notInList).append(")");
            }
            double recheckCost = 0;
            count = 0;
            for (OpAction action : localActionsList) {
                count++;
                if (count >= maxCount) {
                    break;
                }
                if (action.getNext() == null) {
                    sb.append(" NULL_NEXT");
                    if (!visited.contains(action.getName())) {
                        sb.append(" NOT_VISITED(").append(action.getName()).append(") ");
                    }
                    continue;
                }
                recheckCost += action.cost(this);
                if (!visited.contains(action.getName())) {
                    sb.append(" NOT_VISITED(").append(action.getName()).append(") ");
                }
            }
            if (visited.size() != localActionsList.size()) {
                sb.append(" visitedSize(").append(visited.size()).append(") ");
            }
            if (Math.abs(recheckCost - totalCost) > 1e-6) {
                sb.append(" recheckCost(").append(recheckCost).append(") ");
            }
        }
        String ret = sb.toString();
        this.asString = ret;
        return ret;
    }

    public List<String> getOrderedListNames() {
        List<OpAction> l = getOrderedList(true);
        List<String> names = new ArrayList<>();
        for (OpAction act : l) {
            names.add(act.getName());
        }
        return names;
    }

    public List<OpAction> getOrderedList(boolean quiet) {
        List<OpAction> l = new ArrayList<>();
        OpAction startAction = findStartAction();
        if (null == startAction) {
            if (quiet) {
                return l;
            }
            throw new IllegalStateException("findStartAction returned null");
        }
        l.add(startAction);
        OpActionInterface tmp = startAction;
        while (null != tmp) {
            tmp = tmp.getNext();
            if (!(tmp instanceof OpAction)) {
                return l;
            }
            OpAction tmpAction = (OpAction) tmp;
            if (null == tmp) {
                return l;
            }
            if (tmpAction.getOpActionType() == END) {
                l.add(tmpAction);
                return l;
            }
            if (l.contains(tmpAction)) {
                if (quiet) {
                    return l;
                }
                throw new IllegalStateException("loop found: tmp=" + tmp + ",l=" + l);
            }
            l.add(tmpAction);
        }
        return l;
    }

    public List<OpAction> getEffectiveOrderedList(boolean quiet) {
        List<OpAction> l = new ArrayList<>();
        try {
            OpAction startAction = findStartAction();
            if (null == startAction) {
                throw new IllegalStateException("findStartAction returned null");
            }
            l.add(startAction);
            OpActionInterface tmp = startAction;
            while (null != tmp) {
                tmp = tmp.effectiveNext(quiet);
                if (!(tmp instanceof OpAction)) {
                    return l;
                }
                OpAction tmpAction = (OpAction) tmp;
                if (null == tmp) {
                    return l;
                }
                if (tmpAction.getOpActionType() == END) {
                    return l;
                }
                if (l.contains(tmpAction)) {
                    if (quiet) {
                        return l;
                    }
                    throw new IllegalStateException("loop found: tmp=" + tmp);
                }
                l.add(tmpAction);
            }
        } catch (Exception illegalStateException) {
            System.err.println("actions = " + actions);
            throw new IllegalStateException("l =" + l, illegalStateException);
        }
        return l;
    }

    public @Nullable
    OpAction findStartAction() {
        OpAction startAction = null;
        List<OpAction> l = actions;
        if (null != l) {
            for (OpAction action : l) {
                if (action.getOpActionType() == START) {
                    startAction = action;
                }
            }
        }
        return startAction;
    }

}
