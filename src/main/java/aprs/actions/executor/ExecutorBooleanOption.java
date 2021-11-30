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

import java.util.EnumMap;
import java.util.Map;

/**
 *
 * @author shackle
 */
public enum ExecutorBooleanOption implements ExecutorOption {
    REVERSE,
    FORCE_NAME_CHANGE;

    public ExecutorOptionBooleanValuePair with(boolean arg) {
        return new ExecutorOptionBooleanValuePair(this, arg);
    }

    public static Map<ExecutorBooleanOption, Boolean> map(ExecutorOptionValuePair<?,?> ... optionpairs) {
        EnumMap<ExecutorBooleanOption, Boolean> map = new EnumMap(ExecutorBooleanOption.class);
        for (int i = 0; i < optionpairs.length; i++) {
            ExecutorOptionValuePair<?,?> optionpair = optionpairs[i];
            if (optionpair instanceof ExecutorOptionBooleanValuePair) {
                ExecutorOptionBooleanValuePair booloptionpair = (ExecutorOptionBooleanValuePair) optionpair;
                map.put(booloptionpair.getKey(), booloptionpair.getValue());
            }
        }
        return map;
    }
}
