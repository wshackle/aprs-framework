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
package aprs.database.vision;

import aprs.database.*;
import aprs.system.AprsSystem;
import crcl.utils.XFutureVoid;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"all", "serial"})
class VisionToDbMainJFrame extends javax.swing.JFrame implements VisionToDBJFrameInterface {


    /**
     * Creates new form MainJFrame
     */
    @SuppressWarnings({"initialization","nullness"})
    @UIEffect
    @Deprecated
    private VisionToDbMainJFrame() {
        this(null);
    }
    
    /**
     * Creates new form MainJFrame
     */
    @SuppressWarnings({"nullness","initialization"})
    @UIEffect
    public VisionToDbMainJFrame(final AprsSystem aprsSystem) {
        visionToDBJPanel = new aprs.database.vision.VisionToDBJPanel(aprsSystem);
        jMenuBar1 = new javax.swing.JMenuBar();

        super.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        super.setTitle("Vision To Database");
        super.setBackground(new java.awt.Color(210, 12, 12));
        super.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        jMenuBar1.add(visionToDBJPanel.jMenuFile);
        super.setJMenuBar(jMenuBar1);
        super.add(visionToDBJPanel);
        super.pack();
    }

    @Override
    public void updatePerformanceLine() {
        visionToDBJPanel.updatePerformanceLine();
    }

    @Override
    public void connectVision() {
        visionToDBJPanel.connectVision();
    }

    public void updateFromArgs(Map<String, String> _argsMap) {
        this.visionToDBJPanel.updateFromArgs(_argsMap);
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
//        println("formWindowClosing called.");
//        DbMain.closeAll();
    }//GEN-LAST:event_formWindowClosing


    @Override
    public void setAquiring(String s) {
        this.visionToDBJPanel.setAquiring(s);
    }

    @Override
    public void updataPoseQueryInfo(final List<PoseQueryElem> _list) {
        this.visionToDBJPanel.updataPoseQueryInfo(_list);
    }

    @Override
    public boolean isDebug() {
        return this.visionToDBJPanel.isDebug();
    }

    @Override
    public void addLogMessage(String stmnt) {
        this.visionToDBJPanel.addLogMessage(stmnt);
    }

    @Override
    public void setVisionConnected(boolean _val) {
        this.visionToDBJPanel.setVisionConnected(_val);
    }

    @Override
    public void setLastCommand(String c) {
        this.visionToDBJPanel.setLastCommand(c);
    }

    public void setPropertiesFile(File f) {
        this.visionToDBJPanel.setPropertiesFile(f);
    }

    public @Nullable
    File getPropertiesFile() {
        return this.visionToDBJPanel.getPropertiesFile();
    }

    public void saveProperties() {
        this.visionToDBJPanel.saveProperties();
    }

    public void loadProperties() {
        this.visionToDBJPanel.loadProperties();
    }

    public Map<String, String> updateArgsMap() {
        return visionToDBJPanel.updateArgsMap();
    }

    private final javax.swing.JMenuBar jMenuBar1;
    private final aprs.database.vision.VisionToDBJPanel visionToDBJPanel;

    @Override
    public void addLogMessage(Exception exception) {
        visionToDBJPanel.addLogMessage(exception);
    }

    @Override
    public @Nullable
    Connection getSqlConnection() {
        return visionToDBJPanel.getSqlConnection();
    }

    @Override
    public DbType getDbType() {
        return visionToDBJPanel.getDbType();
    }

    @Override
    public void setSqlConnection(Connection connection, DbType dbtype) {
        visionToDBJPanel.setSqlConnection(connection, dbtype);
    }

    @Override
    public Callable<DbSetupPublisher> getDbSetupSupplier() {
        throw new RuntimeException("getDbSetupSupplier not implemented");
    }

    @Override
    public void setDbSetupSupplier(Callable<DbSetupPublisher> dbSetupSupplier) {
    }

    @Override
    public void updateResultsMap(Map<String, UpdateResults> _map) {
        if (null != this.visionToDBJPanel) {
            this.visionToDBJPanel.updateResultsMap(_map);
        }
    }

    /**
     * Get a list of slots with names and relative position offsets for a given
     * kit or parts tray name.
     *
     * @param name name of the type of kit or slot tray
     * @param ignoreEmpty if false no slots being found logs a verbose error
     * message and throws IllegalStateException (good for fail fast) or if true
     * simply returns an empty list (good or display or when multiple will be
     * checked.
     *
     * @return list of slots with relative position offsets.
     */
    @Override
    public List<Slot> getSlotOffsets(String name, boolean ignoreEmpty) {
        return visionToDBJPanel.getSlotOffsets(name, ignoreEmpty);
    }

    @Override
    public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {
        return visionToDBJPanel.absSlotFromTrayAndOffset(tray, offsetItem);
    }

    @Override
    public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem, double rotationOffset) {
        return visionToDBJPanel.absSlotFromTrayAndOffset(tray, offsetItem, rotationOffset);
    }
}
