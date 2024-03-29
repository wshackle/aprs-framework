package aprs;

import aprs.database.vision.VisionSocketServer;
import aprs.launcher.LauncherAprsJFrame;
import aprs.misc.Utils;
import aprs.supervisor.main.Supervisor;
import aprs.system.AprsSystem;
import crcl.ui.misc.MultiLineStringJPanel;
import crcl.utils.CRCLUtils;
import crcl.utils.XFuture;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.testng.Assert;

import org.testng.annotations.*;
/* forced change */
public class MultiSimTestIT {

    @BeforeClass
    public void setUp() {

    }
    private static final String EXAMPLE_SETTINGS_DIR = System.getProperty("test.aprs.exampleSettingsDir", "example_settings/multiple_simulated_systems_settings/");

    private static final String LAUNCHER_PATH = System.getProperty("test.aprs.launcherPath", EXAMPLE_SETTINGS_DIR + "launch.txt");
    private static final String SYSFILE_PATH = System.getProperty("test.aprs.sysFilePath", EXAMPLE_SETTINGS_DIR + "multisim9.csv");
    private static final String POSMAPPINGS_PATH = System.getProperty("test.aprs.posMapPath", EXAMPLE_SETTINGS_DIR + "posmaps.csv");
    private static final String TEACHFILE_PATH = System.getProperty("test.aprs.teachPropsPath", EXAMPLE_SETTINGS_DIR + "teachProps.txt");
    private static final int SLOW_TEST_CYCLES = Integer.parseInt(System.getProperty("test.aprs.slowTestCycles", "2"));

    private static final String FLIPFM_SIM_ITEMS_FILE = System.getProperty("test.aprs.flipFMSimItems", EXAMPLE_SETTINGS_DIR + "simulated_fanuc/fanuc_cart_objects_with_black_gear.csv");

    @Test(groups = {"fast"})
    public void aFastTest() {
        System.out.println("Fast test");
    }

    private static final Logger logger = Logger.getLogger(MultiSimTestIT.class.getName());

    @Test(groups = {"slow"})
    public void multiCycleTest() {
        try {
            long tstart = System.currentTimeMillis();
            System.out.println("");
            System.out.flush();
            System.err.println("");
            System.err.flush();
            System.out.println("");
            System.out.flush();
            System.err.println("");
            System.err.flush();
            logger.severe("starting  multiCycleTest");
            VisionSocketServer.runNetStat(logger, 4001);
            CRCLUtils.setAllowExit(false);
            CRCLUtils.setForceHeadless(true);
            MultiLineStringJPanel.disableShowText = true;
            MultiLineStringJPanel.setIgnoreForceShow(true);
            System.out.println("Slow test");
            Supervisor.addCompleteLoadSysConsumer(Supervisor.FANUC__CART_TASK_NAME, 
                    (AprsSystem aprsSystem) -> aprsSystem.overwriteCurrentToolName("pincher"));
            Supervisor.addCompleteLoadSysConsumer(Supervisor.SHARED__TABLE_TASK_NAME, 
                    (AprsSystem aprsSystem) -> aprsSystem.overwriteCurrentToolName("big_gripper"));
            XFuture<Supervisor.MultiCycleResults> results
                    = LauncherAprsJFrame.multiCycleTest(
                            Utils.file(LAUNCHER_PATH),
                            Utils.file(SYSFILE_PATH),
                            Utils.file(POSMAPPINGS_PATH),
                            Utils.file(TEACHFILE_PATH), SLOW_TEST_CYCLES, false);
            final Supervisor.MultiCycleResults completedResults = results.get(6, TimeUnit.MINUTES);
            System.out.println("results.get().stepsDone = " + completedResults.stepsDone);
            long tend = System.currentTimeMillis();
            long tdiff = tend - tstart;
            System.out.println("tdiff = " + tdiff);
            int minutes = (int) (((double) tdiff) / 60000.0);
            int seconds = (int) (((double) tdiff) / 1000.0)%60;
            System.out.printf("multiCycleTest took %d minutes and %d seconds", minutes, seconds);
            System.out.println("");
            System.out.flush();
            System.err.println("");
            System.err.flush();
            System.out.println("");
            System.out.flush();
            System.err.println("");
            System.err.flush();
            Assert.assertEquals(completedResults.numCycles, completedResults.cyclesComplete);
            Assert.assertEquals(SLOW_TEST_CYCLES, completedResults.cyclesComplete);

        } catch (Throwable ex) {
            ex.printStackTrace();
            Supervisor supervisor = LauncherAprsJFrame.getMultiCycleTestSupervisor();
            if(null != supervisor) {
                supervisor.debugAction();
            }
            throw new RuntimeException(ex);
        } finally {
            logger.severe("finished  multiCycleTest");
//            VisionSocketServer.runNetStat(logger, -1);

            System.out.println("");
            System.out.flush();
            System.err.println("");
            System.err.flush();
            System.out.println("");
            System.out.flush();
            System.err.println("");
            System.err.flush();
//            printThreads();
        }

    }

//    @Test(groups = {"slow"})
//    public void flipFMTest() {
//        try {
//            long tstart = System.currentTimeMillis();
//            System.out.println("");
//            System.out.flush();
//            System.err.println("");
//            System.err.flush();
//            System.out.println("");
//            System.out.flush();
//            System.err.println("");
//            System.err.flush();
//            logger.severe("starting  flipFMTest");
//            VisionSocketServer.runNetStat(logger, 4001);
//            CRCLUtils.setAllowExit(false);
//            CRCLUtils.setForceHeadless(true);
//            MultiLineStringJPanel.disableShowText = true;
//            MultiLineStringJPanel.setIgnoreForceShow(true);
//            System.out.println("Slow test");
//            XFuture<Boolean> results
//                    = LauncherAprsJFrame.flipFMTest(
//                            Utils.file(LAUNCHER_PATH), // launchFile 
//                            Utils.file(SYSFILE_PATH), // sysFile 
//                            Utils.file(POSMAPPINGS_PATH), // posMapsFile
//                            Utils.file(FLIPFM_SIM_ITEMS_FILE) // fanucSimItemsFile
//                    );
//            final Boolean completedResults = results.get(5, TimeUnit.MINUTES);
//            System.out.println("completedResults = " + completedResults);
//            long tend = System.currentTimeMillis();
//            long tdiff = tend - tstart;
//            System.out.println("tdiff = " + tdiff);
//            int minutes = (int) (((double) tdiff) / 60000.0);
//            int seconds = (int) (((double) tdiff) / 1000.0)%60;
//            System.out.printf("flipFMTest took %d minutes and %d seconds", minutes, seconds);
//            System.out.println("");
//            System.out.flush();
//            System.err.println("");
//            System.err.flush();
//            System.out.println("");
//            System.out.flush();
//            System.err.println("");
//            System.err.flush();
//            Assert.assertEquals(completedResults, Boolean.TRUE);
//
//        } catch (Throwable ex) {
//            ex.printStackTrace();
//            throw new RuntimeException(ex);
//        } finally {
//            logger.severe("finished  flipFMTest");
//            System.out.println("");
//            System.out.flush();
//            System.err.println("");
//            System.err.flush();
//            System.out.println("");
//            System.out.flush();
//            System.err.println("");
//            System.err.flush();
//            
////            VisionSocketServer.runNetStat(logger, -1);
////            
////            System.out.println("");
////            System.out.flush();
////            System.err.println("");
////            System.err.flush();
////            System.out.println("");
////            System.out.flush();
////            System.err.println("");
////            System.err.flush();
////            printThreads();
//        }
//    }

    private void printThreads() {
        final Map<Thread, StackTraceElement[]> allStackTracesMap = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTracesMap.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] trace = entry.getValue();
            if (thread.isAlive() && thread != Thread.currentThread()) {
                System.out.println("thread = " + thread);
                System.out.println("trace = " + XFuture.traceToString(trace));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MultiSimTestIT.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("");
                System.out.flush();
                System.err.println("");
                System.err.flush();
//                    try {
//                        thread.join();
//                    } catch (InterruptedException ex) {
//                        Logger.getLogger(MultiSimTestIT.class.getName()).log(Level.SEVERE, null, ex);
//                    }
            }
        }
    }
}
