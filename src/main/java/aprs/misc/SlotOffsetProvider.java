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
package aprs.misc;

import aprs.database.PhysicalItem;
import aprs.database.Slot;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface for classes that can provide slot offset information. Each
 * AprsSystemInterface provides this and the supervisor combines output from multiple
 * systems to create its own SlotOffsetProvider.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public interface SlotOffsetProvider {

    /**
     * Get a list of slots with names and relative position offsets for a given
     * kit or parts tray name.
     *
     * @param name name of the type of kit or slot tray
     * @param ignoreEmpty if false  
     *          no slots being found logs a verbose error message 
     *          and throws IllegalStateException (good for fail fast) or
     *  if true 
     *          simply returns an empty list (good or display or when multiple 
     *          will be checked.
     * 
     * @return list of slots with relative position offsets.
     */
    public List<Slot> getSlotOffsets(String name,boolean ignoreEmpty);

    /**
     * Compute the absolute position of a slot from a slot offset and a tray
     * instance.
     *
     * @param tray tray with absolute position
     * @param offsetItem slot with relative position offset for that kind of
     * tray
     * @return slot with absolute position
     */
    @Nullable public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem);
    
    /**
     * Compute the absolute position of a slot from a slot offset and a tray
     * instance.
     *
     * @param tray tray with absolute position
     * @param offsetItem slot with relative position offset for that kind of
     * tray
     * @return slot with absolute position
     */
    @Nullable public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem,double rotationOffset);
}
