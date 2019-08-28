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
import crcl.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final StackTraceElement[] createTrace;
    private OpActionPlan undoOrigPlanCopy = null;
    private HardSoftLongScore undoOrigEasyScore = null;
    private HardSoftLongScore undoOrigSdScore = null;
    
    public OpActionFrontBackMove(OpAction start, OpEndAction end, OpAction splitter) {
        this.start = start;
        this.end = end;
        this.origEndPrev = end.getPrevious();
        this.splitter = splitter;
        this.origStartNext = (OpAction) start.getNext();
        this.origSplitterPrev = (OpAction) splitter.getPrevious();
        this.createTrace = Thread.currentThread().getStackTrace();
    }
    
    @Override
    public OpActionFrontBackMove createUndoMove(ScoreDirector<OpActionPlan> sd) {
        OpActionFrontBackMove undoMove = new OpActionFrontBackMove(start, end, origStartNext);
        undoMove.undoOrigSdScore = (HardSoftLongScore) sd.calculateScore();
        undoMove.undoOrigPlanCopy = sd.getWorkingSolution().clonePlan();
        undoMove.undoOrigEasyScore = new EasyOpActionPlanScoreCalculator().calculateScore(sd.getWorkingSolution());
        return undoMove;
    }

//    private static volatile List<OpAction> movedActionList = null;
    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<OpActionPlan> scoreDirector) {
//        System.out.println("createTrace = " + Utils.traceToString(createTrace));
        scoreDirector.getWorkingSolution().addMove(this);
        boolean moveable = isMoveDoable(scoreDirector);
//        System.out.println("moveable = " + moveable);
//        System.out.println("start = " + start);
//        System.out.println("splitter = " + splitter);
//        System.out.println("origStartNext = " + origStartNext);
//        System.out.println("origSplitterPrev = " + origSplitterPrev);
//        OpActionPlan plan = scoreDirector.getWorkingSolution();
//        List<OpAction> beforeList = new ArrayList<>(plan.getOrderedList(true));
//        System.out.println("beforeList = " + beforeList);
//        System.out.println("beforeList.size() = " + beforeList.size());
//        int startBeforeIndex = beforeList.indexOf(start);
//        System.out.println("startBeforeIndex = " + startBeforeIndex);
//        int splitterBeforeIndex = beforeList.indexOf(splitter);
//        System.out.println("splitterBeforeIndex = " + splitterBeforeIndex);
//        int lastBeforeIndex = beforeList.indexOf(origEndPrev);
//        System.out.println("lastBeforeIndex = " + lastBeforeIndex);
//        int splitterPrevBeforeIndex = beforeList.indexOf(origSplitterPrev);
//        System.out.println("splitterPrevBeforeIndex = " + splitterPrevBeforeIndex);

        OpActionInterface currentStartNext = start.getNext();
//        System.out.println("currentStartNext = " + currentStartNext);
        OpAction currentEndPrev = end.getPrevious();
//        System.out.println("currentEndPrev = " + currentEndPrev);
        OpAction currentSplitterPrev = splitter.getPrevious();
//        System.out.println("currentSplitterPrev = " + currentSplitterPrev);

        scoreDirector.beforeVariableChanged(start, "next");
        scoreDirector.beforeVariableChanged(currentEndPrev, "next");
        scoreDirector.beforeVariableChanged(currentSplitterPrev, "next");
        start.setNext(splitter);
        currentEndPrev.setNext(currentStartNext);
        currentSplitterPrev.setNext(end);
//        List<OpAction> newList = new ArrayList<>(plan.getOrderedList(true));
////        System.out.println("newList = " + newList);
////        System.out.println("newList.size() = " + newList.size());
////        int startNewIndex = newList.indexOf(start);
////        System.out.println("startNewIndex = " + startNewIndex);
////        int splitterNewIndex = newList.indexOf(splitter);
////        System.out.println("splitterNewIndex = " + splitterNewIndex);
//        movedActionList = newList;

        scoreDirector.afterVariableChanged(start, "next");
        scoreDirector.afterVariableChanged(currentEndPrev, "next");
        scoreDirector.afterVariableChanged(currentSplitterPrev, "next");
        if (null != undoOrigSdScore) {
            HardSoftLongScore newSdScore;
            try {
                newSdScore = IncrementalOpActionPlanScoreCalculator.computeInitScore(scoreDirector.getWorkingSolution());
            } catch (Exception ex) {
                List<Map<String, Object>[]> diffList = undoOrigPlanCopy.createMapsArrayDiffList(scoreDirector.getWorkingSolution());
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
            if (undoOrigSdScore.compareTo(newSdScore) != 0) {
                List<Map<String, Object>[]> diffList = undoOrigPlanCopy.createMapsArrayDiffList(scoreDirector.getWorkingSolution());
                System.out.println("diffList.size() = " + diffList.size());
                OpActionPlan.printMapsArrayDiffList(System.out, diffList);
                System.out.println("");
                System.err.println("");
                System.out.flush();
                System.out.flush();
                throw new RuntimeException("newSdScore = " + newSdScore + ", undoOrigSdScore=" + undoOrigSdScore);
            }
        }
        if (null != undoOrigEasyScore) {
            HardSoftLongScore newEasyScore = new EasyOpActionPlanScoreCalculator().calculateScore(scoreDirector.getWorkingSolution());
            if (undoOrigSdScore.compareTo(newEasyScore) != 0) {
                List<Map<String, Object>[]> diffList = undoOrigPlanCopy.createMapsArrayDiffList(scoreDirector.getWorkingSolution());
                OpActionPlan.printMapsArrayDiffList(System.out, diffList);
                Thread.dumpStack();
                System.out.println("diffList.size() = " + diffList.size());
                OpActionPlan.printMapsArrayDiffList(System.out, diffList);
                System.out.println("");
                System.err.println("");
                System.out.flush();
                System.out.flush();
                throw new RuntimeException("newEasyScore = " + newEasyScore + ", undoOrigSdScore=" + undoOrigSdScore);
            }
        }
//        undoMove.undoOrigSdScore = (HardSoftLongScore) sd.calculateScore();
//        undoMove.undoOrigPlanCopy = sd.getWorkingSolution().clonePlan();
//        undoMove.undoOrigEasyScore = new EasyOpActionPlanScoreCalculator().calculateScore(sd.getWorkingSolution());
//        System.out.println("");
//        System.err.println("");
//        System.out.flush();
//        System.err.flush();
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
        if (currentSplitterPrevious == start) {
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
        if (!end.getPrevious().getPossibleNextActions().contains(currentStartNext)) {
            return false;
        }
        return true;
    }
    
    @Override
    public List<OpAction> getPlanningEntities() {
        List<OpAction> entities = new ArrayList<>();
        entities.addAll(start.previousScoreList());
        entities.addAll(splitter.getPrevious().previousScoreList());
        entities.addAll(end.getPrevious().previousScoreList());
        return entities;
    }
    
    @Override
    public List<OpActionInterface> getPlanningValues() {
        List<OpAction> entities = this.getPlanningEntities();
        List<OpActionInterface> nexts = new ArrayList<>();
        for (OpAction entity : entities) {
            nexts.add(entity.getNext());
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
