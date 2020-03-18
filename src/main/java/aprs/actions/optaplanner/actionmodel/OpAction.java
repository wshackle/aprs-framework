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
import crcl.utils.CRCLUtils;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
    private final String name;

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
    public ActionType getExecutorActionType() {
        return executorActionType;
    }

    private static OpActionType executorActionToOpAction(ActionType at) {
        switch (at) {
            case TAKE_PART:
                return PICKUP;
            case TAKE_PART_BY_TYPE_AND_POSITION:
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
        checkAllowedPartTypes(opActionType, partType);
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

    @Override
    public List<Integer> getPossibleNextOrigIds() {
        List<OpActionInterface> possibleNexts
                = getPossibleNextActions();
        List<Integer> possibleNextOrigIds = new ArrayList<>();
        for (int j = 0; j < possibleNexts.size(); j++) {
            OpActionInterface possibleNext
                    = possibleNexts.get(j);
            possibleNextOrigIds.add(possibleNext.getOrigId());
        }
        return possibleNextOrigIds;
    }

    private volatile @Nullable
    OpAction previous = null;

    @Override
    public @Nullable
    OpAction getPrevious() {
        return previous;
    }

    private volatile StackTraceElement setPreviousTrace  @Nullable []  = null;
    private volatile @Nullable
    Thread setPreviousThread = null;

    @Override
    public void setPrevious(OpAction action) {
        if (action.getNext() != this) {
            throw new RuntimeException("this=" + this + ", action=" + action);
        }
        this.previous = action;
        setPreviousTrace = Thread.currentThread().getStackTrace();
        setPreviousThread = Thread.currentThread();
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
        checkAllowedPartTypes(opActionType, partType);
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

    public static void setAllowedPartTypes(Collection<String> newPartTypes) {
        allowedPartTypes.clear();
        allowedPartTypes.addAll(newPartTypes);
    }

    public static void setAllowedPartTypes(String... newPartTypes) {
        setAllowedPartTypes(Arrays.asList(newPartTypes));
    }

    public OpAction(
            String name,
            double x,
            double y,
            OpActionType opActionType,
            String partType,
            boolean required) {
        checkAllowedPartTypes(opActionType, partType);
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

    static private void checkAllowedPartTypes(
            @Nullable OpActionType opActionType1,
            @Nullable String partType1) throws RuntimeException {
        if (opActionType1 != START
                && null != allowedPartTypes
                && !allowedPartTypes.isEmpty()
                && !allowedPartTypes.contains(partType1)) {
            throw new RuntimeException("partType=" + partType1 + ",allowedPartType=" + allowedPartTypes);
        }
    }

    public OpAction(int origId, ActionType executorActionType, String executorArgs[], double x, double y, String partType, boolean required) {
        this(origId, executorActionType + "-" + Arrays.toString(executorArgs), x, y, executorActionToOpAction(executorActionType), partType, required);

//        checkAllowedPartTypes(opActionType, partType);
//        this.executorActionType = executorActionType;
//        this.executorArgs = executorArgs;
//        this.name = executorActionType + "-" + Arrays.toString(executorArgs);
//        this.location = new Point2D.Double(x, y);
//        this.opActionType = executorActionToOpAction(executorActionType);
//        this.partType = partType;
//        this.id = OpActionPlan.newActionPlanId();
//        this.origId = origId;
//        this.required = required;
//        this.trayType = getTrayType(opActionType, name);
    }

    public OpAction(
            ActionType executorActionType,
            String executorArgs[],
            double x,
            double y,
            String partType,
            boolean required) {
        this(
                executorActionType + "-" + Arrays.toString(executorArgs),
                x,
                y,
                executorActionToOpAction(executorActionType),
                partType,
                required);
//        checkAllowedPartTypes(opActionType, partType);
//        this.executorActionType = executorActionType;
//        this.executorArgs = executorArgs;
//        this.name = executorActionType + "-" + Arrays.toString(executorArgs);
//        this.location = new Point2D.Double(x, y);
//        this.opActionType = executorActionToOpAction(executorActionType);
//        this.partType = partType;
//        this.id = OpActionPlan.newActionPlanId();
//        this.origId = id;
//        this.required = required;
//        this.trayType = getTrayType(opActionType, name);
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

    @Override
    public boolean isFake() {
        return opActionType == FAKE_DROPOFF || opActionType == FAKE_PICKUP;
    }

    /**
     * Get the value of name
     *
     * @return the value of name
     */
    public String getName() {
        return name;
    }

    private final Point2D.Double location;

    public Point2D.Double getLocation() {
        return location;
    }

    public long getDistToNextLong() {
        if (null == next) {
            throw new RuntimeException("null == next");
        }
        return (long) (1000.0 * location.distance(next.getLocation()));
    }

    private volatile @Nullable
    OpActionInterface next;

    final private OpActionType opActionType;

    @Override
    public OpActionType getOpActionType() {
        return opActionType;
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
    @PlanningVariable(graphType = PlanningVariableGraphType.CHAINED,
            valueRangeProviderRefs = {"possibleNextActions"})
    public @Nullable
    OpActionInterface getNext() {
        return next;
    }

    private volatile StackTraceElement setNextTrace @Nullable []  = null;

    public StackTraceElement @Nullable [] getSetNextTrace() {
        return setNextTrace;
    }

    private volatile @Nullable
    CheckNextInfoPair setNextInfoPair = null;

    public @Nullable
    CheckNextInfoPair getSetNextInfoPair() {
        return setNextInfoPair;
    }

    private volatile @Nullable
    OpActionInterface setNextValue;

    private volatile @Nullable
    Thread setNextThread = null;

    @Nullable
    OpActionInterface getSetNextValue() {
        return setNextValue;
    }

    public void setNext(@Nullable OpActionInterface next) {
        this.next = next;
        this.setNextValue = next;
        if (null != next) {
            next.setPrevious(this);
        }
        setNextTrace = Thread.currentThread().getStackTrace();
        setNextThread = Thread.currentThread();
    }

    public void clearNext() {
        this.next = null;
        setNextTrace = Thread.currentThread().getStackTrace();
        setNextInfoPair = null;
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

        @Override
        public String toString() {
            return "CheckNextInfo{" + "actionRequired=" + actionRequired + ", partType=" + partType + ", opActionType=" + opActionType + '}';
        }

    }

    static class CheckNextInfoPair {

        final CheckNextInfo current;
        final CheckNextInfo next;

        public CheckNextInfoPair(CheckNextInfo current, CheckNextInfo next) {
            this.current = current;
            this.next = next;
        }

        @Override
        public String toString() {
            return "CheckNextInfoPair{\n" + "current=" + current + ",\n next=" + next + "\n}";
        }

    }

    public boolean checkNextAction(@Nullable OpActionInterface possibleNextAction) {
        if (null == possibleNextAction) {
            throw new NullPointerException("possibleNextAction");
        }
        return checkNextActionInfoPair(new CheckNextInfoPair(new CheckNextInfo(this), new CheckNextInfo(possibleNextAction)));
    }

    static boolean checkNextActionInfoPair(CheckNextInfoPair infoPair) {

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
                switch (possibleNextOpActionType) {
                    case PICKUP:
                        return !possibleNextActionRequired;
                    case FAKE_PICKUP:
                    case END:
                        return true;
                    default:
                        return false;
                }
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
            System.out.println("addPossibleNextActionsTrace = " + CRCLUtils.traceToString(addPossibleNextActionsTrace));
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

    public List<OpAction> previousScoreList() {
        OpAction actToScore = this;
        List<OpAction> list = new ArrayList<>();
        if (null != next && this != next.getPrevious()) {
            throw new RuntimeException("this=" + this + ", next=" + next + ", next.getPrevious()=" + next.getPrevious());
        }
        final OpAction thisP = this.previous;
        if (null != thisP && this != thisP.next) {
            throw new RuntimeException("this=" + this + ", this.previous=" + thisP + ", getPrevious().next=" + thisP.next);
        }
        if (next instanceof OpAction) {
            list.add((OpAction) next);
        }
        list.add(actToScore);
        actToScore = actToScore.getPrevious();
        if (null == actToScore) {
            return list;
        }
        list.add(actToScore);
        while (needPrvious(actToScore)) {
            OpAction actToScorePrevious = actToScore.getPrevious();
            if (null == actToScorePrevious) {
                return list;
            }
            if (actToScorePrevious.next != actToScore) {
                throw new RuntimeException("actToScorePrevious.next=" + actToScorePrevious.next + ", actToScore=" + actToScore);
            }
            actToScore = actToScorePrevious;
            list.add(actToScore);
        }
        OpAction actToScorePrevious = actToScore.previous;
        if (null != actToScorePrevious) {
            list.add(actToScorePrevious);
            if (!this.required) {
                actToScore = actToScorePrevious;
                while (needPrvious(actToScore)) {
                    actToScorePrevious = actToScore.getPrevious();
                    if (null == actToScorePrevious) {
                        return list;
                    }
                    if (actToScorePrevious.next != actToScore) {
                        throw new RuntimeException("actToScorePrevious.next=" + actToScorePrevious.next + ", actToScore=" + actToScore);
                    }
                    actToScore = actToScorePrevious;
                    list.add(actToScore);
                }
            }
        }
        return list;
    }

    private static boolean needPrvious(OpAction actToScore) {
        final OpAction previous1 = actToScore.getPrevious();
        if (null == previous1) {
            return false;
        } else {
            return !actToScore.isRequired();
        }
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

    public List<OpAction> nextsToEffectiveNextList() {
        OpActionInterface effNext = effectiveNext(true);
        OpActionInterface n = this.next;
        List<OpAction> nextList = new ArrayList<>();
        while (!Objects.equals(n, effNext) && null != n) {
            if (n instanceof OpAction) {
                nextList.add((OpAction) n);
            } else {
                return nextList;
            }
            OpActionInterface n_next = n.getNext();
            if (n_next != null && n_next.getPrevious() != n) {
                throw new RuntimeException("n=" + n + ", n_next=" + n_next + ", n_next.getPrevious()=" + n_next.getPrevious());
            }
            n = n.getNext();
        }
        if (n instanceof OpAction) {
            nextList.add((OpAction) n);
        }
        return nextList;
    }

    private synchronized void printNextPrevInfo() {
        Thread.dumpStack();
        System.err.println("");
        System.err.flush();
        System.out.println("Thread.currentThread() = " + Thread.currentThread());
        System.out.println("printNextPrevInfo");
        System.out.println("this = " + this);
        System.out.println("this.next = " + this.next);
        System.out.println("this.setNextValue = " + this.setNextValue);
        System.out.println("this.setNextInfoPair = " + this.setNextInfoPair);
        System.out.println("this.setNextTrace = " + CRCLUtils.traceToString(this.setNextTrace));
        if (null != this.next) {
            final OpAction nextPrev = this.next.getPrevious();
            System.out.println("this.next.getPrevious = " + nextPrev);
            if (null != nextPrev) {
                System.out.println("nextPrev.setPreviousThread = " + nextPrev.setPreviousThread);
                System.out.println("nextPrev.setPreviousTrace = " + CRCLUtils.traceToString(nextPrev.setPreviousTrace));
            }
        }
        System.out.println("this.previous = " + this.previous);
        if (null != this.previous) {
            final OpActionInterface prevNext = this.previous.next;
            System.out.println("this.previous.next = " + prevNext);
            if (prevNext instanceof OpAction) {
                OpAction prevNextAction = (OpAction) prevNext;
                System.out.println("prevNextAction.setNextThread = " + prevNextAction.setNextThread);
                System.out.println("prevNextAction.setNextTrace = " + CRCLUtils.traceToString(prevNextAction.setNextTrace));
            }
        }
        System.out.println("this = " + this);
        System.err.println("");
        System.err.flush();
    }

    @Override
    public @Nullable
    OpActionInterface effectiveNext(boolean quiet) {
        final OpActionInterface n = next;
        if (getOpActionType() == FAKE_DROPOFF) {
            return n;
        }
        if (null == n) {
            if (quiet) {
                return null;
            }
            throw new IllegalStateException("this=" + name + ", next=" + n + " next.getNext() ==null");
        }
        if (n.getPrevious() != this) {
            throw new IllegalStateException("this=" + name + ", next=" + n + " ext.getPrevious() =" + n.getPrevious());
        }
//        List<OpActionInterface> nxts = new ArrayList<>();
        switch (n.getOpActionType()) {
            case FAKE_DROPOFF:
                return null;

            case PICKUP:
            case FAKE_PICKUP:
                OpActionInterface nextNext = n.getNext();
                if (null == nextNext) {
                    if (quiet) {
                        return null;
                    }
                    throw new IllegalStateException("this=" + name + ", next=" + n + " next.getNext() ==null");
                }
                if (nextNext.getPrevious() != n) {
                    this.printNextPrevInfo();
                    throw new IllegalStateException("this=" + name + ", next=" + n + ",nextNext=" + nextNext + ", nextNext.getPrevious() =" + nextNext.getPrevious());
                }
                if (n.skipNext()) {

//                    nxts.add(next);
//                    nxts.add(nextNext);
                    OpActionInterface nxt2 = nextNext.getNext();
                    if (null == nxt2) {
                        if (quiet) {
                            return null;
                        }
                        throw new IllegalStateException("this=" + name + " nxt2 ==null");
                    }
                    if (nxt2.getPrevious() != nextNext) {
                        this.printNextPrevInfo();
                        if (nxt2 instanceof OpAction) {
                            System.out.println("nxt2 = " + nxt2);
                            OpAction nxt2Action = (OpAction) nxt2;
                            ((OpAction) nxt2).printNextPrevInfo();
                        }
                        throw new IllegalStateException("this=" + name + ", next=" + n + ",nextNext=" + nextNext + ",nxt2=" + nxt2 + ", nxt2.getPrevious() =" + nxt2.getPrevious());
                    }
//                    nxts.add(nxt2);
                    OpActionInterface nxt2Next = nxt2.getNext();
                    if (null == nxt2Next) {
                        if (quiet) {
                            return null;
                        }
                        throw new IllegalStateException("this=" + name + " nxt2.getNext() ==null");
                    }
                    if (nxt2 == n) {
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
                        if (nxt2.getPrevious() != nxt2Next) {
                            this.printNextPrevInfo();
                            if (nxt2 instanceof OpAction) {
                                System.out.println("nxt2 = " + nxt2);
                                OpAction nxt2Action = (OpAction) nxt2;
                                nxt2Action.printNextPrevInfo();
                            }
                            if (nxt2Next instanceof OpAction) {
                                System.out.println("nxt2Next = " + nxt2Next);
                                OpAction nxt2NextAction = (OpAction) nxt2Next;
                                nxt2NextAction.printNextPrevInfo();
                            }
                            throw new IllegalStateException("this=" + name + ", next=" + n + ",nextNext=" + nextNext + ",nxt2=" + nxt2 + ", nxt2.getPrevious() =" + nxt2.getPrevious());
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
                        if (nxt2 == n) {
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
        return n;
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
        if (this.opActionType == DROPOFF && !this.required && null != previous && !previous.required) {
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
            if (null != localNext.getNext() && !isFake() && nextActionType != FAKE_DROPOFF) {
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
