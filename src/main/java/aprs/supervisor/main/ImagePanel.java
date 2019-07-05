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
package aprs.supervisor.main;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import org.checkerframework.checker.guieffect.qual.UIEffect;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
class ImagePanel extends JPanel {

    private BufferedImage image;

    private String label;

    @UIEffect
    @SuppressWarnings("initialization")
    public ImagePanel(BufferedImage image) {
        this(image, "");
    }

    @UIEffect
    @SuppressWarnings("initialization")
    public ImagePanel(BufferedImage image, String label) {
        this.image = image;
        this.label = label;
        if (image != null) {
            if (null != label) {
                super.setSize(image.getWidth(), image.getHeight());
                super.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            } else {
                super.setSize(image.getWidth(), image.getHeight() + 30);
                super.setPreferredSize(new Dimension(image.getWidth(), image.getHeight() + 30));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            if (null == label) {
                g.drawImage(image, 0, 0, this);
            } else {
                g.drawImage(image, 0, 20, this);
            }
        }
        if (null != label) {
            g.drawString(label, 10, 15);
        }
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        if (image != null) {
            this.setSize(image.getWidth(), image.getHeight());
            this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            repaint();
        }
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
