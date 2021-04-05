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

import aprs.database.PhysicalItem;
import java.awt.Frame;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Object2DOuterDialogPanel extends javax.swing.JPanel {

    private volatile boolean cancelled = false;

    /**
     * Creates new form Object2DOuterDialogPanel
     */
    @UIEffect
    @SuppressWarnings({"nullness","initialization"})
    public Object2DOuterDialogPanel() {
        initComponents();
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
        jButtonOk = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();

        jButtonOk.setText("OK");
        jButtonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOkActionPerformed(evt);
            }
        });

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButtonOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCancel))
                    .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 509, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOk))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    @UIEffect
    private void jButtonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOkActionPerformed
        cancelled = false;
        if (null != dialog) {
            dialog.setVisible(false);
        }
    }//GEN-LAST:event_jButtonOkActionPerformed

    @UIEffect
    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        cancelled = true;
        if (null != dialog) {
            dialog.setVisible(false);
        }
    }//GEN-LAST:event_jButtonCancelActionPerformed

    /**
     * Show a dialog where the user can select multiple files for different
     * purposes each with its own label string.
     *
     * @param owner frame owner for the dialog
     * @param title title string
     * @param modal should the dialog be modal
     * @param props property settings
     * @param items list of items to show
     *
     */
    @SuppressWarnings("nullness")
    @UIEffect
    public static boolean showObject2DDialog(
            @Nullable Frame owner,
            String title,
            boolean modal,
            Properties props,
            List<PhysicalItem> items) {
        Object2DOuterDialogPanel panel = new Object2DOuterDialogPanel();
        try {
            panel.dialog = new JDialog(owner, title, modal);
            panel.dialog.add(panel);
            panel.dialog.pack();
            panel.object2DOuterJPanel1.setDialogMode(true);
            panel.object2DOuterJPanel1.setShowAddedToolsAndToolHolders(false);
            Properties newProps = new Properties();
            newProps.putAll(props);
            newProps.remove("tools");
            newProps.put("tools", "false");
            newProps.remove("simulated");
            newProps.put("simulated", "true");
            newProps.remove("connected");
            newProps.put("connected", "false");
            panel.object2DOuterJPanel1.loadProperties(newProps);
            panel.object2DOuterJPanel1.setShowAddedToolsAndToolHolders(false);
            panel.object2DOuterJPanel1.setItems(items);
            panel.dialog.setVisible(true);
            return panel.cancelled;
        } catch (Exception ex) {
            panel.dialog.setVisible(false);
            Logger.getLogger(Object2DOuterDialogPanel.class.getName()).log(Level.SEVERE, null, ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Show a dialog where the user can select multiple files for different
     * purposes each with its own label string.
     *
     * @param owner frame owner for the dialog
     * @param title title string
     * @param modal should the dialog be modal
     * @param propsFile properties file to read
     * @param itemsFile items file to load
     *
     */
    @SuppressWarnings("nullness")
    @UIEffect
    public static boolean showObject2DDialog(
            @Nullable Frame owner,
            String title,
            boolean modal,
            File propsFile,
            File itemsFile,
            File visionLogFile) {
        Object2DOuterDialogPanel panel = new Object2DOuterDialogPanel();
        try {
            panel.dialog = new JDialog(owner, title, modal);
            panel.dialog.add(panel);
            panel.dialog.pack();
            panel.object2DOuterJPanel1.setDialogMode(true);
            panel.object2DOuterJPanel1.setShowAddedToolsAndToolHolders(false);
            panel.object2DOuterJPanel1.setPropertiesFile(propsFile);
            panel.object2DOuterJPanel1.setShowAddedToolsAndToolHolders(false);
            Properties props = new Properties();
            if (null != propsFile) {
                try (FileReader fr = new FileReader(propsFile)) {
                    props.load(fr);
                }
            }
            props.remove("tools");
            props.put("tools", "false");
            props.remove("simulated");
            props.put("simulated", "true");
            props.remove("connected");
            props.put("connected", "false");
            panel.object2DOuterJPanel1.loadProperties(props);
            if (null != itemsFile) {
                panel.object2DOuterJPanel1.loadFile(itemsFile);
            }
            if(null != visionLogFile) {
                panel.object2DOuterJPanel1.loadLogFile(visionLogFile);
            }
            
            panel.dialog.setVisible(true);
            return panel.cancelled;
        } catch (Exception ex) {
            panel.dialog.setVisible(false);
            Logger.getLogger(Object2DOuterDialogPanel.class.getName()).log(Level.SEVERE, null, ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        }
    }

    @SuppressWarnings("nullness")
    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
        javax.swing.SwingUtilities.invokeLater(() -> {
            JFileChooser propsFileChooser = new JFileChooser();
            if (args.length > 0) {
                propsFileChooser.setSelectedFile(new File(args[0]));
            }
            propsFileChooser.setDialogTitle("Properties File");
            if (JFileChooser.APPROVE_OPTION != propsFileChooser.showOpenDialog(null)) {
                return;
            }
            boolean single;
            if (args.length > 1) {
                single = Boolean.valueOf(args[1]);
            } else {
                String fileTypeInputString
                        = (String) JOptionPane.showInputDialog(
                                null, // parent compenent
                                "Object 2D File Type Query", // title
                                "Open single items set file or vision log lines file?", // nessage
                                JOptionPane.QUESTION_MESSAGE,
                                null, // icon
                                new String[]{"single item set", "vision log lines"},
                                "vision log lines");
                single = fileTypeInputString.startsWith("single");
            }
            File singleItemsFile = null;
            File visionLogLinesFile = null;
            if (single) {
                JFileChooser itemsFileChooser = new JFileChooser();
                if (args.length > 2) {
                    itemsFileChooser.setSelectedFile(new File(args[2]));
                }
                itemsFileChooser.setDialogTitle("Items File");
                if (JFileChooser.APPROVE_OPTION != itemsFileChooser.showOpenDialog(null)) {
                    return;
                }
                singleItemsFile = itemsFileChooser.getSelectedFile();
            } else {
                JFileChooser visionLogFileChooser = new JFileChooser();
                if (args.length > 2) {
                    visionLogFileChooser.setSelectedFile(new File(args[2]));
                }
                visionLogFileChooser.setDialogTitle("Vision Log File");
                if (JFileChooser.APPROVE_OPTION != visionLogFileChooser.showOpenDialog(null)) {
                    return;
                }
                visionLogLinesFile = visionLogFileChooser.getSelectedFile();
            }
            showObject2DDialog(null, "Outer2D Dialog test", true,
                    propsFileChooser.getSelectedFile(),
                    singleItemsFile,
                    visionLogLinesFile);
            System.exit(0);
        });
    }

    private @Nullable
    JDialog dialog = null;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOk;
    private aprs.simview.Object2DOuterJPanel object2DOuterJPanel1;
    // End of variables declaration//GEN-END:variables
}
