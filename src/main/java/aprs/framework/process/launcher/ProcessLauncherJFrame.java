/*//GEN-LINE:variables
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
package aprs.framework.process.launcher;

import aprs.framework.Utils;
import aprs.framework.logdisplay.LogDisplayJPanel;
import crcl.ui.XFuture;
import crcl.ui.XFutureVoid;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.drools.core.rule.Collect;

/**
 *
 * @author shackle
 */
public class ProcessLauncherJFrame extends javax.swing.JFrame {

    /**
     * Creates new form ProcessLauncherJFrame
     */
    @SuppressWarnings("initialization")
    public ProcessLauncherJFrame() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jTabbedPaneProcesses = new javax.swing.JTabbedPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("APRS External Processes");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPaneProcesses, javax.swing.GroupLayout.DEFAULT_SIZE, 750, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPaneProcesses, javax.swing.GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>                        

    private void formWindowClosed(java.awt.event.WindowEvent evt) {                                  
        close();
    }                                 

    private void formWindowClosing(java.awt.event.WindowEvent evt) {                                   
        close();
    }                                  

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ProcessLauncherJFrame frm = new ProcessLauncherJFrame();
                    frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frm.addProcess("C:\\Users\\Public\\Documents\\APRS_AntVision_2018_03_06\\VideoTeachTable.exe");
//                    frm.addProcess("C:\\Users\\shackle\\neo4j-community-2.3.11-motoman\\bin\\Neo4j.bat");
                    frm.addProcess("C:\\Users\\shackle\\neo4j-community-2.3.11-fanuc\\bin\\Neo4j.bat");
//                    frm.addProcess(new File("C:\\Users\\shackle\\neo4j-community-2.3.11-fanuc"),
//                            "C:\\Program Files (x86)\\Java\\jre1.8.0_144\\bin\\java.exe",
//                            "-DworkingDir=C:\\Users\\shackle\\neo4j-community-2.3.11-fanuc\\bin\\..",
//                            "-DconfigFile=conf\\neo4j-wrapper.conf",
//                            "-DserverClasspath=lib/*.jar;system/lib/*.jar;plugins/**/*.jar;./conf*",
//                            "-DserverMainClass=org.neo4j.server.CommunityBootstrapper",
//                            "-jar",
//                            "C:\\Users\\shackle\\neo4j-community-2.3.11-fanuc\\bin\\windows-service-wrapper-5.jar"
//                    );

//                            "C:\\Program Files\\Java\\jdk1.8.0_92\\jre\\bin\\java.exe","-DworkingDir=C:\\Users\\shackle\\neo4j-community-2.3.11-fanuc\\bin\\..","-DconfigFile=conf\\neo4j-wrapper.conf","-DserverClasspath=lib/*.jar;system/lib/","..");
                    frm.setVisible(true);
                } catch (IOException ex) {
                    Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    static private class LogDisplayPanelOutputStream extends OutputStream {

        final private LogDisplayJPanel logDisplayJPanel;

        public LogDisplayPanelOutputStream(LogDisplayJPanel logDisplayJInternalFrame, List<Consumer<String>> lineConsumers) {
            this.logDisplayJPanel = logDisplayJInternalFrame;
            if (null == logDisplayJInternalFrame) {
                throw new IllegalArgumentException("logDisplayJInteralFrame may not be null");
            }
            this.lineConsumers = lineConsumers;
        }

        private StringBuffer sb = new StringBuffer();

        private final List<Consumer<String>> lineConsumers;

        private void notifiyLineConsumers(String line) {
            System.out.println("line = " + line);
            System.out.println("lineConsumers = " + lineConsumers);
            for (Consumer<String> consumer : lineConsumers) {
                consumer.accept(line);
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            if (null != logDisplayJPanel) {
                final String s = new String(buf, off, len);
                sb.append(s);
                if (s.contains("\n")) {
                    String fullString = sb.toString();
                    notifiyLineConsumers(fullString);
                    if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                        logDisplayJPanel.appendText(fullString);
                    } else {
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                logDisplayJPanel.appendText(fullString);
                            }
                        });
                    }
                    sb = new StringBuffer();
                }
            }
        }

        @Override
        public void write(int b) throws IOException {
            if (b < 0 || b > 255) {
                throw new IOException("bad byte = " + b);
            }
            byte buf[] = new byte[1];
            buf[0] = (byte) b;
            this.write(buf, 0, 1);
        }
    }
    private final List<WrappedProcess> processes = new ArrayList<>();

    private List<Consumer<String>> lineConsumers = new ArrayList<>();

    public WrappedProcess addProcess(String... command) throws IOException {
        LogDisplayJPanel logPanel = new LogDisplayJPanel();
        String cmdLine = String.join(" ", command);
        this.jTabbedPaneProcesses.add(cmdLine, logPanel);
        OutputStream errPrintStream = new LogDisplayPanelOutputStream(logPanel, lineConsumers);
        lineConsumers = new ArrayList<>();
        WrappedProcess wrappedProcess = new WrappedProcess(errPrintStream, errPrintStream, command);
        wrappedProcess.setDisplayComponent(logPanel);
        processes.add(wrappedProcess);
        return wrappedProcess;
    }

    public WrappedProcess addProcess(List<String> command) throws IOException {
        LogDisplayJPanel logPanel = new LogDisplayJPanel();
        String cmdLine = String.join(" ", command);
        this.jTabbedPaneProcesses.add(cmdLine, logPanel);
        OutputStream errPrintStream = new LogDisplayPanelOutputStream(logPanel, lineConsumers);
        lineConsumers = new ArrayList<>();
        WrappedProcess wrappedProcess = new WrappedProcess(errPrintStream, errPrintStream, command);
        wrappedProcess.setDisplayComponent(logPanel);
        processes.add(wrappedProcess);
        return wrappedProcess;
    }

    public WrappedProcess addProcess(File directory, String... command) throws IOException {
        LogDisplayJPanel logPanel = new LogDisplayJPanel();
        String cmdLine = String.join(" ", command);
        this.jTabbedPaneProcesses.add(cmdLine, logPanel);
        OutputStream errPrintStream = new LogDisplayPanelOutputStream(logPanel, lineConsumers);
        lineConsumers = new ArrayList<>();
        WrappedProcess wrappedProcess = new WrappedProcess(directory, errPrintStream, errPrintStream, command);
        wrappedProcess.setDisplayComponent(logPanel);
        processes.add(wrappedProcess);
        return wrappedProcess;
    }

    public WrappedProcess addProcess(File directory, List<String> command) throws IOException {
        LogDisplayJPanel logPanel = new LogDisplayJPanel();
        String cmdLine = String.join(" ", command);
        this.jTabbedPaneProcesses.add(cmdLine, logPanel);
        OutputStream errPrintStream = new LogDisplayPanelOutputStream(logPanel, lineConsumers);
        lineConsumers = new ArrayList<>();
        WrappedProcess wrappedProcess = new WrappedProcess(directory, errPrintStream, errPrintStream, command);
        wrappedProcess.setDisplayComponent(logPanel);
        processes.add(wrappedProcess);
        return wrappedProcess;
    }

    public static String[] parseCommandLineToArray(String line) {
        List<String> args = parseCommandLine(line);
        return args.toArray(new String[args.size()]);
    }

    public static List<String> parseCommandLine(String line) {
        List<String> args = new ArrayList<>();
        int dquotes = 0;
        int squotes = 0;
        StringBuilder sb = new StringBuilder();
        char lastC = 0;
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            switch (c) {
                case ' ':
                case '\t':
                    if (dquotes % 2 == 0 && squotes % 2 == 0 && lastC != '\\') {
                        args.add(sb.toString());
                        sb = new StringBuilder();
                    } else {
                        sb.append(c);
                    }
                    break;

                case '\"':
                    if (squotes % 2 == 0 && lastC != '\\') {
                        dquotes++;
                    } else {
                        sb.append(c);
                    }
                    break;

                case '\'':
                    if (dquotes % 2 == 0 && lastC != '\\') {
                        squotes++;
                    } else {
                        sb.append(c);
                    }
                    break;

                case '\\':
                    if (lastC == '\\' || isWindows) {
                        sb.append(c);
                        c = 0;
                    }
                    break;

                default:
                    sb.append(c);
                    break;
            }
            lastC = c;
        }
        String last = sb.toString();
        if (last.length() > 0) {
            args.add(last);
        }
        return args;
    }

    private volatile boolean stopLineSeen = false;

    private volatile List<String> stopLines = new ArrayList<>();

    private void parseLaunchFileLine(String line, List<XFutureVoid> futures) throws IOException {
        if (stopLineSeen) {
            stopLines.add(line);
            return;
        }
        if (!line.trim().startsWith("#")) {
            addProcess(parseCommandLine(line));
        } else {
            line = line.trim();
            if (line.startsWith("#waitfor")) {
                String text = line.substring("#waitfor".length()).trim();
                XFutureVoid xf = new XFutureVoid("#waitfor " + text);
                Consumer<String> consumer = (String s) -> {
                    if (s.contains(text)) {
                        xf.complete();
                    }
                };
                lineConsumers.add(consumer);
                futures.add(xf);
            } else if (line.startsWith("#stop")) {
                stopLineSeen = true;
                stopLines = new ArrayList<>();
            }
        }
    }

    @SuppressWarnings({"unchecked","raw_types"})
    public XFuture<Void> run(File f) throws IOException {
        List<XFutureVoid> futures = new ArrayList<>();
        stopLineSeen = false;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while (null != (line = br.readLine())) {
                parseLaunchFileLine(line, futures);
            }
        }
        return XFuture.allOf(futures.toArray(new XFuture<?>[futures.size()]));
    }

    private final ConcurrentLinkedDeque<Runnable> onCloseRunnables = new ConcurrentLinkedDeque<>();

    public void addOnCloseRunnable(Runnable r) {
        onCloseRunnables.add(r);
    }

    public void removeOnCloseRunnable(Runnable r) {
        onCloseRunnables.remove(r);
    }

    private final AtomicBoolean closing = new AtomicBoolean();

    private volatile XFuture<Void> closingFuture = new XFuture<Void>("processLauncherClosingFuture");
    
    public XFuture<Void> close() {
        boolean wasClosing = closing.getAndSet(true);
        if (wasClosing) {
            return closingFuture;
        }
        for (Runnable r : onCloseRunnables) {
            try {
                r.run();
            } catch (Exception ex) {
                Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Thread closingThread = new Thread(this::completeClose, "closeProcessLauncherThread");
        closingThread.start();
//        System.out.println("closingThread = " + closingThread);
//        System.out.println("closingThread.getState() = " + closingThread.getState());
        return closingFuture;
    }

    private void completeClose() {
        List<WrappedProcess> stopProcesses = new ArrayList<>();
//        System.out.println("stopLines = " + stopLines);
        for (String line : stopLines) {
//            System.out.println("line = " + line);
            try {
                stopProcesses.add(addProcess(parseCommandLine(line)));
            } catch (IOException ex) {
                Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
//        System.out.println("stopProcesses = " + stopProcesses);
        for (WrappedProcess p : stopProcesses) {
            try {
                if (!p.waitFor(5, TimeUnit.SECONDS)) {
                    p.close();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
//        System.out.println("this.processes = " + this.processes);
        for (WrappedProcess wp : this.processes) {
            wp.close();
        }
        WrappedProcess.shutdownStarterService();
        try {
            Neo4JKiller.killNeo4J();
        } catch (IOException ex) {
            Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        Utils.runOnDispatchThread("coseProcessLauncher", this::finalFinishClose);
    }
    private void finalFinishClose() {
        this.setVisible(false);
        closingFuture.complete(null);
    }
    // Variables declaration - do not modify                     
    private javax.swing.JTabbedPane jTabbedPaneProcesses;
    // End of variables declaration                   
}
