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

import aprs.learninggoals.GoalLearner;
import aprs.misc.SlotOffsetProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class TrayFillInfo {

    private final List<PhysicalItem> origItems;
    private final SlotOffsetProvider localSlotOffsetProvider;
    private final List<PhysicalItem> kitTrays;
    private final List<PhysicalItem> partTrays;
    private final List<PhysicalItem> partsInKit;
    private final List<PhysicalItem> partsInPartsTrays;
    private final List<TraySlotListPairing> partsTrayPairings;
    private final List<TraySlotListPairing> kitTrayPairings;
    private final List<TraySlotListItem> emptyKitSlots;
    private final List<TraySlotListItem> emptyPartTraySlots;
    private final List<PhysicalItem> unassignedParts;

    private final boolean overrideRotationOffset;
    private final double newRotationOffset;

    static public List<PhysicalItem> filterForKitTrays(List<PhysicalItem> teachItems) {
        return teachItems.stream()
                .filter(x -> "KT".equals(x.getType()))
                .collect(Collectors.toList());
    }

    static public List<PhysicalItem> filterForPartsTrays(List<PhysicalItem> teachItems) {
        return teachItems.stream()
                .filter(x -> "PT".equals(x.getType()))
                .collect(Collectors.toList());
    }

    static public List<PhysicalItem> filterForParts(List<PhysicalItem> teachItems) {
        return teachItems.stream()
                .filter(x -> "P".equals(x.getType()))
                .collect(Collectors.toList());
    }

    @Nullable
    public static PhysicalItem findClosestPart(double sx, double sy, List<PhysicalItem> items) {
        return items.stream()
                .filter(x -> x.getType() != null && x.getType().equals("P"))
                .min(Comparator.comparing(pitem -> Math.hypot(sx - pitem.x, sy - pitem.y)))
                .orElse(null);
    }

    @SuppressWarnings("initialization")
    public TrayFillInfo(List<PhysicalItem> origItems,
            SlotOffsetProvider localSlotOffsetProvider,
            boolean overrideRotationOffset,
            double newRotationOffset) {
        this.origItems = origItems;
        this.localSlotOffsetProvider = localSlotOffsetProvider;
        this.overrideRotationOffset = overrideRotationOffset;
        this.newRotationOffset = newRotationOffset;
        kitTrays = filterForKitTrays(origItems);
        partTrays = filterForPartsTrays(origItems);
        unassignedParts = filterForParts(origItems);
        partsInKit = new ArrayList<>();
        partsInPartsTrays = new ArrayList<>();
        partsTrayPairings = new ArrayList<>();
        kitTrayPairings = new ArrayList<>();
        emptyKitSlots = new ArrayList<>();
        emptyPartTraySlots = new ArrayList<>();
        for (PhysicalItem kit : kitTrays) {
            List<Slot> slotOffsetList = localSlotOffsetProvider.getSlotOffsets(kit.getName(), false);
            if (null == slotOffsetList) {
                throw new IllegalStateException("getSlotOffsetList(" + kit.getName() + ") returned null");
            }
            TraySlotListPairing listPairing = createTraySlotsPairing(kit, slotOffsetList);
            partsInKit.addAll(listPairing.getParts());
            emptyKitSlots.addAll(listPairing.getEmptySlots());
            kitTrayPairings.add(listPairing);
        }
        for (PhysicalItem kit : partTrays) {
            List<Slot> slotOffsetList = localSlotOffsetProvider.getSlotOffsets(kit.getName(), false);
            if (null == slotOffsetList) {
                throw new IllegalStateException("getSlotOffsetList(" + kit.getName() + ") returned null");
            }
            TraySlotListPairing listPairing = createTraySlotsPairing(kit, slotOffsetList);
            partsTrayPairings.add(listPairing);
            partsInPartsTrays.addAll(listPairing.getParts());
            emptyPartTraySlots.addAll(listPairing.getEmptySlots());
        }
    }

    private TraySlotListPairing createTraySlotsPairing(PhysicalItem kit, List<Slot> slotOffsetList) {
        TraySlotListPairing listPairing = new TraySlotListPairing(kit);
        Map<String, String> slotPrpToPartSkuMap = listPairing.getSlotPrpToPartSkuMap();
        for (Slot slotOffset : slotOffsetList) {
            PhysicalItem absSlot;
            if (!overrideRotationOffset) {
                absSlot = localSlotOffsetProvider.absSlotFromTrayAndOffset(kit, slotOffset);
            } else {
                absSlot = localSlotOffsetProvider.absSlotFromTrayAndOffset(kit, slotOffset, newRotationOffset);
            }
            if (null == absSlot) {
                throw new IllegalStateException("No absSlot obtainable for slotOffset name " + slotOffset.getName() + " in kit " + kit.getName());
            }
            PhysicalItem closestPart = findClosestPart(absSlot.x, absSlot.y, unassignedParts);
            if (null == closestPart) {
                slotPrpToPartSkuMap.put(slotOffset.getPrpName(), "empty");
                listPairing.getList().add(new TraySlotListItem(slotOffset, absSlot, closestPart));
                continue;
            }
            double minDist = Math.hypot(absSlot.x - closestPart.x, absSlot.y - closestPart.y);
            if (minDist < 20 + slotOffset.getDiameter() / 2.0) {
                unassignedParts.remove(closestPart);
                listPairing.getList().add(new TraySlotListItem(slotOffset, absSlot, closestPart));
                partsInKit.add(closestPart);
                String shortPartName = closestPart.getName();
                if (shortPartName.startsWith("sku_")) {
                    shortPartName = shortPartName.substring(4);
                }
                String indexString = slotOffset.getSlotIndexString();
                if (null == indexString) {
                    String prpName = slotOffset.getPrpName();
                    if (null != prpName) {
                        indexString = prpName.substring(prpName.lastIndexOf("_") + 1);
                        slotOffset.setSlotIndexString(indexString);
                    } else {
                        throw new IllegalStateException("slotOffset has neither slotIndexString nor prpName :" + slotOffset.getName());
                    }
                }
                slotPrpToPartSkuMap.put(slotOffset.getPrpName(), closestPart.getName());
            } else {
                TraySlotListItem traySlotListItem = new TraySlotListItem(slotOffset, absSlot, null);
                listPairing.getList().add(traySlotListItem);
                listPairing.getEmptySlots().add(traySlotListItem);
                slotPrpToPartSkuMap.put(slotOffset.getPrpName(), "empty");
            }
        }
        return listPairing;
    }

    public List<PhysicalItem> getOrigItems() {
        return origItems;
    }

    public SlotOffsetProvider getLocalSlotOffsetProvider() {
        return localSlotOffsetProvider;
    }

    public List<PhysicalItem> getKitTrays() {
        return kitTrays;
    }

    public List<PhysicalItem> getPartTrays() {
        return partTrays;
    }

    public List<PhysicalItem> getPartsInKit() {
        return partsInKit;
    }

    public List<PhysicalItem> getPartsInPartsTrays() {
        return partsInPartsTrays;
    }

    public List<TraySlotListPairing> getPartsTrayPairings() {
        return partsTrayPairings;
    }

    public List<TraySlotListPairing> getKitTrayPairings() {
        return kitTrayPairings;
    }

    public List<PhysicalItem> getUnassignedParts() {
        return unassignedParts;
    }

    public boolean isOverrideRotationOffset() {
        return overrideRotationOffset;
    }

    public double getNewRotationOffset() {
        return newRotationOffset;
    }

    public List<TraySlotListItem> getEmptyKitSlots() {
        return emptyKitSlots;
    }

    public List<TraySlotListItem> getEmptyPartTraySlots() {
        return emptyPartTraySlots;
    }
    
    

}
