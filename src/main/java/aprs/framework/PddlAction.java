/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework;

import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PddlAction {

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

}
