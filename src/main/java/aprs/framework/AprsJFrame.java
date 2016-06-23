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
 
package aprs.framework;

import com.github.wshackle.visiontodb.Object2DViewJInternalFrame;
import com.github.wshackle.visiontodb.VisionToDbJInternalFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class AprsJFrame extends javax.swing.JFrame implements ActionsToCrclDisplayInterface {

    private VisionToDbJInternalFrame visionToDbJInternalFrame = null;
    private ActionsToCrclJInternalFrame actionsToCrclJInternalFrame1 = null;
    private Object2DViewJInternalFrame object2DViewJInternalFrame = null;

    /**
     * Creates new form AprsPddlWrapperJFrame
     */
    public AprsJFrame() {
        initComponents();
//        jDesktopPane1.setDesktopManager(d);
        jInternalFramePddlPlannerExecutable.setVisible(true);
        jDesktopPane1.add(jInternalFramePddlPlannerExecutable);
        jInternalFramePddlPlannerExecutable.getDesktopPane().getDesktopManager().maximizeFrame(jInternalFramePddlPlannerExecutable);

        actionsToCrclJInternalFrame1 = new ActionsToCrclJInternalFrame();
        this.actionsToCrclJInternalFrame1.pack();
        this.actionsToCrclJInternalFrame1.setVisible(true);
        jDesktopPane1.add(actionsToCrclJInternalFrame1);
        actionsToCrclJInternalFrame1.getDesktopPane().getDesktopManager().maximizeFrame(actionsToCrclJInternalFrame1);

        visionToDbJInternalFrame = new VisionToDbJInternalFrame();
        visionToDbJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "visionToDBProperties.txt"));
        visionToDbJInternalFrame.restoreProperties();
        visionToDbJInternalFrame.pack();
        visionToDbJInternalFrame.setVisible(true);
        jDesktopPane1.add(visionToDbJInternalFrame);
        visionToDbJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(visionToDbJInternalFrame);
        this.actionsToCrclJInternalFrame1.setPropertiesFile(new File(propertiesDirectory, "actionsToCrclProperties.txt"));

        try {
            object2DViewJInternalFrame = new Object2DViewJInternalFrame();
            object2DViewJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "object2DViewProperties.txt"));
            object2DViewJInternalFrame.restoreProperties();
            object2DViewJInternalFrame.pack();
            object2DViewJInternalFrame.setVisible(true);
            jDesktopPane1.add(object2DViewJInternalFrame);
            object2DViewJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(object2DViewJInternalFrame);

        } catch (IOException iOException) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, iOException);
        }
        try {
            loadProperties();
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (JInternalFrame f : jDesktopPane1.getAllFrames()) {
            JMenuItem menuItem = new JMenuItem(f.getTitle());
            final JInternalFrame frameToShow = f;
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frameToShow.setVisible(true);
                    frameToShow.moveToFront();;
                    jDesktopPane1.getDesktopManager().deiconifyFrame(frameToShow);
                    jDesktopPane1.getDesktopManager().activateFrame(frameToShow);
                }
            });
            jMenuWindow.add(menuItem);
        }
        try {
            setIconImage(ImageIO.read(AprsJFrame.class.getResource("aprs.png")));
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jInternalFramePddlPlannerExecutable = new javax.swing.JInternalFrame();
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
        jDesktopPane1 = new javax.swing.JDesktopPane();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenuWindow = new javax.swing.JMenu();

        jInternalFramePddlPlannerExecutable.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
        jInternalFramePddlPlannerExecutable.setIconifiable(true);
        jInternalFramePddlPlannerExecutable.setMaximizable(true);
        jInternalFramePddlPlannerExecutable.setResizable(true);
        jInternalFramePddlPlannerExecutable.setTitle("Pddl Planner Executable");
        jInternalFramePddlPlannerExecutable.setVisible(true);

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

        javax.swing.GroupLayout jInternalFramePddlPlannerExecutableLayout = new javax.swing.GroupLayout(jInternalFramePddlPlannerExecutable.getContentPane());
        jInternalFramePddlPlannerExecutable.getContentPane().setLayout(jInternalFramePddlPlannerExecutableLayout);
        jInternalFramePddlPlannerExecutableLayout.setHorizontalGroup(
            jInternalFramePddlPlannerExecutableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jInternalFramePddlPlannerExecutableLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jInternalFramePddlPlannerExecutableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1)
                    .addComponent(jTextFieldAdditionalArgs, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jInternalFramePddlPlannerExecutableLayout.createSequentialGroup()
                        .addComponent(jTextFieldPlannerProgramExecutable, javax.swing.GroupLayout.PREFERRED_SIZE, 508, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonPlannerProgramExecutableBrowse, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jInternalFramePddlPlannerExecutableLayout.createSequentialGroup()
                        .addGroup(jInternalFramePddlPlannerExecutableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jTextFieldPddlProblem, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
                            .addComponent(jTextFieldPddlDomainFile, javax.swing.GroupLayout.Alignment.LEADING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jInternalFramePddlPlannerExecutableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonPddlDomainBrowse, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButtonPddlProblemBrowse, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jInternalFramePddlPlannerExecutableLayout.createSequentialGroup()
                        .addGroup(jInternalFramePddlPlannerExecutableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jInternalFramePddlPlannerExecutableLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonRunOnce)))
                .addContainerGap())
        );
        jInternalFramePddlPlannerExecutableLayout.setVerticalGroup(
            jInternalFramePddlPlannerExecutableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jInternalFramePddlPlannerExecutableLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jInternalFramePddlPlannerExecutableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldPlannerProgramExecutable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonPlannerProgramExecutableBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jInternalFramePddlPlannerExecutableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldPddlDomainFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonPddlDomainBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jInternalFramePddlPlannerExecutableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldPddlProblem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonPddlProblemBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldAdditionalArgs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jInternalFramePddlPlannerExecutableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jButtonRunOnce))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
                .addContainerGap())
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("APRS Coordinator");

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        jMenuWindow.setText("Window");
        jMenuBar1.add(jMenuWindow);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDesktopPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 807, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDesktopPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 668, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextFieldPddlDomainFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldPddlDomainFileActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldPddlDomainFileActionPerformed

    private void jButtonPlannerProgramExecutableBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlannerProgramExecutableBrowseActionPerformed
        try {
            browseProgramExecutable();
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonPlannerProgramExecutableBrowseActionPerformed

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
            actionsToCrclJInternalFrame1.setLoadEnabled(false);
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonRunOnceActionPerformed

    private Executor executor = Executors.newCachedThreadPool();

    /**
     * Get the value of executor
     *
     * @return the value of executor
     */
    public Executor getExecutor() {
        return executor;
    }

    /**
     * Set the value of executor
     *
     * @param executor new value of executor
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void addAction(PddlAction action) {
        this.actionsToCrclJInternalFrame1.addAction(action);
    }

    public void runPddlPlannerOnce() throws IOException {
        List<String> commandList = new ArrayList<>();
        this.setActionsList(new ArrayList<>());
        commandList.add(new File(jTextFieldPlannerProgramExecutable.getText()).getCanonicalPath());
        commandList.addAll(Arrays.asList(jTextFieldAdditionalArgs.getText().split("[ \t]+")));
        commandList.add(jTextFieldPddlDomainFile.getText());
        commandList.add(jTextFieldPddlProblem.getText());
        ProcessBuilder pb = new ProcessBuilder(commandList);
        final Process process = pb.start();
        jTextAreaOutput.append(commandList.toString() + System.lineSeparator());
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream os = process.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(os));
                    String line = null;
                    while (null != (line = br.readLine())) {
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
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream os = process.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(os));
                    String line = null;
                    boolean planFoundFound = false;
                    while (null != (line = br.readLine())) {
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

    static private File propertiesFile;
    static private final File propertiesDirectory;

    static {
        propertiesDirectory = new File(System.getProperty("user.home"), ".aprs");
        propertiesDirectory.mkdirs();
        propertiesFile = new File(propertiesDirectory, "aprs_pddl_wrapper_propeties.txt");
    }

    @Override
    public void loadProperties() throws IOException {
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
        this.actionsToCrclJInternalFrame1.loadProperties();
        this.visionToDbJInternalFrame.restoreProperties();
        this.object2DViewJInternalFrame.restoreProperties();
    }

    private static final String PROGRAMEXECUTABLE = "program.executable";

    public void saveProperties() throws IOException {
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
        this.visionToDbJInternalFrame.saveProperties();
        this.object2DViewJInternalFrame.saveProperties();
    }
    
    private static final String PDDLADDARGS = "pddl.addargs";
    private static final String PDDLPROBLEM = "pddl.problem";
    private static final String PDDLDOMAIN = "pddl.domain";

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

    @Override
    public void browseActionsFile() throws IOException {
        this.actionsToCrclJInternalFrame1.browseActionsFile();
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
            java.util.logging.Logger.getLogger(AprsJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AprsJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AprsJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AprsJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new AprsJFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonPddlDomainBrowse;
    private javax.swing.JButton jButtonPddlProblemBrowse;
    private javax.swing.JButton jButtonPlannerProgramExecutableBrowse;
    private javax.swing.JButton jButtonRunOnce;
    private javax.swing.JDesktopPane jDesktopPane1;
    private javax.swing.JInternalFrame jInternalFramePddlPlannerExecutable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuWindow;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextAreaOutput;
    private javax.swing.JTextField jTextFieldAdditionalArgs;
    private javax.swing.JTextField jTextFieldPddlDomainFile;
    private javax.swing.JTextField jTextFieldPddlProblem;
    private javax.swing.JTextField jTextFieldPlannerProgramExecutable;
    // End of variables declaration//GEN-END:variables

    @Override
    public List<PddlAction> getActionsList() {
        return actionsToCrclJInternalFrame1.getActionsList();
    }

    @Override
    public void setActionsList(List<PddlAction> actionsList) {
        actionsToCrclJInternalFrame1.setActionsList(actionsList);
    }

    @Override
    public File getPropertiesFile() {
        return propertiesFile;
    }

    @Override
    public void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    @Override
    public void autoResizeTableColWidthsPddlOutput() {
        actionsToCrclJInternalFrame1.autoResizeTableColWidthsPddlOutput();
    }

    @Override
    public boolean isLoadEnabled() {
        return actionsToCrclJInternalFrame1.isLoadEnabled();
    }

    @Override
    public void setLoadEnabled(boolean enable) {
        actionsToCrclJInternalFrame1.setLoadEnabled(enable);
    }
}
