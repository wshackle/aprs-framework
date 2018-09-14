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

import aprs.misc.Utils;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import crcl.ui.XFutureVoid;
import java.awt.HeadlessException;
import java.awt.event.MouseEvent;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import org.checkerframework.checker.guieffect.qual.UIType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author shackle
 */
@UIType
public class OuterConveyorSpeedControlJPanel extends javax.swing.JPanel {

    /**
     * Creates new form ConveyorSpeedControlJPanel
     */
    @SuppressWarnings("initialization")
    public OuterConveyorSpeedControlJPanel() {
        initComponents();
        positionUpdateTimer = new Timer(500, e -> {
            updateEstimatedPosition();
            notifyConveyorStateListeners();
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        conveyorSpeedJPanel1 = new aprs.conveyor.InnerConveyorSpeedJPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldModBusHost = new javax.swing.JTextField();
        jCheckBoxConnect = new javax.swing.JCheckBox();
        jCheckBoxSimulated = new javax.swing.JCheckBox();

        conveyorSpeedJPanel1.setHorizontal(true);
        conveyorSpeedJPanel1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                conveyorSpeedJPanel1MouseDragged(evt);
            }
        });
        conveyorSpeedJPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                conveyorSpeedJPanel1MouseExited(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                conveyorSpeedJPanel1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                conveyorSpeedJPanel1MouseReleased(evt);
            }
        });

        javax.swing.GroupLayout conveyorSpeedJPanel1Layout = new javax.swing.GroupLayout(conveyorSpeedJPanel1);
        conveyorSpeedJPanel1.setLayout(conveyorSpeedJPanel1Layout);
        conveyorSpeedJPanel1Layout.setHorizontalGroup(
            conveyorSpeedJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        conveyorSpeedJPanel1Layout.setVerticalGroup(
            conveyorSpeedJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        jLabel1.setText("ModBus Host: ");

        jTextFieldModBusHost.setText("192.168.1.50");

        jCheckBoxConnect.setText("Connected");
        jCheckBoxConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxConnectActionPerformed(evt);
            }
        });

        jCheckBoxSimulated.setText("Similated");
        jCheckBoxSimulated.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxSimulatedActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextFieldModBusHost, javax.swing.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxConnect)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxSimulated))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(conveyorSpeedJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(conveyorSpeedJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldModBusHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxConnect)
                    .addComponent(jLabel1)
                    .addComponent(jCheckBoxSimulated))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxConnectActionPerformed
        stopConveyor();
        if (null != master) {
            master.disconnect();
            master = null;
        }
        if (jCheckBoxConnect.isSelected()) {
            connectMasterOnDisplay();
        }
    }//GEN-LAST:event_jCheckBoxConnectActionPerformed

    public XFutureVoid connectMaster() {
        return Utils.runOnDispatchThread(this::connectMasterOnDisplay);
    }

    private void connectMasterOnDisplay() throws HeadlessException {
        try {
            master = new ModbusTCPMaster(jTextFieldModBusHost.getText());
            master.connect();
            System.out.println("master.connect() succeeded : master=" + master);
        } catch (Exception ex) {
            Logger.getLogger(OuterConveyorSpeedControlJPanel.class.getName()).log(Level.SEVERE, null, ex);
            String message = ex.getMessage();
            if(null != message) {
                JOptionPane.showMessageDialog(this, message);
            }
        }
    }

    private void conveyorSpeedJPanel1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_conveyorSpeedJPanel1MousePressed
        setConveyorSpeedFromMouseEvent(evt);
    }//GEN-LAST:event_conveyorSpeedJPanel1MousePressed

    private void setConveyorSpeedFromMouseEvent(MouseEvent evt) {
        updateEstimatedPosition();
        notifyConveyorStateListeners();
        if (!positionUpdateTimer.isRunning()) {
            positionUpdateTimer.start();
        }
        boolean forward = conveyorSpeedJPanel1.isPointForward(evt.getPoint());
        conveyorSpeedJPanel1.setForwardDirection(forward);
        int newSpeed = conveyorSpeedJPanel1.pointToSpeed(evt.getPoint());
        conveyorSpeedJPanel1.setCurrentSpeed(newSpeed);
        if (null != master) {
            try {
                master.writeSingleRegister(0x8001, new SimpleRegister(forward ? 1 : 0)); // set direction forward
                master.writeSingleRegister(0x8002, new SimpleRegister(newSpeed)); // set the speed, 0 = off, 32768 = max
                master.writeSingleRegister(0x8000, new SimpleRegister(1)); // make it go
            } catch (ModbusException ex) {
                Logger.getLogger(OuterConveyorSpeedControlJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void conveyorSpeedJPanel1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_conveyorSpeedJPanel1MouseReleased
        stopConveyor();
    }//GEN-LAST:event_conveyorSpeedJPanel1MouseReleased

    private void conveyorSpeedJPanel1MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_conveyorSpeedJPanel1MouseExited
        stopConveyor();
    }//GEN-LAST:event_conveyorSpeedJPanel1MouseExited

    private void conveyorSpeedJPanel1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_conveyorSpeedJPanel1MouseDragged
        setConveyorSpeedFromMouseEvent(evt);
    }//GEN-LAST:event_conveyorSpeedJPanel1MouseDragged

    private void jCheckBoxSimulatedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSimulatedActionPerformed
        stopConveyor();
        if (null != master) {
            master.disconnect();
            master = null;
        }
        boolean simulatedSelected = jCheckBoxSimulated.isSelected();
        jCheckBoxConnect.setSelected(false);
        jCheckBoxConnect.setEnabled(!simulatedSelected);
    }//GEN-LAST:event_jCheckBoxSimulatedActionPerformed

    private void stopConveyor() {
        updateEstimatedPosition();
        conveyorSpeedJPanel1.setCurrentSpeed(0);
//        positionUpdateTimer.stop();
        if (null != master) {
            try {
                master.writeSingleRegister(0x8000, new SimpleRegister(0)); // make it NOT go
                master.writeSingleRegister(0x8002, new SimpleRegister(0)); // set the speed, 0 = off, 32768 = max
            } catch (ModbusException ex) {
                Logger.getLogger(OuterConveyorSpeedControlJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private final javax.swing.Timer positionUpdateTimer;

    private volatile long estimatedPosition = 0;
    private volatile long lastEstimatedPositionTime = System.currentTimeMillis();

    private double speedScale = 0.001;

    /**
     * Get the value of speedScale
     *
     * @return the value of speedScale
     */
    public double getSpeedScale() {
        return speedScale;
    }

    /**
     * Set the value of speedScale
     *
     * @param speedScale new value of speedScale
     */
    public void setSpeedScale(double speedScale) {
        this.speedScale = speedScale;
    }

    private synchronized void updateEstimatedPosition() {
        int speed = conveyorSpeedJPanel1.getCurrentSpeed();
        boolean forward = conveyorSpeedJPanel1.isForwardDirection();
        long time = System.currentTimeMillis();
        long timeDiff = time - lastEstimatedPositionTime;
        double scaledSpeed = speed * speedScale;
        long positionInc = (long) ((forward ? 1 : -1) * scaledSpeed * timeDiff);
        estimatedPosition += positionInc;
        conveyorSpeedJPanel1.setEstimatedPosition(estimatedPosition);
        lastEstimatedPositionTime = time;
    }

    public long getEstimatedPosition() {
        return estimatedPosition;
    }

    private final ConcurrentLinkedDeque<Consumer<ConveyorState>> conveyorStateListeners = new ConcurrentLinkedDeque<>();

    public void addConveyorStateListener(Consumer<ConveyorState> listener) {
        conveyorStateListeners.add(listener);
    }

    public void removerConveyorStateListener(Consumer<ConveyorState> listener) {
        conveyorStateListeners.remove(listener);
    }

    private void notifyConveyorStateListeners() {
        ConveyorState cs = new ConveyorState(conveyorSpeedJPanel1.isForwardDirection(), conveyorSpeedJPanel1.getCurrentSpeed());
        for (Consumer<ConveyorState> listener : conveyorStateListeners) {
            listener.accept(cs);
        }
    }

    @Nullable
    private ModbusTCPMaster master;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.conveyor.InnerConveyorSpeedJPanel conveyorSpeedJPanel1;
    private javax.swing.JCheckBox jCheckBoxConnect;
    private javax.swing.JCheckBox jCheckBoxSimulated;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextField jTextFieldModBusHost;
    // End of variables declaration//GEN-END:variables
}
