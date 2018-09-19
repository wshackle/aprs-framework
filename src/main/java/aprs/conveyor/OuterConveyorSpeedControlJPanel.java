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
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
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
        popupMenu = new JPopupMenu();
        checkBoxMenuItemConnected = new JCheckBoxMenuItem("Connected");
        popupMenu.add(checkBoxMenuItemConnected);
        checkBoxMenuItemSimulated = new JCheckBoxMenuItem("Simulated");
        popupMenu.add(checkBoxMenuItemSimulated);
        JMenuItem propertiesMenuItem = new JMenuItem("Properties");
        propertiesMenuItem.addActionListener(e -> {
            editProperties();
        });
        popupMenu.add(propertiesMenuItem);
        JMenuItem cancelMenuItem = new JMenuItem("Cancel");
        checkBoxMenuItemConnected.addActionListener(e -> {
            handleConnectedCheckBoxEvent();
        });
        cancelMenuItem.addActionListener(e -> popupMenu.setVisible(false));
        popupMenu.add(cancelMenuItem);
        positionUpdateTimer = new Timer(250, e -> {
            handlePostionUpateTimerEvent();
        });
        estimatedPosition = conveyorSpeedJPanel1.getEstimatedPosition();
    }

    @Nullable
    private JFrame getOwner() {
        Component c = this;
        while (c != null) {
            c = c.getParent();
            if (c instanceof JFrame) {
                return (JFrame) c;
            }
        }
        return null;
    }

    public double getMaxPosition() {
        return conveyorSpeedJPanel1.getMaxPosition();
    }

    /**
     * Set the value of maxPosition
     *
     * @param maxPosition new value of maxPosition
     */
    public void setMaxPosition(double maxPosition) {
        conveyorSpeedJPanel1.setMaxPosition(maxPosition);
    }

    /**
     * Get the value of minPosition
     *
     * @return the value of minPosition
     */
    public double getMinPosition() {
        return conveyorSpeedJPanel1.getMinPosition();
    }

    /**
     * Set the value of minPosition
     *
     * @param minPosition new value of minPosition
     */
    public void setMinPosition(double minPosition) {
        conveyorSpeedJPanel1.setMinPosition(minPosition);
    }

    private void editProperties() {
        Map<String, String> map0 = propertiesToMap();
        Map<String, String> map1 = EditPropertiesJPanel.editProperties(getOwner(), "Properties", true, map0);
        if (null != map1) {
            mapToProperties(map1);
        }
    }

    private void mapToProperties(Map<String, String> map1) throws NumberFormatException {
        setScale(Double.parseDouble(map1.get(SCALE)));
        setAxisX(Double.parseDouble(map1.get(AXIS_X)));
        setAxisY(Double.parseDouble(map1.get(AXIS_Y)));
        setMaxPosition(Double.parseDouble(map1.get(MAX_POSITION)));
        setMinPosition(Double.parseDouble(map1.get(MIN_POSITION)));
    }

    private Map<String, String> propertiesToMap() {
        Map<String, String> map0 = new TreeMap<>();
        map0.put(SCALE, Double.toString(getScale()));
        map0.put(AXIS_X, Double.toString(getAxisX()));
        map0.put(AXIS_Y, Double.toString(getAxisY()));
        map0.put(MAX_POSITION, Double.toString(getMaxPosition()));
        map0.put(MIN_POSITION, Double.toString(getMinPosition()));
        return map0;
    }
    private static final String MIN_POSITION = "MinPosition";
    private static final String MAX_POSITION = "MaxPosition";
    private static final String AXIS_Y = "AxisY";
    private static final String AXIS_X = "AxisX";
    private static final String SCALE = "Scale";

    private void handlePostionUpateTimerEvent() {
        updateEstimatedPosition();
        notifyConveyorStateListeners();
    }

    private void handleConnectedCheckBoxEvent() throws HeadlessException {
        if (null != master) {
            master.disconnect();
            master = null;
        }
        if (checkBoxMenuItemConnected.isSelected()) {
            connectMasterOnDisplay();
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

        conveyorSpeedJPanel1 = new aprs.conveyor.InnerConveyorSpeedJPanel();

        conveyorSpeedJPanel1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                conveyorSpeedJPanel1MouseDragged(evt);
            }
        });
        conveyorSpeedJPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                conveyorSpeedJPanel1MouseClicked(evt);
            }
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
            .addGap(0, 462, Short.MAX_VALUE)
        );
        conveyorSpeedJPanel1Layout.setVerticalGroup(
            conveyorSpeedJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 287, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(conveyorSpeedJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(conveyorSpeedJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    public XFutureVoid connectMaster() {
        return Utils.runOnDispatchThread(this::connectMasterOnDisplay);
    }

    private final JPopupMenu popupMenu;
    private final JCheckBoxMenuItem checkBoxMenuItemConnected;

    private final JCheckBoxMenuItem checkBoxMenuItemSimulated;

    public boolean isSimulated() {
        return checkBoxMenuItemSimulated.isSelected();
    }

    public void setSimulated(boolean simulated) {
        checkBoxMenuItemSimulated.setSelected(simulated);
    }

    private String modBusHost = "192.168.1.50";

    /**
     * Get the value of modBusHost
     *
     * @return the value of modBusHost
     */
    public String getModBusHost() {
        return modBusHost;
    }

    /**
     * Set the value of modBusHost
     *
     * @param modBusHost new value of modBusHost
     */
    public void setModBusHost(String modBusHost) {
        this.modBusHost = modBusHost;
    }

    private void connectMasterOnDisplay() throws HeadlessException {
        try {
            String newModBustHost = JOptionPane.showInputDialog(this, "ModBus Host:", modBusHost);
            if (newModBustHost != null && newModBustHost.length() > 0 && !newModBustHost.equals(modBusHost)) {
                setModBusHost(newModBustHost);
                modBusHost = newModBustHost;
            }
            master = new ModbusTCPMaster(modBusHost);
            master.connect();
            System.out.println("master.connect() succeeded : master=" + master);
        } catch (Exception ex) {
            Logger.getLogger(OuterConveyorSpeedControlJPanel.class.getName()).log(Level.SEVERE, null, ex);
            String message = ex.getMessage();
            if (null != message) {
                JOptionPane.showMessageDialog(this, message);
            }
        }
    }

    private void conveyorSpeedJPanel1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_conveyorSpeedJPanel1MousePressed
        if (evt.isPopupTrigger()) {
            popupMenu.show(this.conveyorSpeedJPanel1, evt.getX(), evt.getY());
        } else {
            setConveyorSpeedFromMouseEvent(evt);
        }
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
        if (forward && newSpeed > 0 && estimatedPosition >= getMaxPosition()) {
            newSpeed = 0;
        } else if (!forward && newSpeed > 0 && estimatedPosition <= getMinPosition()) {
            newSpeed = 0;
        }
        conveyorSpeedJPanel1.setCurrentSpeed(newSpeed);
        if (null != master) {
            try {
                master.writeSingleRegister(0x8001, new SimpleRegister(forward ? 1 : 0)); // set direction forward
                master.writeSingleRegister(0x8002, new SimpleRegister(newSpeed)); // set the speed, 0 = off, 32768 = max
                if (newSpeed > 0) {
                    master.writeSingleRegister(0x8000, new SimpleRegister(1)); // make it go
                } else {
                    master.writeSingleRegister(0x8000, new SimpleRegister(0)); // make it go
                }
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

    private void conveyorSpeedJPanel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_conveyorSpeedJPanel1MouseClicked
        if (evt.isPopupTrigger()) {
            popupMenu.show(this.conveyorSpeedJPanel1, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_conveyorSpeedJPanel1MouseClicked

    private double axisX = 0.0;

    /**
     * Get the value of axisX
     *
     * @return the value of axisX
     */
    public double getAxisX() {
        return axisX;
    }

    /**
     * Set the value of axisX
     *
     * @param axisX new value of axisX
     */
    public void setAxisX(double axisX) {
        this.axisX = axisX;
    }

    private double axisY = -1.0;

    /**
     * Get the value of axisY
     *
     * @return the value of axisY
     */
    public double getAxisY() {
        return axisY;
    }

    /**
     * Set the value of axisY
     *
     * @param axisY new value of axisY
     */
    public void setAxisY(double axisY) {
        this.axisY = axisY;
    }

    private void stopConveyor() {
        updateEstimatedPosition();
        stopConveyorNoPosEstimate();
    }

    private void stopConveyorNoPosEstimate() {
        conveyorSpeedJPanel1.setCurrentSpeed(0);
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

    private volatile double estimatedPosition;
    private volatile long lastEstimatedPositionTime = System.currentTimeMillis();
    private double scale = 0.00001;

    /**
     * Get the value of scale
     *
     * @return the value of scale
     */
    public double getScale() {
        return scale;
    }

    /**
     * Set the value of scale
     *
     * @param scale new value of scale
     */
    public void setScale(double scale) {
        this.scale = scale;
    }

    private synchronized void updateEstimatedPosition() {
        int speed = conveyorSpeedJPanel1.getCurrentSpeed();
        boolean forward = conveyorSpeedJPanel1.isForwardDirection();
        long time = System.currentTimeMillis();
        long timeDiff = time - lastEstimatedPositionTime;
        double scaledSpeed = speed * scale;
        double positionInc = ((forward ? 1 : -1) * scaledSpeed * timeDiff);

        if (Math.abs(positionInc) > 0.0001) {
            estimatedPosition += positionInc;
            conveyorSpeedJPanel1.setEstimatedPosition(estimatedPosition);
            lastEstimatedPositionTime = time;
            notifyConveyorPositionListeners();
        }
        lastEstimatedPositionTime = time;
        if (speed > 0) {
            if (forward && estimatedPosition > getMaxPosition()) {
                stopConveyorNoPosEstimate();
            } else if(!forward && estimatedPosition < getMinPosition()) {
                stopConveyorNoPosEstimate();
            }
        }

    }

    public double getEstimatedPosition() {
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

    private final ConcurrentLinkedDeque<Consumer<ConveyorPosition>> conveyorPositionListeners = new ConcurrentLinkedDeque<>();

    public void addConveyorPositionListener(Consumer<ConveyorPosition> listener) {
        conveyorPositionListeners.add(listener);
    }

    public void removerConveyorPositionListener(Consumer<ConveyorPosition> listener) {
        conveyorPositionListeners.remove(listener);
    }

    private void notifyConveyorPositionListeners() {
        ConveyorPosition cs = new ConveyorPosition(axisX * estimatedPosition, axisY * estimatedPosition);
        for (Consumer<ConveyorPosition> listener : conveyorPositionListeners) {
            listener.accept(cs);
        }
    }

    @Nullable
    private ModbusTCPMaster master;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.conveyor.InnerConveyorSpeedJPanel conveyorSpeedJPanel1;
    // End of variables declaration//GEN-END:variables
}
