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
import java.util.HashSet;
import java.util.Set;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;
import aprs.framework.optaplanner.actionmodel.OpActionInterface;
import java.util.List;
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
        List<OpAction> actionsList = solution.getActions();
        double accelleration = solution.getAccelleration();
        double maxSpeed = solution.getMaxSpeed();
        if (null != actionsList) {
            for (OpAction action : actionsList) {
                OpActionInterface nextAction = action.getNext();
                if (nextAction == null) {
                    nulls++;
                } else if (nextAction.getActionType() == END) {
                    ends++;
                } else if (!action.checkNextAction(nextAction)) {
                    badNexts++;
                }
                costTotal +=  action.cost(solution);
                Set<String> visited = new HashSet<>();
                if (action.getActionType() == START) {
                    OpActionInterface tmp = action;
                    if (!actionsList.contains(tmp)) {
                        throw new IllegalStateException(tmp + " not in " + actionsList);
                    }
                    while (tmp != null
                            && tmp.getActionType() != END
                            && tmp instanceof OpAction
                            && !visited.contains(((OpAction) tmp).getName())) {
                        visited.add(((OpAction) tmp).getName());
                        startlength++;
                        tmp = tmp.getNext();
                    }
                }
            }
            long hardScoreLong = -Math.abs(startlength - actionsList.size()) - Math.abs(1 - ends) - 2 * nulls - badNexts;
            long softScoreLong = (long) (-1000.0 * costTotal);
            HardSoftLongScore score = HardSoftLongScore.valueOf(hardScoreLong, softScoreLong);
            return score;
        } else {
            return HardSoftLongScore.valueOf(Long.MIN_VALUE, Long.MIN_VALUE);
        }
    }

}
