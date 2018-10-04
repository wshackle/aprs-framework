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
package aprs.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class TraySlotListPairing {

    private final PhysicalItem tray;
    private final List<PhysicalItem> parts = new ArrayList<>();
    private final List<TraySlotListItem> list = new ArrayList<>();
    private final List<TraySlotListItem> emptySlots = new ArrayList<>();
    private final Map<String, String> slotPrpToPartSkuMap = new HashMap<>();

    public TraySlotListPairing(PhysicalItem tray) {
        this.tray = tray;
    }

    public List<PhysicalItem> getParts() {
        return parts;
    }

    public PhysicalItem getTray() {
        return tray;
    }

    public List<TraySlotListItem> getList() {
        return list;
    }

    public List<TraySlotListItem> getEmptySlots() {
        return emptySlots;
    }

    
    public Map<String, String> getSlotPrpToPartSkuMap() {
        return slotPrpToPartSkuMap;
    }

}
