package aprs;

import aprs.launcher.LauncherAprsJFrame;
import aprs.misc.Utils;
import aprs.supervisor.main.Supervisor;
import crcl.ui.misc.MultiLineStringJPanel;
import crcl.utils.CRCLUtils;
import crcl.utils.XFuture;
import org.testng.Assert;

import org.testng.annotations.*;

public class MultiSimTestIT {

    @BeforeClass
    public void setUp() {

    }
    private static final String EXAMPLE_SETTINGS_DIR = System.getProperty("test.aprs.exampleSettingsDir", "example_settings/multiple_simulated_systems_settings/");

    private static final String LAUNCHER_PATH = System.getProperty("test.aprs.launcherPath", EXAMPLE_SETTINGS_DIR + "launch.txt");
    private static final String SYSFILE_PATH = System.getProperty("test.aprs.sysFilePath", EXAMPLE_SETTINGS_DIR + "multisim9.csv");
    private static final String POSMAPPINGS_PATH = System.getProperty("test.aprs.posMapPath", EXAMPLE_SETTINGS_DIR + "posmaps.csv");
    private static final String TEACHFILE_PATH = System.getProperty("test.aprs.teachPropsPath", EXAMPLE_SETTINGS_DIR + "teachProps.txt");
    private static final int SLOW_TEST_CYCLES = Integer.parseInt(System.getProperty("test.aprs.slowTestCycles", "5"));

    @Test(groups = {"fast"})
    public void aFastTest() {
        System.out.println("Fast test");
    }

    @Test(groups = {"slow"})
    public void aSlowTest() {
        try {
            CRCLUtils.setAllowExit(false);
            CRCLUtils.setForceHeadless(true);
            MultiLineStringJPanel.disableShowText = true;
            MultiLineStringJPanel.setIgnoreForceShow(true);
            System.out.println("Slow test");
            XFuture<Supervisor.MultiCycleResults> results
                    = LauncherAprsJFrame.multiCycleTest(
                            Utils.file(LAUNCHER_PATH),
                            Utils.file(SYSFILE_PATH),
                            Utils.file(POSMAPPINGS_PATH),
                            Utils.file(TEACHFILE_PATH), SLOW_TEST_CYCLES, false);
            final Supervisor.MultiCycleResults completedResults = results.get();
            System.out.println("results.get().stepsDone = " + completedResults.stepsDone);
            Assert.assertEquals(completedResults.numCycles, completedResults.cyclesComplete);
            Assert.assertEquals(SLOW_TEST_CYCLES, completedResults.cyclesComplete);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
