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
package aprs.framework.colortextdisplay;

import aprs.framework.Utils;
import aprs.framework.database.SocketLineReader;
import java.awt.Color;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;

/**
 *
 * @author shackle
 */
public class ColorTextJPanel extends javax.swing.JPanel {

    /**
     * Creates new form ColorTextJPanel
     */
    public ColorTextJPanel() {
        try {
            initComponents();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Set the labels and icon images for the left and right panels.
     *
     * @param leftLabel label for left panel
     * @param leftImage image icon for left panel
     * @param rightLabel label for right panel
     * @param rightImage image icon for right panel
     */
    public void setLabelsAndIcons(String leftLabel, Icon leftImage, String rightLabel, Icon rightImage) {
        jLabelRobotTextLeft.setText(leftLabel);
        jLabelRobotTextRight.setText(rightLabel);
        jLabelRobotIconLeft.setIcon(leftImage);
        jLabelRobotIconRight.setIcon(rightImage);
    }

    private static final Class THIS_CLASS = aprs.framework.colortextdisplay.ColorTextJPanel.class;

    /**
     * Get the Icon for the given robot name.
     * The Icon files are normally stored as resources in the same jar.
     * 
     * @param name name of the robot
     * @return icon
     */
    public static Icon getRobotIcon(String name) {
        if (name == null || name.length() < 1) {
            return null;
        }
        if (name.toUpperCase().contains("MOTOMAN")) {
            return new javax.swing.ImageIcon(THIS_CLASS.getResource("/aprs/framework/screensplash/motoman_small.png"));
        } else if (name.toUpperCase().contains("FANUC")) {
            return new javax.swing.ImageIcon(THIS_CLASS.getResource("/aprs/framework/screensplash/fanuc_small.png"));
        } else {
            return new javax.swing.ImageIcon(THIS_CLASS.getResource("/aprs/framework/screensplash/" + name.toLowerCase() + "_small.png"));
        }
    }

    /**
     * Default TCP port for providing the color and text labels to indicate
     * when robots are disabled.
     */
    public static final int COLORTEXT_SOCKET_PORT = 23444;

    private SocketLineReader reader;

    /**
     * Start a separate thread to read a socket for messages to change colors etc.
     */
    public void startReader() {
        SocketLineReader readerTmp = null;
        try {
            readerTmp = SocketLineReader.startServer(COLORTEXT_SOCKET_PORT, "ColorTextServer", this::parseSocketLine);

        } catch (IOException ex) {
            Logger.getLogger(ColorTextJFrame.class.getName()).log(Level.SEVERE, "Failed to bind color text socket port: " + COLORTEXT_SOCKET_PORT, ex);
        }
        this.reader = readerTmp;
    }

    /**
     * Stop thread and close socket created with startReader.
     */
    public void stopReader() {
        if (null != reader) {
            reader.close();
            reader = null;
        }
    }

    private void parseSocketLine(String line, PrintStream ps) {
        String colorStrings[] = line.split("[ \t,]+");
        ps.println(Arrays.toString(colorStrings));
        if (colorStrings.length != 2) {
            ps.println("2 colors expected : line " + line);
            System.err.println("2 colors expected : line=" + line);
            return;
        }
        Color color1 = Color.decode(colorStrings[1]);
        ps.println("color1 = " + color1);
        Color color2 = Color.decode(colorStrings[0]);
        ps.println("color2 = " + color2);
        Utils.runOnDispatchThread(() -> {
            jPanelRight.setBackground(color1);
            jPanelLeft.setBackground(color2);
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

        jPanelBoth = new javax.swing.JPanel();
        jPanelLeft = new javax.swing.JPanel();
        jLabelRobotTextLeft = new javax.swing.JLabel();
        jLabelRobotIconLeft = new javax.swing.JLabel();
        jPanelRight = new javax.swing.JPanel();
        jLabelRobotTextRight = new javax.swing.JLabel();
        jLabelRobotIconRight = new javax.swing.JLabel();

        jPanelLeft.setBackground(new java.awt.Color(204, 204, 255));
        jPanelLeft.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabelRobotTextLeft.setFont(new java.awt.Font("DejaVu Sans", 0, 36)); // NOI18N
        jLabelRobotTextLeft.setText("Motoman");

        jLabelRobotIconLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/aprs/framework/screensplash/motoman_small.png"))); // NOI18N

        javax.swing.GroupLayout jPanelLeftLayout = new javax.swing.GroupLayout(jPanelLeft);
        jPanelLeft.setLayout(jPanelLeftLayout);
        jPanelLeftLayout.setHorizontalGroup(
            jPanelLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLeftLayout.createSequentialGroup()
                .addGroup(jPanelLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelLeftLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabelRobotTextLeft))
                    .addGroup(jPanelLeftLayout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addComponent(jLabelRobotIconLeft)))
                .addContainerGap(14, Short.MAX_VALUE))
        );
        jPanelLeftLayout.setVerticalGroup(
            jPanelLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLeftLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelRobotTextLeft)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelRobotIconLeft)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jPanelRight.setBackground(new java.awt.Color(204, 204, 255));
        jPanelRight.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabelRobotTextRight.setFont(new java.awt.Font("DejaVu Sans", 0, 36)); // NOI18N
        jLabelRobotTextRight.setText("Fanuc");

        jLabelRobotIconRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/aprs/framework/screensplash/fanuc_small.png"))); // NOI18N

        javax.swing.GroupLayout jPanelRightLayout = new javax.swing.GroupLayout(jPanelRight);
        jPanelRight.setLayout(jPanelRightLayout);
        jPanelRightLayout.setHorizontalGroup(
            jPanelRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRightLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelRobotTextRight)
                    .addComponent(jLabelRobotIconRight))
                .addContainerGap(55, Short.MAX_VALUE))
        );
        jPanelRightLayout.setVerticalGroup(
            jPanelRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRightLayout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(jLabelRobotTextRight)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelRobotIconRight)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanelBothLayout = new javax.swing.GroupLayout(jPanelBoth);
        jPanelBoth.setLayout(jPanelBothLayout);
        jPanelBothLayout.setHorizontalGroup(
            jPanelBothLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBothLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelLeft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelRight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelBothLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jPanelLeft, jPanelRight});

        jPanelBothLayout.setVerticalGroup(
            jPanelBothLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBothLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelBothLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanelRight, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelLeft, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelBoth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelBoth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabelRobotIconLeft;
    private javax.swing.JLabel jLabelRobotIconRight;
    private javax.swing.JLabel jLabelRobotTextLeft;
    private javax.swing.JLabel jLabelRobotTextRight;
    private javax.swing.JPanel jPanelBoth;
    private javax.swing.JPanel jPanelLeft;
    private javax.swing.JPanel jPanelRight;
    // End of variables declaration//GEN-END:variables
}
