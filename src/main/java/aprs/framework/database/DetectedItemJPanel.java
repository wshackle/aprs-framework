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
package aprs.framework.database;

import java.awt.Dialog;
import java.awt.Window;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.table.DefaultTableModel;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class DetectedItemJPanel extends javax.swing.JPanel {

    /**
     * Creates new form DetectedItemJPanel
     */
    @SuppressWarnings("initialization")
    public DetectedItemJPanel() {
        initComponents();
    }

    private boolean cancelled = false;

    private @Nullable JDialog dialog = null;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButtonCancel = new javax.swing.JButton();
        jButtonOK = new javax.swing.JButton();

        jLabel1.setText("Add/Modifiy Detected Item:");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"name", "myPart"},
                {"x", "0.0"},
                {"y", "0.0"},
                {"rotation", "0.0"}
            },
            new String [] {
                "Property", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        jButtonOK.setText("OK");
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 390, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCancel)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOK))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOKActionPerformed
        if (null != dialog) {
            dialog.setVisible(false);
        }
    }//GEN-LAST:event_jButtonOKActionPerformed

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        cancelled = true;
        if (null != dialog) {
            dialog.setVisible(false);
        }
    }//GEN-LAST:event_jButtonCancelActionPerformed

    public static Map<String, Object> itemToMap(PhysicalItem item, Map<String, Object> map) {
        if (map == null) {
            map = new LinkedHashMap<>();
        }
        map.put("name", item.getName());
        map.put("X", item.x);
        map.put("y", item.y);
        map.put("z", item.z);
        map.put("rotation", item.getRotation());
        return map;
    }

    public static PhysicalItem mapToItem(Map<String, Object> map, @Nullable PhysicalItem item) {

        PhysicalItem itemToReturn = item;

        if (null != map) {
            String name = (String) map.get("name");
            if (null  == name ||  name.length() < 1) {
                throw new IllegalArgumentException("map must have non-null and non-empty entry for name. map=" + map);
            }
            if (itemToReturn == null) {
                itemToReturn = new PhysicalItem(name);
            } else {
                itemToReturn.setName(name);
            }
            Object X = map.get("x");
            if (X != null) {
                itemToReturn.x = Double.parseDouble(X.toString());
            }
            Object Y = map.get("y");
            if (Y != null) {
                itemToReturn.y = Double.parseDouble(Y.toString());
            }
            Object Z = map.get("z");
            if (Z != null) {
                itemToReturn.z = Double.parseDouble(Z.toString());
            }
            Object VXI = map.get("vxi");
            if (VXI != null) {
                itemToReturn.setVxi(Double.parseDouble(VXI.toString()));
            }
            Object VXJ = map.get("vxj");
            if (VXJ != null) {
                itemToReturn.setVxj(Double.parseDouble(VXJ.toString()));
            }
            Object VXK = map.get("vxk");
            if (VXK != null) {
                itemToReturn.setVxk(Double.parseDouble(VXK.toString()));
            }
            Object VZI = map.get("vzi");
            if (VZI != null) {
                itemToReturn.setVzi(Double.parseDouble(VZI.toString()));
            }
            Object VZJ = map.get("vzj");
            if (VZJ != null) {
                itemToReturn.setVzj(Double.parseDouble(VZJ.toString()));
            }
            Object VZK = map.get("vzk");
            if (VZK != null) {
                itemToReturn.setVzk(Double.parseDouble(VZK.toString()));
            }
            Object Rot = map.get("rotation");
            if (Rot != null) {
                itemToReturn.setRotation(Double.parseDouble(Rot.toString()));
                itemToReturn.setVxi(Math.cos(itemToReturn.getRotation()));
                itemToReturn.setVxj(Math.sin(itemToReturn.getRotation()));
            }
        } else {
            throw new IllegalArgumentException("map may not be null");
        }
        itemToReturn.normalizeRotation();
        return itemToReturn;
    }

    public static Map<String, Object> showDetectedItemDialog(@Nullable Window owner, String title, Dialog.ModalityType modal, @Nullable Map<String, Object> propsIn) {
        DetectedItemJPanel panel = new DetectedItemJPanel();
        JDialog panelDialog = new JDialog(owner, title, modal);
        panel.dialog = panelDialog;
        panelDialog.add(panel);
        panelDialog.pack();
        if (null != propsIn) {
            DefaultTableModel model = (DefaultTableModel) panel.jTable1.getModel();
            for (Map.Entry<String, Object> entry : propsIn.entrySet()) {
                boolean inTable = false;
                for (int i = 0; i < model.getRowCount(); i++) {
                    Object table0Value = model.getValueAt(i, 0);
                    if (table0Value == null) {
                        System.err.println("null in table at (" + i + ",0)");
                        continue;
                    }
                    String name = table0Value.toString();
                    if (name.equals(entry.getKey())) {
                        model.setValueAt(entry.getValue(), i, 1);
                        inTable = true;
                        break;
                    }
                }
                if (!inTable) {
                    model.addRow(new Object[]{entry.getKey(), entry.getValue()});
                }
            }
        }
        panelDialog.setVisible(true);
        if (!panel.cancelled) {
            Map<String, Object> propsOut = new LinkedHashMap<>();
            DefaultTableModel model = (DefaultTableModel) panel.jTable1.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object table0Value = model.getValueAt(i, 0);
                if (table0Value == null) {
                    System.err.println("null in table at (" + i + ",0)");
                    continue;
                }
                Object table1Value = model.getValueAt(i, 1);
                if (table1Value == null) {
                    System.err.println("null in table at (" + i + ",1)");
                    continue;
                }
                String name = table0Value.toString();
                propsOut.put(name, table1Value);
            }
            return propsOut;
        }
        return Collections.emptyMap();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOK;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
