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
package aprs.framework.simview;

import aprs.framework.database.DetectedItem;
import aprs.framework.database.Main;
import aprs.framework.spvision.VisionSocketClient;
import aprs.framework.spvision.VisionSocketServer;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Object2DOuterJPanel extends javax.swing.JPanel implements Object2DJFrameInterface, VisionSocketClient.VisionSocketClientListener {

    public List<DetectedItem> getItems() {
        return object2DJPanel1.getItems();
    }

    private volatile boolean settingItems = false;

    public void setItems(List<DetectedItem> items) {
        try {
            settingItems = true;
            object2DJPanel1.setItems(items);
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(items.size());
            for (int i = 0; i < items.size(); i++) {
                DetectedItem item = items.get(i);
                model.setValueAt(item.name, i, 0);
                model.setValueAt(item.x, i, 1);
                model.setValueAt(item.y, i, 2);
                model.setValueAt(Math.toDegrees(item.rotation), i, 3);
            }
            if (null != visionSocketServer) {
                visionSocketServer.publishList(items);
            }
        } finally {
            settingItems = false;
        }
    }

    /**
     * Creates new form Object2DOuterJPanel
     */
    public Object2DOuterJPanel() {
        initComponents();
        jTable1.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                boolean changeFound = false;
                if (!settingItems) {
                    List<DetectedItem> l = new ArrayList<>();
                    l.addAll(getItems());
                    DetectedItem item = null;
                    for (int i = 0; i < jTable1.getRowCount(); i++) {
                        if (jTable1.getValueAt(i, 0) == null || jTable1.getValueAt(i, 0).toString().length() < 1) {
                            continue;
                        }
                        if (i < l.size()) {
                            item = l.get(i);
                        } else {
                            item = null;
                        }
                        if (item == null || item.name == null
                                || !Objects.equals(item.name, jTable1.getValueAt(i, i))
                                || Math.abs(item.x - Double.parseDouble(jTable1.getValueAt(i, 1).toString())) > 0.001
                                || Math.abs(item.y - Double.parseDouble(jTable1.getValueAt(i, 2).toString())) > 0.001
                                || Math.abs(item.rotation - Double.parseDouble(jTable1.getValueAt(i, 3).toString())) > 0.001) {
                            changeFound = true;
                        } else {
                            continue;
                        }
                        if (item == null) {
                            item = new DetectedItem();
                        }
                        item.name = jTable1.getValueAt(i, 0).toString();
                        item.x = Double.parseDouble(jTable1.getValueAt(i, 1).toString());
                        item.y = Double.parseDouble(jTable1.getValueAt(i, 2).toString());
                        item.rotation = Math.toRadians(Double.parseDouble(jTable1.getValueAt(i, 3).toString()));
                        while (l.size() < i) {
                            l.add(null);
                        }
                        l.set(i, item);
                    }
                    if (changeFound) {
                        setItems(l);
                    }
                }
            }
        });
        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                object2DJPanel1.setSelectedItemIndex(jTable1.getSelectedRow());
            }
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

        object2DJPanel1 = new aprs.framework.simview.Object2DJPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButtonAdd = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jCheckBoxSimulated = new javax.swing.JCheckBox();
        jTextFieldHost = new javax.swing.JTextField();
        jTextFieldPort = new javax.swing.JTextField();
        jCheckBoxConnected = new javax.swing.JCheckBox();
        jLabelHost = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();

        object2DJPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        object2DJPanel1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseDragged(evt);
            }
        });
        object2DJPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                object2DJPanel1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                object2DJPanel1MouseReleased(evt);
            }
        });

        javax.swing.GroupLayout object2DJPanel1Layout = new javax.swing.GroupLayout(object2DJPanel1);
        object2DJPanel1.setLayout(object2DJPanel1Layout);
        object2DJPanel1Layout.setHorizontalGroup(
            object2DJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 459, Short.MAX_VALUE)
        );
        object2DJPanel1Layout.setVerticalGroup(
            object2DJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Name", "X", "Y", "Rotation"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jButtonAdd.setText("Add");
        jButtonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddActionPerformed(evt);
            }
        });

        jButtonDelete.setText("Delete");
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Connection"));

        jCheckBoxSimulated.setText("Simulated");
        jCheckBoxSimulated.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxSimulatedActionPerformed(evt);
            }
        });

        jTextFieldHost.setText("localhost");

        jTextFieldPort.setText("4000");

        jCheckBoxConnected.setText("Connected");
        jCheckBoxConnected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxConnectedActionPerformed(evt);
            }
        });

        jLabelHost.setText("Host:");

        jLabel1.setText("Port:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jCheckBoxSimulated)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxConnected))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelHost)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxSimulated)
                    .addComponent(jCheckBoxConnected))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelHost)
                    .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(object2DJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButtonDelete)
                        .addGap(10, 10, 10)
                        .addComponent(jButtonAdd)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(object2DJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE)
                        .addGap(9, 9, 9)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButtonDelete)
                            .addComponent(jButtonAdd))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jButtonAdd, jButtonDelete});

    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxSimulatedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSimulatedActionPerformed
        this.jCheckBoxConnected.setSelected(false);
        if (jCheckBoxSimulated.isSelected()) {
            jTextFieldHost.setEditable(false);
            jTextFieldHost.setEnabled(false);
            jLabelHost.setEnabled(false);
            jButtonAdd.setEnabled(true);
            jButtonDelete.setEnabled(true);
        } else {
            jTextFieldHost.setEditable(true);
            jTextFieldHost.setEnabled(true);
            jButtonAdd.setEnabled(false);
            jButtonDelete.setEnabled(false);
            jLabelHost.setEnabled(true);
        }
        if (null != visionSocketServer) {
            visionSocketServer.close();
            visionSocketServer = null;
        }

    }//GEN-LAST:event_jCheckBoxSimulatedActionPerformed

    private VisionSocketServer visionSocketServer = null;
    private VisionSocketClient visionSocketClient = null;

    private void jCheckBoxConnectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxConnectedActionPerformed
        if (this.jCheckBoxConnected.isSelected()) {
            if (this.jCheckBoxSimulated.isSelected()) {
                try {
                    visionSocketServer = new VisionSocketServer(Integer.parseInt(this.jTextFieldPort.getText()));
                    visionSocketServer.publishList(getItems());
                } catch (IOException ex) {
                    Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                if (null != Main.getVisionSocketClient()) {
                    visionSocketClient = Main.getVisionSocketClient();
                } else {
                    visionSocketClient = new VisionSocketClient();
                    Map<String, String> argsMap = new HashMap<>();
                    for (Map.Entry<String, String> e : Main.getArgsMap().entrySet()) {
                        argsMap.put(e.getKey(), e.getValue());
                    }
                    argsMap.put("--visionport", jTextFieldPort.getText());
                    argsMap.put("--visionhost", jTextFieldHost.getText());
                    visionSocketClient.start(argsMap);
                }
                visionSocketClient.addListener(this);
            }
        } else {
            if (null != visionSocketClient) {
                if (visionSocketClient != Main.getVisionSocketClient()) {
                    try {
                        visionSocketClient.close();
                    } catch (Exception ex) {
                        Logger.getLogger(Object2DOuterJPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    visionSocketClient.removeListListener(this);
                }
                visionSocketClient = null;
            }
            if (null != visionSocketServer) {
                visionSocketServer.close();
                visionSocketServer = null;
            }
        }
    }//GEN-LAST:event_jCheckBoxConnectedActionPerformed

    private void jButtonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddActionPerformed
        List<DetectedItem> l = new ArrayList<>();
        l.addAll(getItems());
        l.add(new DetectedItem());
        setItems(l);
    }//GEN-LAST:event_jButtonAddActionPerformed

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteActionPerformed
        List<DetectedItem> l = new ArrayList<>();
        l.addAll(getItems());
        l.remove(jTable1.getSelectedRow());
        setItems(l);
    }//GEN-LAST:event_jButtonDeleteActionPerformed


    private void object2DJPanel1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseDragged
        double scale = object2DJPanel1.getScale();
        Point2D.Double minCorner = object2DJPanel1.getMinCorner();
        if (null == minCorner) {
            return;
        }
        if(null != draggedItem) {
            draggedItem.x = ((evt.getX()-15)/scale) + minCorner.x;
            draggedItem.y = ((evt.getY()-20)/scale) + minCorner.y;
            this.setItems(this.getItems());
        }
    }//GEN-LAST:event_object2DJPanel1MouseDragged

    private DetectedItem draggedItem = null;

    private void object2DJPanel1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MousePressed
        List<DetectedItem> items = this.getItems();
        double scale = object2DJPanel1.getScale();
        Point2D.Double minCorner = object2DJPanel1.getMinCorner();
        draggedItem = null;
        if (null == minCorner) {
            return;
        }
        DetectedItem closestItem = null;
        double minDist = Double.POSITIVE_INFINITY;
        int minIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            DetectedItem item = items.get(i);
            double rel_x = (item.x - minCorner.x) * scale+15;
            double rel_y = (item.y - minCorner.y) * scale+20;
            double diff_x = rel_x -evt.getX();
            double diff_y = rel_y -evt.getY();
            double dist = Math.sqrt(diff_x*diff_x+diff_y*diff_y);
            if(dist < 35 && dist < minDist) {
                minDist=dist;
                closestItem = item;
                minIndex = i;
            }
        }
        if(minIndex >= 0) {
            jTable1.getSelectionModel().setSelectionInterval(minIndex, minIndex);
            object2DJPanel1.setSelectedItemIndex(minIndex);
        }
        draggedItem = closestItem;
    }//GEN-LAST:event_object2DJPanel1MousePressed

    private void object2DJPanel1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_object2DJPanel1MouseReleased
        draggedItem = null;
    }//GEN-LAST:event_object2DJPanel1MouseReleased


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAdd;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JCheckBox jCheckBoxConnected;
    private javax.swing.JCheckBox jCheckBoxSimulated;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabelHost;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextFieldHost;
    private javax.swing.JTextField jTextFieldPort;
    private aprs.framework.simview.Object2DJPanel object2DJPanel1;
    // End of variables declaration//GEN-END:variables

    @Override
    public void dispose() {
        System.err.println("com.github.wshackle.visiontodb.Object2DOuterJPanel.dispose default implementation called.");
    }

    private File propertiesFile;

    @Override
    public void setPropertiesFile(File f) {
        this.propertiesFile = f;
    }

    @Override
    public void saveProperties() throws IOException {
        if (null != propertiesFile) {
            propertiesFile.getParentFile().mkdirs();
            Properties props = new Properties();
            props.put("items", VisionSocketServer.listToLine(getItems()));
            props.put("--visionport", jTextFieldPort.getText());
            props.put("--visionhost", jTextFieldHost.getText());
            props.put("simulated", Boolean.toString(jCheckBoxSimulated.isSelected()));
            try (FileWriter fw = new FileWriter(propertiesFile)) {
                props.store(fw, "");
            }
        }
    }

    @Override
    public void restoreProperties() throws IOException {
        if (null != propertiesFile && propertiesFile.exists()) {
            Properties props = new Properties();
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            }
            String itemsLine = props.getProperty("items");
//            if (null != itemsLine && itemsLine.length() > 0) {
//                setItems(VisionSocketClient.lineToList(itemsLine));
//            }
            String portString = props.getProperty("--visionport");
            try {
                if (null != portString && portString.length() > 0) {
                    int port = Integer.parseInt(portString);
                    jTextFieldPort.setText(Integer.toString(port));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String hostString = props.getProperty("--visionhost");
            try {
                if (null != hostString && hostString.length() > 0) {
                    jTextFieldHost.setText(hostString);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String simulatedString = props.getProperty("simulated");
            if (null != simulatedString && simulatedString.length() > 0) {
                jCheckBoxSimulated.setSelected(Boolean.valueOf(simulatedString));
            }
        }
    }

    @Override
    public File getPropertiesFile() {
        return propertiesFile;
    }

    @Override
    public void accept(VisionSocketClient client) {
        final List<DetectedItem> l = client.getVisionList();
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            setItems(l);
        } else {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setItems(l);
                }
            });
        }
    }
}
