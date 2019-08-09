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

import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_PICKUP;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.move.factory.MoveListFactory;

/**
 *
 * @author shackle
 */
public class OpActionMoveListFactory implements MoveListFactory<OpActionPlan>{

    @Override
    public List<AbstractMove<OpActionPlan>> createMoveList(OpActionPlan plan) {
        Map<String,List<OpAction>> pickupsByPartType = new HashMap<>();
        Map<String,List<OpAction>> dropoffsByPartType = new HashMap<>();
        List<OpAction> actions = plan.getActions();
        for (int i = 0; i < actions.size(); i++) {
            OpAction actionI = actions.get(i);
            switch(actionI.getOpActionType()) {
                case PICKUP:
                case FAKE_PICKUP:
                    pickupsByPartType.compute(actionI.getPartType(), 
                            (String key, List<OpAction> l) -> {
                               if(l == null) {
                                   return new ArrayList<>(Arrays.asList(actionI));
                               } else {
                                   l.add(actionI);
                                   return l;
                               }
                            });
                    break;
                    
                case DROPOFF:
                case FAKE_DROPOFF:
                    dropoffsByPartType.compute(actionI.getPartType(), 
                            (String key, List<OpAction> l) -> {
                               if(l == null) {
                                   return new ArrayList<>(Arrays.asList(actionI));
                               } else {
                                   l.add(actionI);
                                   return l;
                               }
                            });
                    break;
                    
                default:
                    break;
            }
        }
        List<AbstractMove<OpActionPlan>> moveList = new ArrayList<>();
        for(List<OpAction> pickupsByPartList : pickupsByPartType.values()) {
            for (int i = 0; i < pickupsByPartList.size(); i++) {
                OpAction actionI = pickupsByPartList.get(i);
                for (int j = i+1; j < pickupsByPartList.size(); j++) {
                     OpAction actionJ = pickupsByPartList.get(j);
                     if(actionJ.isFake()
                             && actionI.isFake()) {
                         continue;
                     }
                     moveList.add(new OpActionSwapMove(actionI, actionJ));
                }
            }
        }
        for(List<OpAction> dropoffsByPartList : dropoffsByPartType.values()) {
            for (int i = 0; i < dropoffsByPartList.size(); i++) {
                OpAction actionI = dropoffsByPartList.get(i);
                for (int j = i+1; j < dropoffsByPartList.size(); j++) {
                     OpAction actionJ = dropoffsByPartList.get(j);
                     if(actionJ.isFake()
                             && actionI.isFake()) {
                         continue;
                     }
                     moveList.add(new OpActionSwapMove(actionI, actionJ));
                }
            }
        }
        OpAction start = plan.findStartAction();
        OpEndAction end = plan.getEndAction();
        for (int i = 0; i < start.getPossibleNextActions().size(); i++) {
            OpActionInterface nextI = start.getPossibleNextActions().get(i);
            if(nextI instanceof OpAction) {
                OpAction nextActionI = (OpAction) nextI;
                final OpActionFrontBackMove opActionFrontBackMove = new OpActionFrontBackMove(start,end,nextActionI);
//                opActionFrontBackMove.createUndoMove(null); // for testing
                moveList.add(opActionFrontBackMove);
            }
        }
        return moveList;
    }
    
}
