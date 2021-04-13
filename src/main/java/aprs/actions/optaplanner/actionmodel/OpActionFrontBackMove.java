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

import static aprs.actions.optaplanner.actionmodel.OpActionType.START;
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
public class OpActionFrontBackMove extends AbstractMove<OpActionPlan> {

    private final OpAction start;
    private final OpAction origStartNext;
    private final OpEndAction end;
    private final OpAction origEndPrev;
    private final OpAction splitter;
    private final OpAction origSplitterPrev;
//    private final StackTraceElement[] createTrace;
    private @Nullable
    OpActionPlan undoOrigPlanCopy = null;
    private @Nullable
    HardSoftLongScore undoOrigEasyScore = null;
    private @Nullable
    HardSoftLongScore undoOrigSdScore = null;

    public OpActionFrontBackMove(OpAction start, OpEndAction end, OpAction splitter) {
        this.start = start;
        this.end = end;
        final OpAction endPrevious = end.getPrevious();
        if (null == endPrevious) {
            throw new RuntimeException("null == end.getPrevious() : end=" + end);
        }
        this.origEndPrev = endPrevious;
        this.splitter = splitter;
        final OpActionInterface startNext = start.getNext();
        if (!(startNext instanceof OpAction)) {
            throw new RuntimeException("!(start.getNext() instanceof OpAction): start=" + start);
        }
        this.origStartNext = (OpAction) startNext;
        final OpAction splitterPrevious = splitter.getPrevious();
        if (null == splitterPrevious) {
            throw new RuntimeException("null == splitter.getPrevious() : splitter=" + splitter);
        }
        this.origSplitterPrev = splitterPrevious;
//        this.createTrace = Thread.currentThread().getStackTrace();
    }

    @Override
    public OpActionFrontBackMove createUndoMove(ScoreDirector<OpActionPlan> sd) {
        OpActionFrontBackMove undoMove = new OpActionFrontBackMove(start, end, origStartNext);
        if (DO_SCORE_CHECKS) {
            undoMove.undoOrigSdScore = (HardSoftLongScore) sd.calculateScore();
            undoMove.undoOrigPlanCopy = sd.getWorkingSolution().clonePlan();
            undoMove.undoOrigEasyScore = new EasyOpActionPlanScoreCalculator().calculateScore(sd.getWorkingSolution());
        }
        return undoMove;
    }

    private static final boolean DO_SCORE_CHECKS = Boolean.getBoolean("aprs.doScoreChecks");

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<OpActionPlan> scoreDirector) {
        scoreDirector.getWorkingSolution().addMove(this);
//        boolean moveable = isMoveDoable(scoreDirector);

        OpActionInterface currentStartNext = start.getNext();
        OpAction currentEndPrev = end.getPrevious();

        if (null == currentEndPrev) {
            throw new RuntimeException("null == currentEndPrev");
        }
        OpAction currentSplitterPrev = splitter.getPrevious();
        if (null == currentSplitterPrev) {
            throw new RuntimeException("null == currentSplitterPrev");
        }

        scoreDirector.beforeVariableChanged(start, "next");
        scoreDirector.beforeVariableChanged(currentEndPrev, "next");
        scoreDirector.beforeVariableChanged(currentSplitterPrev, "next");
        start.setNext(splitter);
        currentEndPrev.setNext(currentStartNext);
        currentSplitterPrev.setNext(end);

        scoreDirector.afterVariableChanged(start, "next");
        scoreDirector.afterVariableChanged(currentEndPrev, "next");
        scoreDirector.afterVariableChanged(currentSplitterPrev, "next");
        if (DO_SCORE_CHECKS) {
            checkUndoScore(scoreDirector);
        }
    }

    private void checkUndoScore(ScoreDirector<OpActionPlan> scoreDirector) {
        final HardSoftLongScore thisUndoScore = undoOrigSdScore;
        final OpActionPlan thisUndoPlan = undoOrigPlanCopy;
        if (null != thisUndoScore && null != thisUndoPlan) {
            HardSoftLongScore newSdScore;
            try {
                newSdScore = IncrementalOpActionPlanScoreCalculator.computeInitScore(scoreDirector.getWorkingSolution());
            } catch (Exception ex) {
                List<Map<String, Object>[]> diffList = thisUndoPlan.createMapsArrayDiffList(scoreDirector.getWorkingSolution());
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
            if (thisUndoScore.compareTo(newSdScore) != 0) {
                List<Map<String, Object>[]> diffList = thisUndoPlan.createMapsArrayDiffList(scoreDirector.getWorkingSolution());
                System.out.println("diffList.size() = " + diffList.size());
                OpActionPlan.printMapsArrayDiffList(System.out, diffList);
                System.out.println("");
                System.err.println("");
                System.out.flush();
                System.out.flush();
                throw new RuntimeException("newSdScore = " + newSdScore + ", undoOrigSdScore=" + thisUndoScore);
            }
            final HardSoftLongScore thisUndoEasyScore = undoOrigEasyScore;
            if (null != thisUndoEasyScore) {
                HardSoftLongScore newEasyScore = new EasyOpActionPlanScoreCalculator().calculateScore(scoreDirector.getWorkingSolution());
                if (thisUndoScore.compareTo(newEasyScore) != 0) {
                    List<Map<String, Object>[]> diffList = thisUndoPlan.createMapsArrayDiffList(scoreDirector.getWorkingSolution());
                    OpActionPlan.printMapsArrayDiffList(System.out, diffList);
                    Thread.dumpStack();
                    System.out.println("diffList.size() = " + diffList.size());
                    OpActionPlan.printMapsArrayDiffList(System.out, diffList);
                    System.out.println("");
                    System.err.println("");
                    System.out.flush();
                    System.out.flush();
                    throw new RuntimeException("newEasyScore = " + newEasyScore + ", undoOrigSdScore=" + thisUndoScore);
                }
            }
        }
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<OpActionPlan> sd) {
        OpActionInterface splitterNext = splitter.getNext();
        if (!(splitterNext instanceof OpAction)) {
            return false;
        }
        final OpActionInterface currentStartNext = start.getNext();

        if (currentStartNext == splitter) {
            return false;
        }
        final OpAction currentSplitterPrevious = splitter.getPrevious();
        if (currentSplitterPrevious == start) {
            return false;
        }
        if (currentSplitterPrevious == null) {
            return false;
        }
        if (start.getOpActionType() != START) {
            return false;
        }
        if (!start.getPossibleNextActions().contains(splitter)) {
            return false;
        }
        if (!currentSplitterPrevious.getPossibleNextActions().contains(end)) {
            return false;
        }
        final OpAction endPrevious = end.getPrevious();
        if (null != endPrevious && !endPrevious.getPossibleNextActions().contains(currentStartNext)) {
            return false;
        }
        return true;
    }

    @Override
    public List<OpAction> getPlanningEntities() {
        List<OpAction> entities = new ArrayList<>();
        entities.addAll(start.previousScoreList());
        final OpAction splitterPrevious = splitter.getPrevious();
        if (null != splitterPrevious) {
            entities.addAll(splitterPrevious.previousScoreList());
        }
        final OpAction endPrevious = end.getPrevious();
        if (null != endPrevious) {
            entities.addAll(endPrevious.previousScoreList());
        }
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
        hash = 59 * hash + Objects.hashCode(this.start);
        hash = 59 * hash + Objects.hashCode(this.end);
        hash = 59 * hash + Objects.hashCode(this.splitter);
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
        final OpActionFrontBackMove other = (OpActionFrontBackMove) obj;
        if (!Objects.equals(this.start, other.start)) {
            return false;
        }
        if (!Objects.equals(this.end, other.end)) {
            return false;
        }
        if (!Objects.equals(this.splitter, other.splitter)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "OpActionFrontBackMove{" + "start=" + start + ", origStartNext=" + origStartNext + ", end=" + end + ", origEndPrev=" + origEndPrev + ", splitter=" + splitter + ", origSplitterPrev=" + origSplitterPrev + '}';
    }

}
