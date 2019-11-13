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

import java.awt.Image;
import java.awt.image.BufferedImage;
import org.checkerframework.checker.guieffect.qual.UIType;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@UIType
public class PartImageInfo {

    final BufferedImage image;
    final double ratio;
    final double width;
    final boolean ignoreRotations;
    final double xoffset;
    final double yoffset;
    double scale;
    Image scaledImage;
    int scaledImageWidth;
    int scaledImageHeight;

    PartImageInfo(BufferedImage image, double ratio, double width, boolean ignoreRotations, double xoffset, double yoffset) {
        this.image = image;
        this.ratio = ratio;
        this.width = width;
        this.xoffset = xoffset;
        this.yoffset = yoffset;
        this.ignoreRotations = ignoreRotations;
        scale = 1.0;
        scaledImageWidth = image.getWidth();
        scaledImageHeight = image.getHeight();
        if (ratio <= Double.MIN_NORMAL) {
            throw new IllegalArgumentException("ratio must be strictly  greater than 0. ratio=" + ratio);
        }
        scaledImage = image.getScaledInstance(scaledImageWidth, scaledImageHeight, Image.SCALE_DEFAULT);
    }

    public int getScaledImageWidth() {
        return scaledImageWidth;
    }

    public int getScaledImageHeight() {
        return scaledImageHeight;
    }

    Image getScaledImage(double scale) {
        if (scale <= Double.MIN_NORMAL) {
            throw new IllegalArgumentException("scale must be strictly  greater than 0. scale=" + scale);
        }
        int old_w = (int) (ratio * this.scale * image.getWidth());
        int old_h = (int) (ratio * this.scale * image.getHeight());
        int new_w = (int) (ratio * scale * image.getWidth());
        int new_h = (int) (ratio * scale * image.getHeight());
        if (new_w == old_w && new_h == old_h) {
            return scaledImage;
        }
        if (new_w < 2) {
            new_w = 2;
        }
        if (new_h < 2) {
            new_h = 2;
        }
        this.scale = scale;
        scaledImageWidth = new_w;
        scaledImageHeight = new_h;
        scaledImage = image.getScaledInstance(new_w, new_h, Image.SCALE_DEFAULT);
        return scaledImage;
    }

}
