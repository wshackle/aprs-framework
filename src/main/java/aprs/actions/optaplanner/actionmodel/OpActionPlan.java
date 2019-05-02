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
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.factory.Lists;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactProperty;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.SolverFactory;

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
                    if(!isFake || !fake_found) {
                        possiblesNotInSet.add(possibleAction);
                    }
                    if(isFake) {
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
                    OpActionPlan planForAct2 = simpleExhaustiveSearch((OpAction) possibleAction, bestPlan, set, score, needed);
                    if (planForAct2 != this && planForAct2 != bestPlan) {
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
        List<OpAction> actions = clone.actions;
        OpAction start = clone.findStartAction();
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore score = calculator.calculateScore(clone);
        return clone.simpleExhaustiveSearch(start, clone, new TreeSet<>(), score, 1);
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
                action.setNext(minPossibleAction);
                OpActionPlan planForAct2 = greedySearch((OpAction) minPossibleAction, bestPlan, set, score, needed);
                if (planForAct2 != this && planForAct2 != bestPlan) {
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
        EasyOpActionPlanScoreCalculator calculator = new EasyOpActionPlanScoreCalculator();
        HardSoftLongScore score = calculator.calculateScore(clone);
        return clone.greedySearch(start, clone2, new TreeSet<>(), score, 1);
    }

    private volatile int comboSearchSkipped = 0;

    private OpActionPlan comboSearch(OpAction action, OpActionPlan bestPlan, Set<Integer> set, HardSoftLongScore score, double costSoFar, double minRemainingCost, int requiredPickups, int availPickups, int requiredDrops, int availDrops) {

        set.add(action.getOrigId());
        try {
            List<OpAction> possiblesNotInSet = findPossiblesNotInSet(action, set);

//            List<Boolean> reauired = 
//                    possiblesNotInSet.stream()
//                    .map((OpAction a)-> a.isRequired())
//                    .collect(Collectors.toList());
//            System.out.println("reauired = " + reauired);
//            List<Double> costs = possiblesNotInSet.stream()
//                    .map((OpAction a)-> action.costOfNext(a, this))
//                    .collect(Collectors.toList());
//            System.out.println("costs = " + costs);
            Collections.sort(possiblesNotInSet,
                    Comparators.chain(
                            Comparators.byBooleanFunction((OpAction a) -> !a.isRequired()),
                            Comparators.byDoubleFunction((OpAction a) -> action.costOfNext(a, this))));
//            List<Boolean> reauired2 = 
//                    possiblesNotInSet.stream()
//                    .map((OpAction a)-> a.isRequired())
//                    .collect(Collectors.toList());
//            System.out.println("reauired2 = " + reauired2);
//            List<Double> costs2 = possiblesNotInSet.stream()
//                    .map((OpAction a)-> action.costOfNext(a, this))
//                    .collect(Collectors.toList());
//            System.out.println("costs2 = " + costs2);
//            System.out.println("costs = " + costs);
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
//                System.out.println("ratio = " + ratio);
                if (diffScore > 0 || ratio < 0.05) {
                    comboSearchSkipped++;
//                    int setSize = 1;
//                    for (OpAction act : actions) {
//                        if (!set.contains(act.getOrigId())) {
//                            List<OpAction> actPossiblesNotInSet = findPossiblesNotInSet(act, set);
//                            setSize *= actPossiblesNotInSet.size();
//                        }
//                    }
//                    System.out.println("expectedCost = " + expectedCost);
//                    System.out.println("costSoFar = " + costSoFar);
//                    System.out.println("minRemainingCost = " + minRemainingCost);
//                    System.out.println("score = " + score);
//                    System.out.println("comboSearchSkipped = " + comboSearchSkipped);
//                    System.out.println("setSize = " + setSize);
                    return bestPlan;
                }
                double minCost = findMinActCost(action, bestPlan);
                for (int i = 0; i < possiblesNotInSet.size(); i++) {
                    OpAction possibleAction = possiblesNotInSet.get(i);
                    action.setNext(possibleAction);
                    double cost = action.costOfNext(possibleAction, bestPlan);
                    OpActionPlan planForAct2 = comboSearch((OpAction) possibleAction, bestPlan, set, score, costSoFar + cost, minRemainingCost - minCost, requiredPickups, availPickups, requiredDrops, availDrops);
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
        OpAction start = startPlan.findStartAction();
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
        HardSoftLongScore score = calculator.calculateScore(startPlan);
        if (minRemainingCost > -1000.0 * score.getSoftScore()) {
            System.out.println("score = " + score);
            System.out.println("minRemainingCost = " + minRemainingCost);
            System.out.println("totalCost = " + totalCost);
        }

        return startPlan.comboSearch(start, clone, new TreeSet<>(), score, 0, minRemainingCost, requiredPickups, availPickups, requiredDrops, availDrops);
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

    public static OpActionPlan cloneAndShufflePlan(OpActionPlan inPlan) {
        List<OpAction> actions = inPlan.getActions();
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
        newPlan.setAccelleration(inPlan.getAccelleration());
        newPlan.setDebug(inPlan.isDebug());
        newPlan.setMaxSpeed(inPlan.getMaxSpeed());
        newPlan.setStartEndMaxSpeed(inPlan.getStartEndMaxSpeed());
        newPlan.getEndAction().getLocation().x = inPlan.getEndAction().getLocation().x;
        newPlan.getEndAction().getLocation().y = inPlan.getEndAction().getLocation().y;
        newPlan.setUseDistForCost(inPlan.isUseDistForCost());
        newPlan.setUseStartEndCost(inPlan.isUseStartEndCost());
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

    @Nullable
    public List<OpAction> getActions() {
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

    private volatile StackTraceElement lastCheckActionListTrace[] = null;

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
        "Index", "Id", "Name", "Type", "PartType", "ExecutorType", "ExecutorArgs", "X", "Y", "Cost", "Required", "Skipped", "NextId", "PossibleNexts"
    };

    private Object[] propRecord(String propName, Object propValue) {
        return new Object[]{
            -1, -1, propName + "=" + propValue, SET_PROPERTY_PROPNAME, null, null, null, Double.NaN, Double.NaN, Double.NaN, true, false, Collections.emptyList()
        };
    }
    private static final String SET_PROPERTY_PROPNAME = "SET_PROPERTY";

    public static boolean isSkippedAction(OpAction action, OpActionInterface prevAction) {
        if (null != prevAction && prevAction.getNext() != action) {
            throw new IllegalArgumentException("prevAction.getNext() != action : action=" + action + ",prevAction=" + prevAction);
        }
        if (action.getOpActionType() == FAKE_DROPOFF || action.getOpActionType() == FAKE_PICKUP) {
            return true;
        } else if (action.getOpActionType() == PICKUP && action.getNext().getOpActionType() == FAKE_DROPOFF) {
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

    private Object[] actionRecord(int index, OpActionInterface actionInterface, boolean skipped) {
        final OpActionInterface next = actionInterface.getNext();
        int nextId = (next != null) ? next.getId() : -1;
        ActionType executorType = null;
        String executorArgs = null;
        if (actionInterface instanceof OpAction) {
            OpAction action = (OpAction) actionInterface;
            executorType = action.getExecutorActionType();
            executorArgs = Arrays.toString(action.getExecutorArgs());
        }
        return new Object[]{
            index, actionInterface.getId(), actionInterface.getName(), actionInterface.getOpActionType(), actionInterface.getPartType(), executorType, executorArgs, actionInterface.getLocation().x, actionInterface.getLocation().y, Double.NaN, actionInterface.isRequired(), skipped, nextId, actionInterface.getPossibleNextIds()
        };
    }

    public List<OpAction> notInOrderedList() {
        List<OpAction> orderedActions = this.getOrderedList(true);
        return notInOrderedList(orderedActions);
    }

    public List<OpAction> notInOrderedList(List<OpAction> orderedActions) {
        List<OpAction> ret = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            OpAction actionI = actions.get(i);
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
        List<OpAction> ret = new ArrayList<>();
        for (int i = 0; i < orderedActions.size(); i++) {
            OpAction actionI = orderedActions.get(i);
            boolean found = false;
            for (int j = 0; j < actions.size(); j++) {
                OpAction actionJ = actions.get(j);
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

    public void saveActionList(File f) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(f), Utils.preferredCsvFormat().withHeader(CSV_HEADERS))) {
            printer.printRecord(propRecord(USE_DIST_FOR_COST_PROPNAME, useDistForCost));
            printer.printRecord(propRecord(USE_START_END_COST_PROPNAME, useStartEndCost));
            printer.printRecord(propRecord(DEBUG_PROPNAME, debug));
            printer.printRecord(propRecord("score", score));
            printer.printRecord(propRecord(MAX_SPEED_PROPNAME, maxSpeed));
            printer.printRecord(propRecord(START_END_MAX_SPEED_PROPNAME, startEndMaxSpeed));
            printer.printRecord(propRecord(ACCELLERATION_PROPNAME, accelleration));
            List<OpAction> orderedActions = this.getOrderedList(true);
            OpActionInterface prevAction = null;
            for (int i = 0; i < orderedActions.size(); i++) {
                final OpAction action = orderedActions.get(i);
                boolean skipped = isSkippedAction(action, prevAction);
                prevAction = action;
                printer.printRecord(actionRecord(i, action, skipped));
            }
            printer.printRecord(actionRecord(orderedActions.size(), endAction, false));
        }
    }

    public static OpActionPlan loadActionList(File f) throws IOException {
        OpActionPlan plan = new OpActionPlan();
        plan.privateLoadActionList(f);
        return plan;
    }

    private void privateLoadActionList(File f) throws IOException {
        if (null == actions) {
            actions = new ArrayList<>();
        }
        actions.clear();
        try (CSVParser parser = new CSVParser(new FileReader(f), Utils.preferredCsvFormat().withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                String type = record.get("Type");
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
                    }
                    continue;
                } else if (!type.equals("END") && !type.equals("FAKE_PICKUP") && !type.equals("FAKE_DROPOFF")) {
                    // String name, double x, double y, OpActionType opActionType, String partType, boolean required
                    String name = record.get("Name");
                    double x = Double.parseDouble(record.get("X"));
                    double y = Double.parseDouble(record.get("Y"));
                    boolean required = Boolean.parseBoolean(record.get("Required"));
                    OpAction action = new OpAction(type, x, y, OpActionType.valueOf(type), record.get("PartType"), required);
                    action.setName(name);
                    actions.add(action);
                } else if (type.equals("END")) {
                    if (null == endAction) {
                        throw new NullPointerException("endAction");
                    }
                    double x = Double.parseDouble(record.get("X"));
                    double y = Double.parseDouble(record.get("Y"));
                    endAction.setLocation(new Point2D.Double(x, y));
                }
            }
        }
        initNextActions();
    }
    private static final String ACCELLERATION_PROPNAME = "accelleration";
    private static final String START_END_MAX_SPEED_PROPNAME = "startEndMaxSpeed";
    private static final String MAX_SPEED_PROPNAME = "maxSpeed";
    private static final String DEBUG_PROPNAME = "debug";
    private static final String USE_START_END_COST_PROPNAME = "useStartEndCost";
    private static final String USE_DIST_FOR_COST_PROPNAME = "useDistForCost";
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

        if (null == actions) {
            throw new IllegalStateException("actions not initialized");
        }

        List<OpAction> tmpActions = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            OpAction actionI = actions.get(i);
            actionI.getPossibleNextActions().clear();
            actionI.clearNext();
            if (actionI.getOpActionType() != FAKE_PICKUP && actionI.getOpActionType() != FAKE_DROPOFF) {
                tmpActions.add(actionI);
            }
        }
        List<OpAction> origActions = new ArrayList<>(actions);

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
            MutableCollection<OpAction> theseActions = multimapWithList.get(partType);
            long pickupCount = theseActions.count(a -> a.getOpActionType() == PICKUP);
            long dropoffCount = theseActions.count(a -> a.getOpActionType() == DROPOFF);
            if (debug) {
                println("partType = " + partType);
                println("dropoffCount = " + dropoffCount);
                println("pickupCount = " + pickupCount);
                println("theseActions = " + theseActions);
            }
            if (dropoffCount < pickupCount) {
                for (long j = dropoffCount; j < pickupCount; j++) {
                    tmpActions.add(new OpAction("fake_dropoff_" + partType + "_" + j, 0, 0, FAKE_DROPOFF, partType, false));
                }
            } else if (pickupCount < dropoffCount) {
                for (long j = pickupCount; j < dropoffCount; j++) {
                    tmpActions.add(new OpAction("fake_pickup_" + partType + "_" + j, 0, 0, FAKE_PICKUP, partType, false));
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
                System.err.println("act = " + act);
                System.err.println("tmpActions = " + tmpActions);
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
            println("actions = " + actions);
        }

        if (debug) {
            List<OpAction> effectiveOrderedList = getEffectiveOrderedList(true);
            println("effectiveOrderedList = " + effectiveOrderedList);
        }
    }

    private @Nullable
    HardSoftLongScore score;

    @Nullable
    @PlanningScore
    public HardSoftLongScore getScore() {
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

    @Nullable
    public OpAction findStartAction() {
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
