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
package aprs.framework.pddl.planner;

import aprs.framework.pddl.executor.PddlExecutorJInternalFrame;
import aprs.framework.AprsJFrame;
import aprs.framework.DisplayInterface;
import aprs.framework.PddlAction;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PddlPlannerJPanel extends javax.swing.JPanel implements DisplayInterface {

    /**
     * Creates new form PddlPlannerJPanel
     */
    public PddlPlannerJPanel() {
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

        jLabel1 = new javax.swing.JLabel();
        jTextFieldPlannerProgramExecutable = new javax.swing.JTextField();
        jButtonPlannerProgramExecutableBrowse = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldPddlDomainFile = new javax.swing.JTextField();
        jButtonPddlDomainBrowse = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldPddlProblem = new javax.swing.JTextField();
        jButtonPddlProblemBrowse = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldAdditionalArgs = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaOutput = new javax.swing.JTextArea();
        jButtonRunOnce = new javax.swing.JButton();
        jButtonStop = new javax.swing.JButton();

        jLabel1.setText("Planner Program Executable:");

        jTextFieldPlannerProgramExecutable.setText("popf3-clp");

        jButtonPlannerProgramExecutableBrowse.setText("Browse");
        jButtonPlannerProgramExecutableBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlannerProgramExecutableBrowseActionPerformed(evt);
            }
        });

        jLabel2.setText("PDDL Domain File");

        jTextFieldPddlDomainFile.setText("domain-kitting.pddl");
        jTextFieldPddlDomainFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldPddlDomainFileActionPerformed(evt);
            }
        });

        jButtonPddlDomainBrowse.setText("Browse");
        jButtonPddlDomainBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPddlDomainBrowseActionPerformed(evt);
            }
        });

        jLabel3.setText("PDDL Problem File");

        jTextFieldPddlProblem.setText("problem-a2b1c1.pddl");

        jButtonPddlProblemBrowse.setText("Browse");
        jButtonPddlProblemBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPddlProblemBrowseActionPerformed(evt);
            }
        });

        jLabel4.setText("Additional Arguments:");

        jTextFieldAdditionalArgs.setText("-I -n");

        jLabel5.setText("Output");

        jTextAreaOutput.setColumns(20);
        jTextAreaOutput.setRows(5);
        jScrollPane1.setViewportView(jTextAreaOutput);

        jButtonRunOnce.setText("Run Once");
        jButtonRunOnce.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRunOnceActionPerformed(evt);
            }
        });

        jButtonStop.setText("Stop");
        jButtonStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStopActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1)
                    .addComponent(jTextFieldAdditionalArgs, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jTextFieldPlannerProgramExecutable, javax.swing.GroupLayout.PREFERRED_SIZE, 508, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonPlannerProgramExecutableBrowse, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jTextFieldPddlProblem, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
                            .addComponent(jTextFieldPddlDomainFile, javax.swing.GroupLayout.Alignment.LEADING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonPddlDomainBrowse, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButtonPddlProblemBrowse, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonStop)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonRunOnce)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldPlannerProgramExecutable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonPlannerProgramExecutableBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldPddlDomainFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonPddlDomainBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldPddlProblem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonPddlProblemBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldAdditionalArgs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jButtonRunOnce)
                    .addComponent(jButtonStop))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    public void browseProgramExecutable() throws IOException {
        JFileChooser chooser = new JFileChooser(new File(jTextFieldPlannerProgramExecutable.getText()).getParent());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            jTextFieldPlannerProgramExecutable.setText(f.getCanonicalPath());
            saveProperties();
        }
    }

    public void browsePddlDomain() throws IOException {
        JFileChooser chooser = new JFileChooser(new File(jTextFieldPddlDomainFile.getText()).getParent());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            jTextFieldPddlDomainFile.setText(f.getCanonicalPath());
            saveProperties();
        }
    }

    public void browsePddlProblem() throws IOException {
        JFileChooser chooser = new JFileChooser(new File(jTextFieldPddlProblem.getText()).getParent());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            jTextFieldPddlProblem.setText(f.getCanonicalPath());
            saveProperties();
        }
    }

    private File propertiesFile = null;

    /**
     * Get the value of propertiesFile
     *
     * @return the value of propertiesFile
     */
    public File getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * Set the value of propertiesFile
     *
     * @param propertiesFile new value of propertiesFile
     */
    public void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    public void loadProperties() throws IOException {
        if (null == propertiesFile) {
            throw new IllegalStateException("propertiesFile not set");
        }
        if (propertiesFile.exists()) {
            Properties props = new Properties();
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            }
            String executable = props.getProperty(PROGRAMEXECUTABLE);
            if (null != executable) {
                jTextFieldPlannerProgramExecutable.setText(executable);
            }
            String domain = props.getProperty(PDDLDOMAIN);
            if (null != domain) {
                jTextFieldPddlDomainFile.setText(domain);
            }
            String problem = props.getProperty(PDDLPROBLEM);
            if (null != problem) {
                jTextFieldPddlProblem.setText(problem);
            }
//        String output = props.getProperty(PDDLOUTPUT);
//        if (null != output) {
//            jTextFieldPddlOutputActions.setText(output);
//        }
            String addargs = props.getProperty(PDDLADDARGS);
            if (null != addargs) {
                jTextFieldAdditionalArgs.setText(addargs);
            }
        }
    }

    private static final String PROGRAMEXECUTABLE = "program.executable";
    private static final String PDDLADDARGS = "pddl.addargs";
    private static final String PDDLPROBLEM = "pddl.problem";
    private static final String PDDLDOMAIN = "pddl.domain";

    private void jButtonPlannerProgramExecutableBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlannerProgramExecutableBrowseActionPerformed
        try {
            browseProgramExecutable();
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonPlannerProgramExecutableBrowseActionPerformed

    private void jTextFieldPddlDomainFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPddlDomainFileActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldPddlDomainFileActionPerformed

    private void jButtonPddlDomainBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPddlDomainBrowseActionPerformed
        try {
            browsePddlDomain();
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonPddlDomainBrowseActionPerformed

    private void jButtonPddlProblemBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPddlProblemBrowseActionPerformed
        try {
            browsePddlProblem();
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonPddlProblemBrowseActionPerformed

    private void jButtonRunOnceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRunOnceActionPerformed
        try {
            runPddlPlannerOnce();
            if(null != actionsToCrclJInternalFrame1) {
                actionsToCrclJInternalFrame1.setLoadEnabled(false);
            }
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonRunOnceActionPerformed

    private void jButtonStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStopActionPerformed
        this.closePddlProcess();
    }//GEN-LAST:event_jButtonStopActionPerformed

    private ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Get the value of executor
     *
     * @return the value of executor
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Set the value of executor
     *
     * @param executor new value of executor
     */
    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    private PddlExecutorJInternalFrame actionsToCrclJInternalFrame1 = null;

    public PddlExecutorJInternalFrame getActionsToCrclJInternalFrame1() {
        return actionsToCrclJInternalFrame1;
    }

    public void setActionsToCrclJInternalFrame1(PddlExecutorJInternalFrame actionsToCrclJInternalFrame1) {
        this.actionsToCrclJInternalFrame1 = actionsToCrclJInternalFrame1;
    }

    public List<PddlAction> getActionsList() {
        if (null != actionsToCrclJInternalFrame1) {
            return actionsToCrclJInternalFrame1.getActionsList();
        }
        throw new IllegalStateException("actionsToCrclJInternalFrame not set");
    }

    public void setActionsList(List<PddlAction> actionsList) {
        if (null != actionsToCrclJInternalFrame1) {
            actionsToCrclJInternalFrame1.setActionsList(actionsList);
        } 
    }

    private void addAction(PddlAction action) {
        if (null != actionsToCrclJInternalFrame1) {
            this.actionsToCrclJInternalFrame1.addAction(action);
        } 
    }

    private Process pddlProcess = null;
    private InputStream pddlInputStream = null;
    private InputStream pddlErrorStream = null;
    private Future<?> ppdlInputStreamFuture = null;
    private Future<?> ppdlErrorStreamFuture = null;

    public void runPddlPlannerOnce() throws IOException {
        try {
            closePddlProcess();
        } catch (Exception ex) {
            Logger.getLogger(PddlPlannerJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        List<String> commandList = new ArrayList<>();
        this.setActionsList(new ArrayList<>());
        commandList.add(new File(jTextFieldPlannerProgramExecutable.getText()).getCanonicalPath());
        commandList.addAll(Arrays.asList(jTextFieldAdditionalArgs.getText().split("[ \t]+")));
        commandList.add(jTextFieldPddlDomainFile.getText());
        commandList.add(jTextFieldPddlProblem.getText());
        ProcessBuilder pb = new ProcessBuilder(commandList);
        pddlProcess = pb.start();
        pddlInputStream = pddlProcess.getInputStream();
        pddlErrorStream = pddlProcess.getErrorStream();
        jTextAreaOutput.append(commandList.toString() + System.lineSeparator());
        if(null == executor) {
            executor = Executors.newCachedThreadPool();
        }
        ppdlErrorStreamFuture = executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(pddlErrorStream));
                    String line = null;
                    while (null != (line = br.readLine()) && !closing && !Thread.currentThread().isInterrupted()) {
                        final String lineToAppend = line;
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                jTextAreaOutput.append(lineToAppend + System.lineSeparator());
                            }
                        });
                    }
                } catch (IOException ex) {
                    Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        ppdlInputStreamFuture = executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(pddlInputStream));
                    String line = null;
                    boolean planFoundFound = false;
                    while (null != (line = br.readLine()) && !closing && !Thread.currentThread().isInterrupted()) {
                        final String lineToAppend = line;
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                jTextAreaOutput.append(lineToAppend + System.lineSeparator());

                            }
                        });

                        if (planFoundFound) {
                            addAction(PddlAction.parse(lineToAppend));
                        }
                        if (line.contains("; Plan found") || line.contains("Solution Found")) {
                            planFoundFound = true;
                        }
                    }
                    actionsToCrclJInternalFrame1.autoResizeTableColWidthsPddlOutput();
                } catch (IOException ex) {
                    Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonPddlDomainBrowse;
    private javax.swing.JButton jButtonPddlProblemBrowse;
    private javax.swing.JButton jButtonPlannerProgramExecutableBrowse;
    private javax.swing.JButton jButtonRunOnce;
    private javax.swing.JButton jButtonStop;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextAreaOutput;
    private javax.swing.JTextField jTextFieldAdditionalArgs;
    private javax.swing.JTextField jTextFieldPddlDomainFile;
    private javax.swing.JTextField jTextFieldPddlProblem;
    private javax.swing.JTextField jTextFieldPlannerProgramExecutable;
    // End of variables declaration//GEN-END:variables

    @Override
    public void saveProperties() throws IOException {
        if (null == propertiesFile) {
            throw new IllegalStateException("propertiesFile not set");
        }
        propertiesFile.getParentFile().mkdirs();
        Map<String, String> propsMap = new HashMap<>();
        propsMap.put(PROGRAMEXECUTABLE, jTextFieldPlannerProgramExecutable.getText());
        propsMap.put(PDDLDOMAIN, jTextFieldPddlDomainFile.getText());
        propsMap.put(PDDLPROBLEM, jTextFieldPddlProblem.getText());
//        propsMap.put(PDDLOUTPUT, jTextFieldPddlOutputActions.getText());
        propsMap.put(PDDLADDARGS, jTextFieldAdditionalArgs.getText());
        Properties props = new Properties();
        props.putAll(propsMap);
        try (FileWriter fw = new FileWriter(propertiesFile)) {
            props.store(fw, "");
        }
    }

    private volatile boolean closing = false;

    private void closePddlProcess() {
        boolean orig_closing = closing;
        closing=true;
        try {
            if (null != pddlProcess) {
                pddlProcess.destroyForcibly().waitFor(100, TimeUnit.MILLISECONDS);
                pddlProcess = null;
            }
        } catch (InterruptedException interruptedException) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, interruptedException);
        }
        try {
            if (null != ppdlInputStreamFuture) {
                ppdlInputStreamFuture.cancel(true);
                ppdlInputStreamFuture = null;
            }
        } catch (Exception e) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, e);
        }
        try {
            if (null != ppdlErrorStreamFuture) {
                ppdlErrorStreamFuture.cancel(true);
                ppdlErrorStreamFuture = null;
            }
        } catch (Exception e) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, e);
        }
        try {
            if (null != pddlInputStream) {
                pddlInputStream.close();
                pddlInputStream = null;
            }
        } catch (IOException iOException) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, iOException);
        }
        try {
            if (null != pddlErrorStream) {
                pddlErrorStream.close();
                pddlErrorStream = null;
            }
        } catch (IOException iOException) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, iOException);
        }
        closing = orig_closing;
    }
    @Override
    public void close() throws Exception {
        closing = true;
        this.closePddlProcess();
        closing=true;
        if (null != this.executor) {
            this.executor.shutdownNow();
            this.executor.awaitTermination(100, TimeUnit.MILLISECONDS);
            this.executor = null;
        }
    }
}
