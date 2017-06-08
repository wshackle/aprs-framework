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
package aprs.framework.logdisplay;

import aprs.framework.AprsJFrame;
import aprs.framework.Utils;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class LogDisplayJPanel extends javax.swing.JPanel {

    /**
     * Creates new form LogDisplayJPanel
     */
    public LogDisplayJPanel() {
        initComponents();
        jSpinnerMaxLines.setValue(250);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCheckBoxPauseOutput = new javax.swing.JCheckBox();
        jSpinnerMaxLines = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jButtonToExternal = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        jCheckBoxPauseOutput.setText("Pause Output");

        jLabel1.setText("Max Lines");

        jButtonToExternal.setText("To External Viewer");
        jButtonToExternal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonToExternalActionPerformed(evt);
            }
        });

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTextArea1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jTextArea1MouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextArea1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTextArea1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 26, Short.MAX_VALUE)
                        .addComponent(jButtonToExternal)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinnerMaxLines, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBoxPauseOutput))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxPauseOutput)
                    .addComponent(jSpinnerMaxLines, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jButtonToExternal))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    
        private AprsJFrame aprsJFrame;

    /**
     * Get the value of aprsJFrame
     *
     * @return the value of aprsJFrame
     */
    public AprsJFrame getAprsJFrame() {
        return aprsJFrame;
    }

    /**
     * Set the value of aprsJFrame
     *
     * @param aprsJFrame new value of aprsJFrame
     */
    public void setAprsJFrame(AprsJFrame aprsJFrame) {
        this.aprsJFrame = aprsJFrame;
    }

    private void jButtonToExternalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonToExternalActionPerformed
        try {
            File f = Utils.createTempFile("log", ".txt");
            try (FileWriter fw = new FileWriter(f)) {
                fw.write(jTextArea1.getText());
            }
            Desktop.getDesktop().open(f);
        } catch (IOException ex) {
            Logger.getLogger(LogDisplayJPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonToExternalActionPerformed

    private JPopupMenu popMenu = new JPopupMenu();
    {
        JMenuItem copyMenuItem = new JMenuItem("Copy");
        copyMenuItem.addActionListener(e -> copyText());
        popMenu.add(copyMenuItem);
    }
    
    private void copyText() {
        this.jTextArea1.getTransferHandler().exportToClipboard(this.jTextArea1,
                Toolkit.getDefaultToolkit().getSystemClipboard(),
                TransferHandler.COPY);
        popMenu.setVisible(false);
    }

    public void showPopup(Component comp, int x, int y) {
        popMenu.show(comp, x, y);
    }

    private void jTextArea1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextArea1MousePressed
        if (evt.isPopupTrigger()) {
            showPopup(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTextArea1MousePressed

    private void jTextArea1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextArea1MouseReleased
        if (evt.isPopupTrigger()) {
            showPopup(evt.getComponent(),evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTextArea1MouseReleased

    private void jTextArea1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextArea1MouseClicked
        if (evt.isPopupTrigger()) {
            showPopup(evt.getComponent(),evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTextArea1MouseClicked

    List<String> logLines = new ArrayList<>();

    private void appendLine(String l) {
        int maxLines = 100;
        if(l.length() > 204) {
            l = l.substring(0, 200)+" ...";
        }
        try {
            maxLines = (int) jSpinnerMaxLines.getValue();
            if (maxLines < 1) {
                jSpinnerMaxLines.setValue(1);
                maxLines = 1;
            }
        } catch (Exception e) {
        }
        if (logLines.size() < maxLines) {
            addLogLine(l);
            if (!jCheckBoxPauseOutput.isSelected()) {
                jTextArea1.append(l);
            }
        } else {
            while (logLines.size() >= maxLines) {
                logLines.remove(0);
            }
            addLogLine(l);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < logLines.size(); i++) {
                sb.append(logLines.get(i));
            }
            if (!jCheckBoxPauseOutput.isSelected()) {
                jTextArea1.setText(sb.toString());
            }
        }
        if (!jCheckBoxPauseOutput.isSelected()) {
            jTextArea1.setCaretPosition(jTextArea1.getText().length() - 1);
        }
    }

    private void addLogLine(String l) {
        if (logLines.size() > 0) {
            String lastLine = logLines.get(logLines.size() - 1);
            if (lastLine.endsWith("\n")) {
                logLines.add(l);
            } else {
                logLines.set(logLines.size() - 1, lastLine + l);
            }
        } else {
            logLines.add(l);
        }
    }

    public void clearText() {
        logLines.clear();
        jTextArea1.setText("");
        jTextArea1.setCaretPosition(0);
    }
    
    public void appendText(String text) {
        String txt2 = text.replace("\r\n", "\n");
        String lines[] = txt2.split("\n");
        if (lines.length <= 1 || (lines.length == 2) && lines[1].length() < 1) {
            appendLine(txt2);
        } else {
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (i < lines.length - 1 || txt2.endsWith("\n")) {
                    appendLine(line + System.lineSeparator());
                } else {
                    appendLine(line);
                }
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonToExternal;
    private javax.swing.JCheckBox jCheckBoxPauseOutput;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSpinner jSpinnerMaxLines;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables
}
