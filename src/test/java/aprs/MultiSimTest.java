package aprs;

import aprs.launcher.LauncherAprsJFrame;
import aprs.misc.Utils;
import aprs.supervisor.main.Supervisor;
import crcl.utils.CRCLUtils;
import crcl.utils.XFuture;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.*;

public class MultiSimTest {

    @BeforeClass
    public void setUp() {
        CRCLUtils.setAllowExit(false);
        CRCLUtils.setForceHeadless(true);
    }
    
    private static final String LAUNCHER_PATH = System.getProperty("test.aprs.launcherPath", "example_settings/multiple_simulated_systems_settings/launch.txt");
    private static final int SLOW_TEST_CYCLES = Integer.parseInt(System.getProperty("test.aprs.slowTestCycles", "5"));
    
    @Test(groups = {"fast"})
    public void aFastTest() {
        System.out.println("Fast test");
    }
    
    @Test(groups = {"slow"})
    public void aSlowTest() {
        try {
            System.out.println("Slow test");
            XFuture<Supervisor.MultiCycleResults> results
                    = LauncherAprsJFrame.multiCycleTest(Utils.file(LAUNCHER_PATH), SLOW_TEST_CYCLES, false);
            System.out.println("results.get().stepsDone = " + results.get().stepsDone);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
