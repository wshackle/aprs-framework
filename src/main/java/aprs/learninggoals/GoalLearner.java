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
package aprs.learninggoals;

import aprs.actions.executor.Action;
import static aprs.actions.executor.ActionType.CHECK_KITS;
import static aprs.actions.executor.ActionType.CLEAR_KITS_TO_CHECK;
import static aprs.actions.executor.ActionType.END_PROGRAM;
import static aprs.actions.executor.ActionType.SET_CORRECTION_MODE;
import aprs.misc.SlotOffsetProvider;
import aprs.database.PhysicalItem;
import aprs.database.Slot;
import aprs.misc.Utils;
import aprs.system.AprsSystem;
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

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class GoalLearner {

    private @Nullable
    Predicate<PhysicalItem> itemPredicate = null;

    /**
     * Set the value of itemPredicate
     *
     * @param itemPredicate new value of itemPredicate
     */
    public void setItemPredicate(@Nullable Predicate<PhysicalItem> itemPredicate) {
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
     * Set the value of kitTrayListPredicate
     *
     * @param kitTrayListPredicate new value of kitTrayListPredicate
     */
    public void setKitTrayListPredicate(@Nullable Predicate<List<PhysicalItem>> kitTrayListPredicate) {
        this.kitTrayListPredicate = kitTrayListPredicate;
    }

    private boolean checkKitTrays(List<PhysicalItem> kitTrays) {
        if (kitTrayListPredicate == null) {
            return (null != kitTrays && !kitTrays.isEmpty());
        }
        return kitTrayListPredicate.test(kitTrays);
    }

    private volatile @Nullable
    AprsSystem aprsSystem = null;

    public @Nullable
    AprsSystem getAprsSystem() {
        return aprsSystem;
    }

    public void setAprsSystem(AprsSystem aprsSystem) {
        this.aprsSystem = aprsSystem;
    }

    private @Nullable
    SlotOffsetProvider slotOffsetProvider;

    /**
     * Set the value of slotOffsetProvider
     *
     * @param slotOffsetProvider new value of slotOffsetProvider
     */
    public void setSlotOffsetProvider(SlotOffsetProvider slotOffsetProvider) {
        this.slotOffsetProvider = slotOffsetProvider;
    }

    static public @Nullable
    PhysicalItem closestPart(double sx, double sy, List<PhysicalItem> items) {
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
     * @param overrideRotationOffset whether to add rotation offset to tray rotation when computing 
     *  absolute slot positions
     * @param newRotationOffset offset to add if overrideRotationOffset set
     * @return list of Action's that can be used to recreate the configuration
     * of the example data.
     */
    public List<Action> createActionListFromVision(List<PhysicalItem> commonItems, boolean allEmptyA[], boolean overrideRotationOffset, double newRotationOffset) {
        return createActionListFromVision(commonItems, commonItems, allEmptyA, overrideRotationOffset, newRotationOffset);
    }

    private volatile List<String> lastCreateActionListFromVisionKitToCheckStrings = Collections.emptyList();

    public List<String> getLastCreateActionListFromVisionKitToCheckStrings() {
        return new ArrayList<>(lastCreateActionListFromVisionKitToCheckStrings);
    }

    private volatile StackTraceElement setLastCreateActionListFromVisionKitToCheckStringsTrace @Nullable []  = null;
    private volatile @Nullable
    Thread setLastCreateActionListFromVisionKitToCheckStringsThread = null;
    private volatile long setLastCreateActionListFromVisionKitToCheckStringsTime;

    public StackTraceElement @Nullable [] getSetLastCreateActionListFromVisionKitToCheckStringsTrace() {
        return setLastCreateActionListFromVisionKitToCheckStringsTrace;
    }

    public @Nullable
    Thread getSetLastCreateActionListFromVisionKitToCheckStringsThread() {
        return setLastCreateActionListFromVisionKitToCheckStringsThread;
    }

    public long getSetLastCreateActionListFromVisionKitToCheckStringsTime() {
        return setLastCreateActionListFromVisionKitToCheckStringsTime;
    }

    public void setLastCreateActionListFromVisionKitToCheckStrings(List<String> strings) {
        if (null == strings) {
            throw new IllegalArgumentException("null == strings");
        }
        setLastCreateActionListFromVisionKitToCheckStringsTime = System.currentTimeMillis();
        setLastCreateActionListFromVisionKitToCheckStringsThread = Thread.currentThread();
        setLastCreateActionListFromVisionKitToCheckStringsTrace = Thread.currentThread().getStackTrace();
        this.lastCreateActionListFromVisionKitToCheckStrings = new ArrayList<>(strings);
    }

    public static @Nullable
    String kitToCheckStringsEqual(List<String> kitToCheckStrings1, List<String> kitToCheckStrings2) {
        if (kitToCheckStrings1.size() != kitToCheckStrings2.size()) {
            return "sizes differ : " + kitToCheckStrings1.size() + "!=" + kitToCheckStrings2.size();
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
                return "no match for " + s1 + " in first list";
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
                return "no match for " + s2 + " in second list";
            }
        }
        return null;
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
     * @param overrideRotationOffset use newRotationOffset rather than previous
     * @param newRotationOffset new rotation offset to use if overrideRotationOffset is true
     *
     * @return a list of actions than can be run to move parts from trays to
     * recreate the same configuration
     */
    public List<Action> createActionListFromVision(
            List<PhysicalItem> requiredItems,
            List<PhysicalItem> teachItems,
            boolean allEmptyA[],
            boolean overrideRotationOffset,
            double newRotationOffset) {
        Map<String, Integer> requiredItemsMap
                = requiredItems.stream()
                        .filter(this::isWithinLimits)
                        .collect(Collectors.toMap(PhysicalItem::getName, x -> 1, (a, b) -> a + b));

        SlotOffsetProvider localSlotOffsetProvider = this.slotOffsetProvider;
        if (null == localSlotOffsetProvider) {
            throw new IllegalStateException("null == slotOffsetProvider");
        }
//        final Integer black_gear_map_result = requiredItemsMap.get("black_gear");
//
//        if(null != black_gear_map_result && black_gear_map_result > 1) {
//            throw new RuntimeException("requiredItems="+requiredItems);
//        }
//        String requiredItemsString
//                = requiredItemsMap
//                        .entrySet()
//                        .stream()
//                        .map(entry -> entry.getKey() + "=" + entry.getValue())
//                        .collect(Collectors.joining(" "));
        List<PhysicalItem> kitTrays = filterForKitTrays(teachItems);
        if (!checkKitTrays(kitTrays)) {
            return Collections.emptyList();
        }

        List<Action> l = new ArrayList<>();

        l.add(Action.newSingleArgAction(SET_CORRECTION_MODE, correctionMode));
        l.add(Action.newNoArgAction(CLEAR_KITS_TO_CHECK));
        l.add(Action.newLookForParts(0, requiredItemsMap));
        boolean allEmpty = true;
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
            String shortKitName = Utils.shortenItemPartName(kit.getName());
            int kitNumber = -1;
            for (Slot slotOffset : slotOffsetList) {
                Slot absSlot;
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
                final double maxAllowedDist = 20 + slotOffset.getDiameter() / 2.0;
                if (minDist < maxAllowedDist) {
                    int pt_used_num = ptUsedMap.compute(closestPart.getName(), (k, v) -> (v == null) ? 1 : (v + 1));
                    String shortPartName = Utils.shortenItemPartName(closestPart.getName());
                    String partName = shortPartName + "_in_pt_" + pt_used_num;
                    if (!correctionMode) {
                        l.add(Action.newTakePartAction(partName));
                    }
                    final String fullSlotForSkuName = slotOffset.getSlotForSkuName();
                    if(null == fullSlotForSkuName) {
                        throw new NullPointerException("slotOffset.getSlotForSkuName() returned null : slotOffset="+slotOffset);
                    }
                    String shortPartForSlotName = Utils.shortenItemPartName(fullSlotForSkuName);
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
                    String slotName = "empty_slot_" + indexString + "_for_" + shortPartForSlotName + "_in_" + shortKitName + "_" + kitNumber;
                    if (!correctionMode) {
                        l.add(Action.newPlacePartAction(slotName, shortPartName));
                    }
                    slotPrpToPartSkuMap.put(slotOffset.getPrpName(), closestPart.getName());
                    allEmpty = false;
                } else {
                    slotPrpToPartSkuMap.put(slotOffset.getPrpName(), "empty");
                }
            }
            kitToCheckStrings.add("(add-kit-to-check " + kit.getName() + " "
                    + slotPrpToPartSkuMap.entrySet().stream()
                            .sorted(Comparator.comparing(Map.Entry<String, String>::getKey))
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining(" "))
                    + ")");
        }
        if (!correctionMode) {
            l.add(Action.newLookForParts());
        }
        l.add(Action.newNoArgAction(CLEAR_KITS_TO_CHECK));
        for (String kitToCheckString : kitToCheckStrings) {
            l.add(Action.parse(kitToCheckString));
        }
        l.add(Action.newNoArgAction(CHECK_KITS));
        l.add(Action.newLookForParts());
        l.add(Action.newNoArgAction(CLEAR_KITS_TO_CHECK));
        l.add(Action.newNoArgAction(END_PROGRAM));
        if (null != allEmptyA && allEmptyA.length > 0) {
            allEmptyA[0] = allEmpty;
        }
        lastCreateActionListFromVisionKitToCheckStrings = kitToCheckStrings;
        return l;
    }

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
}
