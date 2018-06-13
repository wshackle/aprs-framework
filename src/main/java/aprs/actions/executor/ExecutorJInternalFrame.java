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
package aprs.actions.executor;

import aprs.system.AprsSystemInterface;
import aprs.database.DbSetupPublisher;
import aprs.database.PhysicalItem;
import aprs.actions.executor.CrclGenerator.PoseProvider;
import crcl.base.CRCLProgramType;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.ui.XFuture;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.swing.JMenu;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class ExecutorJInternalFrame extends javax.swing.JInternalFrame implements ExecutorDisplayInterface {

    /**
     * Creates new form ActionsToCrclJInternalFrame
     */
    @SuppressWarnings("initialization")
    public ExecutorJInternalFrame() {
        initComponents();
    }

    public void setForceFakeTakeFlag(boolean val) {
        this.executorJPanel1.setForceFakeTakeFlag(val);
    }

    public boolean getForceFakeTakeFlag() {
        return executorJPanel1.getForceFakeTakeFlag();
    }

    public void showPaused(boolean state) {
        executorJPanel1.showPaused(state);
    }

    public JMenu getToolMenu() {
        return executorJPanel1.getToolMenu();
    }
    
    /**
     * Get the value of externalGetPoseFunction
     *
     * @return the value of externalGetPoseFunction
     */
    @Nullable public PoseProvider getExternalPoseProvider() {
        return executorJPanel1.getExternalPoseProvider();
    }

    /**
     * Set the value of externalGetPoseFunction
     *
     * @param externalGetPoseProvider new value of externalGetPoseFunction
     */
    public void setExternalPoseProvider(PoseProvider externalGetPoseProvider) {
        this.executorJPanel1.setExternalPoseProvider(externalGetPoseProvider);
    }

    public void setLookForXYZ(double x, double y, double z) {
        executorJPanel1.setLookForXYZ(x,y,z);
    }

    public CRCLProgramType createLookForPartsProgram() {
        return executorJPanel1.createLookForPartsProgram();
    }

    /**
     * Modify the given pose by applying all of the currently added position
     * maps.
     *
     * @param poseIn the pose to correct or transform
     * @return pose after being corrected by all currently added position maps
     */
    public PoseType correctPose(PoseType poseIn) {
        return executorJPanel1.correctPose(poseIn);
    }

    public void reloadActionsFile() throws IOException {
        this.executorJPanel1.reloadActionsFile();
    }

    public void setReverseFlag(boolean reverseFlag) {
        this.executorJPanel1.setReverseFlag(reverseFlag);
    }

    /**
     * Apply inverses of currently added position maps in reverse order.
     *
     * @param ptIn point to reverse correction
     * @return point in original vision/database coordinates
     */
    public PointType reverseCorrectPoint(PointType ptIn) {
        return executorJPanel1.reverseCorrectPoint(ptIn);
    }

    public void abortProgram() {
        executorJPanel1.abortProgram();
    }

    public XFuture<Void> startSafeAbort(String name) {
        return this.executorJPanel1.startSafeAbort(name);
    }

    public int getCurrentActionIndex() {
        return this.executorJPanel1.getCurrentActionIndex();
    }

    public int getSafeAbortRequestCount() {
        return executorJPanel1.getSafeAbortRequestCount();
    }

    @Override
    public List<Action> getActionsList() {
        return this.executorJPanel1.getActionsList();
    }

    public boolean completeActionList(String comment, int startAbortCount) {
        return this.executorJPanel1.completeActionList(comment, startAbortCount);
    }

    public int getActionSetsCompleted() {
        return executorJPanel1.getActionSetsCompleted();
    }

    public void debugAction() {
        this.executorJPanel1.debugAction();
    }

//    public void pause() {
//        this.actionsToCrclJPanel1.pause();
//    }
    public String getErrorString() {
        return this.executorJPanel1.getErrorString();
    }

    public void setErrorString(@Nullable String errorString) {
        this.executorJPanel1.setErrorString(errorString);
    }

    public long incrementAndGetCommandId() {
        return this.executorJPanel1.incrementAndGetCommandId();
    }

    /**
     * Add a position map.
     *
     * The position map is similar to a transform in that it may offset
     * positions output by the executor but may also be used to change scaling
     * or correct for non uniform distortions from the sensor system or
     * imperfect kinematic functions in the robot. Multiple position maps may be
     * stacked to account for different sources of error or transformation.
     *
     * @param pm position map to be added
     */
    public void addPositionMap(PositionMap pm) {
        executorJPanel1.addPositionMap(pm);
    }

    /**
     * Remove a previously added position map.
     *
     * @param pm position map to be removed.
     */
    public void removePositionMap(PositionMap pm) {
        executorJPanel1.removePositionMap(pm);
    }

    public Map<String, String> getOptions() {
        return this.executorJPanel1.getTableOptions();
    }

    public void setOption(String key, String value) {
        this.executorJPanel1.setOption(key, value);
    }

    public void setToolHolderOperationEnabled(boolean enable) {
        executorJPanel1.setToolHolderOperationEnabled(enable);
    }

    public boolean isToolHolderOperationEnabled() {
        return executorJPanel1.isToolHolderOperationEnabled();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        executorJPanel1 = new aprs.actions.executor.ExecutorJPanel();

        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("PDDL Actions to CRCL (Executor)");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(executorJPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 775, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(executorJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 619, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void browseActionsFile() throws IOException {
        this.executorJPanel1.browseActionsFile();
    }

    public void loadActionsFile(File f) throws IOException {
        this.executorJPanel1.loadActionsFile(f, false);
    }

    public void loadActionsList(Iterable<Action> newActions) {
        this.executorJPanel1.loadActionsList(newActions);
    }

//    @Override
//    public void setActionsList(List<PddlAction> actionsList) {
//        this.actionsToCrclJPanel1.setActionsList(actionsList);
//    }
    @Override
    public void addAction(Action action) {
        this.executorJPanel1.addAction(action);
    }

    @Override
    public void processActions() {
        this.executorJPanel1.processActions();
    }

    public File getPropertiesFile() {
        return this.executorJPanel1.getPropertiesFile();
    }

    @Override
    public void setPropertiesFile(File propertiesFile) {
        this.executorJPanel1.setPropertiesFile(propertiesFile);
    }

    @Override
    public void saveProperties() {
        this.executorJPanel1.saveProperties();
    }

    public XFuture<Boolean> startActions() {
        return executorJPanel1.startActions();
    }

    public boolean isDoingActions() {
        return executorJPanel1.isDoingActions();
    }

    public boolean doActions(String comment, int safeAbortCount) {
        return executorJPanel1.doActions(comment, safeAbortCount);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.actions.executor.ExecutorJPanel executorJPanel1;
    // End of variables declaration//GEN-END:variables

    @Override
    public void loadProperties() throws IOException {
        this.executorJPanel1.loadProperties();
    }

    @Override
    public void autoResizeTableColWidthsPddlOutput() {
        this.executorJPanel1.autoResizeTableColWidthsPddlOutput();
    }

    public void refresh() {
        executorJPanel1.refresh();
    }

    @Override
    public void close() {
        executorJPanel1.close();
        this.setVisible(false);
    }

    public void setDbSetupSupplier(Callable<DbSetupPublisher> dbSetupSupplier) {
        executorJPanel1.setDbSetupSupplier(dbSetupSupplier);
    }

    @Override
    public void setAprsSystemInterface(AprsSystemInterface aprsSystemInterface) {
        executorJPanel1.setAprsSystemInterface(aprsSystemInterface);
    }

    @Override
    public AprsSystemInterface getAprsSystemInterface() {
        return executorJPanel1.getAprsSystemInterface();
    }

    public boolean readyForNewActionsList() {
        return executorJPanel1.readyForNewActionsList();
    }

    public void warnIfNewActionsNotReady() {
        executorJPanel1.warnIfNewActionsNotReady();
    }

    @Override
    public void clearActionsList() {
        executorJPanel1.clearActionsList();
    }
    
    public List<PhysicalItem> getAvailableToolHolders() {
        return executorJPanel1.getAvailableToolHolders();
    }
}
