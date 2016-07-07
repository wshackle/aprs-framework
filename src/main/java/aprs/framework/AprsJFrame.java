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

import aprs.framework.database.DbSetup;
import aprs.framework.database.DbSetupBuilder;
import aprs.framework.database.DbSetupJInternalFrame;
import aprs.framework.database.DbSetupListener;
import aprs.framework.database.DbSetupPublisher;
import aprs.framework.logdisplay.LogDisplayJInternalFrame;
import aprs.framework.pddl.executor.PddlExecutorJInternalFrame;
import aprs.framework.pddl.planner.PddlPlannerJInternalFrame;
import aprs.framework.simview.Object2DViewJInternalFrame;
import aprs.framework.spvision.VisionToDbJInternalFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import aprs.framework.pddl.executor.PddlExecutorDisplayInterface;
import crcl.base.CRCLProgramType;
import crcl.ui.client.PendantClientJInternalFrame;
import crcl.ui.server.SimServerJInternalFrame;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import javax.xml.bind.JAXBException;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class AprsJFrame extends javax.swing.JFrame implements PddlExecutorDisplayInterface {

    
    private static WeakReference<AprsJFrame> aprsJFrameWeakRef = null;
    
    public static AprsJFrame getCurrentAprsJFrame() {
       if(aprsJFrameWeakRef != null) {
           return aprsJFrameWeakRef.get();
       } else {
           return null;
       }
    }
    private VisionToDbJInternalFrame visionToDbJInternalFrame = null;
    private PddlExecutorJInternalFrame pddlExecutorJInternalFrame1 = null;
    private Object2DViewJInternalFrame object2DViewJInternalFrame = null;
    private PddlPlannerJInternalFrame pddlPlannerJInternalFrame = null;
    private DbSetupJInternalFrame dbSetupJInternalFrame = null;
    private PendantClientJInternalFrame pendantClientJInternalFrame = null;
    private SimServerJInternalFrame simServerJInternalFrame = null;
    private LogDisplayJInternalFrame logDisplayJInternalFrame = null;
    
    public void setCRCLProgram(CRCLProgramType program) throws JAXBException {
        if(null != pendantClientJInternalFrame) {
            pendantClientJInternalFrame.setProgram(program);
        }
    }
    
    private PrintStream origOut = null;
    private PrintStream origErr = null;
    
    private class MyPrintStream  extends PrintStream {
        
        final private PrintStream ps;

        public MyPrintStream(PrintStream ps) {
            super(ps,true);
            this.ps = ps;
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len); 
            if(null != logDisplayJInternalFrame) {
                final String s = new String(buf, off, len);
                if(javax.swing.SwingUtilities.isEventDispatchThread()) {
                    logDisplayJInternalFrame.appendText(s);
                } else {
                    
                    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            logDisplayJInternalFrame.appendText(s);
                        }
                    });
                }
            }
        }
        
    }
    
    /**
     * Creates new form AprsPddlWrapperJFrame
     */
    public AprsJFrame() {
        try {
            initComponents();
            loadProperties();
            if (jCheckBoxMenuItemStartupPDDLPlanner.isSelected()) {
                startPddlPlanner();
            }
            if (jCheckBoxMenuItemStartupPDDLExecutor.isSelected()) {
                startActionsToCrclJInternalFrame();
            }
            if (jCheckBoxMenuItemStartupObjectSP.isSelected()) {
                startVisionToDbJinternalFrame();
            }
            if (jCheckBoxMenuItemStartupObject2DView.isSelected()) {
                startObject2DJinternalFrame();
            }
            if (jCheckBoxMenuItemStartupRobotCrclGUI.isSelected()) {
                startPendantClientJInternalFrame();
            }
            if (jCheckBoxMenuItemStartupRobtCRCLSimServer.isSelected()) {
                startSimServerJInternalFrame();
            }
            if (null == logDisplayJInternalFrame) {
                logDisplayJInternalFrame = new LogDisplayJInternalFrame();
                logDisplayJInternalFrame.pack();
            }
            logDisplayJInternalFrame.setVisible(true);
            jDesktopPane1.add(logDisplayJInternalFrame);
            System.setOut(new MyPrintStream(System.out));
            System.setErr(new MyPrintStream(System.err));
            
            if (null == dbSetupJInternalFrame) {
                dbSetupJInternalFrame = new DbSetupJInternalFrame();
                dbSetupJInternalFrame.pack();
            }
            dbSetupJInternalFrame.setVisible(true);
            jDesktopPane1.add(dbSetupJInternalFrame);
            DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
            if (null != pub) {
                pub.setDbSetup(dbSetup);
                pub.addDbSetupListener(toVisListener);
            }
            setupWindowsMenu();
            System.err.println("error test");
            System.out.println("out test");
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            setIconImage(ImageIO.read(AprsJFrame.class.getResource("aprs.png")));
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        aprsJFrameWeakRef = new WeakReference<>(this);
    }

    private void setupWindowsMenu() {
        jMenuWindow.removeAll();
        int count = 1;
        for (JInternalFrame f : jDesktopPane1.getAllFrames()) {
            JMenuItem menuItem = new JMenuItem(count + " " + f.getTitle());
            final JInternalFrame frameToShow = f;
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frameToShow.setVisible(true);
                    jDesktopPane1.getDesktopManager().deiconifyFrame(frameToShow);
                    jDesktopPane1.getDesktopManager().activateFrame(frameToShow);
                    frameToShow.moveToFront();
                }
            });
            jMenuWindow.add(menuItem);
            count++;
        }
    }

    private void startObject2DJinternalFrame() {
        try {
            object2DViewJInternalFrame = new Object2DViewJInternalFrame();
            object2DViewJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "object2DViewProperties.txt"));
            object2DViewJInternalFrame.restoreProperties();
            object2DViewJInternalFrame.pack();
            object2DViewJInternalFrame.setVisible(true);
            jDesktopPane1.add(object2DViewJInternalFrame);
            object2DViewJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(object2DViewJInternalFrame);
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startPendantClientJInternalFrame() {
        try {
            pendantClientJInternalFrame = new PendantClientJInternalFrame();
//            pendantClientJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "object2DViewProperties.txt"));
//            pendantClientJInternalFrame.restoreProperties();
            pendantClientJInternalFrame.pack();
            pendantClientJInternalFrame.setVisible(true);
            jDesktopPane1.add(pendantClientJInternalFrame);
            pendantClientJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(pendantClientJInternalFrame);
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startSimServerJInternalFrame() {
        try {
            simServerJInternalFrame = new SimServerJInternalFrame();
//            pendantClientJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "object2DViewProperties.txt"));
//            pendantClientJInternalFrame.restoreProperties();
            simServerJInternalFrame.pack();
            simServerJInternalFrame.setVisible(true);
            jDesktopPane1.add(simServerJInternalFrame);
            simServerJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(simServerJInternalFrame);
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private final Callable<DbSetupPublisher> dbSetupPublisherSupplier = new Callable<DbSetupPublisher>() {
        @Override
        public DbSetupPublisher call() throws Exception {
            if (null == dbSetupJInternalFrame) {
                dbSetupJInternalFrame = new DbSetupJInternalFrame();
                dbSetupJInternalFrame.pack();
            }
            dbSetupJInternalFrame.setVisible(true);
            jDesktopPane1.getDesktopManager().deiconifyFrame(dbSetupJInternalFrame);
            jDesktopPane1.getDesktopManager().activateFrame(dbSetupJInternalFrame);
            dbSetupJInternalFrame.moveToFront();
            return dbSetupJInternalFrame.getDbSetupPublisher();
        }
    };

    private void startVisionToDbJinternalFrame() {
        visionToDbJInternalFrame = new VisionToDbJInternalFrame();
        visionToDbJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "visionToDBProperties.txt"));
        visionToDbJInternalFrame.restoreProperties();
        visionToDbJInternalFrame.pack();
        visionToDbJInternalFrame.setVisible(true);
        visionToDbJInternalFrame.setDbSetupSupplier(dbSetupPublisherSupplier);
        jDesktopPane1.add(visionToDbJInternalFrame);
        visionToDbJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(visionToDbJInternalFrame);
        DbSetupPublisher pub = visionToDbJInternalFrame.getDbSetupPublisher();
        if (null != pub) {
            pub.addDbSetupListener(toDbListener);
        }
    }

    private void startActionsToCrclJInternalFrame() {
        try {
            if (null == pddlExecutorJInternalFrame1) {
                pddlExecutorJInternalFrame1 = new PddlExecutorJInternalFrame();
                this.pddlExecutorJInternalFrame1.pack();
            }
            this.pddlExecutorJInternalFrame1.setVisible(true);
            jDesktopPane1.add(pddlExecutorJInternalFrame1);
            pddlExecutorJInternalFrame1.getDesktopPane().getDesktopManager().maximizeFrame(pddlExecutorJInternalFrame1);
            this.pddlExecutorJInternalFrame1.setPropertiesFile(new File(propertiesDirectory, "actionsToCrclProperties.txt"));
            this.pddlExecutorJInternalFrame1.loadProperties();
            pddlExecutorJInternalFrame1.setDbSetupSupplier(dbSetupPublisherSupplier);
            if (null != pddlPlannerJInternalFrame) {
                pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);
            }

        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startPddlPlanner() {
        try {
            //        jDesktopPane1.setDesktopManager(d);
            if (pddlPlannerJInternalFrame == null) {
                pddlPlannerJInternalFrame = new PddlPlannerJInternalFrame();
                pddlPlannerJInternalFrame.pack();
            }
            pddlPlannerJInternalFrame.setVisible(true);
            jDesktopPane1.add(pddlPlannerJInternalFrame);
            pddlPlannerJInternalFrame.getDesktopPane().getDesktopManager().maximizeFrame(pddlPlannerJInternalFrame);
            this.pddlPlannerJInternalFrame.setPropertiesFile(new File(propertiesDirectory, "pddlPlanner.txt"));
            pddlPlannerJInternalFrame.loadProperties();
            pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);
        } catch (IOException ex) {
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

        jDesktopPane1 = new javax.swing.JDesktopPane();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItemLoadProperties = new javax.swing.JMenuItem();
        jMenuItemSaveProperties = new javax.swing.JMenuItem();
        jMenuItemSetProperiesFile = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jCheckBoxMenuItemStartupPDDLPlanner = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupPDDLExecutor = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupObjectSP = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupObject2DView = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupRobotCrclGUI = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemStartupRobtCRCLSimServer = new javax.swing.JCheckBoxMenuItem();
        jMenuWindow = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("APRS Coordinator");

        jMenu1.setText("File");

        jMenuItemLoadProperties.setText("Reload Property Settings");
        jMenuItemLoadProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadPropertiesActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemLoadProperties);

        jMenuItemSaveProperties.setText("Save Properties");
        jMenuItemSaveProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSavePropertiesActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSaveProperties);

        jMenuItemSetProperiesFile.setText("Set Properties File ...");
        jMenuItemSetProperiesFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetProperiesFileActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSetProperiesFile);
        jMenu1.add(jSeparator1);

        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemExit);

        jMenuBar1.add(jMenu1);

        jMenu3.setText("Startup");

        jCheckBoxMenuItemStartupPDDLPlanner.setSelected(true);
        jCheckBoxMenuItemStartupPDDLPlanner.setText("PDDL Planner");
        jCheckBoxMenuItemStartupPDDLPlanner.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupPDDLPlannerActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupPDDLPlanner);

        jCheckBoxMenuItemStartupPDDLExecutor.setSelected(true);
        jCheckBoxMenuItemStartupPDDLExecutor.setText("PDDL Executor");
        jCheckBoxMenuItemStartupPDDLExecutor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupPDDLExecutorActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupPDDLExecutor);

        jCheckBoxMenuItemStartupObjectSP.setSelected(true);
        jCheckBoxMenuItemStartupObjectSP.setText("Object SP");
        jCheckBoxMenuItemStartupObjectSP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupObjectSPActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupObjectSP);

        jCheckBoxMenuItemStartupObject2DView.setSelected(true);
        jCheckBoxMenuItemStartupObject2DView.setText("Object 2D View/Simulate");
        jCheckBoxMenuItemStartupObject2DView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupObject2DViewActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupObject2DView);

        jCheckBoxMenuItemStartupRobotCrclGUI.setSelected(true);
        jCheckBoxMenuItemStartupRobotCrclGUI.setText("Robot CRCL Client Gui");
        jCheckBoxMenuItemStartupRobotCrclGUI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed(evt);
            }
        });
        jMenu3.add(jCheckBoxMenuItemStartupRobotCrclGUI);

        jCheckBoxMenuItemStartupRobtCRCLSimServer.setSelected(true);
        jCheckBoxMenuItemStartupRobtCRCLSimServer.setText("Robot CRCL SimServer");
        jMenu3.add(jCheckBoxMenuItemStartupRobtCRCLSimServer);

        jMenuBar1.add(jMenu3);

        jMenuWindow.setText("Window");
        jMenuBar1.add(jMenuWindow);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDesktopPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1058, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jDesktopPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 721, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxMenuItemStartupPDDLPlannerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupPDDLPlannerActionPerformed
        try {
            if (jCheckBoxMenuItemStartupPDDLPlanner.isSelected()) {
                startPddlPlanner();
            } else {
                closePddlPlanner();
            }
            setupWindowsMenu();
            saveProperties();
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupPDDLPlannerActionPerformed

    private void jCheckBoxMenuItemStartupPDDLExecutorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupPDDLExecutorActionPerformed
        try {
            if (jCheckBoxMenuItemStartupPDDLExecutor.isSelected()) {
                startActionsToCrclJInternalFrame();
            } else {
                closeActionsToCrclJInternalFrame();
            }
            setupWindowsMenu();
            saveProperties();
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupPDDLExecutorActionPerformed

    private void jCheckBoxMenuItemStartupObjectSPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupObjectSPActionPerformed
        try {
            if (jCheckBoxMenuItemStartupObjectSP.isSelected()) {
                startVisionToDbJinternalFrame();
            }
            setupWindowsMenu();
            saveProperties();
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupObjectSPActionPerformed

    private void jCheckBoxMenuItemStartupObject2DViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupObject2DViewActionPerformed
        try {
            if (jCheckBoxMenuItemStartupObject2DView.isSelected()) {
                startObject2DJinternalFrame();
            }
            setupWindowsMenu();
            saveProperties();
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupObject2DViewActionPerformed

    private void jMenuItemLoadPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemLoadPropertiesActionPerformed
        try {
            this.loadProperties();
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemLoadPropertiesActionPerformed

    private void jMenuItemSavePropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSavePropertiesActionPerformed
        try {
            this.saveProperties();
        } catch (IOException ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemSavePropertiesActionPerformed

    private void jMenuItemSetProperiesFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetProperiesFileActionPerformed
        JFileChooser chooser = new JFileChooser(propertiesDirectory);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                setPropertiesFile(chooser.getSelectedFile());
                propertiesDirectory = propertiesFile.getParentFile();
                if (propertiesFile.exists()) {
                    loadProperties();
                } else {
                    saveProperties();
                }
            } catch (IOException ex) {
                Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jMenuItemSetProperiesFileActionPerformed

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        try {
            close();
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed
        try {
            if (jCheckBoxMenuItemStartupRobotCrclGUI.isSelected()) {
                startPendantClientJInternalFrame();
            } else {
//                closePddlPlanner();
            }
            setupWindowsMenu();
            saveProperties();
        } catch (Exception ex) {
            Logger.getLogger(AprsJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemStartupRobotCrclGUIActionPerformed

    public void addAction(PddlAction action) {
        this.pddlExecutorJInternalFrame1.addAction(action);
    }

    static private File propertiesFile;
    static private File propertiesDirectory;

    static {
        propertiesDirectory = new File(System.getProperty("user.home"), ".aprs");
        propertiesDirectory.mkdirs();
        propertiesFile = new File(propertiesDirectory, "aprs_pddl_wrapper_propeties.txt");
    }

    private final DbSetupListener toVisListener = new DbSetupListener() {
        @Override
        public void accept(DbSetup setup) {
//            if (setup.isConnected() == (dbSetup != null && !dbSetup.isConnected())) {
//                dbSetup = setup;
//                DbSetup oldDbSetup = DbSetupBuilder.loadFromPropertiesFile(dbPropertiesFile).build();
//                if (!Objects.equals(oldDbSetup.getDbType(), setup.getDbType())) {
//                    setup = DbSetupBuilder.loadFromPropertiesFile(propertiesFile, setup.getDbType(), null, -1).build();
//                    if (null != dbSetupJInternalFrame) {
//                        DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
//                        if (null != pub) {
//                            DbSetupBuilder.savePropertiesFile(dbPropertiesFile, setup);
//                            pub.setDbSetup(setup);
//                        }
//                    }
//                }
//            }
            dbSetup = setup;
            if (null != visionToDbJInternalFrame) {
                DbSetupPublisher pub = visionToDbJInternalFrame.getDbSetupPublisher();
                if (null != pub) {
                    DbSetupBuilder.savePropertiesFile(new File(propertiesDirectory, "dbsetup.txt"), dbSetup);
                    pub.setDbSetup(setup);
                }
            }
        }
    };

    private File dbPropertiesFile = new File(propertiesDirectory, "dbsetup.txt");

    private final DbSetupListener toDbListener = new DbSetupListener() {
        @Override
        public void accept(DbSetup setup) {
//            if (setup.isConnected() == (dbSetup != null && !dbSetup.isConnected())) {
//                dbSetup = setup;
//
//                DbSetup oldDbSetup = DbSetupBuilder.loadFromPropertiesFile(dbPropertiesFile).build();
//                if (!Objects.equals(oldDbSetup.getDbType(), setup.getDbType())) {
//                    setup = DbSetupBuilder.loadFromPropertiesFile(propertiesFile, setup.getDbType(), null, -1).build();
//                    DbSetupBuilder.savePropertiesFile(dbPropertiesFile, setup);
//                    if (null != visionToDbJInternalFrame) {
//                        DbSetupPublisher pub = visionToDbJInternalFrame.getDbSetupPublisher();
//                        if (null != pub) {
//                            pub.setDbSetup(setup);
//                        }
//                    }
//                }
//            }
            dbSetup = setup;
            if (null != dbSetupJInternalFrame) {
                DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
                if (null != pub) {
                    DbSetupBuilder.savePropertiesFile(dbPropertiesFile, dbSetup);
                    pub.setDbSetup(setup);
                }
            }
        }
    };

    DbSetup dbSetup = null;

    @Override
    public void loadProperties() throws IOException {
        Properties props = new Properties();
        try (FileReader fr = new FileReader(propertiesFile)) {
            props.load(fr);
        }
        String startPddlPlannerString = props.getProperty(STARTUPPDDLPLANNER);
        if (null != startPddlPlannerString) {
            jCheckBoxMenuItemStartupPDDLPlanner.setSelected(Boolean.valueOf(startPddlPlannerString));
        }
        String startPddlExecutorString = props.getProperty(STARTUPPDDLEXECUTOR);
        if (null != startPddlExecutorString) {
            jCheckBoxMenuItemStartupPDDLExecutor.setSelected(Boolean.valueOf(startPddlExecutorString));
        }
        String startObjectSpString = props.getProperty(STARTUPPDDLOBJECTSP);
        if (null != startObjectSpString) {
            jCheckBoxMenuItemStartupObjectSP.setSelected(Boolean.valueOf(startObjectSpString));
        }

        String startObjectViewString = props.getProperty(STARTUPPDDLOBJECTVIEW);
        if (null != startObjectViewString) {
            jCheckBoxMenuItemStartupObject2DView.setSelected(Boolean.valueOf(startObjectViewString));
        }
        String startCRCLClientString = props.getProperty(STARTUPROBOTCRCLCLIENT);
        if (null != startCRCLClientString) {
            jCheckBoxMenuItemStartupRobotCrclGUI.setSelected(Boolean.valueOf(startCRCLClientString));
        }
        String startCRCLSimServerString = props.getProperty(STARTUPROBOTCRCLSIMSERVER);
        if (null != startCRCLSimServerString) {
            jCheckBoxMenuItemStartupRobtCRCLSimServer.setSelected(Boolean.valueOf(startCRCLSimServerString));
        }
//        String executable = props.getProperty(PROGRAMEXECUTABLE);
//        if (null != executable) {
//            jTextFieldPlannerProgramExecutable.setText(executable);
//        }
//        String domain = props.getProperty(PDDLDOMAIN);
//        if (null != domain) {
//            jTextFieldPddlDomainFile.setText(domain);
//        }
//        String problem = props.getProperty(PDDLPROBLEM);
//        if (null != problem) {
//            jTextFieldPddlProblem.setText(problem);
//        }
//        String output = props.getProperty(PDDLOUTPUT);
//        if (null != output) {
//            jTextFieldPddlOutputActions.setText(output);
//        }
//        String addargs = props.getProperty(PDDLADDARGS);
//        if (null != addargs) {
//            jTextFieldAdditionalArgs.setText(addargs);
//        }
        if (null != pddlPlannerJInternalFrame) {
            this.pddlPlannerJInternalFrame.loadProperties();
        }
        if (null != pddlExecutorJInternalFrame1) {
            this.pddlExecutorJInternalFrame1.loadProperties();
        }
        dbSetup = DbSetupBuilder.loadFromPropertiesFile(new File(propertiesDirectory, "dbsetup.txt")).build();

        if (null != dbSetupJInternalFrame) {
            DbSetupPublisher pub = dbSetupJInternalFrame.getDbSetupPublisher();
            if (null != pub) {
                pub.setDbSetup(dbSetup);
            }
        }
        if (null != visionToDbJInternalFrame) {
            this.visionToDbJInternalFrame.restoreProperties();
            DbSetupPublisher pub = visionToDbJInternalFrame.getDbSetupPublisher();
            if (null != pub) {
                pub.setDbSetup(dbSetup);
            }
        }
        if (null != object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.restoreProperties();
        }
    }

    @Override
    public void saveProperties() throws IOException {
        Map<String, String> propsMap = new HashMap<>();
//        propsMap.put(PROGRAMEXECUTABLE, jTextFieldPlannerProgramExecutable.getText());
//        propsMap.put(PDDLDOMAIN, jTextFieldPddlDomainFile.getText());
//        propsMap.put(PDDLPROBLEM, jTextFieldPddlProblem.getText());
////        propsMap.put(PDDLOUTPUT, jTextFieldPddlOutputActions.getText());
//        propsMap.put(PDDLADDARGS, jTextFieldAdditionalArgs.getText());
        propsMap.put(STARTUPPDDLPLANNER, Boolean.toString(jCheckBoxMenuItemStartupPDDLPlanner.isSelected()));
        propsMap.put(STARTUPPDDLEXECUTOR, Boolean.toString(jCheckBoxMenuItemStartupPDDLExecutor.isSelected()));
        propsMap.put(STARTUPPDDLOBJECTSP, Boolean.toString(jCheckBoxMenuItemStartupObjectSP.isSelected()));
        propsMap.put(STARTUPPDDLOBJECTVIEW, Boolean.toString(jCheckBoxMenuItemStartupObject2DView.isSelected()));
        propsMap.put(STARTUPROBOTCRCLCLIENT, Boolean.toString(jCheckBoxMenuItemStartupRobotCrclGUI.isSelected()));
        propsMap.put(STARTUPROBOTCRCLSIMSERVER, Boolean.toString(jCheckBoxMenuItemStartupRobtCRCLSimServer.isSelected()));
        Properties props = new Properties();
        props.putAll(propsMap);
        try (FileWriter fw = new FileWriter(propertiesFile)) {
            props.store(fw, "");
        }
        if (null != this.pddlPlannerJInternalFrame) {
            this.pddlPlannerJInternalFrame.saveProperties();
        }
        if (null != this.pddlExecutorJInternalFrame1) {
            this.pddlExecutorJInternalFrame1.saveProperties();
        }
        if (null != this.visionToDbJInternalFrame) {
            this.visionToDbJInternalFrame.saveProperties();
        }
        if (null != this.object2DViewJInternalFrame) {
            this.object2DViewJInternalFrame.saveProperties();
        }
        if (null != dbSetup) {
            DbSetupBuilder.savePropertiesFile(new File(propertiesDirectory, "dbsetup.txt"), dbSetup);
        }
    }

    private static final String STARTUPPDDLPLANNER = "startup.pddl.planner";
    private static final String STARTUPPDDLEXECUTOR = "startup.pddl.executor";
    private static final String STARTUPPDDLOBJECTSP = "startup.pddl.objectsp";
    private static final String STARTUPPDDLOBJECTVIEW = "startup.pddl.objectview";
    private static final String STARTUPROBOTCRCLCLIENT = "startup.robotcrclclient";
    private static final String STARTUPROBOTCRCLSIMSERVER = "startup.robotcrclsimserver";

    @Override
    public void browseActionsFile() throws IOException {
        this.pddlExecutorJInternalFrame1.browseActionsFile();
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
            java.util.logging.Logger.getLogger(AprsJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AprsJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AprsJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AprsJFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
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
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupObject2DView;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupObjectSP;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupPDDLExecutor;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupPDDLPlanner;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupRobotCrclGUI;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartupRobtCRCLSimServer;
    private javax.swing.JDesktopPane jDesktopPane1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemLoadProperties;
    private javax.swing.JMenuItem jMenuItemSaveProperties;
    private javax.swing.JMenuItem jMenuItemSetProperiesFile;
    private javax.swing.JMenu jMenuWindow;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    // End of variables declaration//GEN-END:variables

    @Override
    public List<PddlAction> getActionsList() {
        return pddlExecutorJInternalFrame1.getActionsList();
    }

    @Override
    public void setActionsList(List<PddlAction> actionsList) {
        pddlExecutorJInternalFrame1.setActionsList(actionsList);
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
        pddlExecutorJInternalFrame1.autoResizeTableColWidthsPddlOutput();
    }

    @Override
    public boolean isLoadEnabled() {
        return pddlExecutorJInternalFrame1.isLoadEnabled();
    }

    @Override
    public void setLoadEnabled(boolean enable) {
        pddlExecutorJInternalFrame1.setLoadEnabled(enable);
    }

    @Override
    public void close() throws Exception {
        try {
            closePddlPlanner();

        } catch (Exception exception) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, exception);
        }
        try {
            closeActionsToCrclJInternalFrame();

        } catch (Exception exception) {
            Logger.getLogger(AprsJFrame.class
                    .getName()).log(Level.SEVERE, null, exception);
        }
    }

    private void closeActionsToCrclJInternalFrame() throws Exception {
        if (null != pddlExecutorJInternalFrame1) {
            pddlExecutorJInternalFrame1.close();
            pddlExecutorJInternalFrame1.setVisible(false);
            pddlExecutorJInternalFrame1.dispose();
            pddlExecutorJInternalFrame1 = null;
            if (null != pddlPlannerJInternalFrame) {
                pddlPlannerJInternalFrame.setActionsToCrclJInternalFrame1(pddlExecutorJInternalFrame1);
            }
        }
    }

    private void closePddlPlanner() throws Exception {
        if (null != pddlPlannerJInternalFrame) {
            pddlPlannerJInternalFrame.close();
            pddlPlannerJInternalFrame.setVisible(false);
            pddlPlannerJInternalFrame.dispose();
            pddlPlannerJInternalFrame = null;
        }
    }
}
