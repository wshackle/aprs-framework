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
package aprs.supervisor.screensplash;

import aprs.misc.Utils;
import crcl.utils.CRCLUtils;
import crcl.utils.XFutureVoid;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"SameReturnValue", "unused","serial"})
public class SplashScreen extends JFrame {

    private final SplashPanel panel;

    private static class SplashPanel extends JPanel {

        private final Font font;

        @UIEffect
        @SuppressWarnings({"nullness","initialization"})
        SplashPanel(String message, float fontSize, @Nullable Image image) {
            this.messageLines = message.split("[\r\n]+");
            this.fontSize = fontSize;
            font = super.getFont().deriveFont(fontSize);
            this.image = image;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Color color = this.getBackground();
            g.setFont(font);
            if (null != messageLines) {
                int fontHeight = g.getFontMetrics(font).getHeight() + g.getFontMetrics(font).getLeading() + 1;
                for (int i = 0; i < messageLines.length; i++) {
                    g.drawString(messageLines[i], this.getSize().width / 6, this.getSize().height / 6 + i * fontHeight);
                }

            }
            if (null != image) {
                drawImageNoObserver(g);
            }
        }

        @SuppressWarnings("nullness")
        private void drawImageNoObserver(Graphics g) {
            g.drawImage(image, this.getSize().width / 3, this.getSize().height / 3, null);
        }

        final String messageLines[];
        final float fontSize;

        final @MonotonicNonNull
        Image image;
    }

    @UIEffect
    @SuppressWarnings({"nullness","initialization"})
    private SplashScreen(String message, float fontSize, @Nullable Image image) {
        super.setUndecorated(true);
        this.panel = new SplashPanel(message, fontSize, image);
        super.add(panel);
    }

    private volatile javax.swing.@Nullable Timer timer = null;

    @UIEffect
    private void close(GraphicsDevice gd, XFutureVoid returnFuture) {
        clearFullScreenMode(gd);
        setVisible(false);
        if (null != timer) {
            timer.stop();
        }
        returnFuture.complete(null);
        dispose();
    }

    @SuppressWarnings("nullness")
    private void clearFullScreenMode(GraphicsDevice gd) {
        gd.setFullScreenWindow(null);
    }

    private static class RobotArmImageHider {

        static final @MonotonicNonNull
        BufferedImage ROBOT_ARM_IMAGE = readImageOrNull("robot-arm.jpeg");

    }

    private static @Nullable
    BufferedImage readImageOrNull(String name) {
        try {
            InputStream stream = SplashScreen.class.getResourceAsStream(name);
            if (null == stream) {
                Logger.getLogger(SplashScreen.class.getName()).log(Level.WARNING, "getResourceAsStream({0}) returned null", name);
                return null;
            }
            return ImageIO.read(stream);
        } catch (IOException ex) {
            Logger.getLogger(SplashScreen.class.getName()).log(Level.SEVERE, "", ex);
        }
        return null;
    }

    static public @Nullable
    Image getRobotArmImage() {
        return RobotArmImageHider.ROBOT_ARM_IMAGE;
    }

    private static class DisableImageHider {

        static final @MonotonicNonNull
        BufferedImage DISABLED_IMAGE = readImageOrNull("DisabledRobotHalf.jpg");

    }

    public static @Nullable
    Image getDisableImageImage() {
        return DisableImageHider.DISABLED_IMAGE;
    }

    private static class ListHider {

        static final List<Color> RED_YELLOW_COLOR_LIST
                = Arrays.asList(Color.red, Color.yellow, Color.red, Color.yellow, Color.red, Color.yellow);

        static final List<Color> BLUE_WHITE_GREEN_COLOR_LIST
                = Arrays.asList(Color.blue, Color.white, Color.green, Color.blue, Color.white, Color.green);

    }

    public static List<Color> getRedYellowColorList() {
        return ListHider.RED_YELLOW_COLOR_LIST;
    }

    public static List<Color> getBlueWhiteGreenColorList() {
        return ListHider.BLUE_WHITE_GREEN_COLOR_LIST;
    }

    public static XFutureVoid showMessageFullScreen(String message, float fontSize, @Nullable Image image, List<Color> colors, @Nullable GraphicsDevice graphicsDevice) {
        if(CRCLUtils.isGraphicsEnvironmentHeadless()) {
            return XFutureVoid.completedFuture();
        }
        XFutureVoid returnFuture = new XFutureVoid("showMessageFullScreen(" + message + ")");
        XFutureVoid step1Future = Utils.runOnDispatchThread("showMessageFullScreen(" + message + ").start", () -> {
            showMessageFullScreenOnDisplay(returnFuture,message, fontSize, image, colors, graphicsDevice);
        });
        return step1Future.thenComposeAsyncToVoid(() -> returnFuture,Utils.getDispatchThreadExecutorService());
    }

    private static void showMessageFullScreenOnDisplay(XFutureVoid returnFuture, String message, float fontSize,@Nullable Image image, List<Color> colors, @Nullable GraphicsDevice graphicsDevice) {
        SplashScreen ss = new SplashScreen(message, fontSize, image);
        ss.setVisible(true);
        GraphicsDevice gd0 = graphicsDevice;
        if (null == gd0) {
            final GraphicsConfiguration graphicsConfiguration = ss.getGraphicsConfiguration();
            if(null == graphicsConfiguration) {
                returnFuture.complete();
                return;
            }
            gd0 = graphicsConfiguration.getDevice();
        }
        gd0.setFullScreenWindow(ss);
        
        final GraphicsDevice gd = gd0;
        javax.swing.Timer ssTimer
                = new javax.swing.Timer(500, new ActionListener() {
                    int colorIndex = 0;
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            if (colorIndex < colors.size()) {
                                Color color = colors.get(colorIndex);
                                ss.setBackground(color);
                                ss.panel.setBackground(color);
                                ss.repaint();
                                colorIndex++;
                            } else {
                                ss.close(gd, returnFuture);
                            }
                        } catch (Exception exception) {
                            returnFuture.completeExceptionally(exception);
                            Logger.getLogger(SplashScreen.class.getName()).log(Level.WARNING, "", exception);
                        }
                    }
                });
        
        KeyListener kl = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                ssTimer.stop();
                ss.close(gd, returnFuture);
            }
            
            @Override
            public void keyPressed(KeyEvent e) {
                ssTimer.stop();
                ss.close(gd, returnFuture);
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
            }
        };
        ss.addKeyListener(kl);
        MouseListener ml = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                ssTimer.stop();
                ss.close(gd, returnFuture);
            }
        };
        ss.timer = ssTimer;
        ss.addMouseListener(ml);
        ssTimer.start();
        
    }

    public static void main(String[] args) {
        showMessageFullScreen("my \nmessage", 80.0f,
                getRobotArmImage(),
                getBlueWhiteGreenColorList(), null);
    }
}
