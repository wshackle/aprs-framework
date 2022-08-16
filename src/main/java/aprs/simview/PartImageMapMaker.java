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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PartImageMapMaker {
    
    public static Map<String, PartImageInfo> createPartImageMap() {
        Map<String, PartImageInfo> partImageMap = new HashMap<>();
        initializePartImageMap(partImageMap);
        return partImageMap;
    }
    
    private static void initializePartImageMap(Map<String, PartImageInfo> partImageMap) {
        try {
            BufferedImage fanucToolHolderRLImage = getImageFromResourceName("aprs/partImages/fanuc_tool_holderRL.png");
//            addPartImage(partImageMap, "private", fanucToolHolder2Image, 115.0, true, false, -60.0, 10.0);
//            addPartImage(partImageMap, "fanucBackLeftHolder", fanucToolHolder2Image, 115.0, true, false, 0.0, -60.0);
            addPartImage(partImageMap, "sharedHolder", fanucToolHolderRLImage, 115.0, true, false, -60.0, 10.0);
            addPartImage(partImageMap, "farLeftHolder", fanucToolHolderRLImage, 115.0, true, false, -60.0, 10.0);
            addPartImage(partImageMap, "nearLeftHolder", fanucToolHolderRLImage, 115.0, true, false, -60.0, 10.0);
//            addPartImage(partImageMap, "sharedBackCenterHolder", fanucToolHolder2Image, 115.0, true, false, 0.0, -60.0);
//            addPartImage(partImageMap, "small_private_holder", fanucToolHolder2Image, 115.0, true, false, 0.0, -60.0);
//            addPartImage(partImageMap, "motomanBackCenterRightHolder", fanucToolHolder2Image, 115.0, true, false, 0.0, -60.0);
            addPartImage(partImageMap, "shared_with_fanuc_holder", fanucToolHolderRLImage, 115.0, true, false, 10.0, -60.0);
            BufferedImage pincherSmallVacTopImage = getImageFromResourceName("aprs/partImages/pincher_small_vac_top.png");

            addPartImage(partImageMap, "pincher", pincherSmallVacTopImage, 115.0, true, false, -4.0, 10.0);
            addPartImage(partImageMap, "small_vacuum", pincherSmallVacTopImage, 115.0, true, false, -4.0, 10.0);
            addPartImage(partImageMap, "big_gripper_holder", "aprs/partImages/big_gripper_holder2.png", 125.0, true, false, 0.0, 0.0);
            addPartImage(partImageMap, "big_vacuum_holder", "aprs/partImages/big_vacuum_holder.png", 215.0, true, false, 0.0, -15.0);
            addPartImage(partImageMap, "big_vacuum", "aprs/partImages/big_vacuum.png", 215.0, true, false, 0.0, -15.0);
            addPartImage(partImageMap, "big_gripper", "aprs/partImages/big_vacuum.png", 125.0, true, false, 0.0, 0.0);
            addPartImage(partImageMap, "sku_part_medium_gear", "aprs/partImages/medium_orange_gear.png", 75.0);
            addPartImage(partImageMap, "sku_part_large_gear", "aprs/partImages/large_green_gear.png", 100.0);
             addPartImage(partImageMap, "sku_part_black_gear", "aprs/partImages/black_gear.png", 90.0);
            addPartImage(partImageMap, "sku_part_small_gear", "aprs/partImages/small_yellow_gear.png", 45.0);
            addPartImage(partImageMap, "sku_kit_s2l2_vessel", "aprs/partImages/red_s2l2_kit_tray_up.png", 220.0);
            addPartImage(partImageMap, "sku_large_gear_vessel", "aprs/partImages/purple_large_gear_tray_horz.png", 220.0);
            addPartImage(partImageMap, "sku_medium_gear_vessel", "aprs/partImages/blue_medium_gear_parts_tray.png", 160.0);
            addPartImage(partImageMap, "sku_small_gear_vessel", "aprs/partImages/orange_small_gear_parts_tray.png", 110.0);
            addPartImage(partImageMap, "sku_kit_m2l1_vessel", "aprs/partImages/m2l1_kit_tray_right.png", 190.0);
           
        } catch (Exception ex) {
            Logger.getLogger(Object2DJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }
    
    private static void addPartImage(Map<String, PartImageInfo> partImageMap,
            String partName, String resName, double realWidth) {
        addPartImage(partImageMap, partName, resName, realWidth, false, false, 0.0, 0.0);
    }

    private static void addPartImage(Map<String, PartImageInfo> partImageMap,
            String partName, String resName, double realWidth, boolean ignoreRotations, boolean useHeight, double xoffset, double yoffset) {
        try {
            BufferedImage img = getImageFromResourceName(resName);
            addPartImage(partImageMap, partName, img, realWidth, ignoreRotations, useHeight, xoffset, yoffset);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void addPartImage(Map<String, PartImageInfo> partImageMap,
            String partName, BufferedImage img, double realWidth, boolean ignoreRotations, boolean useHeight, double xoffset, double yoffset) {
        try {
            if (null != img) {
                double ratio = useHeight ? realWidth / img.getHeight() : realWidth / img.getWidth();
                if (null != partImageMap) {
                    partImageMap.put(partName, new PartImageInfo(img, ratio, realWidth, ignoreRotations, xoffset, yoffset));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static private BufferedImage getImageFromResourceName(String resName) throws IOException {
        URL url = null;
        try {
            if (null == url) {
                ClassLoader cl = Object2DJPanel.class.getClassLoader();
                if (null != cl) {
                    url = cl.getResource(resName);//.getResource(resName);
                }
            }
        } catch (Exception ignored) {
        }
        try {
            if (null == url) {
                url = ClassLoader.getSystemResource(resName);//.getResource(resName);
            }
        } catch (Exception ignored) {
        }
        try {
            if (null == url) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (null != cl) {
                    url = cl.getResource(resName);//.getResource(resName);
                }
            }
        } catch (Exception ignored) {
        }
        if (null == url) {
            throw new IllegalArgumentException("ClassLoader.getSystemResource(" + resName + ") returned null");
        }
        return ImageIO.read(url);
    }

}
