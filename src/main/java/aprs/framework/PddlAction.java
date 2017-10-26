/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copywrite/Warranty Disclaimer
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
package aprs.framework;

import java.util.Arrays;
import java.util.Objects;

/**
 * Instances of PDDL (Planning Domain Definition Language) Actions
 * 
 * An ordered list of actions is the typical output of a PDDL planner.
 * The list may be read from a file or generated automatically.
 * 
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PddlAction {

    /**
     * Create an instance from the required parameters.
     * @param label string added to the display table 
     * @param type  the kind of action to be performed  
     * @param args arguments for the action
     * @param cost cost as reported by planner
     */
    public PddlAction(String label, String type, String[] args, String cost) {
        this.label = label;
        this.type = type;
        this.args = args;
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
        final PddlAction other = (PddlAction) obj;
        if (!Objects.equals(this.label, other.label)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (!Objects.equals(this.cost, other.cost)) {
            return false;
        }
        if (!Arrays.deepEquals(this.args, other.args)) {
            return false;
        }
        return true;
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
    
    private final String type;

    /**
     * Get the action type
     *
     * @return the value of label
     */
    public String getType() {
        return type;
    }
    
    private final String args[];

    /**
     * Get the action args
     *
     * @return the value of label
     */
    public String []getArgs() {
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
     * Convert a string typically taken from on line in the file created by the PDDL planner to
     * create a corresponding object instance.
     * 
     * @param s string to parse
     * @return corresponding object instance
     */
    public static PddlAction parse(String s) {
       

        String label = "";
        int endlabelindex = s.indexOf(':');
        if (endlabelindex < s.length() && endlabelindex > 0) {
            label = s.substring(0, endlabelindex).trim();
            s = s.substring(endlabelindex + 1).trim();
        } 
        int p1indx = s.indexOf('(');
        if (p1indx >= 0) {
            s = s.substring(p1indx + 1).trim();
        } else {
            return null;
        }
        int p2indx = s.indexOf(')');
        String cost = "";
        if (p2indx > 0 && p2indx < s.length()) {
            cost = s.substring(p2indx + 1).trim();
            s = s.substring(0, p2indx);
        } else {
            return null;
        }
        String args[] = s.split("[ \t]+");
        String type = args[0];
        args = Arrays.copyOfRange(args, 1, args.length);
        return new PddlAction(label, type, args, cost);
    }

    @Override
    public String toString() {
        return type + "-" + Arrays.toString(args);
    }

    public String asPddlLine() {
        return ("("+type +" "+String.join(" ", args)+")");
    }
    
}
