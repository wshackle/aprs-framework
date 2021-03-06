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
package aprs.kitinspection;

import aprs.misc.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author zeid
 */
@SuppressWarnings({"all", "serial"})
public class KitInspectionJInternalFrame extends javax.swing.JInternalFrame {

    /**
     * Creates new form Test
     */
    @SuppressWarnings({"nullness","initialization"})
    public KitInspectionJInternalFrame() {
        initComponents();
        doc = (HTMLDocument) InspectionResultJTextPane.getDocument();
        editorKit = (HTMLEditorKit) InspectionResultJTextPane.getEditorKit();
        DefaultCaret caret = (DefaultCaret) InspectionResultJTextPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "nullness"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        kitImageLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        kitTitleLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        InspectionResultJTextPane = new javax.swing.JTextPane();

        setBackground(new java.awt.Color(255, 255, 255));
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Kit Inspection");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(0, 412, Short.MAX_VALUE)
                    .addComponent(kitImageLabel)
                    .addGap(0, 413, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 319, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(kitImageLabel)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );

        jPanel2.setBackground(new java.awt.Color(255, 204, 0));
        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        kitTitleLabel.setFont(new java.awt.Font("Tahoma", Font.BOLD, 14)); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addGap(0, 412, Short.MAX_VALUE)
                    .addComponent(kitTitleLabel)
                    .addGap(0, 413, Short.MAX_VALUE)))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 82, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addGap(0, 32, Short.MAX_VALUE)
                    .addComponent(kitTitleLabel)
                    .addGap(0, 33, Short.MAX_VALUE)))
        );

        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));
        jScrollPane1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        InspectionResultJTextPane.setEditable(false);
        InspectionResultJTextPane.setBorder(null);
        InspectionResultJTextPane.setContentType("text/html");
        jScrollPane1.setViewportView(InspectionResultJTextPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 861, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        Utils.setToAprsLookAndFeel();

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new KitInspectionJInternalFrame().setVisible(true);
            }
        });
    }

    public javax.swing.JLabel getKitTitleLabel() {
        return this.kitTitleLabel;
    }

//    public javax.swing.JPanel getTitleJPanel() {
//        return titleJPanel;
//    }
    public javax.swing.JLabel getKitImageLabel() {
        return this.kitImageLabel;
    }

    public javax.swing.JTextPane getInspectionResultJTextArea() {
        return this.InspectionResultJTextPane;
    }
    private String kitImage = "complete";

    public String getKitImage() {
        return this.kitImage;
    }

    public void setKitImage(String kitImage) {
        this.kitImage = kitImage;
    }

    public void addToInspectionResultJTextPane(String text) throws BadLocationException {
        try {
            editorKit.insertHTML(doc, doc.getLength(), text, 0, 0, null);
        } catch (IOException ex) {
            Logger.getLogger(KitInspectionJInternalFrame.class.getName()).log(Level.SEVERE, "", ex);
        }
        String oldtext = InspectionResultJTextPane.getText();
        InspectionResultJTextPane.setText(oldtext.concat(text + "\n"));
    }

    private @Nullable
    File propertiesFile = null;

    /**
     * Get the value of propertiesFile
     *
     * @return the value of propertiesFile
     */
    public @Nullable File getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * Set the value of propertiesFile
     *
     * @param propertiesFile new value of propertiesFile
     */
    public void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    public javax.swing.JTextPane getInspectionResultJTextPane() {
        return InspectionResultJTextPane;
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     *
     * @param path resource path to image
     * @return new image
     */
    public @Nullable
    ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    public void saveProperties() {
        Properties props = new Properties();
        props.put(IMAGE_KIT_PATH, kitinspectionImageKitPath);
        props.put(IMAGE_EMPTY_KIT, "123.png");
        if (null == propertiesFile) {
            throw new IllegalStateException("propertiesFile not set");
        }
        Utils.saveProperties(propertiesFile, props);
    }

    public void loadProperties() throws IOException {
        if (null == propertiesFile) {
            throw new IllegalStateException("propertiesFile not set");
        }
        if (propertiesFile.exists()) {
            Properties props = new Properties();
            try (FileReader fr = new FileReader(propertiesFile)) {
                props.load(fr);
            }

            kitinspectionImageKitPath = props.getProperty(IMAGE_KIT_PATH, DEFAULT_KIT_M2L1_VESSEL_PATH);
            String kitinspectionImageEmptyKit = props.getProperty(IMAGE_EMPTY_KIT, "init.png");
            if (null != kitinspectionImageKitPath) {
                if (null != kitinspectionImageEmptyKit) {
                    String originalImage = kitinspectionImageKitPath + "/" + kitinspectionImageEmptyKit;
                    kitImageLabel.setIcon(createImageIcon(originalImage)); // NOI18N
                }
            }

            String robot = props.getProperty(ROBOT_NAME);
            if (null != robot) {
                //jTextFieldPlannerProgramExecutable.setText(executable);

                String kitsku = props.getProperty(KIT_SKU);
                if (null != kitsku) {
                    getKitTitleLabel().setText("Waiting for Commands");
                }
            }

        } else {
            kitinspectionImageKitPath = DEFAULT_KIT_M2L1_VESSEL_PATH;
        }
    }
    private static final String DEFAULT_KIT_M2L1_VESSEL_PATH = "/aprs/kitinspection/sku_kit_m2l1_vessel";

    private static final String ROBOT_NAME = "kitinspection.robot";
    private static final String IMAGE_KIT_PATH = "kitinspection.image.kit.path";
    private static final String IMAGE_EMPTY_KIT = "kitinspection.image.empty.kit";
    private static final String KIT_SKU = "kitinspection.kit.sku";
    private static final String BANNER_COLOR = "kitinspection.banner.color";
    private static final String WARNING_COLOR = "kitinspection.warning.color";

    private String kitinspectionImageKitPath;

    public String getKitinspectionImageKitPath() {
        return kitinspectionImageKitPath;
    }

    private HTMLEditorKit editorKit;
    private HTMLDocument doc;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextPane InspectionResultJTextPane;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel kitImageLabel;
    private javax.swing.JLabel kitTitleLabel;
    // End of variables declaration//GEN-END:variables
}
