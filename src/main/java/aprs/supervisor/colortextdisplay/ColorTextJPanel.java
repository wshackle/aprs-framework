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

import aprs.misc.Utils;
import aprs.database.SocketLineReader;
import crcl.utils.XFuture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import org.checkerframework.checker.guieffect.qual.UIType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"MagicConstant","serial"})
@UIType
public class ColorTextJPanel extends javax.swing.JPanel {

    /**
     * Creates new form ColorTextJPanel
     */
    @SuppressWarnings({"nullness","initialization"})
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
    public void setLabelsAndIcons(String leftLabel, @Nullable Icon leftImage, String rightLabel, @Nullable Icon rightImage) {
        jLabelRobotTextLeft.setText(leftLabel);
        jLabelRobotTextRight.setText(rightLabel);
        if (null != leftImage) {
            jLabelRobotIconLeft.setIcon(leftImage);
        }
        if (null != rightImage) {
            jLabelRobotIconRight.setIcon(rightImage);
        }
    }

    private static final Class<aprs.supervisor.colortextdisplay.ColorTextJPanel> THIS_CLASS = aprs.supervisor.colortextdisplay.ColorTextJPanel.class;

    /**
     * Get the Icon for the given robot name. The Icon files are normally stored
     * as resources in the same jar.
     *
     * @param name name of the robot
     * @return icon
     */
    public static @Nullable
    Icon getRobotIcon(String name) {
        if (name == null || name.length() < 1) {
            return null;
        }
        if (name.toUpperCase().contains("MOTOMAN")) {
            return getClassResourceIcon("/aprs/supervisor/screensplash/motoman_small.png");
        } else if (name.toUpperCase().contains("FANUC")) {
            return getClassResourceIcon("/aprs/supervisor/screensplash/fanuc_small.png");
        } else {
            return getClassResourceIcon("/aprs/supervisor/screensplash/" + name.toLowerCase() + "_small.png");
        }
    }

    private static Icon getClassResourceIcon(String resourcePath) throws IllegalStateException {
        URL url = getClassResourceURL(resourcePath);
        return new javax.swing.ImageIcon(url);
    }

    private static final ConcurrentHashMap<String, BufferedImage> robotImageMap = new ConcurrentHashMap<>();

    /**
     * Get the Icon for the given robot name. The Icon files are normally stored
     * as resources in the same jar.
     *
     * @param name name of the robot
     * @return icon
     */
    public static @Nullable
    BufferedImage getRobotImage(String name) throws IllegalStateException {
        if (name == null || name.length() < 1) {
            return null;
        }
        return robotImageMap.computeIfAbsent(name, ColorTextJPanel::getRobotImageNew);
    }

    private static BufferedImage getRobotImageNew(String name) {
        try {
            if (name.toUpperCase().contains("MOTOMAN")) {
                return getClassResourceImage("/aprs/supervisor/screensplash/motoman_small.png");
            } else if (name.toUpperCase().contains("FANUC")) {
                return getClassResourceImage("/aprs/supervisor/screensplash/fanuc_small.png");
            } else {
                return getClassResourceImage("/aprs/supervisor/screensplash/" + name.toLowerCase() + "_small.png");
            }
        } catch (IllegalStateException | IOException ex) {
            Logger.getLogger(ColorTextJFrame.class.getName()).log(Level.SEVERE, "", ex);
            BufferedImage img = new BufferedImage(BufferedImage.TYPE_3BYTE_BGR, 100, 100);
            img.getGraphics().drawString(ex.toString(), 10, 10);
            return img;
        }
    }

    private static BufferedImage getClassResourceImage(String resourcePath) throws IllegalStateException, IOException {
        URL url = getClassResourceURL(resourcePath);
        return ImageIO.read(url);
    }

    private static URL getClassResourceURL(String resourcePath) throws IllegalStateException {
        URL url = THIS_CLASS.getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("getResource(\"" + resourcePath + "\") returned null");
        }
        return url;
    }

    /**
     * Default TCP port for providing the color and text labels to indicate when
     * robots are disabled.
     */
    public static final int COLORTEXT_SOCKET_PORT = 23444;

    private @Nullable
    SocketLineReader reader;

    private volatile StackTraceElement startReaderTrace @Nullable [] = null;
    
    /**
     * Start a separate thread to read a socket for messages to change colors
     * etc.
     */
    public void startReader() {
        try {
            if(null != startReaderTrace) {
                throw new RuntimeException("startReader already called. startReaderTrace="+XFuture.traceToString(startReaderTrace));
            }
            startReaderTrace = Thread.currentThread().getStackTrace();
            SocketLineReader readerTmp 
                    = SocketLineReader.startServer(COLORTEXT_SOCKET_PORT, "ColorTextServer", this::parseSocketLine);
            this.reader = readerTmp;
        } catch (Exception e) {
            Logger.getLogger(ColorTextJFrame.class.getName()).log(Level.SEVERE, "Failed to bind color text socket port: " + COLORTEXT_SOCKET_PORT, e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private volatile boolean stopReading = false;

    /**
     * Stop thread and close socket created with startReader.
     */
    public void stopReader() {
        stopReading = true;
        if (null != reader) {
            reader.close();
            reader = null;
            startReaderTrace = null;
        }
    }

    private void parseSocketLine(String line, @Nullable PrintStream returnStream) {
        if (stopReading) {
            return;
        }
        String colorStrings[] = line.split("[ \t,]+");
        Color color1;
        Color color2;
        try {
            if (null != returnStream) {
                returnStream.println(Arrays.toString(colorStrings));
            }
            if (colorStrings.length != 2) {
                if (null != returnStream) {
                    returnStream.println("2 colors expected : line " + line);
                }
                System.err.println("2 colors expected : line=" + line);
                System.err.println("colorStrings=" + Arrays.toString(colorStrings));
                stopReader();
                return;
            }
            color1 = Color.decode(colorStrings[1]);
            color2 = Color.decode(colorStrings[0]);
            if (null != returnStream) {
                returnStream.println("color1 = " + color1);
                returnStream.println("color2 = " + color2);
            }
        } catch (NumberFormatException numberFormatException) {
            Logger.getLogger(ColorTextJFrame.class.getName()).log(Level.SEVERE,
                    "line=" + line + "\ncolorStrings=" + Arrays.toString(colorStrings),
                    numberFormatException);
            stopReader();
            throw new RuntimeException(numberFormatException);
        }
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
    @SuppressWarnings({"unchecked", "nullness"})
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

        jLabelRobotTextLeft.setFont(new java.awt.Font("DejaVu Sans", 0, 24)); // NOI18N
        jLabelRobotTextLeft.setText("Motoman");

        jLabelRobotIconLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/aprs/supervisor/screensplash/motoman_small.png"))); // NOI18N

        javax.swing.GroupLayout jPanelLeftLayout = new javax.swing.GroupLayout(jPanelLeft);
        jPanelLeft.setLayout(jPanelLeftLayout);
        jPanelLeftLayout.setHorizontalGroup(
            jPanelLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLeftLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelRobotTextLeft)
                    .addComponent(jLabelRobotIconLeft))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelLeftLayout.setVerticalGroup(
            jPanelLeftLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLeftLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelRobotTextLeft)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelRobotIconLeft)
                .addContainerGap())
        );

        jPanelRight.setBackground(new java.awt.Color(204, 204, 255));
        jPanelRight.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabelRobotTextRight.setFont(new java.awt.Font("DejaVu Sans", 0, 24)); // NOI18N
        jLabelRobotTextRight.setText("Fanuc");

        jLabelRobotIconRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/aprs/supervisor/screensplash/fanuc_small.png"))); // NOI18N

        javax.swing.GroupLayout jPanelRightLayout = new javax.swing.GroupLayout(jPanelRight);
        jPanelRight.setLayout(jPanelRightLayout);
        jPanelRightLayout.setHorizontalGroup(
            jPanelRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRightLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelRobotTextRight)
                    .addComponent(jLabelRobotIconRight))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
