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
package aprs.framework.spvision;

import aprs.framework.AprsSystemInterface;
import aprs.framework.database.DbSetupPublisher;
import aprs.framework.database.DbType;
import aprs.framework.database.PhysicalItem;
import aprs.framework.database.PoseQueryElem;
import aprs.framework.database.PartsTray;
import aprs.framework.database.Slot;
import aprs.framework.database.Tray;
import crcl.ui.XFuture;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class VisionToDbJInternalFrame extends javax.swing.JInternalFrame implements VisionToDBJFrameInterface {

    /**
     * Creates new form VisionToDbJInternalFrame
     */
    @SuppressWarnings("initialization")
    public VisionToDbJInternalFrame() {
        initComponents();
//        DbMain.setDisplayInterface(this);
    }

    @Override
    public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem) {
        return visionToDBJPanel.absSlotFromTrayAndOffset(tray, offsetItem);
    }

    @Override
    public Slot absSlotFromTrayAndOffset(PhysicalItem tray, Slot offsetItem, double rotationOffset) {
        return visionToDBJPanel.absSlotFromTrayAndOffset(tray, offsetItem, rotationOffset);
    }

    public boolean isDbConnected() {
        return visionToDBJPanel.isDbConnected();
    }

    /**
     * Get the most recent list of parts and kit trays from the vision system.
     * This will not block waiting for the vision system or database but could
     * return null or an empty list if the vision system has not been connected
     * or no frame has been received.
     *
     * @return list of trays
     */
    public List<PartsTray> getPartsTrayList() {
        return visionToDBJPanel.getPartsTrayList();
    }

//    public DbSetupPublisher getDbSetupPublisher() {
//        return this.visionToDBJPanel.getDbSetupPublisher();
//    }
    public double getRotationOffset() {
        return this.visionToDBJPanel.getRotationOffset();
    }

    public XFuture<Void> startNewItemsImageSave(File f) {
        return this.visionToDBJPanel.startNewItemsImageSave(f);
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
        List<Slot> l = this.visionToDBJPanel.getSlotOffsets(name, ignoreEmpty);
        if (null != l) {
            return l;
        }
        return Collections.emptyList();
    }

    public List<Slot> getSlots(Tray item) {
        List<Slot> l = this.visionToDBJPanel.getSlots(item);
        if (null != l) {
            return l;
        }
        return Collections.emptyList();
    }

    public void setAprsSystemInterface(AprsSystemInterface aprsJFrame) {
        this.visionToDBJPanel.setAprsSystemInterface(aprsJFrame);
    }

//    public XFuture<List<PhysicalItem>> getNextUpdate() {
//        return visionToDBJPanel.getNextUpdate();
//    }
    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system.
     *
     * @return future with list of items updated in the next frame from the
     * vision
     */
    public XFuture<List<PhysicalItem>> getSingleUpdate() {
        return visionToDBJPanel.getSingleUpdate();
    }

    public long getLastUpdateTime() {
        return visionToDBJPanel.getLastUpdateTime();
    }

    public long getNotifySingleUpdateListenersTime() {
        return visionToDBJPanel.getNotifySingleUpdateListenersTime();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        visionToDBJPanel = new aprs.framework.spvision.VisionToDBJPanel();

        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("[Object SP] Vision To Database");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1004, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(visionToDBJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 746, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(visionToDBJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 722, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void setAquiring(String s) {
        this.visionToDBJPanel.setAquiring(s);
    }

    @Override
    public void updateInfo(List<PhysicalItem> _list, String line) {
        this.visionToDBJPanel.updateInfo(_list, line);
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
    public void setDBConnected(boolean _val) {
        this.visionToDBJPanel.setDBConnected(_val);
    }

    @Override
    public void setLastCommand(String c) {
        this.visionToDBJPanel.setLastCommand(c);
    }

    @Override
    public void setCommandConnected(boolean _val) {
        this.visionToDBJPanel.setCommandConnected(_val);
    }

    public void startCommand(Map<String, String> argsMap) {
        visionToDBJPanel.startCommand(argsMap);
    }

//     public void startVision(Map<String, String> argsMap) {
//         visionToDBJPanel.startVision(argsMap);
//     }
//     
//     public Map<String,String> getArgsMap() {
//         return visionToDBJPanel.getArgsMap();
//     }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.framework.spvision.VisionToDBJPanel visionToDBJPanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void updateFromArgs(Map<String, String> _argsMap) {
        this.visionToDBJPanel.updateFromArgs(_argsMap);
    }

    public void setPropertiesFile(File f) {
        this.visionToDBJPanel.setPropertiesFile(f);
    }

    public File getPropertiesFile() {
        return this.visionToDBJPanel.getPropertiesFile();
    }

    public void saveProperties() {
        this.visionToDBJPanel.saveProperties();
    }

    public void loadProperties() {
        this.visionToDBJPanel.loadProperties();
    }

    @Override
    public Map<String, String> updateArgsMap() {
        return visionToDBJPanel.updateArgsMap();
    }

    @Override
    public void addLogMessage(Exception exception) {
        visionToDBJPanel.addLogMessage(exception);
    }

    @Override
    @Nullable
    public Connection getSqlConnection() {
        return visionToDBJPanel.getSqlConnection();
    }

    @Override
    public DbType getDbType() {
        return visionToDBJPanel.getDbType();
    }

    @Override
    public void setSqlConnection(Connection connection, DbType dbtype) throws SQLException {
        visionToDBJPanel.setSqlConnection(connection, dbtype);
    }

    @Override
    @Nullable
    public Callable<DbSetupPublisher> getDbSetupSupplier() {
        return visionToDBJPanel.getDbSetupSupplier();
    }

    @Override
    public void setDbSetupSupplier(Callable<DbSetupPublisher> dbSetupSupplier) {
        visionToDBJPanel.setDbSetupSupplier(dbSetupSupplier);
    }

    @Override
    public void connectVision() {
        visionToDBJPanel.connectVision();
    }

    public void disconnectVision() {
        visionToDBJPanel.disconnectVision();
    }

    @Override
    public void updateResultsMap(Map<String, UpdateResults> _map) {
        visionToDBJPanel.updateResultsMap(_map);
    }

    public boolean isEnableDatabaseUpdates() {
        return visionToDBJPanel.isEnableDatabaseUpdates();
    }

    /**
     * Set the value of enableDatabaseUpdates
     *
     * @param enableDatabaseUpdates new value of enableDatabaseUpdates
     * @param requiredParts map of part names to required number of each type of
     * part
     */
    public void setEnableDatabaseUpdates(boolean enableDatabaseUpdates, Map<String, Integer> requiredParts) {
        visionToDBJPanel.setEnableDatabaseUpdates(enableDatabaseUpdates, requiredParts);
    }

    public Map<String, UpdateResults> getUpdatesResultMap() {
        return visionToDBJPanel.getUpdatesResultMap();
    }
}
