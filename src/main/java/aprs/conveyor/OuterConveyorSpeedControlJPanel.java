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

import aprs.database.PhysicalItem;
import aprs.misc.Utils;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import crcl.ui.misc.MultiLineStringJPanel;
import crcl.utils.XFutureVoid;
import org.checkerframework.checker.guieffect.qual.UIType;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static aprs.misc.AprsCommonLogger.println;
import crcl.ui.misc.NotificationsJPanel;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@UIType
@SuppressWarnings("serial")
public class OuterConveyorSpeedControlJPanel extends javax.swing.JPanel {

    /**
     * Creates new form ConveyorSpeedControlJPanel
     */
    @SuppressWarnings({"nullness","initialization"})
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
        checkBoxMenuItemConnected.addActionListener(e -> {
            handleConnectedCheckBoxEvent();
        });
        JMenuItem cancelMenuItem = new JMenuItem("Cancel");
        cancelMenuItem.addActionListener(e -> popupMenu.setVisible(false));
        popupMenu.add(cancelMenuItem);

        positionUpdateTimer = new Timer(updateTimerPeriod, e -> {
            handlePostionUpateTimerEvent();
        });
    }

    /**
     * Get the value of goalPostion
     *
     * @return the value of goalPostion
     */
    public double getGoalPosition() {
        return conveyorSpeedJPanel1.getGoalPosition();
    }

    /**
     * Get the value of goalSet
     *
     * @return the value of goalSet
     */
    public boolean isGoalSet() {
        return conveyorSpeedJPanel1.isGoalSet();
    }

    /**
     * Set the value of goalSet
     *
     * @param goalSet new value of goalSet
     */
    public void setGoalSet(boolean goalSet) {
        conveyorSpeedJPanel1.setGoalSet(goalSet);
    }

    /**
     * Set the value of goalPostion
     *
     * @param goalPosition new value of goalPostion
     */
    public void setGoalPosition(double goalPosition) {
        if (goalPosition > conveyorSpeedJPanel1.getMaxPosition()) {
            stopConveyorNoPosEstimate();
            setGoalSet(false);
            throw new IllegalStateException("goalPosition > conveyorSpeedJPanel1.getMaxPosition() : goalPostion=" + goalPosition + ", conveyorSpeedJPanel1.getMaxPosition()=" + conveyorSpeedJPanel1.getMaxPosition());
        }
        if (goalPosition < conveyorSpeedJPanel1.getMinPosition()) {
            stopConveyorNoPosEstimate();
            setGoalSet(false);
            throw new IllegalStateException("goalPosition < conveyorSpeedJPanel1.getMinPosition() : goalPostion=" + goalPosition + ", conveyorSpeedJPanel1.getMinPosition()=" + conveyorSpeedJPanel1.getMinPosition());
        }
        conveyorSpeedJPanel1.setGoalPosition(goalPosition);
    }

    private volatile double trayDiff = 300.0;

    public double getTrayDiff() {
        return trayDiff;
    }

    public void setTrayDiff(double trayDiff) {
        this.trayDiff = trayDiff;
    }

    private volatile long nextPrevTrayFutureStartTime = -1;

    public XFutureVoid previousTray() {
        println("previosTray: pos=" + getEstimatedPosition());
        setGoalSet(false);
        updateEstimatedPosition();
        setGoalPosition(getEstimatedPosition() - trayDiff);
        setGoalSet(true);
        println("previosTray: goal=" + getGoalPosition());
        setSpeedAndDirection(conveyorSpeedJPanel1.getMaxSpeed() / 2, false);
        final XFutureVoid nextPrevTrayFutureFinal = nextPrevTrayFuture;
        if (null != nextPrevTrayFutureFinal) {
            if (!nextPrevTrayFutureFinal.isDone()) {
                Thread.dumpStack();
                System.err.println("Cancelling nextPrevTrayFuture="+nextPrevTrayFutureFinal);
                nextPrevTrayFutureFinal.cancelAll(true);
            }
        }
        nextPrevTrayFutureStartTime = System.currentTimeMillis();
        nextPrevTrayFuture = new XFutureVoid("nextTray");
        return nextPrevTrayFuture;
    }

      private volatile @Nullable  XFutureVoid nextPrevTrayFuture = null;

    public XFutureVoid nextTray() {
//        computeTrayDiff();
        println("nextTray: pos=" + getEstimatedPosition());
        setGoalSet(false);
        updateEstimatedPosition();
        setGoalPosition(getEstimatedPosition() + trayDiff);
        println("nextTray: goal=" + getGoalPosition());
        setGoalSet(true);
        setSpeedAndDirection(conveyorSpeedJPanel1.getMaxSpeed() / 2, true);
        final XFutureVoid nextPrevTrayFutureFinal = nextPrevTrayFuture;
        if (null != nextPrevTrayFutureFinal) {
           if (!nextPrevTrayFutureFinal.isDone()) {
                Thread.dumpStack();
                System.err.println("Cancelling nextPrevTrayFuture="+nextPrevTrayFutureFinal);
                nextPrevTrayFutureFinal.cancelAll(true);
            }
        }
        nextPrevTrayFutureStartTime = System.currentTimeMillis();
        nextPrevTrayFuture = new XFutureVoid("nextTray");
        return nextPrevTrayFuture;
    }

    public XFutureVoid prevTray() {
//        computeTrayDiff();
        setGoalSet(false);
        updateEstimatedPosition();
        setGoalPosition(getEstimatedPosition() - trayDiff);
        setGoalSet(true);
        setSpeedAndDirection(conveyorSpeedJPanel1.getMaxSpeed() / 2, false);
        final XFutureVoid nextPrevTrayFutureFinal = nextPrevTrayFuture;
        if (null != nextPrevTrayFutureFinal) {
            if (!nextPrevTrayFutureFinal.isDone()) {
                Thread.dumpStack();
                System.err.println("Cancelling nextPrevTrayFuture="+nextPrevTrayFutureFinal);
                nextPrevTrayFutureFinal.cancelAll(true);
            }
        }
        nextPrevTrayFutureStartTime = System.currentTimeMillis();
        nextPrevTrayFuture = new XFutureVoid("prevTray");
        return nextPrevTrayFuture;
    }

     private  @Nullable  JFrame getOwner() {
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

    private static double parseDouble(@Nullable String txt, double defaultVal) {
        if (txt == null || txt.length() < 1) {
            return defaultVal;
        }
        return Double.parseDouble(txt);
    }

//    private static long parseLong(@Nullable String txt, long defaultVal) {
//        if (txt == null || txt.length() < 1) {
//            return defaultVal;
//        }
//        return Long.parseLong(txt);
//    }

    private static int parseInt(@Nullable String txt, int defaultVal) {
        if (txt == null || txt.length() < 1) {
            return defaultVal;
        }
        return Integer.parseInt(txt);
    }

    public void mapToProperties(Map<String, String> map1) throws NumberFormatException {
        setScale(parseDouble(map1.get(SCALE), getScale()));
        setAxisX(parseDouble(map1.get(AXIS_X), getAxisX()));
        setAxisY(parseDouble(map1.get(AXIS_Y), getAxisY()));
        setMaxPosition(parseDouble(map1.get(MAX_POSITION), getMaxPosition()));
        setEstimatedPosition(parseDouble(map1.get(CURRENT_POSITION), getEstimatedPosition()));

        setMinPosition(parseDouble(map1.get(MIN_POSITION), getMinPosition()));
        double g = parseDouble(map1.get(GOAL_POSITION), getGoalPosition());
        setGoalPosition(Math.min(getMaxPosition(), Math.max(g, getMinPosition())));
        setTrayDiff(parseDouble(map1.get(TRAY_DIFF), getTrayDiff()));
        setUpdateTimerPeriod(parseInt(map1.get(UPDATE_TIMER_PERIOD), getUpdateTimerPeriod()));
        setMaxAssumedTimeDiff(parseInt(map1.get(MAX_ASSUMED_TIME_DIFF), getMaxAssumedTimeDiff()));
        String modbusHostName = map1.get(MODBUS_HOST);
        if (null != modbusHostName) {
            setModBusHost(modbusHostName);
        }
        String nextDelayMillisString = map1.get(NEXT_DELAY_MILLIS);
        if (null != nextDelayMillisString) {
            setNextDelayMillis(Integer.parseInt(nextDelayMillisString));
        }
        String maxSpeedString = map1.get(MAX_SPEED);
        if (null != maxSpeedString) {
            setMaxSpeed(Integer.parseInt(maxSpeedString));
        }
    }

    public Map<String, String> propertiesToMap() {
        Map<String, String> map0 = new TreeMap<>();
        map0.put(SCALE, Double.toString(getScale()));
        map0.put(AXIS_X, Double.toString(getAxisX()));
        map0.put(AXIS_Y, Double.toString(getAxisY()));
        map0.put(MAX_POSITION, Double.toString(getMaxPosition()));
        map0.put(MIN_POSITION, Double.toString(getMinPosition()));
        map0.put(CURRENT_POSITION, Double.toString(getEstimatedPosition()));
        map0.put(GOAL_POSITION, Double.toString(getGoalPosition()));
        map0.put(TRAY_DIFF, Double.toString(getTrayDiff()));
        map0.put(MAX_ASSUMED_TIME_DIFF, Integer.toString(getMaxAssumedTimeDiff()));
        map0.put(UPDATE_TIMER_PERIOD, Integer.toString(getUpdateTimerPeriod()));
        map0.put(MODBUS_HOST, getModBusHost());
        map0.put(NEXT_DELAY_MILLIS, Integer.toString(getNextDelayMillis()));
        map0.put(MAX_SPEED, Integer.toString(getMaxSpeed()));
        return map0;
    }

    private static final String MODBUS_HOST = "modbusHost";
    private static final String TRAY_DIFF = "trayDiff";
    private static final String MAX_ASSUMED_TIME_DIFF = "maxAssumedTimeDiff";
    private static final String UPDATE_TIMER_PERIOD = "updateTimerPeriod";
    private static final String MIN_POSITION = "MinPosition";
    private static final String MAX_POSITION = "MaxPosition";
    private static final String CURRENT_POSITION = "CurrentPosition";
    private static final String GOAL_POSITION = "GoalPosition";

    private static final String AXIS_Y = "AxisY";
    private static final String AXIS_X = "AxisX";
    private static final String SCALE = "Scale";
    private static final String NEXT_DELAY_MILLIS = "nextDelayMillis";
    private static final String MAX_SPEED = "maxSpeed";

    private void handlePostionUpateTimerEvent() {
        try {
            updateEstimatedPosition();
            notifyConveyorStateListeners();

        } catch (Exception ex) {
            if (null != positionUpdateTimer) {
                positionUpdateTimer.stop();
            }
            Logger.getLogger(OuterConveyorSpeedControlJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    public void disconnect() {
        checkBoxMenuItemConnected.setSelected(false);
        stopConveyorNoPosEstimate();
        if (null != master) {
            master.disconnect();
            master = null;
        }
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
        jButtonNextTray = new javax.swing.JButton();
        jButtonPrevTray = new javax.swing.JButton();

        conveyorSpeedJPanel1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                conveyorSpeedJPanel1MouseDragged(evt);
            }
        });
        conveyorSpeedJPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                conveyorSpeedJPanel1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                conveyorSpeedJPanel1MouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                conveyorSpeedJPanel1MouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                conveyorSpeedJPanel1MouseExited(evt);
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
            .addGap(0, 259, Short.MAX_VALUE)
        );

        jButtonNextTray.setText("Next Tray");
        jButtonNextTray.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNextTrayActionPerformed(evt);
            }
        });

        jButtonPrevTray.setText("Prev Tray");
        jButtonPrevTray.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPrevTrayActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(conveyorSpeedJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButtonPrevTray)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonNextTray)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(conveyorSpeedJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonPrevTray)
                    .addComponent(jButtonNextTray))
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

    public void connectMasterOnDisplay() throws HeadlessException {
        try {
            if (null == master) {
                String newModBustHost = JOptionPane.showInputDialog(this, "Conveyor ModBus Host:", modBusHost);
                if (newModBustHost != null && newModBustHost.length() > 0 && !newModBustHost.equals(modBusHost)) {
                    setModBusHost(newModBustHost);
                    modBusHost = newModBustHost;
                }
                master = new ModbusTCPMaster(modBusHost);
                master.connect();
                println("master.connect() succeeded : master=" + master);
            }
        } catch (Exception ex) {
            Logger.getLogger(OuterConveyorSpeedControlJPanel.class.getName()).log(Level.SEVERE, null, ex);
            String message = ex.getMessage();
            if (null != message) {
                NotificationsJPanel.showText(message);
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

        boolean forward = conveyorSpeedJPanel1.isPointForward(evt.getPoint());
        conveyorSpeedJPanel1.setForwardDirection(forward);
        int newSpeed = conveyorSpeedJPanel1.pointToSpeed(evt.getPoint());
        double estimatedPosition = getEstimatedPosition();
        if (forward && newSpeed > 0 && estimatedPosition >= getMaxPosition()) {
            newSpeed = 0;
        } else if (!forward && newSpeed > 0 && estimatedPosition <= getMinPosition()) {
            newSpeed = 0;
        }
        setSpeedAndDirection(newSpeed, forward);
    }

    private void setSpeedAndDirection(int newSpeed, boolean forward) {
        conveyorSpeedJPanel1.setCurrentSpeed(newSpeed);
        conveyorSpeedJPanel1.setForwardDirection(forward);
        if (newSpeed > 0) {
            if (!positionUpdateTimer.isRunning()) {
                positionUpdateTimer.start();
            }
        } else if (positionUpdateTimer.isRunning()) {
            positionUpdateTimer.stop();
        }
        if (null != master) {
            try {
                master.writeSingleRegister(0x8001, new SimpleRegister(forward ? 1 : 0)); // set direction forward
                master.writeSingleRegister(0x8002, new SimpleRegister(newSpeed)); // set the speed, 0 = off, 32768 = max
                if (newSpeed > 0) {
                    master.writeSingleRegister(0x8000, new SimpleRegister(1)); // make it go
                } else {
                    master.writeSingleRegister(0x8000, new SimpleRegister(0)); // make NOT it go
                }
            } catch (ModbusException ex) {
                Logger.getLogger(OuterConveyorSpeedControlJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void conveyorSpeedJPanel1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_conveyorSpeedJPanel1MouseReleased

        if (evt.isPopupTrigger()) {
            popupMenu.show(this.conveyorSpeedJPanel1, evt.getX(), evt.getY());
        } else if (!isGoalSet() && conveyorSpeedJPanel1.getCurrentSpeed() > 0) {
            stopConveyor();
        }
    }//GEN-LAST:event_conveyorSpeedJPanel1MouseReleased

    private void conveyorSpeedJPanel1MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_conveyorSpeedJPanel1MouseExited

        if (!isGoalSet() && conveyorSpeedJPanel1.getCurrentSpeed() > 0) {
            stopConveyor();
        }
    }//GEN-LAST:event_conveyorSpeedJPanel1MouseExited

    private void conveyorSpeedJPanel1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_conveyorSpeedJPanel1MouseDragged
        setConveyorSpeedFromMouseEvent(evt);
    }//GEN-LAST:event_conveyorSpeedJPanel1MouseDragged

    private void conveyorSpeedJPanel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_conveyorSpeedJPanel1MouseClicked
        if (evt.isPopupTrigger()) {
            popupMenu.show(this.conveyorSpeedJPanel1, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_conveyorSpeedJPanel1MouseClicked

    private void jButtonPrevTrayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPrevTrayActionPerformed
        previousTray();
    }//GEN-LAST:event_jButtonPrevTrayActionPerformed

    private void jButtonNextTrayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNextTrayActionPerformed
        nextTray();
    }//GEN-LAST:event_jButtonNextTrayActionPerformed

    /**
     * Get the value of axisX
     *
     * @return the value of axisX
     */
    public double getAxisX() {
        return conveyorSpeedJPanel1.getAxisX();
    }

    /**
     * Set the value of axisX
     *
     * @param axisX new value of axisX
     */
    public void setAxisX(double axisX) {
        conveyorSpeedJPanel1.setAxisX(axisX);
    }

    /**
     * Get the value of axisY
     *
     * @return the value of axisY
     */
    public double getAxisY() {
        return conveyorSpeedJPanel1.getAxisY();
    }

    /**
     * Set the value of axisY
     *
     * @param axisY new value of axisY
     */
    public void setAxisY(double axisY) {
        conveyorSpeedJPanel1.setAxisY(axisY);
    }

    private void stopConveyor() {
        updateEstimatedPosition();
        stopConveyorNoPosEstimate();
    }

    private volatile long nextPrevTrayFutureCompletTime = -1;
    private volatile long stopConveyorTime = -1;

    private void stopConveyorNoPosEstimate() {
        XFutureVoid future = nextPrevTrayFuture;
        nextPrevTrayFuture = null;
        conveyorSpeedJPanel1.setCurrentSpeed(0);
        if (null != master) {
            try {
                master.writeSingleRegister(0x8000, new SimpleRegister(0)); // make it NOT go
                master.writeSingleRegister(0x8002, new SimpleRegister(0)); // set the speed, 0 = off, 32768 = max
            } catch (ModbusException ex) {
                Logger.getLogger(OuterConveyorSpeedControlJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (positionUpdateTimer.isRunning()) {
            positionUpdateTimer.stop();
        }
        if (null != future) {
            javax.swing.Timer timer = new javax.swing.Timer(nextDelayMillis, e -> {
                long tm = System.currentTimeMillis();
                nextPrevTrayFutureCompletTime = tm;
                long diff = tm - nextPrevTrayFutureStartTime;
                println("next tray took " + diff + " ms");
                long diff2 = tm - stopConveyorTime;
                println("time since conveyor stop is " + diff2 + " ms");
                future.complete();

            });
            timer.setInitialDelay(nextDelayMillis);
            timer.setRepeats(false);
            stopConveyorTime = System.currentTimeMillis();
            timer.start();
        }
    }

    private int nextDelayMillis = 10000;

    /**
     * Get the value of nextDelayMillis
     *
     * @return the value of nextDelayMillis
     */
    public int getNextDelayMillis() {
        return nextDelayMillis;
    }

    /**
     * Set the value of nextDelayMillis
     *
     * @param nextDelayMillis new value of nextDelayMillis
     */
    public void setNextDelayMillis(int nextDelayMillis) {
        this.nextDelayMillis = nextDelayMillis;
    }

    /**
     * Get the value of nextDelayMillis
     *
     * @return the value of maxSpeed in unknown conveyor specific units
     */
    public int getMaxSpeed() {
        return this.conveyorSpeedJPanel1.getMaxSpeed();
    }

    /**
     * Set the value of nextDelayMillis
     *
     * @param maxSpeed in unknown conveyor specific units
     */
    public void setMaxSpeed(int maxSpeed) {
        this.conveyorSpeedJPanel1.setMaxSpeed(maxSpeed);
    }

    private final javax.swing.Timer positionUpdateTimer;

    private volatile long lastEstimatedPositionTime = 0;

    /**
     * Get the value of scale
     *
     * @return the value of scale
     */
    public double getScale() {
        return conveyorSpeedJPanel1.getScale();
    }

    /**
     * Set the value of scale
     *
     * @param scale new value of scale
     */
    public void setScale(double scale) {
        conveyorSpeedJPanel1.setScale(scale);
    }

    public void setEstimatedPosition(double newEstimatedPosition) {
        conveyorSpeedJPanel1.setEstimatedPosition(newEstimatedPosition);
    }

    private int updateTimerPeriod = 250;

    /**
     * Get the value of updateTimerPeriod
     *
     * @return the value of updateTimerPeriod
     */
    public int getUpdateTimerPeriod() {
        return updateTimerPeriod;
    }

    /**
     * Set the value of updateTimerPeriod
     *
     * @param updateTimerPeriod new value of updateTimerPeriod
     */
    public void setUpdateTimerPeriod(int updateTimerPeriod) {
        this.updateTimerPeriod = updateTimerPeriod;
    }

    private int maxAssumedTimeDiff = 250;

    /**
     * Get the value of maxAssumedTimeDiff
     *
     * @return the value of maxAssumedTimeDiff
     */
    public int getMaxAssumedTimeDiff() {
        return maxAssumedTimeDiff;
    }

    /**
     * Set the value of maxAssumedTimeDiff
     *
     * @param maxAssumedTimeDiff new value of maxAssumedTimeDiff
     */
    public void setMaxAssumedTimeDiff(int maxAssumedTimeDiff) {
        this.maxAssumedTimeDiff = maxAssumedTimeDiff;
    }

    private synchronized void updateEstimatedPosition() {
        int speed = conveyorSpeedJPanel1.getCurrentSpeed();
        boolean forward = conveyorSpeedJPanel1.isForwardDirection();
        long time = System.currentTimeMillis();
        long timeDiff = time - lastEstimatedPositionTime;
        if (lastEstimatedPositionTime < 1 || timeDiff < 1 || timeDiff > maxAssumedTimeDiff) {
            timeDiff = maxAssumedTimeDiff;
        }
        final double scale = getScale();
        double scaledSpeed = speed * scale;
        double positionInc = ((forward ? 1 : -1) * scaledSpeed * timeDiff);

        if (Math.abs(positionInc) > 0.0001) {
            setEstimatedPosition(getEstimatedPosition() + positionInc);
            lastEstimatedPositionTime = time;
            notifyConveyorPositionListeners();
        }
        lastEstimatedPositionTime = time;
        if (speed > 0) {
            final double estimatedPosition = getEstimatedPosition();
            if (forward && estimatedPosition > getMaxPosition()) {
                stopConveyorNoPosEstimate();
            } else if (!forward && estimatedPosition < getMinPosition()) {
                stopConveyorNoPosEstimate();
            }
            final boolean goalSet = isGoalSet();
            if (goalSet) {
                final double goalPosition = this.getGoalPosition();
                if (forward && estimatedPosition > goalPosition) {
                    setGoalSet(false);
                    stopConveyorNoPosEstimate();
                } else if (!forward && estimatedPosition < goalPosition) {
                    setGoalSet(false);
                    stopConveyorNoPosEstimate();
                }
            }
        }
    }

    public double getEstimatedPosition() {
        return conveyorSpeedJPanel1.getEstimatedPosition();
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
        final double estimatedPosition = getEstimatedPosition();
        ConveyorPosition cs = new ConveyorPosition(getAxisX() * estimatedPosition, getAxisY() * estimatedPosition);
        for (Consumer<ConveyorPosition> listener : conveyorPositionListeners) {
            listener.accept(cs);
        }
    }

    /**
     * Get the value of items
     *
     * @return the value of items
     */
    public List<PhysicalItem> getItems() {
        return conveyorSpeedJPanel1.getItems();
    }

    /**
     * Set the value of items
     *
     * @param items new value of items
     */
    public void setItems(List<PhysicalItem> items) {
        conveyorSpeedJPanel1.setItems(items);
    }

     private  @Nullable  ModbusTCPMaster master;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.conveyor.InnerConveyorSpeedJPanel conveyorSpeedJPanel1;
    private javax.swing.JButton jButtonNextTray;
    private javax.swing.JButton jButtonPrevTray;
    // End of variables declaration//GEN-END:variables
}
