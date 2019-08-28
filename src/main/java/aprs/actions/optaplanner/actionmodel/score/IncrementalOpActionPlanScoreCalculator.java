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
package aprs.actions.optaplanner.actionmodel.score;

import aprs.actions.optaplanner.actionmodel.OpAction;
import aprs.actions.optaplanner.actionmodel.OpActionPlan;
import aprs.misc.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.impl.score.director.incremental.IncrementalScoreCalculator;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class IncrementalOpActionPlanScoreCalculator implements IncrementalScoreCalculator<OpActionPlan> {

    private volatile HardSoftLongScore score;
    private final EasyOpActionPlanScoreCalculator easyOpActionPlanScoreCalculator;
    private volatile @Nullable
    OpActionPlan checkPlan;

    private volatile @Nullable
    OpActionPlan lastCheckedPlanCopy;

    public IncrementalOpActionPlanScoreCalculator() {
        this.easyOpActionPlanScoreCalculator = new EasyOpActionPlanScoreCalculator();
        this.score = this.easyOpActionPlanScoreCalculator.calculateScore(new OpActionPlan());
    }

    public static HardSoftLongScore computeInitScore(OpActionPlan opActionPlan) {
        IncrementalOpActionPlanScoreCalculator incrementalOpActionPlanScoreCalculator
                = new IncrementalOpActionPlanScoreCalculator();
        incrementalOpActionPlanScoreCalculator.resetWorkingSolution(opActionPlan);
        return incrementalOpActionPlanScoreCalculator.score;
    }

    private HardSoftLongScore ofAction(OpAction act, OpActionPlan opActionPlan) {
        long hardScore;
        if (!act.checkNextAction(act.getNext())) {
            hardScore = -1;
        } else {
            hardScore = 0;
        }
        long softScore = (long) (-1000.0 * act.cost(opActionPlan));
        return HardSoftLongScore.of(hardScore, softScore);
    }

    private final Map<Integer, Double> sumActionsCostMap = new TreeMap<>();
    private final Map<Integer, Double> lastSumActionsCostMap = new TreeMap<>();

    private HardSoftLongScore sumActions(OpActionPlan opActionPlan) {
        final List<OpAction> actionsList = opActionPlan.getActions();
        sumActionsCostMap.clear();
        if (null == actionsList) {
            return HardSoftLongScore.of(Long.MIN_VALUE, Long.MIN_VALUE);
        }
        HardSoftLongScore sumActionsScore = HardSoftLongScore.ZERO;
        for (OpAction act : actionsList) {
            sumActionsCostMap.put(opActionPlan.getActions().indexOf(act), act.cost(opActionPlan));
            final HardSoftLongScore actScore = ofAction(act, opActionPlan);
            sumActionsScore = sumActionsScore.add(actScore);
        }
        return sumActionsScore;
    }

    @Override
    public void resetWorkingSolution(OpActionPlan workingSolution) {
        this.checkPlan = workingSolution;
        lastCheckedPlanCopy = checkPlan.clonePlan();
        final HardSoftLongScore sumActionsScore = sumActions(workingSolution);
        this.score = sumActionsScore;
        beforeCount = 0;
        afterCount = 0;
        if (DO_SCORE_CHECKS) {
            HardSoftLongScore easyScore = this.easyOpActionPlanScoreCalculator.calculateScore(workingSolution);
            if (!scoresCloseEnough(easyScore, sumActionsScore)) {
                throw new RuntimeException("easyScore=" + easyScore + ", sumActionsScore=" + sumActionsScore);
            }
            callInfoLog.add("resetWorkingSolution : score=" + score + ", workingSolution=" + workingSolution);
        }
    }

    private boolean scoresCloseEnough(HardSoftLongScore score1, HardSoftLongScore score2) {
        return score1.getHardScore() == score2.getHardScore()
                && Math.abs(score1.getSoftScore() - score2.getSoftScore()) < 25;
    }

    @Override
    public void beforeEntityAdded(Object entity) {
        if (!(entity instanceof OpAction)) {
            throw new RuntimeException("entity= " + entity);
        }

    }

    @Override
    public void afterEntityAdded(Object entity) {
        if (!(entity instanceof OpAction)) {
            throw new RuntimeException("entity= " + entity);
        }
        OpAction act = (OpAction) entity;
        final HardSoftLongScore actScore = ofAction(act, checkPlan);
        score = score.add(actScore);
        if (DO_SCORE_CHECKS) {
            checkScore();
            callInfoLog.add("afterEntityAdded : act=" + act + ",score=" + score);
        }
    }

    private static final boolean DO_SCORE_CHECKS =  Boolean.getBoolean("aprs.doScoreChecks");

    private volatile HardSoftLongScore lastCheckedScore = null;
    private volatile StackTraceElement lastCheckScoreTrace[] = null;
    private Map<String, Object>[] checkScoreVals = null;

    private void checkScore() throws RuntimeException {
        HardSoftLongScore initScore = computeInitScore(checkPlan);
        if (!score.equals(initScore)) {
            printErrorsAndThrowRuntimeException(initScore);
        }
        if (DO_SCORE_CHECKS) {
            final HardSoftLongScore sumActionsScore = sumActions(checkPlan);
            if (!scoresCloseEnough(score, sumActionsScore)) {
                printErrorsAndThrowRuntimeException(sumActionsScore);
            }
            lastSumActionsCostMap.clear();
            lastSumActionsCostMap.putAll(sumActionsCostMap);
            final HardSoftLongScore easyScore = this.easyOpActionPlanScoreCalculator.calculateScore(checkPlan);
            if (!scoresCloseEnough(score, easyScore)) {
                Map<Integer, Double> costMap = this.easyOpActionPlanScoreCalculator.getCostMap();
                System.out.println("costMap.keySet() = " + costMap.keySet());
                System.out.println("sumActionsCostMap.keySet() = " + sumActionsCostMap.keySet());
                Set<Integer> costMapKeySetCopy1 = new TreeSet<>(costMap.keySet());
                Set<Integer> sumActionsCostMapKeySetCopy1 = new TreeSet<>(sumActionsCostMap.keySet());
                costMapKeySetCopy1.removeAll(sumActionsCostMapKeySetCopy1);
                System.out.println("costMapKeySetCopy1.removeAll(sumActionsCostMapKeySetCopy1)= " + costMapKeySetCopy1);
                Set<Integer> costMapKeySetCopy2 = new TreeSet<>(costMap.keySet());
                Set<Integer> sumActionsCostMapKeySetCopy2 = new TreeSet<>(sumActionsCostMap.keySet());
                sumActionsCostMapKeySetCopy2.removeAll(costMapKeySetCopy2);
                System.out.println("csumActionsCostMapKeySetCopy2.removeAll(costMapKeySetCopy2)= " + sumActionsCostMapKeySetCopy2);
                for (Integer index : sumActionsCostMapKeySetCopy2) {
                    OpAction act = checkPlan.getActions().get(index);
                    System.out.println("act = " + act + ", index=" + index);
                }
                System.out.println("costMap = " + costMap);
                System.out.println("sumActionsCostMap = " + sumActionsCostMap);
                throw new RuntimeException("score=" + score + ", easyScore=" + easyScore + ",lastCheckedScore=" + lastCheckedScore + ",costMap=" + costMap + ",sumActionsCostMap=" + sumActionsCostMap);
            }
            callInfoLog.clear();
            lastCheckScoreTrace = Thread.currentThread().getStackTrace();
            Map<String, Object>[] scoreVals = checkPlan.createMapsArray();
            checkScoreVals = scoreVals;
        }
        lastCheckedPlanCopy = checkPlan.clonePlan();
        lastCheckedScore = HardSoftLongScore.of(score.getHardScore(), score.getSoftScore());
        processedActions.clear();
        processedActionsNewScoreMap.clear();
        processedActionsOldScoreMap.clear();
    }

    private void printErrorsAndThrowRuntimeException(final HardSoftLongScore sumActionsScore) throws RuntimeException {
        synchronized (this.getClass()) {
            long diffSoftScoreSumActionsScore = score.getSoftScore() - sumActionsScore.getSoftScore();
            if (null != lastCheckedScore) {
                long diffSoftScoreLastCheckedScore = score.getSoftScore() - lastCheckedScore.getSoftScore();
                System.out.println("diffSoftScoreLastCheckedScore = " + diffSoftScoreLastCheckedScore);
                long diffSoftSumActionScoreLastCheckedScore = sumActionsScore.getSoftScore() - lastCheckedScore.getSoftScore();
                System.out.println("diffSoftSumActionScoreLastCheckedScore = " + diffSoftSumActionScoreLastCheckedScore);
            }
            final String errMsg = "score=" + score + ",diffSoftScoreSumActionsScore=" + diffSoftScoreSumActionsScore + ", sumActionsScore=" + sumActionsScore + ",lastCheckedScore=" + lastCheckedScore;
            System.err.println(errMsg);
            flushDumpStackFlush();
            System.out.println("checkPlan = " + checkPlan);
            System.out.println("lastCheckedPlanCopy = " + lastCheckedPlanCopy);
            lastCheckedPlanCopy.printDiffs(System.out, checkPlan);
            System.out.println("callInfoLog=");
            for (String callInfoString : callInfoLog) {
                System.out.println(callInfoString);
            }
            System.err.println("");
            System.out.flush();
            System.err.flush();
            System.out.println("lastCheckScoreTrace = " + Utils.traceToString(lastCheckScoreTrace));
            Map<String, Object>[] scoreVals = checkPlan.createMapsArray();
            System.out.println("scoreVals.length = " + scoreVals.length);
            if (null != checkScoreVals) {
                System.out.println("checkScoreVals.length = " + checkScoreVals.length);
                for (int i = 0; i < scoreVals.length; i++) {
                    Map<String, Object> scoreVal = scoreVals[i];
                    if (processedActions.contains(i)) {
                        continue;
                    }
                    Map<String, Object> checkScoreVal = checkScoreVals[i];
                    final Object checkScoreValCostObject = checkScoreVal.get("cost");
                    if (!(checkScoreValCostObject instanceof Double)) {
                        System.out.println("checkScoreVal = " + checkScoreVal);
                        System.out.println("checkScoreValCostObject = " + checkScoreValCostObject);
                        continue;
                    }
                    final Double checkCost = (Double) checkScoreValCostObject;
                    final Object scoreValCostObject = scoreVal.get("cost");
                    if (!(scoreValCostObject instanceof Double)) {
                        System.out.println("scoreVal = " + scoreVal);
                        System.out.println("scoreValCostObject = " + scoreValCostObject);
                        continue;
                    }
                    final Double scoreCost = (Double) scoreValCostObject;
                    final double scoreDiff = scoreCost - checkCost;
                    if (Math.abs(scoreDiff) < 0.0001) {
                        continue;
                    }
                    System.out.println("i = " + i);
                    System.out.println("scoreDiff = " + scoreDiff);
                    for (String key : scoreVal.keySet()) {
                        System.out.println("old " + key + "=" + checkScoreVal.get(key));
                        System.out.println("new " + key + "=" + scoreVal.get(key));
                    }
                    System.out.println("");
                    System.err.println("");
                    System.out.flush();
                    System.err.flush();
                }
            } else {
                System.out.println("checkScoreVals=null");
            }
            System.out.println("processedActions = " + processedActions);
            Collections.sort(processedActions);
            System.out.println("processedActions = " + processedActions);
            System.out.println("processedActionsOldScoreMap = " + processedActionsOldScoreMap);
            System.out.println("processedActionsNewScoreMap = " + processedActionsNewScoreMap);
            System.out.println("sumActionsCostMap     = " + sumActionsCostMap);
            System.out.println("lastSumActionsCostMap = " + lastSumActionsCostMap);
            flushDumpStackFlush();
            System.err.println(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    private void flushDumpStackFlush() {
        System.out.println();
        System.err.println();
        System.out.flush();
        System.err.flush();
        Thread.dumpStack();
        System.out.println();
        System.err.println();
        System.out.flush();
        System.err.flush();
    }

    private Map<OpAction, HardSoftLongScore> oldScoreMap = new IdentityHashMap<>();
    private Map<OpAction, List<OpAction>> oldPrevListMap = new IdentityHashMap<>();
    private final ConcurrentLinkedDeque<String> callInfoLog = new ConcurrentLinkedDeque<>();

    private int beforeCount = 0;

    @Override
    public void beforeVariableChanged(Object entity, String variableName) {
        if (!(entity instanceof OpAction)) {
            throw new RuntimeException("entity= " + entity);
        }
        beforeCount++;
        OpAction act = (OpAction) entity;
        int actIndex = checkPlan.getActions().indexOf(act);
        List<OpAction> actToScoreList = act.previousScoreList();
        List<Integer> actToScoreIndexList = new ArrayList<>();
        List<HardSoftLongScore> actScoreList = new ArrayList<>();
        for (OpAction actToScore : actToScoreList) {
            actToScoreIndexList.add(checkPlan.getActions().indexOf(actToScore));
            final HardSoftLongScore actScore = ofAction(actToScore, checkPlan);
            actScoreList.add(actScore);
            oldScoreMap.put(actToScore, actScore);
        }
        oldPrevListMap.put(act, actToScoreList);
        if (DO_SCORE_CHECKS) {
            callInfoLog.add("beforeVariableChanged actIndex=" + actIndex + ",actToScoreIndexList=" + actToScoreIndexList + ", act=" + act + ",actToScoreList=" + actToScoreList + ", actScoreList=" + actScoreList + ",score=" + score);
        }
    }

    private final List<Integer> processedActions = new ArrayList<>();
    private final Map<Integer, HardSoftLongScore> processedActionsOldScoreMap = new TreeMap<>();
    private final Map<Integer, HardSoftLongScore> processedActionsNewScoreMap = new TreeMap<>();

    private int afterCount = 0;

    @Override
    public void afterVariableChanged(Object entity, String variableName) {
        if (!(entity instanceof OpAction)) {
            throw new RuntimeException("entity= " + entity);
        }
        afterCount++;
        OpAction act = (OpAction) entity;
        int actIndex = checkPlan.getActions().indexOf(act);
        List<OpAction> actToScoreList = oldPrevListMap.remove(act);
        if(null == actToScoreList) {
            throw new RuntimeException("oldPrevListMap.remove("+act+") returned null");
        }
        List<Integer> actToScoreIndexList = new ArrayList<>();
        List<HardSoftLongScore> oldScoresList = new ArrayList<>();
        for (OpAction actToScore : actToScoreList) {
            final int actToScoreIndex = checkPlan.getActions().indexOf(actToScore);
            actToScoreIndexList.add(actToScoreIndex);
            HardSoftLongScore oldScore = oldScoreMap.remove(actToScore);
            oldScoresList.add(oldScore);
            if (null != oldScore) {
                final HardSoftLongScore actScore = ofAction(actToScore, checkPlan);
                processedActions.add(actToScoreIndex);
                processedActionsOldScoreMap.put(actToScoreIndex, oldScore);
                processedActionsNewScoreMap.put(actToScoreIndex, actScore);
                score = score.subtract(oldScore);
                score = score.add(actScore);
                if (DO_SCORE_CHECKS) {
                    callInfoLog.add("afterVariableChanged actIndex=" + actIndex + ", actToScoreIndex=" + actToScoreIndex + ",oldScore=" + oldScore + ",actScore=" + actScore + ",score=" + score + ",act=" + act.getName());
                }
            } else if (DO_SCORE_CHECKS) {
                final HardSoftLongScore actScore = ofAction(actToScore, checkPlan);
                callInfoLog.add("afterVariableChanged actIndex=" + actIndex + ", actToScoreIndex=" + actToScoreIndex + ",oldScore=" + oldScore + ",actScore=" + actScore + ",score=" + score + ",act=" + act.getName());
            }
        }
        if (DO_SCORE_CHECKS) {
            List<OpAction> newActToScoreList = act.previousScoreList();
            List<Integer> newActToScoreIndexList = new ArrayList<>();
            for (OpAction newActToScore : newActToScoreList) {
                newActToScoreIndexList.add(checkPlan.getActions().indexOf(newActToScore));
            }
            callInfoLog.add("afterVariableChanged actIndex=" + actIndex + ", actToScoreIndexList=" + actToScoreIndexList + ",newActToScoreIndexList=" + newActToScoreIndexList + ",act=" + act.getName() + ",actToScoreList=" + actToScoreList + ",next=" + act.getNext() + ", oldScoresList=" + oldScoresList + ",score=" + score);
            if (beforeCount == afterCount) {
                try {
                    checkScore();
                } catch (Exception exception) {
                    Logger.getLogger(IncrementalOpActionPlanScoreCalculator.class.getName()).log(Level.SEVERE, "actToScoreList=" + actToScoreList + ",act=" + act + ",variableName=" + variableName, exception);
                    if (exception instanceof RuntimeException) {
                        throw (RuntimeException) exception;
                    } else {
                        throw new RuntimeException(exception);
                    }
                }
            }
        }
    }

    @Override
    public void beforeEntityRemoved(Object entity) {
        if (!(entity instanceof OpAction)) {
            throw new RuntimeException("entity= " + entity);
        }
    }

    @Override
    public void afterEntityRemoved(Object entity) {
        if (!(entity instanceof OpAction)) {
            throw new RuntimeException("entity= " + entity);
        }
        OpAction act = (OpAction) entity;
        final HardSoftLongScore actScore = ofAction(act, checkPlan);
        score = score.subtract(actScore);
        checkScore();
    }

    @Override
    public HardSoftLongScore calculateScore() {
        if (!oldScoreMap.isEmpty()) {
            throw new RuntimeException("oldScoreMap=" + oldScoreMap);
        }
        checkScore();
        return score;
    }

}
