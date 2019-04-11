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
package aprs.conveyor;

import aprs.system.AprsSystem;
import crcl.ui.XFutureVoid;
import java.awt.HeadlessException;
import java.util.Map;
import org.checkerframework.checker.guieffect.qual.UIType;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@UIType
public class ConveyorVisJPanel extends javax.swing.JPanel {
    
    private AprsSystem clonedSystem;

    /**
     * Get the value of clonedSystem
     *
     * @return the value of clonedSystem
     */
    public AprsSystem getClonedSystem() {
        return clonedSystem;
    }

    public void disconnect() {
        outerConveyorSpeedControlJPanel1.disconnect();
    }
    /**
     * Set the value of clonedSystem
     *
     * @param clonedSystem new value of clonedSystem
     */
    public void setClonedSystem(AprsSystem clonedSystem) {
        this.clonedSystem = clonedSystem;
        object2DOuterJPanel1.setObjectPanelToClone(clonedSystem.getObjectViewPanel());
    }

    public void connectMasterOnDisplay() throws HeadlessException {
        outerConveyorSpeedControlJPanel1.connectMasterOnDisplay();
    }
    
    /**
     * Creates new form ConveyorVisJPanel
     */
    @SuppressWarnings("initialization")
    public ConveyorVisJPanel() {
        initComponents();
        outerConveyorSpeedControlJPanel1.addConveyorPositionListener(object2DOuterJPanel1::handleConveyorPositionUpdate);
        outerConveyorSpeedControlJPanel1.setItems(object2DOuterJPanel1.getItems());
        object2DOuterJPanel1.addSetItemsListener(outerConveyorSpeedControlJPanel1::setItems);
    }
    
    public boolean isSimulated() {
        return object2DOuterJPanel1.isSimulated() && outerConveyorSpeedControlJPanel1.isSimulated();
    }
    
    public double getEstimatedPosition() {
        return outerConveyorSpeedControlJPanel1.getEstimatedPosition();
    }
    
    public XFutureVoid nextTray() {
        return outerConveyorSpeedControlJPanel1.nextTray();
    }
    
    public XFutureVoid prevTray() {
        return outerConveyorSpeedControlJPanel1.prevTray();
    }
    
    public void mapToProperties(Map<String, String> map1) throws NumberFormatException {
        outerConveyorSpeedControlJPanel1.mapToProperties(map1);
    }
    
    public Map<String, String> propertiesToMap() {
        return outerConveyorSpeedControlJPanel1.propertiesToMap();
    }
    
    public XFutureVoid previousTray() {
        return outerConveyorSpeedControlJPanel1.previousTray();
    }
    
    public void setSimulated(boolean simulated) {
        object2DOuterJPanel1.setSimulated(simulated);
        outerConveyorSpeedControlJPanel1.setSimulated(simulated);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        outerConveyorSpeedControlJPanel1 = new aprs.conveyor.OuterConveyorSpeedControlJPanel();
        object2DOuterJPanel1 = new aprs.simview.Object2DOuterJPanel();

        object2DOuterJPanel1.setAutoscale(false);
        object2DOuterJPanel1.setIgnoreLosingItemsLists(false);
        object2DOuterJPanel1.setSimulated(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(outerConveyorSpeedControlJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(outerConveyorSpeedControlJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.simview.Object2DOuterJPanel object2DOuterJPanel1;
    private aprs.conveyor.OuterConveyorSpeedControlJPanel outerConveyorSpeedControlJPanel1;
    // End of variables declaration//GEN-END:variables
}
