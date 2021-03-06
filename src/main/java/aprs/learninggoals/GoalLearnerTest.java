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

import aprs.misc.ActiveWinEnum;
import aprs.actions.executor.Action;
import aprs.misc.SlotOffsetProvider;
import aprs.database.KitTray;
import aprs.database.Part;
import aprs.database.PartsTray;
import aprs.database.PhysicalItem;
import aprs.database.Slot;
import aprs.database.Tray;
import aprs.simview.Object2DOuterJPanel;
import aprs.database.vision.DatabasePoseUpdater;
import crcl.base.PoseType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import javax.swing.JFrame;

import aprs.actions.executor.CrclGenerator.PoseProvider;
import aprs.system.AprsSystem;
import crcl.utils.XFuture;
import crcl.utils.XFutureVoid;
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Test/Demo of the GoalLearner
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("guieffect")
public class GoalLearnerTest {

    /**
     * Demonstrate the GoalLearner by first presenting a set of data to learn
     * from that can be modified by the user, passing it to the GoalLearner and
     * then running the created plan on a simulated robot.
     *
     * The passed SlotOffsetProvider replaces the need for a database, and the
     * PoseProvider replaces the need for a live camera and computer vision
     * system.
     *
     * This is the only externally callable method in this class.
     *
     * @param args not used
     */
    public static void main(String[] args) {

        // Create a mapping of KitTray's to Lists of relative slot offsets.
        // This would otherwise be obtained from the database.
        Map<String, List<Slot>> map = new HashMap<>();

        // Kit Tray has 4 slots two for small gears and two for large gears
        KitTray tray1 = KitTray.newKitTrayFromSkuIdRotXY("kit_s2l2_vessel", 1, 0, 50, 50);
        Slot s11 = Slot.slotFromTrayPartNameIndexRotationXYDiameter(tray1, "small_gear", 1, 0.0, +32, +55,50);
        addMap(s11, map);
        Slot s12 = Slot.slotFromTrayPartNameIndexRotationXYDiameter(tray1, "small_gear", 2, 0.0, -32, +55,50);
        addMap(s12, map);
        Slot s13 = Slot.slotFromTrayPartNameIndexRotationXYDiameter(tray1, "large_gear", 1, 0.0, +54, -28,100);
        addMap(s13, map);
        Slot s14 = Slot.slotFromTrayPartNameIndexRotationXYDiameter(tray1, "large_gear", 2, 0.0, -54, -28,100);
        addMap(s14, map);

        // Small gear parts tray has two slots for small gears
        PartsTray tray2 = PartsTray.newPartsTrayFromSkuIdRotXY("small_gear_vessel", 1, 0, 200, 200);
        Slot s21 = Slot.slotFromTrayPartNameIndexRotationXYDiameter(tray2, "small_gear", 1, 0.0, +26.5, +26.5,50);
        addMap(s21, map);
        Slot s22 = Slot.slotFromTrayPartNameIndexRotationXYDiameter(tray2, "small_gear", 2, 0.0, -26.5, +26.5,50);
        addMap(s22, map);
        Slot s23 = Slot.slotFromTrayPartNameIndexRotationXYDiameter(tray2, "small_gear", 1, 0.0, +26.5, -26.5,50);
        addMap(s23, map);
        Slot s24 = Slot.slotFromTrayPartNameIndexRotationXYDiameter(tray2, "small_gear", 2, 0.0, -26.5, -26.5,50);
        addMap(s24, map);

        // Large gear parts tray has two slots for large gears
        PartsTray tray3 = PartsTray.newPartsTrayFromSkuIdRotXY("large_gear_vessel", 1, 0, 400, 400);
        Slot s31 = Slot.slotFromTrayPartNameIndexRotationXYDiameter(tray3, "large_gear", 1, 0.0, +59, +0,100);
        addMap(s31, map);
        Slot s32 = Slot.slotFromTrayPartNameIndexRotationXYDiameter(tray3, "large_gear", 2, 0.0, -59, +0,100);
        addMap(s32, map);

        // The slot offset provider is a simple replacement for the part of 
        // the database that would normally be used. (Retrieving from the HashMap instead.)
        SlotOffsetProvider sop = new HashMapSlotOffsetProvider(map);

        List<PhysicalItem> trainingData = new ArrayList<>();

        addList(tray1, s11, "small_gear", sop, trainingData);
        addList(tray1, s13, "large_gear", sop, trainingData);
        addList(tray2, s22, "small_gear", sop, trainingData);
        addList(tray3, s31, "large_gear", sop, trainingData);

        List<PhysicalItem> newTrainingData
                = Object2DOuterJPanel.showAndModifyData(trainingData, sop, -100, -100, +500, +500);
        GoalLearner gl = new GoalLearner();
        gl.setSlotOffsetProvider(sop);
        boolean allEmptyA[] = new boolean[1];
        List<Action> actions = gl.createActionListFromVision(newTrainingData, allEmptyA, false, 0);

        printActionsList(actions);

        // Create some data to test against.
        // Trays will be at new positions and all gears in the parts trays
        // rather than some in the kit tray
        Random random = new Random(System.nanoTime());
        tray1.x = random.nextDouble() * 100.0;
        tray1.y = random.nextDouble() * 100.0;
        tray1.setRotation(random.nextDouble() * 2 * Math.PI);
        tray2.x = random.nextDouble() * 100.0 + 150.0;
        tray2.y = random.nextDouble() * 100.0 + 150.0;
        tray2.setRotation(random.nextDouble() * 2 * Math.PI);
        tray3.x = random.nextDouble() * 100.0 + 300.0;
        tray3.y = random.nextDouble() * 100.0 + 300.0;
        tray3.setRotation(random.nextDouble() * 2 * Math.PI);

        List<PhysicalItem> testData = new ArrayList<>();

        testData.add(tray1);
        addList(tray2, s21, "small_gear", sop, testData);
        addList(tray2, s22, "small_gear", sop, testData);
        addList(tray3, s31, "large_gear", sop, testData);
        addList(tray3, s32, "large_gear", sop, testData);

        javax.swing.SwingUtilities.invokeLater(() -> {
            createSimpleSimViewer(sop, testData)
                    .thenCompose((AprsSystem aprsSystem) -> {
                        return aprsSystem.startActionsList("goalLearnerTest", actions, false);
                    });
        });
    }

    // There is no need to ever create an instance of this class.
    // just use the static main method.
    private GoalLearnerTest() {

    }

    private static class HashMapSlotOffsetProvider implements SlotOffsetProvider {

        final Map<String, List<Slot>> map;

        HashMapSlotOffsetProvider(Map<String, List<Slot>> map) {
            this.map = map;
        }

        @Override
        public List<Slot> getSlotOffsets(String name, boolean ignoreEmpty) {
            List<Slot> slots = map.get(name);
            if (null == slots) {
                return Collections.emptyList();
            }
            return slots;
        }


        @Override
        public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem, double rotationOffset) {

            String name = offsetItem.getFullName();
            if (null == name || name.length() < 1) {
                throw new IllegalStateException("offset item has bad fullName " + offsetItem);
            }
            double x = offsetItem.x;
            double y = offsetItem.y;
            double angle = tray.getRotation() + rotationOffset;
            Slot item = new Slot(name, angle,
                    tray.x + x * Math.cos(angle) - y * Math.sin(angle),
                    tray.y + x * Math.sin(angle) + y * Math.cos(angle)
            );
            item.setDiameter(offsetItem.getDiameter());
            item.setType("S");
            item.setTray(tray);
            String offsetItemSlotForSkuName = offsetItem.getSlotForSkuName();
            if (null != offsetItemSlotForSkuName) {
                item.setSlotForSkuName(offsetItemSlotForSkuName);
            }
            item.setVisioncycle(tray.getVisioncycle());
            String offsetItemPrpName = offsetItem.getPrpName();
            if (null != offsetItemPrpName) {
                item.setPrpName(offsetItemPrpName);
            }
            item.setZ(tray.z);
            item.setVxi(tray.getVxi());
            item.setVxj(tray.getVxj());
            item.setVxk(tray.getVxk());
            item.setVzi(tray.getVzi());
            item.setVzj(tray.getVzj());
            item.setVzk(tray.getVzk());
            item.setRotation(tray.getRotation());
            return item;
        }
    }

    private static void addMap(Slot s, Map<String, List<Slot>> map) {
        PhysicalItem tray = s.getTray();
        if (null != tray) {
            String trayName = tray.getName();
            map.compute(trayName, (k, v) -> {
                if (v == null) {
                    return new ArrayList<>(Collections.singletonList(s));
                } else {
                    v.add(s);
                    return v;
                }
            });
        }
    }

    private static void addList(Tray tray, Slot slot, String itemName, SlotOffsetProvider sop, List<PhysicalItem> l) {
        if (!l.contains(tray)) {
            l.add(tray);
        }
        Slot absSlot = sop.absSlotFromTrayAndOffset(tray, slot);
        if (null != absSlot) {
            Part item = new Part(itemName);
            item.setPose(absSlot.getPose());
            item.setName(itemName);
            l.add(item);
        }
    }

    private static void printActionsList(List<Action> actions) {
        for (Action act : actions) {
            System.out.println(act.asPddlLine());
        }
    }

    private static XFuture<AprsSystem> createSimpleSimViewer(SlotOffsetProvider sop, List<PhysicalItem> testData) {
        return AprsSystem.createEmptySystem()
                .thenCompose((AprsSystem aprsSystem) -> {
                    return completeCreateSimpleViewer(aprsSystem, sop, testData)
                            .thenApply(x -> aprsSystem);
                });
    }

    private static XFutureVoid completeCreateSimpleViewer(AprsSystem aprsSystem, SlotOffsetProvider sop, List<PhysicalItem> testData) {
        aprsSystem.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        aprsSystem.setExternalSlotOffsetProvider(sop);

        PoseProvider poseProvider = new PoseProvider() {

            private @Nullable
            List<PhysicalItem> rawList;
            private @Nullable
            Map<String, PoseType> poseMap;
            private @Nullable
            Map<String, List<PhysicalItem>> instanceMap;
            private @Nullable
            Map<String, List<String>> instanceNameMap;

            @Override
            @SuppressWarnings("nullness") //getSku is checked for null 
            public List<PhysicalItem> getNewPhysicalItems() {
                rawList
                        = aprsSystem.getSimItemsData();
                if (null == rawList) {
                    rawList = testData;
                }
                List<PhysicalItem> fullDbList = DatabasePoseUpdater.processItemList(testData, sop);
                if (null == fullDbList) {
                    return Collections.emptyList();
                }
                poseMap
                        = fullDbList.stream()
                                .collect(Collectors.toMap(PhysicalItem::getFullName, PhysicalItem::getPose));

                instanceMap = fullDbList.stream()
                        .filter(item -> item.getSku() != null)
                        .collect(Collectors.groupingBy(PhysicalItem::getSku));
                Map<String, List<String>> localInstanceNameMap = new HashMap<>();
                for (Entry<String, List<PhysicalItem>> entry : instanceMap.entrySet()) {
                    List<String> names = entry.getValue().stream().map(PhysicalItem::getFullName).collect(Collectors.toList());
                    localInstanceNameMap.put(entry.getKey(), names);
                }
                this.instanceNameMap = localInstanceNameMap;
                return fullDbList;
            }

            @Override
             public  @Nullable  PoseType getPose(String name) {
                if (null == poseMap) {
                    getNewPhysicalItems();
                }
                if (null == poseMap) {
                    return null;
                }
                return poseMap.get(name);
            }

            @Override
            public List<String> getInstanceNames(String skuName) {
                if (null == instanceNameMap) {
                    getNewPhysicalItems();
                }
                if (null == instanceNameMap) {
                    return Collections.emptyList();
                }
                List<String> names = instanceNameMap.get(skuName);
                if (names == null) {
                    return Collections.emptyList();
                }
                return names;
            }
        };

        XFutureVoid xfv1 = aprsSystem.startActionListExecutor()
                .thenRun(() -> {
                    aprsSystem.setExecExternalPoseProvider(poseProvider);
                });
        XFutureVoid xfv2 = aprsSystem.startObject2DJinternalFrame();
        XFutureVoid xfv3 = aprsSystem.startSimServerJInternalFrame();
        XFutureVoid xfv4 = aprsSystem.startCrclClientJInternalFrame();
        return XFutureVoid.allOf(xfv1, xfv2, xfv3, xfv4)
                .thenComposeToVoid(() -> {
                    aprsSystem.setSnapShotHeight(600);
                    aprsSystem.setSnapShotWidth(800);
                    aprsSystem.setSnapshotsSelected(true);
                    aprsSystem.setRobotName("SimulatedRobot");
                    aprsSystem.setTaskName("GoalLearnerTest");
                    aprsSystem.setSimItemsData(testData);
                    aprsSystem.setViewLimits(-100, -100, +500, +500);

                    aprsSystem.simViewSimulateAndDisconnect();
                    aprsSystem.setSimViewTrackCurrentPos(true);
                    aprsSystem.setActiveWin(ActiveWinEnum.SIMVIEW_WINDOW);
                    aprsSystem.setVisible(true);
                    return aprsSystem.setLookForXYZ(-80, -80, 0);
                })
                .thenComposeToVoid(() -> aprsSystem.connectRobot());
    }

}
