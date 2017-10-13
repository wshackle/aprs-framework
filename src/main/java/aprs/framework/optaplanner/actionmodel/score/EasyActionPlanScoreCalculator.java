/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework.optaplanner.actionmodel.score;

import aprs.framework.optaplanner.actionmodel.Action;
import aprs.framework.optaplanner.actionmodel.ActionInterface;
import aprs.framework.optaplanner.actionmodel.ActionPlan;
import static aprs.framework.optaplanner.actionmodel.ActionType.END;
import static aprs.framework.optaplanner.actionmodel.ActionType.START;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.optaplanner.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class EasyActionPlanScoreCalculator implements EasyScoreCalculator<ActionPlan> {

    @Override
    public HardSoftBigDecimalScore calculateScore(ActionPlan solution) {
        double costTotal = 0;
        int ends = 0;
        int nulls = 0;
        int starts = 0;
        int startlength = 0;
        int badNexts = 0;
        for(Action action: solution.getActions()) {
            if(action.getNext() == null) {
                nulls++;
            } else if(action.getNext().getActionType() == END) {
                ends++;
            } 
            if(!action.checkNextAction(action.getNext())) {
                badNexts++;
            }
            costTotal += action.cost();
            Set<String> visited = new HashSet<>();
            if(action.getActionType() == START) {
                ActionInterface tmp = action;
                if(!solution.getActions().contains(tmp)) {
                    throw new IllegalStateException(tmp +" not in "+solution.getActions());
                }
                while(tmp != null
                        && tmp.getActionType() != END
                        && tmp instanceof Action
                        && !visited.contains(((Action)tmp).getName())) {
                    visited.add(((Action)tmp).getName());
                    startlength++;
                    tmp = tmp.getNext();
                }
            }
        }
        HardSoftBigDecimalScore score = HardSoftBigDecimalScore.valueOf(BigDecimal.valueOf(-Math.abs(startlength-solution.getActions().size())-Math.abs(1-ends) - 2*nulls - badNexts),BigDecimal.valueOf(-costTotal));
//        System.out.println("solution = " + solution);
//        System.out.println("score = " + score);
        return score;
    }
    
}
