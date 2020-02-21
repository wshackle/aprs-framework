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
package aprs.actions.executor;

import static aprs.actions.executor.ActionType.DISABLE_OPTIMIZATION;
import static aprs.actions.executor.ActionType.LOOK_FOR_PARTS;
import static aprs.actions.executor.ActionType.PLACE_PART;
import static aprs.actions.executor.ActionType.TAKE_PART;
import static aprs.actions.executor.ActionType.TAKE_PART_BY_TYPE_AND_POSITION;
import static aprs.actions.executor.ActionType.UNINITIALIZED;
import static aprs.actions.optaplanner.actionmodel.OpAction.allowedPartTypes;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Instances of PDDL (Planning Domain Definition Language) Actions
 *
 * An ordered list of actions is the typical output of a PDDL planner. The list
 * may be read from a file or generated automatically.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("unused")
public class Action {

    public static Action newTakePartAction(String part) {
        return new Action.ActionBuilder()
                .type(TAKE_PART)
                .args(new String[]{part})
                .build();
    }

    public static Action newDisableOptimization() {
        return new Action.ActionBuilder()
                .type(DISABLE_OPTIMIZATION)
                .args(new String[]{})
                .build();
    }
    
    public static Action newTakePartByTypeAndPostion(String part,double x, double y) {
        return new Action.ActionBuilder()
                .type(TAKE_PART_BY_TYPE_AND_POSITION)
                .args(new String[]{part,Double.toString(x),Double.toString(y)})
                .build();
    }
    
    public static Action newPlacePartAction(String slotName, @Nullable String partType) {
        ActionBuilder ab1
                = new Action.ActionBuilder()
                        .type(PLACE_PART)
                        .args(new String[]{slotName});
        if (null != partType) {
            ab1 = ab1.partType(partType);
        }
        return ab1
                .build();
    }
    
    public static Action newPlacePartByPosition(double x, double y, @Nullable String partName) {
        ActionBuilder ab1
                = new Action.ActionBuilder()
                        .type(ActionType.PLACE_PART_BY_POSITION)
                        .args(new String[]{Double.toString(x),Double.toString(y)});
        if (null != partName) {
            ab1 = ab1.partType(CrclGenerator.partNameToPartType(partName));
        }
        return ab1
                .build();
    }
    

    public static Action newSingleArgAction(ActionType type, Object arg) {
        return new Action.ActionBuilder()
                .type(type)
                .args(new String[]{arg.toString()})
                .build();
    }

    public static Action newNoArgAction(ActionType type) {
        return new Action.ActionBuilder()
                .type(type)
                .args(new String[]{})
                .build();
    }

    public static Action newLookForParts(int arg0, Map<String, Integer> requiredItemsMap) {
        String newArgs[] = new String[requiredItemsMap.size() + 1];
        newArgs[0] = Integer.toString(arg0);
        Iterator<String> mapIterator = requiredItemsMap.keySet().iterator();
        for (int i = 1; i < newArgs.length && mapIterator.hasNext(); i++) {
            String mapKey = mapIterator.next();
            Integer mapValue = requiredItemsMap.get(mapKey);
            if (mapValue != null) {
                newArgs[i] = mapKey + "=" + mapValue;
            } else {
                newArgs[i] = mapKey + "=" + 0;
            }
        }
        return new Action.ActionBuilder()
                .type(ActionType.LOOK_FOR_PARTS)
                .args(newArgs)
                .build();
    }

    public static Action newLookForParts(int arg0) {
        return new Action.ActionBuilder()
                .type(ActionType.LOOK_FOR_PARTS)
                .args(new String[]{Integer.toString(arg0)})
                .build();
    }

    public static class ActionBuilder {

        private @Nullable
        String label = null;
        private ActionType type = UNINITIALIZED;
        private @Nullable
        String typename = null;
        private String @Nullable [] args = null;
        private @Nullable
        String partType = null;
        private double cost = 0;

        public ActionBuilder label(String label) {
            this.label = label;
            return this;
        }

        public ActionBuilder type(ActionType type) {
            this.type = type;
            return this;
        }

        public ActionBuilder type(String typename) {
            if (type != UNINITIALIZED) {
                this.typename = typename;
            } else {
                throw new IllegalStateException("type=" + type + " is already set");
            }
            return this;
        }

        public ActionBuilder args(String[] args) {
            this.args = args;
            return this;
        }

        public ActionBuilder partType(@Nullable String partType) {
            if (null != partType && !allowedPartTypes.contains(partType)) {
                throw new RuntimeException("partType=" + partType +" not in allowedPartTypes="+allowedPartTypes);
            }
            this.partType = partType;
            return this;
        }

        public ActionBuilder cost(double cost) {
            this.cost = cost;
            return this;
        }

        public Action build() {
            final String newArgs[];
            if (null == args) {
                newArgs = new String[0];
            } else {
                newArgs = args;
            }
            if (type != UNINITIALIZED) {
                return new Action(label, type, newArgs, partType, cost);
            } else {
                final String typenameFinal = typename;
                if (typenameFinal != null) {
                    return new Action(label, typenameFinal, newArgs, partType, cost);
                } else {
                    throw new IllegalStateException("type or typename must be set before building");
                }
            }
        }
    }
    private volatile long planTime;
    private volatile long execTime;
    private volatile boolean executed;
    private volatile @Nullable
    String partType;

    public @Nullable
    String getPartType() {
        return partType;
    }

    public boolean getExecuted() {
        return executed;
    }

    void setExecTime() {
        executed = true;
        execTime = System.currentTimeMillis();
    }

    public long getExecTime() {
        return execTime;
    }

    void setPlanTime() {
        planTime = System.currentTimeMillis();
    }

    @SuppressWarnings("unused")
    public long getPlanTime() {
        return planTime;
    }

    /**
     * Create an instance from the required parameters.
     *
     * @param actionType enumerated type of action
     */
    private Action(ActionType actionType) {
        this("", actionType, new String[]{}, null, 0.0);
    }

    /**
     * Create an instance from the required parameters.
     *
     * @param actionType enumerated type of action
     * @param arg single argument for the action
     */
    private Action(ActionType actionType, String arg) {
        this("", actionType, new String[]{arg}, null, 0.0);
    }

    /**
     * Create an instance from the required parameters.
     *
     * @param label string added to the display table
     * @param typename name of the kind of action to be performed
     * @param args arguments for the action
     * @param cost cost as reported by planner
     */
    private Action(@Nullable String label, String typename, @Nullable String[] args, @Nullable String partType,
            double cost) {
        this(label,
                "look-for-part".equals(typename) // annoying legacy corner case
                ? LOOK_FOR_PARTS
                : ActionType.valueOf(typename.trim().replace('-', '_').toUpperCase()),
                args,
                partType,
                cost);
    }

    /**
     * Create an instance from the required parameters.
     *
     * @param label string added to the display table
     * @param type the kind of action to be performed
     * @param args arguments for the action
     * @param cost cost as reported by planner
     */
    private Action(@Nullable String label, ActionType type, @Nullable String[] args, @Nullable String partType, double cost) {
        if(null != partType && !allowedPartTypes.contains(partType)) {
            throw new RuntimeException("partType="+partType);
        }
        this.label = label;
        this.type = type;
        this.partType = partType;
        int nonnullArgsCount = 0;
        for (String arg1 : args) {
            if (arg1 != null) {
                nonnullArgsCount++;
            }
        }
        this.args = new String[nonnullArgsCount];
        int j = 0;
        for (String arg : args) {
            if (arg != null) {
                this.args[j] = arg;
                j++;
            }
        }
        this.cost = cost;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.label);
        hash = 53 * hash + Objects.hashCode(this.type);
        hash = 53 * hash + Arrays.deepHashCode(this.args);
        hash = 53 * hash + Objects.hashCode(this.cost);
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
        final Action other = (Action) obj;
        if (!Objects.equals(this.label, other.label)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (!Objects.equals(this.cost, other.cost)) {
            return false;
        }
        return Arrays.deepEquals(this.args, other.args);
    }

    private final @Nullable
    String label;

    /**
     * Get the label
     *
     * @return the value of label
     */
    public @Nullable
    String getLabel() {
        return label;
    }

    private final ActionType type;

    /**
     * Get the action type
     *
     * @return the value of label
     */
    public ActionType getType() {
        return type;
    }

    private final String args[];

    /**
     * Get the action args
     *
     * @return the value of label
     */
    public String[] getArgs() {
        return args;
    }

    private final double cost;

    /**
     * Get the action cost
     *
     * @return the value of label
     */
    public double getCost() {
        return cost;
    }

    /**
     * Convert a string typically taken from on line in the file created by the
     * PDDL planner to create a corresponding object instance.
     *
     * @param s string to parse
     * @return corresponding object instance
     */
    public static Action parse(String s) {

        String label = "";
        int endLabelIndex = s.indexOf(':');
        if (endLabelIndex < s.length() && endLabelIndex > 0) {
            label = s.substring(0, endLabelIndex).trim();
            s = s.substring(endLabelIndex + 1).trim();
        }
        int p1index = s.indexOf('(');
        if (p1index >= 0) {
            s = s.substring(p1index + 1).trim();
        } else {
            throw new IllegalArgumentException(" \"" + s + "\".indexOf('(') returned  " + p1index);
        }
        int p2index = s.indexOf(')');
        double cost = 0.0;
        if (p2index > 0 && p2index < s.length()) {
            final String costString = s.substring(p2index + 1).trim();
            if (costString.length() > 0) {
                cost = Double.parseDouble(costString);
            } else {
                cost = 0;
            }
            s = s.substring(0, p2index);
        } else {
            throw new IllegalArgumentException(" \"" + s + "\".indexOf(')') returned  " + p2index);
        }
        String args[] = s.split("[ \t]+");
        String typename = args[0];
        return new Action(label, typename, Arrays.copyOfRange(args, 1, args.length), null, cost);
    }

    @Override
    public String toString() {
        return type + "-" + Arrays.toString(args);
    }

    /**
     * Create a line that could be put in a PDDL file that would perform this
     * action. Essentially the reverse of parse.
     *
     * @return line of text
     */
    public String asPddlLine() {
        return ("(" + type + " " + String.join(" ", args) + ")");
    }

    public Object[] toTableArray() {
        return new Object[]{
            type, (args != null && args.length > 0) ? args[0] : "", (args != null && args.length > 1) ? args[1] : "", (args != null && args.length > 2) ? args[2] : ""
        };
    }

}
