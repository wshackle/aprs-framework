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

import aprs.framework.AprsJFrame;
import aprs.framework.AprsJFrame.ActiveWinEnum;
import aprs.framework.PddlAction;
import aprs.framework.SlotOffsetProvider;
import aprs.framework.database.KitTray;
import aprs.framework.database.Part;
import aprs.framework.database.PartsTray;
import aprs.framework.database.PhysicalItem;
import aprs.framework.database.Slot;
import aprs.framework.database.Tray;
import aprs.framework.simview.Object2DOuterJPanel;
import aprs.framework.spvision.DatabasePoseUpdater;
import crcl.base.PoseType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import javax.swing.JFrame;

import aprs.framework.pddl.executor.PddlActionToCrclGenerator.PoseProvider;
import java.util.Map.Entry;

/**
 * Test/Demo of the GoalLearner
 * 
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class GoalLearnerTest {

    /**
     * Demonstrate the GoalLearner by first presenting a set of data to learn
     * from that can be modified by the user, passing it to the GoalLearner
     * and then running the created plan on a simulated robot.
     * 
     * The passed SlotOffsetProvider replaces the need for a database, and the
     * PoseProvider replaces the need for a live camera and computer vision system.
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
        KitTray tray1 = KitTray.newKitTrayFromSkuIdRotXY("kitTray", 1, 0, 50, 50);
        Slot s11 = Slot.slotFromTrayPartNameIndexRotationXY(tray1, "small_gear", 1, 0.0, +50, +50);
        addMap(s11, map);
        Slot s12 = Slot.slotFromTrayPartNameIndexRotationXY(tray1, "small_gear", 2, 0.0, -50, +50);
        addMap(s12, map);
        Slot s13 = Slot.slotFromTrayPartNameIndexRotationXY(tray1, "large_gear", 1, 0.0, +50, -50);
        addMap(s13, map);
        Slot s14 = Slot.slotFromTrayPartNameIndexRotationXY(tray1, "large_gear", 2, 0.0, -50, -50);
        addMap(s14, map);

        // Small gear parts tray has two slots for small gears
        PartsTray tray2 = PartsTray.newPartsTrayFromSkuIdRotXY("smallPartsTray", 1, 0, 200, 200);
        Slot s21 = Slot.slotFromTrayPartNameIndexRotationXY(tray2, "small_gear", 1, 0.0, +50, +50);
        addMap(s21, map);
        Slot s22 = Slot.slotFromTrayPartNameIndexRotationXY(tray2, "small_gear", 2, 0.0, -50, +50);
        addMap(s22, map);

        // Large gear parts tray has two slots for large gears
        PartsTray tray3 = PartsTray.newPartsTrayFromSkuIdRotXY("largePartsTray", 1, 0, 400, 400);
        Slot s31 = Slot.slotFromTrayPartNameIndexRotationXY(tray3, "large_gear", 1, 0.0, +50, +50);
        addMap(s31, map);
        Slot s32 = Slot.slotFromTrayPartNameIndexRotationXY(tray3, "large_gear", 2, 0.0, -50, +50);
        addMap(s32, map);

        // The slot offset provider is a simple replacement for the part of 
        // the database that would normally be used. (Retrieving from the HashMap instead.)
        SlotOffsetProvider sop = new HashMapSlotOffsetProvider(map);

        List<PhysicalItem> trainingData = new ArrayList<>();

        addList(tray1, s11, "small_gear", sop, trainingData);
        addList(tray1, s13, "large_gear", sop, trainingData);
        addList(tray2, s22, "small_gear", sop, trainingData);
        addList(tray3, s13, "large_gear", sop, trainingData);

        List<PhysicalItem> newTrainingData
                = Object2DOuterJPanel.showAndModifyData(trainingData, sop, -100, -100, +500, +500);
        GoalLearner gl = new GoalLearner();
        gl.setSlotOffsetProvider(sop);
        boolean allEmptyA[] = new boolean[1];
        List<PddlAction> actions = gl.createActionListFromVision(newTrainingData, allEmptyA);

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
            AprsJFrame aFrame = createSimpleSimViewer(sop, testData);
            aFrame.startActionsList(actions);
        });
    }
    
    // There is no need to ever create an instance of this class.
    // just use the static main method.
    private GoalLearnerTest() {
        
    }
    
    private static class HashMapSlotOffsetProvider implements SlotOffsetProvider {

        final Map<String, List<Slot>> map;

        public HashMapSlotOffsetProvider(Map<String, List<Slot>> map) {
            this.map = map;
        }

        @Override
        public List<Slot> getSlotOffsets(String name) {
            return map.get(name);
        }

        @Override
        public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {

            String name = offsetItem.getFullName();
            double x = offsetItem.x;
            double y = offsetItem.y;
            double angle = tray.getRotation();
            Slot item = new Slot(name, angle,
                    tray.x + x * Math.cos(angle) - y * Math.sin(angle),
                    tray.y + x * Math.sin(angle) + y * Math.cos(angle)
            );
            item.setDiameter(offsetItem.getDiameter());
            item.setType("S");
            item.setTray(tray);
            item.setSlotForSkuName(offsetItem.getSlotForSkuName());
            item.setVisioncycle(tray.getVisioncycle());
            item.setPrpName(offsetItem.getPrpName());
            item.setZ(tray.z);
            item.setVxi(tray.getVxi());
            item.setVxj(tray.getVxj());
            item.setVxk(tray.getVxk());
            item.setVzi(tray.getVzi());
            item.setVzj(tray.getVzj());
            item.setVzk(tray.getVzk());
            return item;
        }
    }

    private static void addMap(Slot s, Map<String, List<Slot>> map) {
        map.compute(s.getTray().getName(), (k, v) -> {
            if (v == null) {
                return new ArrayList<>(Collections.singletonList(s));
            } else {
                v.add(s);
                return v;
            }
        });
    }

    private static void addList(Tray tray, Slot slot, String itemName, SlotOffsetProvider sop, List<PhysicalItem> l) {
        if (!l.contains(tray)) {
            l.add(tray);
        }
        Slot absSlot = sop.absSlotFromTrayAndOffset(tray, slot);
        Part item = new Part(itemName);
        item.setPose(absSlot.getPose());
        item.setName(itemName);
        l.add(item);
    }

    

    private static void printActionsList(List<PddlAction> actions) {
        for (PddlAction act : actions) {
            System.out.println(act.asPddlLine());
        }
    }

    private static AprsJFrame createSimpleSimViewer(SlotOffsetProvider sop, List<PhysicalItem> testData) {
        AprsJFrame aFrame = new AprsJFrame();
        aFrame.emptyInit();
        aFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        aFrame.setExternalSlotOffsetProvider(sop);
        aFrame.startActionsToCrclJInternalFrame();
        aFrame.startObject2DJinternalFrame();
        PoseProvider poseProvider = new PoseProvider() {

            List<PhysicalItem> rawList;
            List<PhysicalItem> fullDbList;
            Map<String, PoseType> poseMap;
            Map<String, List<PhysicalItem>> instanceMap;
            Map<String, List<String>> instanceNameMap;

            @Override
            public List<PhysicalItem> getNewPhysicalItems() {
                rawList
                        = aFrame.getSimItemsData();
                if (null == rawList) {
                    rawList = testData;
                }
                fullDbList = DatabasePoseUpdater.processItemList(testData, sop);
                System.out.println("fullDbList = " + fullDbList);
                poseMap
                        = fullDbList.stream()
                        .collect(Collectors.toMap(PhysicalItem::getFullName, PhysicalItem::getPose));
                instanceMap = fullDbList.stream()
                        .filter(item -> item.getSku() != null)
                        .collect(Collectors.groupingBy(PhysicalItem::getSku));
                instanceNameMap = new HashMap<>();
                for (Entry<String, List<PhysicalItem>> entry : instanceMap.entrySet()) {
                    List<String> names = entry.getValue().stream().map(PhysicalItem::getFullName).collect(Collectors.toList());
                    instanceNameMap.put(entry.getKey(), names);
                }

                return fullDbList;
            }

            @Override
            public PoseType getPose(String name) {
                if (null == poseMap) {
                    getNewPhysicalItems();
                }
                return poseMap.get(name);
            }

            @Override
            public List<String> getInstanceNames(String skuName) {
                if (null == instanceNameMap) {
                    getNewPhysicalItems();
                }
                return instanceNameMap.get(skuName);
            }
        };

        aFrame.setPddlExecExternalPoseProvider(poseProvider);
        aFrame.startSimServerJInternalFrame();
        aFrame.startPendantClientJInternalFrame();
        aFrame.setRobotName("SimulatedRobot");
        aFrame.setSimItemsData(testData);
        aFrame.setSimViewLimits(-100, -100, +500, +500);
        aFrame.simViewSimulateAndDisconnect();
        aFrame.setSimViewTrackCurrentPos(true);
        aFrame.setActiveWin(ActiveWinEnum.SIMVIEW_WINDOW);
        aFrame.setVisible(true);
        return aFrame;
    }

}