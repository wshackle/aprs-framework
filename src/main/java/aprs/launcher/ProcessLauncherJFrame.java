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
package aprs.launcher;

import static aprs.launcher.Neo4JKiller.JPS_COMMAND_FILENAME_STRING;
import aprs.misc.Utils;
import aprs.logdisplay.LogDisplayJPanel;
import crcl.ui.XFuture;
import crcl.ui.XFutureVoid;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author shackle
 */
@SuppressWarnings("unused")
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
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPaneProcesses = new javax.swing.JTabbedPane();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        jCheckBoxMenuItemDebug = new javax.swing.JCheckBoxMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("APRS External Processes");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        jMenu3.setText("Options");

        jCheckBoxMenuItemDebug.setText("Debug");
        jCheckBoxMenuItemDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemDebugActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemDebug);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

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
                .addComponent(jTabbedPaneProcesses, javax.swing.GroupLayout.DEFAULT_SIZE, 565, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        close();
    }//GEN-LAST:event_formWindowClosed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        close();
    }//GEN-LAST:event_formWindowClosing

    private void jCheckBoxMenuItemDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDebugActionPerformed
        Neo4JKiller.setDebug(jCheckBoxMenuItemDebug.isSelected());
    }//GEN-LAST:event_jCheckBoxMenuItemDebugActionPerformed

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
        } catch (ClassNotFoundException | javax.swing.UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException ex) {
            java.util.logging.Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ProcessLauncherJFrame frm = new ProcessLauncherJFrame();
                    frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
                } catch (Exception ex) {
                    Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    private static abstract class LineConsumer implements Consumer<String> {

        public abstract boolean isFinished();
    }

    static private class LogDisplayPanelOutputStream extends OutputStream {

        final private LogDisplayJPanel logDisplayJPanel;

        LogDisplayPanelOutputStream(LogDisplayJPanel logDisplayJInternalFrame, List<LineConsumer> lineConsumers) {
            this.logDisplayJPanel = logDisplayJInternalFrame;
            if (null == logDisplayJInternalFrame) {
                throw new IllegalArgumentException("logDisplayJInteralFrame may not be null");
            }
            this.lineConsumers = new ArrayList<>(lineConsumers);
        }

        private StringBuffer sb = new StringBuffer();

        private final List<LineConsumer> lineConsumers;

        private void notifiyLineConsumers(String line) {
//            System.out.println("line = " + line);
//            System.out.println("lineConsumers = " + lineConsumers);
            for (int i = 0; i < lineConsumers.size(); i++) {
                LineConsumer consumer = lineConsumers.get(i);
                if (consumer.isFinished()) {
                    lineConsumers.remove(consumer);
                }
            }
            for (LineConsumer consumer : lineConsumers) {
                consumer.accept(line);
            }
            for (int i = 0; i < lineConsumers.size(); i++) {
                LineConsumer consumer = lineConsumers.get(i);
                if (consumer.isFinished()) {
                    lineConsumers.remove(consumer);
                }
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

    private volatile List<LineConsumer> lineConsumers = new ArrayList<>();
    private volatile List<LineConsumer> errorLineConsumers = new ArrayList<>();

    @SuppressWarnings("UnusedReturnValue")
    private WrappedProcess addProcess(String... command) {
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

    private WrappedProcess addProcess(List<String> command) {
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

    public WrappedProcess addProcess(File directory, String... command) {
        LogDisplayJPanel logPanel = new LogDisplayJPanel();
        String [] command2 = replaceDotDir(directory, command);
        String cmdLine = String.join(" ", command2);
        this.jTabbedPaneProcesses.add(cmdLine, logPanel);
        OutputStream errPrintStream = new LogDisplayPanelOutputStream(logPanel, lineConsumers);
        lineConsumers = new ArrayList<>();
        WrappedProcess wrappedProcess = new WrappedProcess(directory, errPrintStream, errPrintStream, command2);
        wrappedProcess.setDisplayComponent(logPanel);
        processes.add(wrappedProcess);
        return wrappedProcess;
    }

    private static String replaceDotDir(File dir, String in) {
        if(!in.startsWith(".")) {
            return in;
        }
        if(in.startsWith("./") || in.startsWith(".\\")) {
            return dir.toString()+in.substring(1);
        }
        String tmpIn = in;
        File parentFile = dir;
        while((tmpIn.startsWith("../")  || tmpIn.startsWith("..\\")) && parentFile!=null) {
            tmpIn = tmpIn.substring(3);
            parentFile = parentFile.getParentFile();
        }
        if(null != parentFile && tmpIn.length() >0 && tmpIn.length() < in.length()) {
            return parentFile.toString()+File.separator+tmpIn;
        }
        return in;
    }
    
    private static String []replaceDotDir(File dir, String in[]) {
        for (int i = 0; i < in.length; i++) {
            in[i]  = replaceDotDir(dir, in[i]);
        }
        return in;
    }
    
    private static List<String> replaceDotDir(File dir, List<String> in) {
        for (int i = 0; i < in.size(); i++) {
            in.set(i, replaceDotDir(dir, in.get(i)));
        }
        return in;
    }
    
    private WrappedProcess addProcess(File directory, List<String> command) {
        LogDisplayJPanel logPanel = new LogDisplayJPanel();
        List<String> command2 = replaceDotDir(directory, command);
        String cmdLine = String.join(" ", command2);
        this.jTabbedPaneProcesses.add(cmdLine, logPanel);
        OutputStream errPrintStream = new LogDisplayPanelOutputStream(logPanel, lineConsumers);
        lineConsumers = new ArrayList<>();
        WrappedProcess wrappedProcess = new WrappedProcess(directory, errPrintStream, errPrintStream, command2);
        wrappedProcess.setDisplayComponent(logPanel);
        processes.add(wrappedProcess);
        return wrappedProcess;
    }

    public static String[] parseCommandLineToArray(String line) {
        List<String> args = parseCommandLine(line);
        return args.toArray(new String[0]);
    }

    private static List<String> parseCommandLine(String line) {
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

    @Nullable private volatile File processLaunchDirectory = null;

    @Nullable private volatile String onFailLine = null;
    @Nullable private volatile XFutureVoid waitForFuture = null;

    @Nullable private WrappedProcess parseLaunchFileLine(String line, List<XFutureVoid> futures) throws IOException {
        if (line.length() < 1) {
            return null;
        }
        if (stopLineSeen) {
            stopLines.add(line);
            return null;
        }
        String currentOnFailLine = onFailLine;
        XFutureVoid currentWaitForFuture = waitForFuture;
        List<LineConsumer> currentErrorLineConsumers = errorLineConsumers;

        line = line.trim();
        if (line.length() < 1) {
            return null;
        }
        line = replaceVarsInLine(line, "%", "%");
        line = replaceVarsInLine(line, "${", "}");
        line = replaceVarsInLine(line, "$", " ");
        line = replaceVarsInLine(line, "$", "\n");
        line = replaceVarsInLine(line, "$", "\r");
        line = replaceVarsInLine(line, "$", null);

        if (!line.startsWith("#")) {
            errorLineConsumers = new ArrayList<>();
            waitForFuture = null;
            onFailLine = null;
            if (null != processLaunchDirectory) {
                return addProcess(processLaunchDirectory, parseCommandLine(line));
            } else {
                return addProcess(parseCommandLine(line));
            }

        } else {
            if (line.startsWith("#!recoverWaitFor")) {
                String text = line.substring("#!recoverWaitFor".length()).trim();
                final List<LineConsumer> containingList = errorLineConsumers;
                LineConsumer consumer = new LineConsumer() {

                    private volatile boolean finished = false;

                    @Override
                    public void accept(String s) {
                        if (s.contains(text)) {
                            if (null != currentWaitForFuture) {
                                currentWaitForFuture.complete();
                            }
                            finished = true;
                        }
                    }

                    @Override
                    public boolean isFinished() {
                        return finished;
                    }
                };
                containingList.add(consumer);
            } else if (line.startsWith("#!onfail")) {
                String text = line.substring("#!onfail".length()).trim();
                onFailLine = text;
            } else if (line.startsWith("#!checkfail")) {
                String text = line.substring("#!checkfail".length()).trim();
                final List<LineConsumer> containingList = lineConsumers;
                LineConsumer consumer = new LineConsumer() {

                    private volatile boolean finished = false;

                    @Override
                    public void accept(String s) {
                        if (s.contains(text)) {
                            String line = currentOnFailLine;
                            if (null != line) {
                                String lineToParse = line;
                                Utils.runOnDispatchThread(() -> {
                                    List<LineConsumer> origLineConsumers = lineConsumers;
                                    try {
                                        lineConsumers = currentErrorLineConsumers;
                                        File dir = processLaunchDirectory;
                                        if (null != dir) {
                                            addProcess(dir, parseCommandLine(lineToParse));

                                        } else {
                                            addProcess(parseCommandLine(lineToParse));
                                        }
                                    } catch (Exception ex) {
                                        Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    lineConsumers = origLineConsumers;
                                });
                            }
                            finished = true;
                        }
                    }

                    @Override
                    public boolean isFinished() {
                        return finished;
                    }
                };
                containingList.add(consumer);
            } else if (line.startsWith("#!waitfor")) {
                String text = line.substring("#!waitfor".length()).trim();
                XFutureVoid xf = new XFutureVoid("#!waitfor " + text);
                final List<LineConsumer> containingList = lineConsumers;
                LineConsumer consumer = new LineConsumer() {

                    private volatile boolean finished = false;

                    @Override
                    public void accept(String s) {
                        if (s.contains(text)) {
                            xf.complete();
                            finished = true;
                        }
                    }

                    @Override
                    public boolean isFinished() {
                        return finished;
                    }
                };
                containingList.add(consumer);
                futures.add(xf);
                waitForFuture = xf;
            } else if (line.startsWith("#!killNeo4J")) {
                Neo4JKiller.killNeo4J();
            } else if (line.startsWith("#!cd")) {
                String text = line.substring("#!cd".length()).trim();
                processLaunchDirectory = new File(text);
            } else if (line.startsWith("#!stop")) {
                stopLineSeen = true;
                stopLines = new ArrayList<>();
            } else if (line.startsWith("#!")) {
                throw new IllegalArgumentException("line starts with #! but is not recognized : line=" + line);
            }
        }

        return null;
    }

    private String replaceVarsInLine(String line, String startString, @Nullable String endString) {
        int varStartIndex = line.indexOf(startString);
        int endStringLength = (null != endString) ? endString.length():0;
        int startStringLength = startString.length();
        while (varStartIndex >= 0) {

            int varEndIndex
                    = (endString != null)
                            ? line.indexOf(endString, varStartIndex + startStringLength)
                            : line.length();
            if (varEndIndex <= varStartIndex) {
                break;
            }
            String substring = line.substring(varStartIndex + startStringLength, varEndIndex);
            boolean isidentifier = true;
            for (int i = 0; i < substring.length(); i++) {
                char c = substring.charAt(i);
                if (i == 0 && !Character.isLetter(c)) {
                    isidentifier = false;
                    break;
                }
                if (c != '.' && c != '_' && !Character.isLetterOrDigit(c)) {
                    isidentifier = false;
                    break;
                }
            }
            if (isidentifier) {
                String env = System.getenv(substring);
                if (null == env) {
                    env = System.getProperty(substring);
                }
                if (null != env && env.length() > 0) {
                    String linestart = line.substring(0, varStartIndex);
                    String lineend = line.substring(varEndIndex + endStringLength);
                    line = linestart + env + lineend;
                    varEndIndex = linestart.length() + env.length();
                }
            }
            varStartIndex = line.indexOf(startString, varEndIndex + 1);
        }
        return line;
    }

    @SuppressWarnings({"unchecked", "raw_types"})
    public XFutureVoid run(File f) throws IOException {
        List<XFutureVoid> futures = new ArrayList<>();
        stopLineSeen = false;
        processLaunchDirectory = f.getParentFile();
        File jpsCommandFile = Neo4JKiller.getJpsCommandFile();
                if(null == jpsCommandFile) {
                    jpsCommandFile = new File(processLaunchDirectory,JPS_COMMAND_FILENAME_STRING);
                }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while (null != (line = br.readLine())) {
                parseLaunchFileLine(line, futures);
            }
        }
        return XFuture.allOf(futures.toArray(new XFuture<?>[0]));
    }

    private final ConcurrentLinkedDeque<Runnable> onCloseRunnables = new ConcurrentLinkedDeque<>();

    public void addOnCloseRunnable(Runnable r) {
        onCloseRunnables.add(r);
    }

    public void removeOnCloseRunnable(Runnable r) {
        onCloseRunnables.remove(r);
    }

    private final AtomicBoolean closing = new AtomicBoolean();

    @SuppressWarnings("CanBeFinal")
    private volatile XFutureVoid closingFuture = new XFutureVoid("processLauncherClosingFuture");

    public XFutureVoid close() {
        boolean wasClosing = closing.getAndSet(true);
        if (wasClosing) {
            return closingFuture;
        }
        for (Runnable r : onCloseRunnables) {
            try {
                r.run();

            } catch (Exception ex) {
                Logger.getLogger(ProcessLauncherJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
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
        List<XFutureVoid> futures = new ArrayList<>();
        stopLineSeen = false;
        processLaunchDirectory = null;
        for (String line : stopLines) {
            try {
                WrappedProcess p = parseLaunchFileLine(line, futures);
                if (null != p) {
                    stopProcesses.add(p);

                }
            } catch (IOException ex) {
                Logger.getLogger(ProcessLauncherJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        for (WrappedProcess p : stopProcesses) {
            try {
                if (!p.waitFor(5, TimeUnit.SECONDS)) {
                    p.close();

                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ProcessLauncherJFrame.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        for (WrappedProcess wp : this.processes) {
            wp.close();
        }
        WrappedProcess.shutdownStarterService();
        Utils.runOnDispatchThread("coseProcessLauncher", this::finalFinishClose);
    }

    private void finalFinishClose() {
        this.setVisible(false);
        closingFuture.complete(null);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDebug;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JTabbedPane jTabbedPaneProcesses;
    // End of variables declaration//GEN-END:variables
}
