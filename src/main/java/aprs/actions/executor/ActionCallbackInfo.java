package aprs.actions.executor;

import crcl.utils.CRCLCommandWrapper;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * Class to hold information about a given action to be passed to callback
 * methods when the action is executed.
 */
class ActionCallbackInfo {

    private final int actionIndex;
    private final String comment;
    private final Action action;
    private final CRCLCommandWrapper wrapper;
    private final List<Action> actions;

    private final @Nullable
    List<Action> origActions;

    private final int actionsSize;

    ActionCallbackInfo(
            int actionIndex,
            String comment, Action action,
            CRCLCommandWrapper wrapper,
            List<Action> actions,
            @Nullable List<Action> origActions) {
        this.actionIndex = actionIndex;
        this.comment = comment;
        this.action = action;
        this.wrapper = wrapper;
        this.actions = actions;
        this.origActions = origActions;
        actionsSize = this.actions.size();
    }

    int getActionsSize() {
        return actionsSize;
    }

    public int getActionIndex() {
        return actionIndex;
    }

    public Action getAction() {
        return action;
    }

    @SuppressWarnings("unused")
    public CRCLCommandWrapper getWrapper() {
        return wrapper;
    }

    public List<Action> getActions() {
        return actions;
    }

    @SuppressWarnings("unused")
    public @Nullable
    List<Action> getOrigActions() {
        return origActions;
    }

    @SuppressWarnings("unused")
    @Override
    public String toString() {
        return "ActionCallbackInfo{" + "actionIndex=" + actionIndex + "comment=" + comment + ", action=" + action + ", wrapper=" + wrapper + ", actions=" + actions + '}';
    }

}
