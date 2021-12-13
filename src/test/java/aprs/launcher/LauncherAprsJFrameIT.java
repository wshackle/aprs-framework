/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package aprs.launcher;

import aprs.supervisor.main.Supervisor;
import crcl.utils.XFuture;
import crcl.utils.XFutureVoid;
import java.awt.Frame;
import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author shackle
 */
public class LauncherAprsJFrameIT {
    
    public LauncherAprsJFrameIT() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of prevMultiNullLaunch method, of class LauncherAprsJFrame.
     */
    @Test
    public void testPrevMultiNullLaunch() {
        System.out.println("prevMultiNullLaunch");
        XFuture<Supervisor> expResult = null;
        XFuture<Supervisor> result = LauncherAprsJFrame.prevMultiNullLaunch();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of prevMultiLastLaunch method, of class LauncherAprsJFrame.
     */
    @Test
    public void testPrevMultiLastLaunch() throws Exception {
        System.out.println("prevMultiLastLaunch");
        XFuture<Supervisor> expResult = null;
        XFuture<Supervisor> result = LauncherAprsJFrame.prevMultiLastLaunch();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLastLaunchFile method, of class LauncherAprsJFrame.
     */
    @Test
    public void testGetLastLaunchFile() throws Exception {
        System.out.println("getLastLaunchFile");
        File expResult = null;
        File result = LauncherAprsJFrame.getLastLaunchFile();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of prevMulti method, of class LauncherAprsJFrame.
     */
    @Test
    public void testPrevMulti() {
        System.out.println("prevMulti");
        File launchFile = null;
        XFuture<Supervisor> expResult = null;
        XFuture<Supervisor> result = LauncherAprsJFrame.prevMulti(launchFile);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of newMulti method, of class LauncherAprsJFrame.
     */
    @Test
    public void testNewMulti() throws Exception {
        System.out.println("newMulti");
        Supervisor expResult = null;
        Supervisor result = LauncherAprsJFrame.newMulti();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of openMultiWithLaunchFile method, of class LauncherAprsJFrame.
     */
    @Test
    public void testOpenMultiWithLaunchFile() throws Exception {
        System.out.println("openMultiWithLaunchFile");
        File launcherFile = null;
        File setupFile = null;
        Frame parent = null;
        XFuture<Supervisor> expResult = null;
        XFuture<Supervisor> result = LauncherAprsJFrame.openMultiWithLaunchFile(launcherFile, setupFile, parent);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of goalLearningTest method, of class LauncherAprsJFrame.
     */
    @Test
    public void testGoalLearningTest() {
        System.out.println("goalLearningTest");
        LauncherAprsJFrame.goalLearningTest();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of optaplannerTest method, of class LauncherAprsJFrame.
     */
    @Test
    public void testOptaplannerTest() {
        System.out.println("optaplannerTest");
        LauncherAprsJFrame.optaplannerTest();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of prevSingleWithLaunchFile method, of class LauncherAprsJFrame.
     */
    @Test
    public void testPrevSingleWithLaunchFile() throws Exception {
        System.out.println("prevSingleWithLaunchFile");
        File launchFile = null;
        XFutureVoid expResult = null;
        XFutureVoid result = LauncherAprsJFrame.prevSingleWithLaunchFile(launchFile);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of multiCycleTest method, of class LauncherAprsJFrame.
     */
    @Test
    public void testMultiCycleTest_3args() {
        System.out.println("multiCycleTest");
        File launchFile = null;
        int numCycles = 0;
        boolean useConveyor = false;
        XFuture<Supervisor.MultiCycleResults> expResult = null;
        XFuture<Supervisor.MultiCycleResults> result = LauncherAprsJFrame.multiCycleTest(launchFile, numCycles, useConveyor);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of multiCycleTest method, of class LauncherAprsJFrame.
     */
    @Test
    public void testMultiCycleTest_6args() {
        System.out.println("multiCycleTest");
        File launchFile = null;
        File sysFile = null;
        File posMapsFile = null;
        File teachPropsFile = null;
        int numCycles = 0;
        boolean useConveyor = false;
        XFuture<Supervisor.MultiCycleResults> expResult = null;
        XFuture<Supervisor.MultiCycleResults> result = LauncherAprsJFrame.multiCycleTest(launchFile, sysFile, posMapsFile, teachPropsFile, numCycles, useConveyor);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of checkForConveyorUse method, of class LauncherAprsJFrame.
     */
    @Test
    public void testCheckForConveyorUse() {
        System.out.println("checkForConveyorUse");
        LauncherAprsJFrame instance = new LauncherAprsJFrame();
        boolean expResult = false;
        boolean result = instance.checkForConveyorUse();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of main method, of class LauncherAprsJFrame.
     */
    @Test
    public void testMain() {
        System.out.println("main");
        String[] args = null;
        LauncherAprsJFrame.main(args);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
