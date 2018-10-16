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
package aprs.simview;

import aprs.system.AprsSystem;
import aprs.misc.SlotOffsetProvider;
import aprs.database.PhysicalItem;
import aprs.misc.Utils;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.ui.XFutureVoid;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import rcs.posemath.PmCartesian;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Object2DViewJInternalFrame extends javax.swing.JInternalFrame implements Object2DJFrameInterface {

    public Object2DOuterJPanel getObjectViewPanel() {
        return object2DOuterJPanel1;
    }

    /**
     * Creates new form Object2DViewJInternalFrame
     */
    @SuppressWarnings("initialization")
    @UIEffect
    public Object2DViewJInternalFrame() {
        initComponents();
    }

    public void refresh(boolean loadFile) {
        object2DOuterJPanel1.refresh(loadFile);
    }

    public boolean isPartMoving() {
        return object2DOuterJPanel1.isUserMouseDown();
    }

    public double getClosestRobotPartDistance() {
        return this.object2DOuterJPanel1.getClosestRobotPartDistance();
    }

    public void setReverseFlag(boolean reverseFlag) {
        this.object2DOuterJPanel1.setReverseFlag(reverseFlag);
    }

    public void reloadDataFile() throws IOException {
        this.object2DOuterJPanel1.reloadDataFile();
    }

    public void stopSimUpdateTimer() {
        this.object2DOuterJPanel1.stopSimUpdateTimer();
    }

    public long getLastRefreshTime() {
        return object2DOuterJPanel1.getLastRefreshTime();
    }

    public int getRefreshCount() {
        return object2DOuterJPanel1.getRefreshCount();
    }

    public long getLastPublishTime() {
        return object2DOuterJPanel1.getLastPublishTime();
    }

    public int getPublishCount() {
        return object2DOuterJPanel1.getPublishCount();
    }

    public void addPublishCountListener(Consumer<Integer> l) {
        object2DOuterJPanel1.addPublishCountListener(l);
    }

    public void removePublishCountListener(Consumer<Integer> l) {
        object2DOuterJPanel1.removePublishCountListener(l);
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

        object2DOuterJPanel1 = new aprs.simview.Object2DOuterJPanel();

        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Object2D View/Simulate");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.simview.Object2DOuterJPanel object2DOuterJPanel1;
    // End of variables declaration//GEN-END:variables

    public BufferedImage createSnapshotImage() {
        return object2DOuterJPanel1.createSnapshotImage();
    }

    public BufferedImage createSnapshotImage(Object2DJPanel.ViewOptions opts) {
        return object2DOuterJPanel1.createSnapshotImage(opts);
    }

    public BufferedImage createSnapshotImage(Object2DJPanel.ViewOptions opts, Collection<? extends PhysicalItem> itemsToPaint) {
        return object2DOuterJPanel1.createSnapshotImage(opts, itemsToPaint);
    }

    @Override
    public List<PhysicalItem> getItems() {
        return object2DOuterJPanel1.getItems();
    }

    
    public List<PhysicalItem> getOutputItems() {
        return object2DOuterJPanel1.getOutputItems();
    }
    public void setViewLimits(double minX, double minY, double maxX, double maxY) {
        object2DOuterJPanel1.setViewLimits(minX, minY, maxX, maxY);
    }

    public void setSimSenseLimits(double minX, double minY, double maxX, double maxY) {
        object2DOuterJPanel1.setViewLimits(minX, minY, maxX, maxY);
    }
    
    @Override
    public void setItems(List<PhysicalItem> items) {
        object2DOuterJPanel1.setItems(items);
    }

    public void setPropertiesFile(File f) {
        object2DOuterJPanel1.setPropertiesFile(f);
    }

    public File getPropertiesFile() {
        return object2DOuterJPanel1.getPropertiesFile();
    }

    public XFutureVoid saveProperties() {
        return object2DOuterJPanel1.saveProperties();
    }

    @Nullable private volatile XFutureVoid loadPropertiesFuture=null;
    
    public XFutureVoid loadProperties() throws IOException {
        XFutureVoid ret = object2DOuterJPanel1.loadProperties();
        loadPropertiesFuture = ret;
        return ret;
    }

    public void setAprsSystem(AprsSystem aprsSystemInterface) {
        object2DOuterJPanel1.setAprsSystem(aprsSystemInterface);
    }

    public void setSlotOffsetProvider(SlotOffsetProvider slotOffsetProvider) {
        object2DOuterJPanel1.setSlotOffsetProvider(slotOffsetProvider);
    }

    @Nullable public AprsSystem getAprsSystem() {
        return object2DOuterJPanel1.getAprsSystem();
    }

    @Override
    public void takeSnapshot(File f, PoseType pose, String label) {
        object2DOuterJPanel1.takeSnapshot(f, pose, label);
    }

    @Override
    public void takeSnapshot(File f, PointType point, String label) {
        object2DOuterJPanel1.takeSnapshot(f, point, label);
    }

    @Override
    public void takeSnapshot(File f, PmCartesian point, String label) {
        object2DOuterJPanel1.takeSnapshot(f, point, label);
    }

    @Override
    public void takeSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint) {
        this.object2DOuterJPanel1.takeSnapshot(f, itemsToPaint);
    }

    @Override
    public void takeSnapshot(File f, @Nullable PoseType pose, @Nullable String label, int w, int h) {
        object2DOuterJPanel1.takeSnapshot(f, pose, label, w, h);
    }

    @Override
    public void takeSnapshot(File f, PointType point, String label, int w, int h) {
        object2DOuterJPanel1.takeSnapshot(f, point, label, w, h);
    }

    @Override
    public void takeSnapshot(File f, @Nullable PmCartesian point, @Nullable String label, int w, int h) {
        object2DOuterJPanel1.takeSnapshot(f, point, label, w, h);
    }

    @Override
    public void takeSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint, int w, int h) {
        this.object2DOuterJPanel1.takeSnapshot(f, itemsToPaint, w, h);
    }

    public boolean isSimulated() {
        return this.object2DOuterJPanel1.isSimulated();
    }

    public boolean isConnected() {
        return this.object2DOuterJPanel1.isConnected();
    }
    
    public int getPort() {
        return this.object2DOuterJPanel1.getPort();
    }

    @Override
    public void dispose() {
        Utils.runOnDispatchThread(() -> {
            super.dispose();
            this.object2DOuterJPanel1.dispose();
        });
    }

    public void setTrackCurrentPos(boolean v) {
        this.object2DOuterJPanel1.setTrackCurrentPos(v);
    }

    public void setSimulatedAndDisconnect() {
        this.object2DOuterJPanel1.setSimulated(true);
    }
}
