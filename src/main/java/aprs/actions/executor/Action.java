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

import static aprs.actions.executor.ActionType.LOOK_FOR_PARTS;
import java.util.Arrays;
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

    private volatile long planTime;
    private volatile long execTime;
    private volatile boolean executed;

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
    public Action(ActionType actionType) {
        this("",actionType,new String[]{},"");
    }
        
    
    /**
     * Create an instance from the required parameters.
     *
     * @param actionType enumerated type of action
     * @param arg single argument for the action
     */
    public Action(ActionType actionType, String arg) {
        this("",actionType,new String[]{arg},"");
    }
        
    /**
     * Create an instance from the required parameters.
     *
     * @param actionType enumerated type of action
     * @param args arguments for the action
     */
    public Action(ActionType actionType, String []args) {
        this("",actionType,args,"");
    }
    
    
    /**
     * Create an instance from the required parameters.
     *
     * @param actionType type of action
     * @param args arguments for the action
     * @param cost cost as reported by planner
     */
    public Action(ActionType actionType, String []args,double cost) {
        this("",actionType,args,""+cost);
    }
    
    /**
     * Create an instance from the required parameters.
     *
     * @param label string added to the display table
     * @param typename name of the kind of action to be performed
     * @param args arguments for the action
     * @param cost cost as reported by planner
     */
    private Action(String label, String typename, @Nullable String[] args, String cost) {
        this(label,
                "look-for-part".equals(typename) // annoying legacy corner case
                ? LOOK_FOR_PARTS
                : ActionType.valueOf(typename.trim().replace('-', '_').toUpperCase()),
                args,
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
    private Action(String label, ActionType type, @Nullable String[] args, String cost) {
        this.label = label;
        this.type = type;
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

    private final String label;

    /**
     * Get the label
     *
     * @return the value of label
     */
    public String getLabel() {
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

    private final String cost;

    /**
     * Get the action cost
     *
     * @return the value of label
     */
    public String getCost() {
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
        String cost = "";
        if (p2index > 0 && p2index < s.length()) {
            cost = s.substring(p2index + 1).trim();
            s = s.substring(0, p2index);
        } else {
            throw new IllegalArgumentException(" \"" + s + "\".indexOf(')') returned  " + p2index);
        }
        String args[] = s.split("[ \t]+");
        String typename = args[0];
        return new Action(label, typename, Arrays.copyOfRange(args, 1, args.length), cost);
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

}
