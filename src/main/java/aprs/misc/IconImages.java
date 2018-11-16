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
package aprs.misc;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class IconImages {

    private static final int ICON_IMAGE_WIDTH = 32;
    private static final int ICON_IMAGE_HEIGHT = 32;

    private static final Dimension ICON_SIZE = new Dimension(ICON_IMAGE_WIDTH, ICON_IMAGE_HEIGHT);
    public static final Image BASE_IMAGE = getBaseImage();
    public static final Image SERVER_IMAGE = createImage(ICON_SIZE, Color.MAGENTA, Color.BLACK, BASE_IMAGE);
    public static final Image DONE_IMAGE = createImage(ICON_SIZE, Color.white, Color.BLACK, BASE_IMAGE);
    public static final Image ERROR_IMAGE = createImage(ICON_SIZE, Color.red, Color.BLACK, BASE_IMAGE);
    public static final Image WORKING_IMAGE = createImage(ICON_SIZE, Color.green, Color.BLACK, BASE_IMAGE);
    public static final Image DISCONNECTED_IMAGE = createImage(ICON_SIZE, Color.GRAY, Color.BLACK, BASE_IMAGE);

    @MonotonicNonNull private static BufferedImage baseImage = null;

    private static class DefaultBaseImageLoader {

        final static BufferedImage DEFAULT_BASE_IMAGE = getDefaultBaseImage();
        
        private static BufferedImage getDefaultBaseImage() {
            try {
                URL baseImageUrl = Utils.getAprsIconUrl();
                if (null != baseImageUrl) {
                    BufferedImage bi = ImageIO.read(baseImageUrl);
//                    for (int x = 0; x < bi.getWidth(); x++) {
//                        for (int y = 0; y < bi.getHeight(); y++) {
//                            int color = bi.getRGB(x, y);
//                            if ((color & 0xFFFFFF) == 0xFFFFFF) {
//                                bi.setRGB(x, y, 0xFFFFFF);
//                            }
//                        }
//                    }
                    return bi;
                }
            } catch (Exception ex) {
                Logger.getLogger(IconImages.class.getName()).log(Level.SEVERE, null, ex);
            }
            return new BufferedImage(ICON_IMAGE_WIDTH, ICON_IMAGE_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
        }
    }

    private static BufferedImage getBaseImage() {
        if (null != baseImage) {
            return baseImage;
        }
        baseImage = DefaultBaseImageLoader.DEFAULT_BASE_IMAGE;
        return DefaultBaseImageLoader.DEFAULT_BASE_IMAGE;
    }

    private static Image createImage(Dimension d, Color bgColor, Color textColor, Image baseImage) {
        BufferedImage bi = new BufferedImage(d.width, d.height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = bi.createGraphics();
        g2d.setBackground(bgColor);
        g2d.setColor(textColor);
        g2d.clearRect(0, 0, d.width, d.height);
        g2d.setFont(new Font(g2d.getFont().getName(), g2d.getFont().getStyle(), 24));
        g2d.drawImage(baseImage, 0, 0, null);
        bi.flush();
        return bi;
    }

}
