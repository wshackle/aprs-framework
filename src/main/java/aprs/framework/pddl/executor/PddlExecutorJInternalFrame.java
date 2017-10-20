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
package aprs.framework.pddl.executor;

import aprs.framework.AprsJFrame;
import aprs.framework.PddlAction;
import aprs.framework.database.DbSetupPublisher;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.ui.XFuture;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PddlExecutorJInternalFrame extends javax.swing.JInternalFrame implements PddlExecutorDisplayInterface {

    /**
     * Creates new form ActionsToCrclJInternalFrame
     */
    public PddlExecutorJInternalFrame() {
        initComponents();
    }

    public void setForceFakeTakeFlag(boolean val) {
        this.actionsToCrclJPanel1.setForceFakeTakeFlag(val);
    }

    public boolean getForceFakeTakeFlag() {
        return actionsToCrclJPanel1.getForceFakeTakeFlag();
    }

//    public void runProgramCompleteRunnables() {
//        this.actionsToCrclJPanel1.runProgramCompleteRunnables();
//    }
//    public XFuture<Boolean> checkSafeAbortAsync(Supplier<XFuture<Boolean>> supplier) {
//        return actionsToCrclJPanel1.checkSafeAbortAsync(supplier);
//    }
    public XFuture<Boolean> startLookForParts() {
        return actionsToCrclJPanel1.lookForParts();
    }

    /**
     * Modify the given pose by applying all of the currently added position
     * maps.
     *
     * @param poseIn the pose to correct or transform
     * @return pose after being corrected by all currently added position maps
     */
    public PoseType correctPose(PoseType poseIn) {
        return actionsToCrclJPanel1.correctPose(poseIn);
    }

    public void reloadActionsFile() throws IOException {
        this.actionsToCrclJPanel1.reloadActionsFile();
    }

    public void setReverseFlag(boolean reverseFlag) {
        this.actionsToCrclJPanel1.setReverseFlag(reverseFlag);
    }

    /**
     * Apply inverses of currently added position maps in reverse order.
     *
     * @param ptIn point to reverse correction
     * @return point in original vision/database coordinates
     */
    public PointType reverseCorrectPoint(PointType ptIn) {
        return actionsToCrclJPanel1.reverseCorrectPoint(ptIn);
    }

    public void abortProgram() {
        actionsToCrclJPanel1.abortProgram();
    }

    public XFuture<Void> startSafeAbort(String name) {
        return this.actionsToCrclJPanel1.startSafeAbort(name);
    }

    public int getCurrentActionIndex() {
        return this.actionsToCrclJPanel1.getCurrentActionIndex();
    }
    
    public int getSafeAbortRequestCount() {
        return actionsToCrclJPanel1.getSafeAbortRequestCount();
    }
    

    @Override
    public List<PddlAction> getActionsList() {
        return this.actionsToCrclJPanel1.getActionsList();
    }

    public XFuture<Void> continueActionList() {
        return this.actionsToCrclJPanel1.continueActionList();
    }

    public boolean completeActionList(String comment,int startAbortCount) {
        return this.actionsToCrclJPanel1.completeActionList(comment,startAbortCount);
    }
    
    public int getActionSetsCompleted() {
        return actionsToCrclJPanel1.getActionSetsCompleted();
    }

    public void debugAction() {
        this.actionsToCrclJPanel1.debugAction();
    }

    public void pause() {
        this.actionsToCrclJPanel1.pause();
    }

    public String getErrorString() {
        return this.actionsToCrclJPanel1.getErrorString();
    }

    public void setErrorString(String errorString) {
        this.actionsToCrclJPanel1.setErrorString(errorString);
    }

    public long incrementAndGetCommandId() {
        return this.actionsToCrclJPanel1.incrementAndGetCommandId();
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
        actionsToCrclJPanel1.addPositionMap(pm);
    }

    /**
     * Remove a previously added position map.
     *
     * @param pm position map to be removed.
     */
    public void removePositionMap(PositionMap pm) {
        actionsToCrclJPanel1.removePositionMap(pm);
    }

    public Map<String, String> getOptions() {
        return this.actionsToCrclJPanel1.getTableOptions();
    }

    public void setOption(String key, String value) {
        this.actionsToCrclJPanel1.setOption(key, value);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        actionsToCrclJPanel1 = new aprs.framework.pddl.executor.PddlExecutorJPanel();

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
                .addComponent(actionsToCrclJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 775, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(actionsToCrclJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 619, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void browseActionsFile() throws IOException {
        this.actionsToCrclJPanel1.browseActionsFile();
    }

    public void loadActionsFile(File f) throws IOException {
        this.actionsToCrclJPanel1.loadActionsFile(f);
    }

//    @Override
//    public void setActionsList(List<PddlAction> actionsList) {
//        this.actionsToCrclJPanel1.setActionsList(actionsList);
//    }

    @Override
    public void addAction(PddlAction action) {
        this.actionsToCrclJPanel1.addAction(action);
    }

    @Override
    public void processActions() {
        this.actionsToCrclJPanel1.processActions();
    }

    @Override
    public File getPropertiesFile() {
        return this.actionsToCrclJPanel1.getPropertiesFile();
    }

    @Override
    public void setPropertiesFile(File propertiesFile) {
        this.actionsToCrclJPanel1.setPropertiesFile(propertiesFile);
    }

    @Override
    public void saveProperties() throws IOException {
        this.actionsToCrclJPanel1.saveProperties();
    }

    public XFuture<Boolean> startActions() {
        return actionsToCrclJPanel1.startActions();
    }

    public boolean isDoingActions() {
        return actionsToCrclJPanel1.isDoingActions();
    }
    
    public boolean doActions(String comment, int safeAbortCount) {
        return actionsToCrclJPanel1.doActions(comment,safeAbortCount);
    }


    public void setGeneateCrclService(ExecutorService geneateCrclService) {
        actionsToCrclJPanel1.setGenerateCrclService(geneateCrclService);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.framework.pddl.executor.PddlExecutorJPanel actionsToCrclJPanel1;
    // End of variables declaration//GEN-END:variables

    @Override
    public void loadProperties() throws IOException {
        this.actionsToCrclJPanel1.loadProperties();
    }

    @Override
    public void autoResizeTableColWidthsPddlOutput() {
        this.actionsToCrclJPanel1.autoResizeTableColWidthsPddlOutput();
    }

    public void refresh() {
        actionsToCrclJPanel1.refresh();
    }

    @Override
    public void close() throws Exception {
        actionsToCrclJPanel1.close();
        this.setVisible(false);
    }

    public Callable<DbSetupPublisher> getDbSetupSupplier() {
        return actionsToCrclJPanel1.getDbSetupSupplier();
    }

    public void setDbSetupSupplier(Callable<DbSetupPublisher> dbSetupSupplier) {
        actionsToCrclJPanel1.setDbSetupSupplier(dbSetupSupplier);
    }

    @Override
    public void setAprsJFrame(AprsJFrame aprsJFrame) {
        actionsToCrclJPanel1.setAprsJFrame(aprsJFrame);
    }

    @Override
    public AprsJFrame getAprsJFrame() {
        return actionsToCrclJPanel1.getAprsJFrame();
    }

    @Override
    public void clearActionsList() {
        actionsToCrclJPanel1.clearActionsList();
    }
}
