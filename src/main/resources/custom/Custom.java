package custom;

import aprs.supervisor.main.*;
import aprs.system.*;
import java.io.*;
import java.util.function.Consumer;

public class Custom implements Consumer<AprsSupervisorDisplayJFrame> {

    @Override
    public void accept(AprsSupervisorDisplayJFrame supDisplay) {
        final Supervisor supervisor = supDisplay.getSupervisor();
        // PUT YOUR CODE HERE:\n"

        // For example
        try {
            final AprsSystem fanucSys = supervisor.getSysByTaskOrThrow("Fanuc Cart");
            final AprsSystem motomanSys = supervisor.getSysByTaskOrThrow("Shared Table");
            System.out.println("fanucSys = " + fanucSys);
            System.out.println("motomanSys = " + motomanSys);
            fanucSys.loadActionsFile(File.file("example_pddl/fanuc_move_one_large_gear.txt"));
            motomanSys.loadActionsFile(File.file("example_pddl/motoman_move_one_large_gear.txt"));
            fanucSys.connectRobot()
                    .thenCompose(x -> fanucSys.startActions("firstStep", false))
                    .thenCompose(x -> motomanSys.connectRobot())
                    .thenCompose(x -> motomanSys.startActions("secondStep", false));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
