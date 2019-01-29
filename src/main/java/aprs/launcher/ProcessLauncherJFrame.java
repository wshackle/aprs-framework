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
import aprs.misc.AprsCommonLogger;
import aprs.misc.IconImages;
import crcl.ui.XFuture;
import crcl.ui.XFutureVoid;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"unused", "guieffect"})
public class ProcessLauncherJFrame extends javax.swing.JFrame {

    /**
     * Creates new form ProcessLauncherJFrame
     */
    @SuppressWarnings("initialization")
    @UIEffect
    public ProcessLauncherJFrame() {
        AprsCommonLogger.instance();
        initComponents();
        try {
            setIconImage(IconImages.BASE_IMAGE);
        } catch (Exception ex) {
            Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
        launchFileRunner = new LaunchFileRunner(this);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    @UIEffect
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jTabbedPaneProcesses = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaLauncherFile = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        jCheckBoxMenuItemDebug = new javax.swing.JCheckBoxMenuItem();

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

        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setRightComponent(jTabbedPaneProcesses);

        jTextAreaLauncherFile.setColumns(20);
        jTextAreaLauncherFile.setRows(5);
        jScrollPane1.setViewportView(jTextAreaLauncherFile);

        jSplitPane1.setLeftComponent(jScrollPane1);

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
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 750, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 565, Short.MAX_VALUE)
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

    @UIEffect
    private void jCheckBoxMenuItemDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDebugActionPerformed
        Neo4JKiller.setDebug(jCheckBoxMenuItemDebug.isSelected());
        int timeoutMillis = launchFileRunner.getTimeoutMillis();
        long timeoutStart = launchFileRunner.getTimeoutStart();
        System.out.println("timeoutMillis = " + timeoutMillis);
        if (timeoutMillis > 0) {
            System.out.println("timeoutStart = " + timeoutStart);
            long t = System.currentTimeMillis();
            long timeleft = timeoutMillis - (t - timeoutStart);
            System.out.println("timeleft = " + timeleft);
        }
        List<WrappedProcess> processes = getProcesses();
        System.out.println("processes.size() = " + processes.size());
        for (WrappedProcess proc : processes) {
            System.out.println("");
            proc.printInfo(System.out);
            System.out.println("");
        }
        List<LineConsumer> lineConsumers = getLineConsumers();
        System.out.println("lineConsumers.size() = " + lineConsumers.size());
        for (LineConsumer lc : lineConsumers) {
            System.out.println("lc.isFinished() = " + lc.isFinished());
        }
        System.out.println("lastRunFuture = " + lastRunFuture);
        System.out.println("lastRunFile = " + lastRunFile);
        if(null != lastRunFuture) {
            lastRunFuture.printStatus();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemDebugActionPerformed

    public List<WrappedProcess> getProcesses() {
        return launchFileRunner.getProcesses();
    }

    WrappedProcess addProcess(String... command) {
        return launchFileRunner.addProcess(command);
    }

    WrappedProcess addProcess(List<String> command) {
        return launchFileRunner.addProcess(command);
    }
    
    WrappedProcess addProcess(File directory, String... command) {
        return launchFileRunner.addProcess(directory,command);
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
        } catch (ClassNotFoundException | javax.swing.UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException ex) {
            java.util.logging.Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(java.util.logging.Level.SEVERE, "", ex);
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
                    Logger.getLogger(ProcessLauncherJFrame.class.getName()).log(Level.SEVERE, "", ex);
                }
            }
        });
    }

//    static private class LogDisplayPanelOutputStream extends OutputStream {
//
//        final private LogDisplayJPanel logDisplayJPanel;
//
//        LogDisplayPanelOutputStream(LogDisplayJPanel logDisplayJInternalFrame, List<LineConsumer> lineConsumers) {
//            this.logDisplayJPanel = logDisplayJInternalFrame;
//            if (null == logDisplayJInternalFrame) {
//                throw new IllegalArgumentException("logDisplayJInteralFrame may not be null");
//            }
//            this.lineConsumers = new ArrayList<>(lineConsumers);
//        }
//
//        private StringBuffer sb = new StringBuffer();
//
//        private final List<LineConsumer> lineConsumers;
//
//        private void notifiyLineConsumers(String line) {
////            System.out.println("line = " + line);
////            System.out.println("lineConsumers = " + lineConsumers);
//            for (int i = 0; i < lineConsumers.size(); i++) {
//                LineConsumer consumer = lineConsumers.get(i);
//                if (consumer.isFinished()) {
//                    lineConsumers.remove(consumer);
//                }
//            }
//            for (LineConsumer consumer : lineConsumers) {
//                consumer.accept(line);
//            }
//            for (int i = 0; i < lineConsumers.size(); i++) {
//                LineConsumer consumer = lineConsumers.get(i);
//                if (consumer.isFinished()) {
//                    lineConsumers.remove(consumer);
//                }
//            }
//        }
//
//        @Override
//        public void write(byte[] buf, int off, int len) {
//            if (null != logDisplayJPanel) {
//                final String s = new String(buf, off, len);
//                sb.append(s);
//                if (s.contains("\n")) {
//                    String fullString = sb.toString();
//                    notifiyLineConsumers(fullString);
//                    if (javax.swing.SwingUtilities.isEventDispatchThread()) {
//                        logDisplayJPanel.appendText(fullString);
//                    } else {
//                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
//                            @Override
//                            public void run() {
//                                logDisplayJPanel.appendText(fullString);
//                            }
//                        });
//                    }
//                    sb = new StringBuffer();
//                }
//            }
//        }
//
//        @Override
//        public void write(int b) throws IOException {
//            if (b < 0 || b > 255) {
//                throw new IOException("bad byte = " + b);
//            }
//            byte buf[] = new byte[1];
//            buf[0] = (byte) b;
//            this.write(buf, 0, 1);
//        }
//    }
    public List<LineConsumer> getLineConsumers() {
        return launchFileRunner.getLineConsumers();
    }

    public JTabbedPane getjTabbedPaneProcesses() {
        return jTabbedPaneProcesses;
    }

    public List<LineConsumer> getErrorLineConsumers() {
        return launchFileRunner.getErrorLineConsumers();
    }

    public List<String> getStopLines() {
        return launchFileRunner.getStopLines();
    }

    @Nullable
    public File getProcessLaunchDirectory() {
        return launchFileRunner.getProcessLaunchDirectory();
    }

    public Deque<Boolean> getIfStack() {
        return launchFileRunner.getIfStack();
    }

    public JTextArea getjTextAreaLauncherFile() {
        return jTextAreaLauncherFile;
    }

    /**
     * Get the value of debug
     *
     * @return the value of debug
     */
    public boolean isDebug() {
        return launchFileRunner.isDebug();
    }

    /**
     * Set the value of debug
     *
     * @param debug new value of debug
     */
    public void setDebug(boolean debug) {
        launchFileRunner.setDebug(debug);
        jCheckBoxMenuItemDebug.setSelected(debug);
    }


    /**
     * Get the value of timeoutMillis
     *
     * @return the value of timeoutMillis
     */
    public int getTimoutMillis() {
        return launchFileRunner.getTimeoutMillis();
    }

    /**
     * Set the value of timeoutMillis
     *
     * @param timeoutMillis new value of timeoutMillis
     */
    public void setTimoutMillis(int timeoutMillis) {
        launchFileRunner.setTimeoutMillis(timeoutMillis);
    }

    private final LaunchFileRunner launchFileRunner;

    private volatile File lastRunFile = null;
    private volatile XFutureVoid lastRunFuture = null;
    
    
    
    @SuppressWarnings({"unchecked", "raw_types"})
    public XFutureVoid run(File f) throws IOException {
        setTitle("Process Launcher Running "+f);
        XFutureVoid ret =  launchFileRunner.run(f, getTimoutMillis(), jCheckBoxMenuItemDebug.isSelected());
        lastRunFuture = ret;
        lastRunFile = f;
        return ret.thenRun(() -> setTitle("Process Launcher completed "+f));
    }

   

    public void setStopLineSeen(boolean stopLineSeen) {
        launchFileRunner.setStopLineSeen(stopLineSeen);
    }

    
    public void setProcessLaunchDirectory(@Nullable File processLaunchDirectory) {
        launchFileRunner.setProcessLaunchDirectory(processLaunchDirectory);
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
                        .getName()).log(Level.SEVERE, "", ex);
            }
        }
        Thread closingThread = new Thread(this::completeClose, "closeProcessLauncherThread");
        closingThread.start();
        return closingFuture;
    }

    void completeClose() {
        launchFileRunner.completeClose();
         Utils.runOnDispatchThread("closeProcessLauncher", this::finalFinishClose);
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
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPaneProcesses;
    private javax.swing.JTextArea jTextAreaLauncherFile;
    // End of variables declaration//GEN-END:variables
}
