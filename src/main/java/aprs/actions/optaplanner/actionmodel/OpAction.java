/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.actions.optaplanner.actionmodel;

import aprs.actions.executor.ActionType;
import static aprs.actions.executor.ActionType.END_PROGRAM;
import static aprs.actions.executor.ActionType.INVALID_ACTION_TYPE;
import static aprs.actions.executor.ActionType.PLACE_PART;
import static aprs.actions.executor.ActionType.TAKE_PART;
import static aprs.actions.optaplanner.actionmodel.OpActionType.DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.END;
import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_DROPOFF;
import static aprs.actions.optaplanner.actionmodel.OpActionType.FAKE_PICKUP;
import static aprs.actions.optaplanner.actionmodel.OpActionType.PICKUP;
import static aprs.actions.optaplanner.actionmodel.OpActionType.START;
import aprs.actions.optaplanner.actionmodel.score.DistToTime;
import crcl.utils.Utils;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.collections.api.collection.MutableCollection;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariableGraphType;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@PlanningEntity
public class OpAction implements OpActionInterface {

    private final ActionType executorActionType;
    private final String[] executorArgs;
    private String name;

    /**
     * Get the value of executorArgs
     *
     * @return the value of executorArgs
     */
    public String[] getExecutorArgs() {
        return executorArgs;
    }

    /**
     * Get the value of executorActionType
     *
     * @return the value of executorActionType
     */
    public
    ActionType getExecutorActionType() {
        return executorActionType;
    }

    private static OpActionType executorActionToOpAction(ActionType at) {
        switch (at) {
            case TAKE_PART:
                return PICKUP;
            case PLACE_PART:
                return DROPOFF;
            case FAKE_TAKE_PART:
                return FAKE_PICKUP;
            case END_PROGRAM:
                return END;
            default:
                throw new IllegalArgumentException("at=" + at + " : only TAKE_PART,PLACE_PART,FAKE_TAKE_PART,END_PROGRAM supported");
        }
    }

    private static ActionType opActionToExecutorAction(OpActionType opAction) {
        switch (opAction) {
            case PICKUP:
                return TAKE_PART;
            case DROPOFF:
                return PLACE_PART;
            case END:
                return END_PROGRAM;
            case START:
                return INVALID_ACTION_TYPE;
            case FAKE_DROPOFF:
                return INVALID_ACTION_TYPE;
            case FAKE_PICKUP:
                return INVALID_ACTION_TYPE;
            default:
                throw new IllegalArgumentException("opAction=" + opAction);
        }
    }

    private static String[] argsFromName(String name) {
        int dindex = name.indexOf("-[");
        if (dindex > 0) {
            return name.substring(dindex + 2).split("[,{}\\-\\[\\]]+");
        }
        return new String[]{};
    }

    public OpAction(int origId, String name, double x, double y, OpActionType opActionType, String partType, boolean required) {
        if (opActionType != START && !allowedPartTypes.contains(partType)) {
            throw new RuntimeException("partType=" + partType);
        }
        this.executorActionType = opActionToExecutorAction(opActionType);
        this.executorArgs = argsFromName(name);
        this.name = name;
        this.location = new Point2D.Double(x, y);
        this.opActionType = opActionType;
        this.partType = partType;
        this.id = OpActionPlan.newActionPlanId();
        this.origId = origId;
        this.required = required;
        this.trayType = getTrayType(opActionType, name);
    }

    public static class AddFakeInfo {

        final MutableCollection<OpAction> pickupThisPartActions;
        final MutableCollection<OpAction> dropoffThisPartActions;

        public AddFakeInfo(MutableCollection<OpAction> pickupThisPartActions, MutableCollection<OpAction> dropoffThisPartActions) {
            this.pickupThisPartActions = pickupThisPartActions;
            this.dropoffThisPartActions = dropoffThisPartActions;
        }

        @Override
        public String toString() {
            return "AddFakeInfo{" + "pickupThisPartActions=" + pickupThisPartActions + ", dropoffThisPartActions=" + dropoffThisPartActions + '}';
        }

    }

    @Nullable
    AddFakeInfo addFakeInfo = null;

    public OpAction(String name, double x, double y, OpActionType opActionType, String partType, AddFakeInfo addFakeInfo) {
        if (opActionType != START && !allowedPartTypes.contains(partType)) {
            throw new RuntimeException("partType=" + partType);
        }
        this.addFakeInfo = addFakeInfo;
        this.executorActionType = opActionToExecutorAction(opActionType);
        this.executorArgs = argsFromName(name);
        this.name = name;
        this.location = new Point2D.Double(x, y);
        this.opActionType = opActionType;
        this.partType = partType;
        this.id = OpActionPlan.newActionPlanId();
        this.origId = id;
        this.required = false;
        this.trayType = getTrayType(opActionType, name);
    }

    public static final HashSet<String> allowedPartTypes = new HashSet<>(Arrays.asList(
            "small_gear",
            "medium_gear",
            "large_gear"
    ));

    public OpAction(String name, double x, double y, OpActionType opActionType, String partType, boolean required) {
        if (opActionType != START && !allowedPartTypes.contains(partType)) {
            throw new RuntimeException("partType=" + partType);
        }
        this.executorActionType = opActionToExecutorAction(opActionType);
        this.executorArgs = argsFromName(name);
        this.name = name;
        this.location = new Point2D.Double(x, y);
        this.opActionType = opActionType;
        this.partType = partType;
        this.id = OpActionPlan.newActionPlanId();
        this.origId = id;
        this.required = required;
        this.trayType = getTrayType(opActionType, name);
    }

    public OpAction(int origId, ActionType executorActionType, String executorArgs[], double x, double y, String partType, boolean required) {
        if (opActionType != START && !allowedPartTypes.contains(partType)) {
            throw new RuntimeException("partType=" + partType);
        }
        this.executorActionType = executorActionType;
        this.executorArgs = executorArgs;
        this.name = executorActionType + "-" + Arrays.toString(executorArgs);
        this.location = new Point2D.Double(x, y);
        this.opActionType = executorActionToOpAction(executorActionType);
        this.partType = partType;
        this.id = OpActionPlan.newActionPlanId();
        this.origId = origId;
        this.required = required;
        this.trayType = getTrayType(opActionType, name);
    }

    public OpAction(ActionType executorActionType, String executorArgs[], double x, double y, String partType, boolean required) {
        if (opActionType != START && !allowedPartTypes.contains(partType)) {
            throw new RuntimeException("partType=" + partType);
        }
        this.executorActionType = executorActionType;
        this.executorArgs = executorArgs;
        this.name = executorActionType + "-" + Arrays.toString(executorArgs);
        this.location = new Point2D.Double(x, y);
        this.opActionType = executorActionToOpAction(executorActionType);
        this.partType = partType;
        this.id = OpActionPlan.newActionPlanId();
        this.origId = id;
        this.required = required;
        this.trayType = getTrayType(opActionType, name);
    }

    private static @Nullable
    String getTrayType(OpActionType opActionType, String name) {
        switch (opActionType) {
            case PICKUP:
            case DROPOFF:
                if (name.contains("_in_pt_") || name.endsWith("in_pt")) {
                    return "PT";
                } else if (name.contains("_in_kit_") || name.contains("_in_kt_")) {
                    return "KT";
                }
                break;

            default:
                break;
        }
        return null;
    }

    private final boolean required;

    @Override
    public boolean isRequired() {
        return required;
    }

    /**
     * Get the value of name
     *
     * @return the value of name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the value of name
     *
     * @param name new value of name
     */
    public void setName(String name) {
        this.name = name;
    }

    private final Point2D.Double location;

    public Point2D.Double getLocation() {
        return location;
    }

    @PlanningVariable(graphType = PlanningVariableGraphType.CHAINED,
            valueRangeProviderRefs = {"possibleNextActions", "endActions"})
    private volatile @Nullable
    OpActionInterface next;

    private OpActionType opActionType;

    @Override
    public OpActionType getOpActionType() {
        return opActionType;
    }

    public void setOpActionType(OpActionType opActionType) {
        this.opActionType = opActionType;
    }

    final private String partType;

    @Override
    public String getPartType() {
        return partType;
    }

    private final List<OpActionInterface> possibleNextActions = new ArrayList<>();

    @ValueRangeProvider(id = "possibleNextActions")
    public List<OpActionInterface> getPossibleNextActions() {
        return possibleNextActions;
    }

    @Override
    public @Nullable
    OpActionInterface getNext() {
        return next;
    }

    private volatile StackTraceElement setNextTrace @Nullable []  = null;
    private volatile @Nullable
    CheckNextInfoPair setNextInfoPair = null;

    public void setNext(OpActionInterface next) {
        final CheckNextInfoPair checkNextInfoPair = new CheckNextInfoPair(new CheckNextInfo(this), new CheckNextInfo(next));
        if (!checkNextActionInfoPair(checkNextInfoPair)) {
            System.err.println("setNextTrace = " + Utils.traceToString(setNextTrace));
            throw new IllegalStateException("this=" + name + " Settting next for to " + next.shortString() + " : possibles=" + getPossibleNextActions());
        }
        this.next = next;
        setNextTrace = Thread.currentThread().getStackTrace();
        setNextInfoPair = checkNextInfoPair;
    }

    public void clearNext() {
        this.next = null;
    }

    private boolean actionRequired() {
        return required; // Objects.equals(trayType, "KT");
    }

    static class CheckNextInfo {

        final boolean actionRequired;
        final String partType;
        final OpActionType opActionType;

        public CheckNextInfo(boolean actionRequired, String partType, OpActionType opActionType) {
            this.actionRequired = actionRequired;
            this.partType = partType;
            this.opActionType = opActionType;
        }

        public CheckNextInfo(OpActionInterface oai) {
            this(oai.isRequired(), oai.getPartType(), oai.getOpActionType());
        }

        public boolean isActionRequired() {
            return actionRequired;
        }

        public String getPartType() {
            return partType;
        }

        public OpActionType getOpActionType() {
            return opActionType;
        }

    }

    static class CheckNextInfoPair {

        final CheckNextInfo current;
        final CheckNextInfo next;

        public CheckNextInfoPair(CheckNextInfo current, CheckNextInfo next) {
            this.current = current;
            this.next = next;
        }
    }

    public boolean checkNextAction(OpActionInterface possibleNextAction) {
        if (null == possibleNextAction) {
            throw new NullPointerException("possibleNextAction");
        }
        return checkNextActionInfoPair(new CheckNextInfoPair(new CheckNextInfo(this), new CheckNextInfo(possibleNextAction)));
    }

    private static boolean checkNextActionInfoPair(CheckNextInfoPair infoPair) {

        OpActionType possibleNextOpActionType = infoPair.next.getOpActionType();
        final String possibleNextActionPartType = infoPair.next.getPartType();
        final boolean possibleNextActionRequired = infoPair.next.isActionRequired();
        final boolean actionRequired = infoPair.current.isActionRequired();
        final OpActionType opActionType = infoPair.current.getOpActionType();
        final String partType = infoPair.current.getPartType();
        switch (opActionType) {
            case START:
                return (possibleNextOpActionType == PICKUP || possibleNextOpActionType == END);
            case FAKE_DROPOFF:
            case DROPOFF:
                switch (possibleNextOpActionType) {
                    case PICKUP:
                    case FAKE_PICKUP:
                    case END:
                        return true;
                    default:
                        return false;
                }

            case PICKUP:
                if (Objects.equals(partType, possibleNextActionPartType)) {
                    switch (possibleNextOpActionType) {
                        case DROPOFF:
                            return true;
                        case FAKE_DROPOFF:
                            return !actionRequired;
                        default:
                            return false;
                    }
                } else {
                    return false;
                }

            case FAKE_PICKUP:
                if (Objects.equals(partType, possibleNextActionPartType)) {
                    switch (possibleNextOpActionType) {
                        case DROPOFF:
                            return !possibleNextActionRequired;
                        case FAKE_DROPOFF:
                            return true;
                        default:
                            return false;
                    }
                } else {
                    return false;
                }
            case END:
            default:
                return false;
        }
    }

    private int maxNextEffectiveCount = 0;

    private StackTraceElement addPossibleNextActionsTrace @Nullable []  = null;

    @Override
    public List<Integer> getPossibleNextIds() {
        List<OpActionInterface> possibleNexts
                = getPossibleNextActions();
        List<Integer> possibleNextIds = new ArrayList<>();
        for (int j = 0; j < possibleNexts.size(); j++) {
            OpActionInterface possibleNext
                    = possibleNexts.get(j);
            possibleNextIds.add(possibleNext.getId());
        }
        return possibleNextIds;
    }

    void addPossibleNextActions(List<? extends OpActionInterface> allActions) {
        maxNextEffectiveCount = allActions.size();
        List<OpActionInterface> startPossibleNextAction = new ArrayList<>(possibleNextActions);
        if (!startPossibleNextAction.isEmpty()) {
            System.out.println("startPossibleNextAction = " + startPossibleNextAction);
            System.out.println("addPossibleNextActionsTrace = " + Utils.traceToString(addPossibleNextActionsTrace));
        }
        for (OpActionInterface action : allActions) {
            if (!possibleNextActions.contains(action)) {
                if (checkNextAction(action)) {
                    possibleNextActions.add(action);
                }
            } else {
                System.out.println("possibleNextActions.contains(" + action + ")");
            }
        }
        addPossibleNextActionsTrace = Thread.currentThread().getStackTrace();
    }

    private @Nullable
    String trayType;

    @Override
    public @Nullable
    String getTrayType() {
        return this.trayType;
    }

    public void setTrayType(String trayType) {
        this.trayType = trayType;
    }

    @SuppressWarnings("WeakerAccess")
    @Override
    public boolean skipNext() {
        OpActionInterface n = this.next;
        if (null == n) {
            return false;
        }
        if (this.opActionType == FAKE_PICKUP) {
            return true;
        }
        if (this.opActionType != PICKUP) {
            return false;
        }
        if (n.getOpActionType() == FAKE_DROPOFF) {
            return true;
        }
        return !this.isRequired() && !n.isRequired();
    }

    @Override
    public @Nullable
    OpActionInterface effectiveNext(boolean quiet) {
        if (getOpActionType() == FAKE_DROPOFF) {
            return next;
        }
        if (null == next) {
            if (quiet) {
                return null;
            }
            throw new IllegalStateException("this=" + name + ", next=" + next + " next.getNext() ==null");
        }
//        List<OpActionInterface> nxts = new ArrayList<>();
        switch (next.getOpActionType()) {
            case FAKE_DROPOFF:
                return null;

            case PICKUP:
            case FAKE_PICKUP:
                OpActionInterface nextNext = next.getNext();
                if (null == nextNext) {
                    if (quiet) {
                        return null;
                    }
                    throw new IllegalStateException("this=" + name + ", next=" + next + " next.getNext() ==null");
                }
                if (next.skipNext()) {

//                    nxts.add(next);
//                    nxts.add(nextNext);
                    OpActionInterface nxt2 = nextNext.getNext();
                    if (null == nxt2) {
                        if (quiet) {
                            return null;
                        }
                        throw new IllegalStateException("this=" + name + " nxt2 ==null");
                    }
//                    nxts.add(nxt2);
                    OpActionInterface nxt2Next = nxt2.getNext();
                    if (null == nxt2Next) {
                        if (quiet) {
                            return null;
                        }
                        throw new IllegalStateException("this=" + name + " nxt2.getNext() ==null");
                    }
                    if (nxt2 == next) {
                        if (quiet) {
                            return null;
                        }
                        throw new IllegalStateException("this=" + name + " nxt2 == next");
                    }
                    if (nxt2 == this) {
                        if (quiet) {
                            return null;
                        }
                        throw new IllegalStateException("this=" + name + " nxt2 == this");
                    }
//                    nxts.add(nxt2Next);
                    int count = 0;
                    while (nxt2.skipNext()) {
                        count++;
                        if (count > maxNextEffectiveCount) {
                            if (quiet) {
                                return null;
                            }
                            throw new IllegalStateException("this=" + name + " count > maxCount, count=" + count);//,nxts=" + nxts);
                        }

                        nxt2 = nxt2Next.getNext();
                        if (null == nxt2) {
                            if (quiet) {
                                return null;
                            }
                            throw new IllegalStateException("this=" + name + " nxt2Next=" + nxt2Next.getName() + ", nxt2Next.getNext() ==null, count=" + count);
                        }
//                        nxts.add(nxt2);
                        nxt2Next = nxt2.getNext();
                        if (null == nxt2Next) {
                            if (quiet) {
                                return null;
                            }
                            throw new IllegalStateException("this=" + name + " nxt2=" + nxt2 + " nxt2.getNext() ==null");
                        }
//                        nxts.add(nxt2Next);
                        if (nxt2 == next) {
                            if (quiet) {
                                return null;
                            }
                            throw new IllegalStateException("this=" + name + " nxt2 == next");
                        }
                        if (nxt2 == this) {
                            if (quiet) {
                                return null;
                            }
                            throw new IllegalStateException("this=" + name + " nxt2 == this");
                        }
                    }
                    return nxt2;
                }
        }
        return next;
    }

    public double cost(OpActionPlan plan) {
        OpActionInterface n = this.next;
        if (null == n) {
            return Double.POSITIVE_INFINITY;
        }
        if (opActionType == FAKE_PICKUP || opActionType == FAKE_DROPOFF || n.getOpActionType() == FAKE_DROPOFF) {
            return 0;
        }
        if (this.skipNext()) {
            return 0;
        }
        if (this.opActionType == START && !plan.isUseStartEndCost()) {
            return 0;
        }
        if (this.opActionType == START) {
            return DistToTime.distToTime(this.distance(false), plan.getAccelleration(), plan.getStartEndMaxSpeed());
        }
        OpActionInterface effNext = effectiveNext(false);
        return costOfNext(effNext, plan);
    }

    public double costOfNext(@Nullable OpActionInterface effNext, OpActionPlan plan) {
        if (plan.isUseDistForCost()) {
            return distanceToNext(false, effNext);
        }
        if (null == effNext) {
            return Double.POSITIVE_INFINITY;
        }
        if (effNext.getOpActionType() == END && !plan.isUseStartEndCost()) {
            return 0;
        }
        if (effNext.getOpActionType() == END) {
            return DistToTime.distToTime(this.distanceToNext(false, effNext), plan.getAccelleration(), plan.getStartEndMaxSpeed());
        }
        return DistToTime.distToTime(this.distanceToNext(false, effNext), plan.getAccelleration(), plan.getMaxSpeed());
    }

    public double distance(boolean quiet) {
        if (null == next) {
            if (quiet) {
                return Double.POSITIVE_INFINITY;
            }
            throw new IllegalStateException("this=" + name + " next==null");
        }
        if (null == next.getLocation()) {
            if (quiet) {
                return Double.POSITIVE_INFINITY;
            }
            throw new IllegalStateException("this=" + name + " next.getLocation()==null");
        }
        OpActionInterface effNext = effectiveNext(quiet);
        return distanceToNext(quiet, effNext);
    }

    private double distanceToNext(boolean quiet, @Nullable OpActionInterface effNext) throws IllegalStateException {

        if (null == location) {
            if (quiet) {
                return Double.POSITIVE_INFINITY;
            }
            throw new IllegalStateException("this=" + name + " location ==null");
        }

        if (null == effNext) {
            if (quiet) {
                return Double.POSITIVE_INFINITY;
            }
            throw new IllegalStateException("this=" + name + " effNext == null, next =" + next);
        }
        return effNext.getLocation().distance(location);
    }

    public String locationString() {
        if (null != location) {
            return String.format("{%.1f,%.1f}", location.x, location.y);
        } else {
            return "";
        }
    }

    @Override
    public String shortString() {
        return name + ":" + getId() + locationString();
    }

    public String longString() {
        String effNextString = makeEffNextString();
        return makeLongString(effNextString);
    }

    @Override
    public String toString() {
        return makeLongString("");
    }

    private String makeLongString(String effNextString) {
        String partTypeString = "";
        if (null != this.partType && this.partType.length() > 0) {
            partTypeString = "(partType=" + this.partType + ")";
        }
        String infoString = partTypeString + effNextString;
        if (partTypeString.endsWith(")") && effNextString.startsWith("(")) {
            infoString = partTypeString.substring(0, partTypeString.length() - 1) + (isRequired() ? ",required" : "") + "," + effNextString.substring(1);
        }
        OpActionInterface localNext = this.next;
        if (localNext instanceof OpAction) {
            OpActionType nextActionType = localNext.getOpActionType();
            OpActionType actionType = getOpActionType();
            if (null != localNext.getNext() && actionType != FAKE_DROPOFF && nextActionType != FAKE_DROPOFF) {
                double dist = Double.POSITIVE_INFINITY;
                try {
                    dist = distance(true);
                } catch (Exception e) {
                    Logger.getLogger(OpAction.class.getName()).log(Level.SEVERE, "", e);
                }
                return shortString() + infoString + " -> " + localNext.shortString() + "(cost=" + String.format("%.3f", dist) + ")";
            } else {
                return shortString() + infoString + " -> " + localNext.shortString();
            }
        } else if (null != localNext) {
            return shortString() + infoString + " -> " + localNext.getOpActionType();
        } else {
            return shortString() + "-> null";
        }
    }

    private String makeEffNextString() {
        try {
            OpActionInterface effNext = effectiveNext(true);
            if (effNext != next && null != effNext) {
                return "(effectiveNext=" + effNext.getName() + ")";
            } else {
                return "";
            }
        } catch (Exception e) {
            Logger.getLogger(OpAction.class.getName()).log(Level.SEVERE, "", e);
            return e.toString();
        }
    }

    private final int origId;

    public int getOrigId() {
        return origId;
    }

    private final int id;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Objects.hashCode(this.location);
        hash = 97 * hash + Objects.hashCode(this.opActionType);
        hash = 97 * hash + Objects.hashCode(this.partType);
        return hash;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OpAction other = (OpAction) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.partType, other.partType)) {
            return false;
        }
        if (!Objects.equals(this.location, other.location)) {
            return false;
        }
        return this.opActionType == other.opActionType;
    }

    @Override
    public int getPriority(boolean prevRequired) {
        switch (opActionType) {
            case START:
                return 0;

            case END:
                return 10;

            case PICKUP:
            case DROPOFF:
                return required ? 1 : 2;

            case FAKE_DROPOFF:
            case FAKE_PICKUP:
                return 3;

            default:
                return 3;
        }
    }

}
