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
package aprs.framework.learninggoals;

import aprs.framework.PddlAction;
import aprs.framework.SlotOffsetProvider;
import aprs.framework.database.PhysicalItem;
import aprs.framework.database.Slot;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.collections.impl.block.factory.Comparators;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class GoalLearner {

    private @Nullable
    Predicate<PhysicalItem> itemPredicate = null;

    /**
     * Get the value of itemPredicate
     *
     * @return the value of itemPredicate
     */
    @Nullable
    public Predicate<PhysicalItem> getItemPredicate() {
        return itemPredicate;
    }

    /**
     * Set the value of itemPredicate
     *
     * @param itemPredicate new value of itemPredicate
     */
    public void setItemPredicate(Predicate<PhysicalItem> itemPredicate) {
        this.itemPredicate = itemPredicate;
    }

    private boolean isWithinLimits(PhysicalItem item) {
        if (null == itemPredicate) {
            return true;
        }
        return itemPredicate.test(item);
    }

    private @Nullable
    Predicate<List<PhysicalItem>> kitTrayListPredicate;

    /**
     * Get the value of kitTrayListPredicate
     *
     * @return the value of kitTrayListPredicate
     */
    @Nullable
    public Predicate<List<PhysicalItem>> getKitTrayListPredicate() {
        return kitTrayListPredicate;
    }

    /**
     * Set the value of kitTrayListPredicate
     *
     * @param kitTrayListPredicate new value of kitTrayListPredicate
     */
    public void setKitTrayListPredicate(Predicate<List<PhysicalItem>> kitTrayListPredicate) {
        this.kitTrayListPredicate = kitTrayListPredicate;
    }

    boolean checkKitTrays(List<PhysicalItem> kitTrays) {
        if (kitTrayListPredicate == null) {
            return (null != kitTrays && !kitTrays.isEmpty());
        }
        return kitTrayListPredicate.test(kitTrays);
    }

    private @Nullable
    SlotOffsetProvider slotOffsetProvider;

    /**
     * Get the value of slotOffsetProvider
     *
     * @return the value of slotOffsetProvider
     */
    @Nullable
    public SlotOffsetProvider getSlotOffsetProvider() {
        return slotOffsetProvider;
    }

    /**
     * Set the value of slotOffsetProvider
     *
     * @param slotOffsetProvider new value of slotOffsetProvider
     */
    public void setSlotOffsetProvider(SlotOffsetProvider slotOffsetProvider) {
        this.slotOffsetProvider = slotOffsetProvider;
    }

    @Nullable
    private static PhysicalItem closestPart(double sx, double sy, List<PhysicalItem> items) {
        return items.stream()
                .filter(x -> x.getType() != null && x.getType().equals("P"))
                .min(Comparator.comparing(pitem -> Math.hypot(sx - pitem.x, sy - pitem.y)))
                .orElse(null);
    }

    /**
     * Create an action list to recreate the configuration in the provided
     * commonItems list.
     *
     * @param commonItems list of kits and parts with positions to learn from
     * @param allEmptyA optional boolean array to receive flag if all trays were
     * empty
     * @return list of PddlAction's that can be used to recreate the
     * configuration of the example data.
     */
    public List<PddlAction> createActionListFromVision(List<PhysicalItem> commonItems, boolean allEmptyA[], boolean overrideRotationOffset, double newRotationOffset) {
        return createActionListFromVision(commonItems, commonItems, allEmptyA, overrideRotationOffset, newRotationOffset);
    }

    private volatile List<String> lastCreateActionListFromVisionKitToCheckStrings = Collections.emptyList();

    public List<String> getLastCreateActionListFromVisionKitToCheckStrings() {
        return new ArrayList<>(lastCreateActionListFromVisionKitToCheckStrings);
    }
    
    public void setLastCreateActionListFromVisionKitToCheckStrings(List<String> strings) {
        if(null == strings) {
            throw new IllegalArgumentException("null == strings");
        }
        this.lastCreateActionListFromVisionKitToCheckStrings = new ArrayList<>(strings);
    }

    public static boolean kitToCheckStringsEqual(List<String> kitToCheckStrings1, List<String> kitToCheckStrings2) {
        if (kitToCheckStrings1.size() != kitToCheckStrings2.size()) {
            return false;
        }
        for (String s1 : kitToCheckStrings1) {
            boolean matchFound = false;
            for (String s2 : kitToCheckStrings2) {
                if (s1.equals(s2)) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                return false;
            }
        }
        for (String s2 : kitToCheckStrings2) {
            boolean matchFound = false;
            for (String s1 : kitToCheckStrings1) {
                if (s1.equals(s2)) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                return false;
            }
        }
        return true;
    }

    public boolean isCorrectionMode() {
        return correctionMode;
    }

    private volatile boolean correctionMode = false;

    public void setCorrectionMode(boolean newCorrectionMode) {
        this.correctionMode = newCorrectionMode;
    }

    /**
     * Use the provided list of items create a set of actions that will fill
     * empty trays to match.Load this list into the PDDL executor.
     *
     * @param requiredItems list of items that have to be seen before the robot
     * can begin
     * @param teachItems list of trays and items in the trays as they should be
     * when complete.
     * @param allEmptyA optional array to pass where value of whether all trays
     * were empty will be stored.
     *
     * @return a list of actions than can be run to move parts from trays to
     * recreate the same configuration
     */
    public List<PddlAction> createActionListFromVision(List<PhysicalItem> requiredItems, List<PhysicalItem> teachItems, boolean allEmptyA[], boolean overrideRotationOffset, double newRotationOffset) {
        Map<String, Integer> requiredItemsMap
                = requiredItems.stream()
                .filter(this::isWithinLimits)
                .collect(Collectors.toMap(PhysicalItem::getName, x -> 1, (a, b) -> a + b));

        SlotOffsetProvider localSlotOffsetProvider = this.slotOffsetProvider;
        if (null == localSlotOffsetProvider) {
            throw new IllegalStateException("null == slotOffsetProvider");
        }

        String requiredItemsString
                = requiredItemsMap
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(" "));
        List<PhysicalItem> kitTrays = teachItems.stream()
                .filter(x -> "KT".equals(x.getType()))
                .collect(Collectors.toList());
        if (!checkKitTrays(kitTrays)) {
            return Collections.emptyList();
        }

        List<PddlAction> l = new ArrayList<>();

        boolean allEmpty = true;

        if(!correctionMode) {
            l.add(PddlAction.parse("(clear-kits-to-check)"));
        }
        l.add(PddlAction.parse("(look-for-parts 0 " + requiredItemsString + ")"));
        ConcurrentMap<String, Integer> kitUsedMap = new ConcurrentHashMap<>();
        ConcurrentMap<String, Integer> ptUsedMap = new ConcurrentHashMap<>();
        List<String> kitToCheckStrings = new ArrayList<>();
        for (PhysicalItem kit : kitTrays) {
            Map<String, String> slotPrpToPartSkuMap = new HashMap<>();
            assert (null != localSlotOffsetProvider) : "@AssumeAssertion(nullness)";
            List<Slot> slotOffsetList = localSlotOffsetProvider.getSlotOffsets(kit.getName(), false);
            if (null == slotOffsetList) {
                throw new IllegalStateException("getSlotOffsetList(" + kit.getName() + ") returned null");
            }
            double x = kit.x;
            double y = kit.y;
            double rot = kit.getRotation();
            String shortKitName = kit.getName();
            if (shortKitName.startsWith("sku_")) {
                shortKitName = shortKitName.substring(4);
            }
            int kitNumber = -1;
            for (int slotOffsetIndex = 0; slotOffsetIndex < slotOffsetList.size(); slotOffsetIndex++) {
                Slot slotOffset = slotOffsetList.get(slotOffsetIndex);
                PhysicalItem absSlot;
                if (!overrideRotationOffset) {
                    absSlot = localSlotOffsetProvider.absSlotFromTrayAndOffset(kit, slotOffset);
                } else {
                    absSlot = localSlotOffsetProvider.absSlotFromTrayAndOffset(kit, slotOffset, newRotationOffset);
                }
                if (null == absSlot) {
                    throw new IllegalStateException("No absSlot obtainable for slotOffset name " + slotOffset.getName() + " in kit " + kit.getName());
                }
                PhysicalItem closestPart = closestPart(absSlot.x, absSlot.y, teachItems);
                if (null == closestPart) {
                    slotPrpToPartSkuMap.put(slotOffset.getPrpName(), "empty");
                    continue;
                }
                double minDist = Math.hypot(absSlot.x - closestPart.x, absSlot.y - closestPart.y);
                if (minDist < 20 + slotOffset.getDiameter()/2.0) {
                    int pt_used_num = ptUsedMap.compute(closestPart.getName(), (k, v) -> (v == null) ? 1 : (v + 1));
                    String shortPartName = closestPart.getName();
                    if (shortPartName.startsWith("sku_")) {
                        shortPartName = shortPartName.substring(4);
                    }
                    String partName = shortPartName + "_in_pt_" + pt_used_num;
                    if (!correctionMode) {
                        l.add(PddlAction.parse("(take-part " + partName + ")"));
                    }
                    String shortSkuName = slotOffset.getSlotForSkuName();
                    if (null == shortSkuName) {
                        throw new IllegalStateException("slotOffset has no slotForSkuName :" + slotOffset.getName());
                    }
                    if (shortSkuName.startsWith("sku_")) {
                        shortSkuName = shortSkuName.substring(4);
                    }
                    if (shortSkuName.startsWith("part_")) {
                        shortSkuName = shortSkuName.substring(5);
                    }
                    if (kitNumber < 0) {
                        kitNumber = kitUsedMap.compute(kit.getName(), (k, v) -> (v == null) ? 1 : (v + 1));
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
                    String slotName = "empty_slot_" + indexString + "_for_" + shortSkuName + "_in_" + shortKitName + "_" + kitNumber;
                    if (!correctionMode) {
                        l.add(PddlAction.parse("(place-part " + slotName + ")"));
                    }
                    slotPrpToPartSkuMap.put(slotOffset.getPrpName(), closestPart.getName());
                    allEmpty = false;
                } else {
                    slotPrpToPartSkuMap.put(slotOffset.getPrpName(), "empty");
                }
            }
            kitToCheckStrings.add("(add-kit-to-check " + kit.getName() + " "
                    + slotPrpToPartSkuMap.entrySet().stream()
                    .sorted(Comparators.byFunction(Map.Entry<String,String>::getKey))
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(" "))
                    + ")");
        }
        if (!correctionMode) {
            l.add(PddlAction.parse("(look-for-parts 2)"));
        }
        l.add(PddlAction.parse("(clear-kits-to-check)"));
        for (String kitToCheckString : kitToCheckStrings) {
            l.add(PddlAction.parse(kitToCheckString));
        }
        l.add(PddlAction.parse("(check-kits)"));
        if (!correctionMode) {
            l.add(PddlAction.parse("(clear-kits-to-check)"));
        }
        l.add(PddlAction.parse("(end-program)"));
        if (null != allEmptyA && allEmptyA.length > 0) {
            allEmptyA[0] = allEmpty;
        }
        lastCreateActionListFromVisionKitToCheckStrings = kitToCheckStrings;
        return l;
    }

}
