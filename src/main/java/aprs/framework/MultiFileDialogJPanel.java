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
package aprs.framework;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.table.DefaultTableModel;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class MultiFileDialogJPanel extends javax.swing.JPanel {

    /**
     * Creates new form MultiFileDialogJPanel
     */
    @SuppressWarnings("initialization")
    public MultiFileDialogJPanel() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "nullness","rawtypes"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTableFiles = new javax.swing.JTable();
        jButtonCancel = new javax.swing.JButton();
        jButtonOk = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();

        jTableFiles.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Setup", null},
                {"Position Map", null},
                {"Teach Properties", null},
                {"Sim Teach", null}
            },
            new String [] {
                "Type", "File"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
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
        jScrollPane1.setViewportView(jTableFiles);

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        jButtonOk.setText("OK");
        jButtonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOkActionPerformed(evt);
            }
        });

        jButton1.setText("Browse Selected");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonOk)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonCancel)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 309, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOk)
                    .addComponent(jButton1))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOkActionPerformed
        cancelled = false;
        if (null != dialog) {
            dialog.setVisible(false);
        }
    }//GEN-LAST:event_jButtonOkActionPerformed

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        cancelled = true;
        if (null != dialog) {
            dialog.setVisible(false);
        }
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        int row = jTableFiles.getSelectedRow();
        if (row < 0 || row > jTableFiles.getRowCount()) {
            throw new IllegalStateException("row not selected");
        }
        File f = new File(Objects.toString(jTableFiles.getValueAt(row, 1))).getParentFile();

        JFileChooser chooser;
        if (null != f) {
            chooser = new JFileChooser(f);
        } else {
            chooser = new JFileChooser();
        }
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            try {
                DefaultTableModel model = (DefaultTableModel) jTableFiles.getModel();
                model.setValueAt(chooser.getSelectedFile().getCanonicalPath(), row, 1);
            } catch (IOException ex) {
                Logger.getLogger(MultiFileDialogJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private @Nullable JDialog dialog = null;
    private boolean cancelled = false;

    private void loadMap(Map<String, String> map) {
        DefaultTableModel model = (DefaultTableModel) jTableFiles.getModel();
        model.setRowCount(0);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            model.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
    }

    @Nullable private Map<String, String> getMap() {
        if (cancelled) {
            return null;
        }
        DefaultTableModel model = (DefaultTableModel) jTableFiles.getModel();
        Map<String, String> mapOut = new HashMap<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            mapOut.put(Objects.toString(model.getValueAt(i, 0)), Objects.toString(model.getValueAt(i, 1)));
        }
        return mapOut;
    }

    /**
     * Show a dialog where the user can select multiple files for different
     * purposes each with its own label string.
     *
     * @param owner frame owner for the dialog
     * @param title title string
     * @param modal should the dialog be modal
     * @param mapIn initial map of labels to filenames
     * @return map after modification by user
     */
    @Nullable public static Map<String, String> showMultiFileDialog(Frame owner, String title, boolean modal, Map<String, String> mapIn) {
        MultiFileDialogJPanel panel = new MultiFileDialogJPanel();
        panel.loadMap(mapIn);
        panel.dialog = new JDialog(owner, title, modal);
        panel.dialog.add(panel);
        panel.dialog.pack();
        panel.dialog.setVisible(true);
        return panel.getMap();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOk;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableFiles;
    // End of variables declaration//GEN-END:variables
}
