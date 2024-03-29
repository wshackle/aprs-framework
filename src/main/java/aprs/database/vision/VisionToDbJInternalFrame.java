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

import aprs.system.AprsSystem;
import aprs.database.DbSetupPublisher;
import aprs.database.DbType;
import aprs.database.PhysicalItem;
import aprs.database.PoseQueryElem;
import aprs.database.PartsTray;
import aprs.database.Slot;
import aprs.database.Tray;
import crcl.utils.XFuture;
import crcl.utils.XFutureVoid;
import java.io.File;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("serial")
public class VisionToDbJInternalFrame extends javax.swing.JInternalFrame implements VisionToDBJFrameInterface {
    
    
    /**
     * Creates new form VisionToDbJInternalFrame
     * @param aprsSystem system this frame will be connected to
     */
    @SuppressWarnings({"nullness","initialization"})
    @UIEffect
    public VisionToDbJInternalFrame(AprsSystem aprsSystem) {
        visionToDBJPanel = new aprs.database.vision.VisionToDBJPanel(aprsSystem);

        super.setIconifiable(true);
        super.setMaximizable(true);
        super.setResizable(true);
        super.setTitle("[Object SP] Vision To Database");
        super.add(visionToDBJPanel);
        super.pack();
    }

    public void addLineCountListener(Consumer<Integer> l) {
        visionToDBJPanel.addLineCountListener(l);
    }

    public void removeLineCountListener(Consumer<Integer> l) {
        visionToDBJPanel.removeLineCountListener(l);
    }

    public int getLineCount() {
        return visionToDBJPanel.getLineCount();
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

    public boolean databasesUpdatesEnabled() {
        return visionToDBJPanel.databasesUpdatesEnabled();
    }

    /**
     * Get the most recent list of parts and kit trays from the vision system.
     * This will not block waiting for the vision system or database but could
     * return null or an empty list if the vision system has not been connected
     * or no frame has been received.
     *
     * @return list of trays
     */
    @SafeEffect
    public List<PartsTray> getPartsTrayList() {
        return visionToDBJPanel.getPartsTrayList();
    }


    public double getRotationOffset() {
        return this.visionToDBJPanel.getRotationOffset();
    }

    public XFutureVoid startNewItemsImageSave(File f) {
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
    @SafeEffect
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


    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system.
     *
     * @return future with list of items updated in the next frame from the
     * vision
     */
    @SafeEffect
    public XFuture<List<PhysicalItem>> getNewSingleUpdate() {
        return visionToDBJPanel.getNewSingleUpdate();
    }
    
    public boolean removeSingleUpdate(XFuture<List<PhysicalItem>> future) {
        return visionToDBJPanel.removeSingleUpdate(future);
    }

    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system.
     *
     * @return future with list of items updated in the next frame from the
     * vision
     */
    @SafeEffect
    public XFuture<List<PhysicalItem>> getSingleUpdate() {
        return visionToDBJPanel.getSingleUpdate();
    }

    /**
     * Asynchronously get a list of PhysicalItems updated in one frame from the
     * vision system.
     *
     * @return future with list of items updated in the next frame from the
     * vision
     */
    @SafeEffect
    public XFuture<List<PhysicalItem>> getRawUpdate() {
        return visionToDBJPanel.getRawUpdate();
    }

    public void clearVisionRequiredParts() {
        visionToDBJPanel.clearVisionRequiredParts();
    }

    /**
     * Get a list of items with names and poses from the simulation.
     *
     * @return list of items as generated by the simulation
     * @throws IllegalStateException Object 2D view was not opened.
     */
    public List<PhysicalItem> getLastVisItemsData() {
        return visionToDBJPanel.getLastVisItemsData();
    }

    @SafeEffect
    public long getLastUpdateTime() {
        return visionToDBJPanel.getLastUpdateTime();
    }

    @SafeEffect
    public long getNotifySingleUpdateListenersTime() {
        return visionToDBJPanel.getNotifySingleUpdateListenersTime();
    }
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

    public void startCommand(Map<String, String> argsMap) {
        visionToDBJPanel.startCommand(argsMap);
    }

    public int getVisionClientUpdateCount() {
        return visionToDBJPanel.getVisionClientUpdateCount();
    }

    public int getVisionClientUpdateAquireOffCount() {
        return visionToDBJPanel.getVisionClientUpdateAquireOffCount();
    }

    public int getVisionClientUpdateNoCheckRequiredPartsCount() {
        return visionToDBJPanel.getVisionClientUpdateNoCheckRequiredPartsCount();
    }

    public int getVisionClientUpdateSingleUpdateListenersEmptyCount() {
        return visionToDBJPanel.getVisionClientUpdateSingleUpdateListenersEmptyCount();
    }

    public int getVisionClientSkippedCount() {
        return visionToDBJPanel.getVisionClientSkippedCount();
    }

    public int getVisionClientIgnoreCount() {
        return visionToDBJPanel.getVisionClientIgnoreCount();
    }

    public @Nullable
    String getPerformanceLine() {
        return visionToDBJPanel.getPerformanceLine();
    }

    private final aprs.database.vision.VisionToDBJPanel visionToDBJPanel;

    public void updateFromArgs(Map<String, String> _argsMap) {
        this.visionToDBJPanel.updateFromArgs(_argsMap);
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
    public @Nullable
    Callable<DbSetupPublisher> getDbSetupSupplier() {
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

    @Override
    public void updatePerformanceLine() {
        visionToDBJPanel.updatePerformanceLine();
    }
}
