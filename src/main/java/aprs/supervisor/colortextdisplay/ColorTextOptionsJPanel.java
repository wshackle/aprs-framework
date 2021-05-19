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
package aprs.supervisor.colortextdisplay;

import java.awt.Frame;
import javax.swing.JDialog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.guieffect.qual.UIType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A panel for querying the user on the port, host etc for listening for info on
 * when to change color for a ColorTextJPanel/ColorTextJFrame.
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"unused", "guieffect", "serial", "initialization", "nullness", "RedundantArrayCreation"})
@UIType
public class ColorTextOptionsJPanel extends javax.swing.JPanel {

    /**
     * Options returned when user presses OK button.
     */
    public static class ColorTextOptions {

        private final int port;
        private final String host;
        private final boolean startDisplay;

        ColorTextOptions(int port, String host, boolean startDisplay) {
            this.port = port;
            this.host = host;
            this.startDisplay = startDisplay;
        }

        /**
         * Get the port chosen by the user to connect to for information on when
         * to change color or text.
         *
         * @return port to connect to for information on when to change color or
         * text.
         */
        public int getPort() {
            return port;
        }

        /**
         * Get the host chosen by the user to connect to for information on when
         * to change color or text.
         *
         * @return host to connect to for information on when to change color or
         * text.
         */
        public String getHost() {
            return host;
        }

        /**
         * Get the selected state of the checkbox for choosing to start a
         * separate window (ColorTextJFrame) for display.
         *
         * @return selected state of the checkbox for separate display
         */
        public boolean isStartDisplay() {
            return startDisplay;
        }
    }

    /**
     * Get options chosen by the user.
     *
     * @return options selected by user or null if user chose to cancel.
     */
    private @Nullable
    ColorTextOptions getOptions() {
        if (cancelled) {
            return null;
        }
        return new ColorTextOptions(
                Integer.parseInt(jTextFieldPort.getText()),
                jTextFieldHost.getText(),
                jCheckBoxStartDisplay.isSelected());
    }

    /**
     * Query the user with a custom dialog and return user selected options.
     *
     * @param owner the Frame from which the dialog is displayed
     * @param modal specifies whether dialog blocks user input to other
     * top-level windows when shown. If true, the modality type property is set
     * to DEFAULT_MODALITY_TYPE, otherwise the dialog is modeless.
     *
     * @return options selected by user or null if user chose to cancel.
     */
    static public @Nullable
    ColorTextOptions query(Frame owner, boolean modal) {
        JDialog dialog = new JDialog(owner, modal);
        ColorTextOptionsJPanel panel = new ColorTextOptionsJPanel();
        panel.dialog = dialog;
        dialog.add(panel);
        dialog.pack();
        dialog.setVisible(true);
        return panel.getOptions();
    }

    private JDialog dialog;

    /**
     * Creates new form ColorTexOptionsJPanel
     */
    @SuppressWarnings({"initialization", "WeakerAccess"})
    public ColorTextOptionsJPanel() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"all","unchecked"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jTextFieldHost = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldPort = new javax.swing.JTextField();
        jCheckBoxStartDisplay = new javax.swing.JCheckBox();
        jButtonCancel = new javax.swing.JButton();
        jButtonOk = new javax.swing.JButton();

        jLabel1.setText("Host:");

        jTextFieldHost.setText("localhost");

        jLabel2.setText("Port:");

        jTextFieldPort.setText("2344");

        jCheckBoxStartDisplay.setSelected(true);
        jCheckBoxStartDisplay.setText("Start Display");

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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBoxStartDisplay)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jButtonOk)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jButtonCancel))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1)
                                .addComponent(jLabel2))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jTextFieldHost, jTextFieldPort});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxStartDisplay)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 49, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOk))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private boolean cancelled = false;

    @UIEffect
    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        cancelled = true;
        dialog.setVisible(false);
    }//GEN-LAST:event_jButtonCancelActionPerformed

    @UIEffect
    private void jButtonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOkActionPerformed
        dialog.setVisible(false);
    }//GEN-LAST:event_jButtonOkActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOk;
    private javax.swing.JCheckBox jCheckBoxStartDisplay;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField jTextFieldHost;
    private javax.swing.JTextField jTextFieldPort;
    // End of variables declaration//GEN-END:variables
}
