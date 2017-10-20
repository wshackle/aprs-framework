/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework.optaplanner.actionmodel.score;

import aprs.framework.optaplanner.actionmodel.OpAction;
import aprs.framework.optaplanner.actionmodel.OpActionPlan;
import static aprs.framework.optaplanner.actionmodel.OpActionType.END;
import static aprs.framework.optaplanner.actionmodel.OpActionType.START;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;
import aprs.framework.optaplanner.actionmodel.OpActionInterface;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class EasyOpActionPlanScoreCalculator implements EasyScoreCalculator<OpActionPlan> {

    @Override
    public HardSoftLongScore calculateScore(OpActionPlan solution) {
        double costTotal = 0;
        int ends = 0;
        int nulls = 0;
        int starts = 0;
        int startlength = 0;
        int badNexts = 0;
        for(OpAction action: solution.getActions()) {
            if(action.getNext() == null) {
                nulls++;
            } else if(action.getNext().getActionType() == END) {
                ends++;
            } 
            else if(!action.checkNextAction(action.getNext())) {
                badNexts++;
            }
            costTotal += action.cost();
            Set<String> visited = new HashSet<>();
            if(action.getActionType() == START) {
                OpActionInterface tmp = action;
                if(!solution.getActions().contains(tmp)) {
                    throw new IllegalStateException(tmp +" not in "+solution.getActions());
                }
                while(tmp != null
                        && tmp.getActionType() != END
                        && tmp instanceof OpAction
                        && !visited.contains(((OpAction)tmp).getName())) {
                    visited.add(((OpAction)tmp).getName());
                    startlength++;
                    tmp = tmp.getNext();
                }
            }
        }
        long hardScoreLong = -Math.abs(startlength-solution.getActions().size())-Math.abs(1-ends) - 2*nulls - badNexts;
        long softScoreLong = (long) (-1000.0*costTotal);
        HardSoftLongScore score = HardSoftLongScore.valueOf(hardScoreLong,softScoreLong);
//        System.out.println("solution = " + solution);
//        System.out.println("score = " + score);
        return score;
    }
    
}
