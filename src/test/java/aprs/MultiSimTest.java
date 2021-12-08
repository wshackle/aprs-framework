package aprs;

import aprs.launcher.LauncherAprsJFrame;
import aprs.misc.Utils;
import java.io.IOException;
import org.testng.annotations.*;

public class MultiSimTest {
    @BeforeClass
    public void setUp() {
      // code that will be invoked when this test is instantiated
    }
    
    private static final String LAUNCHER_PATH = System.getProperty("test.aprs.launcherPath", "example_settings/multiple_simulated_systems_settings/launch.txt");
    private static final int SLOW_TEST_CYCLES = Integer.parseInt(System.getProperty("test.aprs.slowTestCycles", "5"));
    
    @Test(groups = { "fast" })
    public void aFastTest() {
      System.out.println("Fast test");
    }
    
    @Test(groups = { "slow" })
    public void aSlowTest() throws IOException {
       System.out.println("Slow test");
        LauncherAprsJFrame.multiCycleTest(Utils.file(LAUNCHER_PATH), SLOW_TEST_CYCLES, false);
    }
}
