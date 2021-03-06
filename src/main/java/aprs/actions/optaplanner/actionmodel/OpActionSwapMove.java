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
package aprs.actions.optaplanner.actionmodel;

import static aprs.actions.optaplanner.actionmodel.OpActionType.DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_PICKUP;
import static aprs.actions.optaplanner.actionmodel.OpActionType.PICKUP;
import aprs.actions.optaplanner.actionmodel.score.EasyOpActionPlanScoreCalculator;
import aprs.actions.optaplanner.actionmodel.score.IncrementalOpActionPlanScoreCalculator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;
import org.optaplanner.core.impl.score.director.ScoreDirector;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class OpActionSwapMove extends AbstractMove<OpActionPlan> {

    private final OpAction action1;
    private final OpAction action2;

    private @Nullable
    OpActionPlan undoOrigPlanCopy = null;
    private @Nullable
    HardSoftLongScore undoOrigEasyScore = null;
    private @Nullable
    HardSoftLongScore undoOrigSdScore = null;

    public OpActionSwapMove(OpAction action1, OpAction action2) {
        this.action1 = action1;
        this.action2 = action2;
    }

    public OpAction getAction1() {
        return action1;
    }

    public OpAction getAction2() {
        return action2;
    }

    private static final boolean DO_SCORE_CHECKS = Boolean.getBoolean("aprs.doScoreChecks");

    @Override
    protected OpActionSwapMove createUndoMove(ScoreDirector<OpActionPlan> scoreDirector) {
        OpActionSwapMove undoMove = new OpActionSwapMove(action2, action1);
        if (DO_SCORE_CHECKS) {
            undoMove.undoOrigSdScore = (HardSoftLongScore) scoreDirector.calculateScore();
            undoMove.undoOrigPlanCopy = scoreDirector.getWorkingSolution().clonePlan();
            undoMove.undoOrigEasyScore = new EasyOpActionPlanScoreCalculator().calculateScore(scoreDirector.getWorkingSolution());
        }
        return undoMove;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<OpActionPlan> scoreDirector) {
        final OpAction action1Previous = action1.getPrevious();
        if (null == action1Previous) {
            throw new RuntimeException("null ==  action1.getPrevious() : action1=" + action1);
        }
        final OpAction action2Previous = action2.getPrevious();
        if (null == action2Previous) {
            throw new RuntimeException("null ==  action2.getPrevious() : action2=" + action2);
        }
        final OpActionInterface action1Next = action1.getNext();
        final OpActionInterface action2Next = action2.getNext();

        scoreDirector.getWorkingSolution().addMove(this);
        scoreDirector.beforeVariableChanged(action1, "next");
        scoreDirector.beforeVariableChanged(action2, "next");
        scoreDirector.beforeVariableChanged(action1Previous, "next");
        scoreDirector.beforeVariableChanged(action2Previous, "next");
        action1Previous.setNext(action2);
        action2Previous.setNext(action1);
        action1.setNext(action2Next);
        action2.setNext(action1Next);
        scoreDirector.afterVariableChanged(action1, "next");
        scoreDirector.afterVariableChanged(action2, "next");
        scoreDirector.afterVariableChanged(action1Previous, "next");
        scoreDirector.afterVariableChanged(action2Previous, "next");
        if (DO_SCORE_CHECKS) {
            checkUndoScore(scoreDirector);
        }

    }

    private void checkUndoScore(ScoreDirector<OpActionPlan> scoreDirector) throws RuntimeException {
        final HardSoftLongScore undoOrigSdScoreLocalFinal = undoOrigSdScore;
        final OpActionPlan undoOrigPlanCopyLocalFinal = undoOrigPlanCopy;
        if (null != undoOrigSdScoreLocalFinal && null != undoOrigPlanCopyLocalFinal) {
            synchronized (OpActionSwapMove.class) {
                HardSoftLongScore newSdScore;
                try {
                    newSdScore = IncrementalOpActionPlanScoreCalculator.computeInitScore(scoreDirector.getWorkingSolution());
                } catch (Exception ex) {
                    List<Map<String, Object>[]> diffList
                            = undoOrigPlanCopyLocalFinal.createMapsArrayDiffList(scoreDirector.getWorkingSolution());
                    System.out.println("diffList.size() = " + diffList.size());
                    OpActionPlan.printMapsArrayDiffList(System.out, diffList);
                    System.out.println("");
                    System.err.println("");
                    System.out.flush();
                    System.out.flush();
                    if (ex instanceof RuntimeException) {
                        throw (RuntimeException) ex;
                    } else {
                        throw new RuntimeException(ex);
                    }
                }

                if (undoOrigSdScoreLocalFinal.compareTo(newSdScore) != 0) {
                    List<Map<String, Object>[]> diffList = undoOrigPlanCopyLocalFinal.createMapsArrayDiffList(scoreDirector.getWorkingSolution());
                    System.out.println("diffList.size() = " + diffList.size());
                    OpActionPlan.printMapsArrayDiffList(System.out, diffList);
                    System.out.println("");
                    System.err.println("");
                    System.out.flush();
                    System.out.flush();
                    throw new RuntimeException("newSdScore = " + newSdScore + ", undoOrigSdScore=" + undoOrigSdScoreLocalFinal);
                }
            }
            if (null != undoOrigEasyScore) {
                HardSoftLongScore newEasyScore = new EasyOpActionPlanScoreCalculator().calculateScore(scoreDirector.getWorkingSolution());
                if (undoOrigSdScoreLocalFinal.compareTo(newEasyScore) != 0) {
                    Thread.dumpStack();
                    List<Map<String, Object>[]> diffList = undoOrigPlanCopyLocalFinal.createMapsArrayDiffList(scoreDirector.getWorkingSolution());
                    System.out.println("diffList.size() = " + diffList.size());
                    OpActionPlan.printMapsArrayDiffList(System.out, diffList);
                    System.out.println("");
                    System.err.println("");
                    System.out.flush();
                    System.out.flush();
                    throw new RuntimeException("newEasyScore = " + newEasyScore + ", undoOrigSdScore=" + undoOrigSdScoreLocalFinal);
                }
            }
        }
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<OpActionPlan> scoreDirector) {
        if (action1 == action2) {
            return false;
        }
        final OpActionInterface action1Next = action1.getNext();
        if (action1Next == action2) {
            return false;
        }
        final OpAction action1Previous = action1.getPrevious();
        if (action1Previous == action2) {
            return false;
        }
        if (action1Next == null) {
            return false;
        }
        if (action1Previous == null) {
            return false;
        }
        final OpActionInterface action2Next = action2.getNext();
        if (action2Next == action1) {
            return false;
        }
        final OpAction action2Previous = action2.getPrevious();
        if (action2Previous == action1) {
            return false;
        }
        if (action2Next == null) {
            return false;
        }
        if (action2Previous == null) {
            return false;
        }
        switch (action1.getOpActionType()) {
            case PICKUP:
            case FAKE_PICKUP:
                if (action2.getOpActionType() == DROPOFF || action2.getOpActionType() == FAKE_DROPOFF) {
                    return false;
                }
                break;

            case DROPOFF:
            case FAKE_DROPOFF:
                if (action2.getOpActionType() == PICKUP || action2.getOpActionType() == FAKE_PICKUP) {
                    return false;
                }
                break;
            case END:
            case START:
                return false;
        }
        switch (action2.getOpActionType()) {
            case END:
            case START:
                return false;

            default:
                break;
        }
        final String action1PartType = action1.getPartType();
        if (action1PartType == null) {
            return false;
        }
        if (!action1PartType.equals(action2.getPartType())) {
            return false;
        }
        if (!action1Previous.getPossibleNextActions().contains(action2)) {
            return false;
        }
        if (!action2Previous.getPossibleNextActions().contains(action1)) {
            return false;
        }
        if (!action1.getPossibleNextActions().contains(action2Next)) {
            return false;
        }
        if (!action2.getPossibleNextActions().contains(action1Next)) {
            return false;
        }
        return true;
    }

    @Override
    public List<OpAction> getPlanningEntities() {
        List<OpAction> entities = new ArrayList<>();
        entities.addAll(action1.previousScoreList());
        entities.addAll(action2.previousScoreList());
        return entities;
    }

    @Override
    public List<OpActionInterface> getPlanningValues() {
        List<OpAction> entities = this.getPlanningEntities();
        List<OpActionInterface> nexts = new ArrayList<>();
        for (OpAction entity : entities) {
            final OpActionInterface entityNext = entity.getNext();
            if (null != entityNext) {
                nexts.add(entityNext);
            }
        }
        return nexts;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.action1);
        hash = 59 * hash + Objects.hashCode(this.action2);
        return hash;
    }

    @Override
    @SuppressWarnings({"nullness", "initialization"})
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OpActionSwapMove other = (OpActionSwapMove) obj;
        if (!Objects.equals(this.action1, other.action1)) {
            return false;
        }
        if (!Objects.equals(this.action2, other.action2)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "OpActionSwapMove{" + "action1=" + action1 + ", action2=" + action2 + ", undoOrigPlanCopy=" + undoOrigPlanCopy + ", undoOrigEasyScore=" + undoOrigEasyScore + ", undoOrigSdScore=" + undoOrigSdScore + '}';
    }

}
