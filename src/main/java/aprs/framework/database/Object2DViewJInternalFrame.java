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
package aprs.framework.database;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class Object2DViewJInternalFrame extends javax.swing.JInternalFrame implements Object2DJFrameInterface {

    /**
     * Creates new form Object2DViewJInternalFrame
     */
    public Object2DViewJInternalFrame() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        object2DOuterJPanel1 = new aprs.framework.database.Object2DOuterJPanel();

        setTitle("[Cognex] Object2D View/Simulate");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(object2DOuterJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.framework.database.Object2DOuterJPanel object2DOuterJPanel1;
    // End of variables declaration//GEN-END:variables

    @Override
    public List<DetectedItem> getItems() {
        return object2DOuterJPanel1.getItems();
    }

    @Override
    public void setItems(List<DetectedItem> items) {
        object2DOuterJPanel1.setItems(items);
    }

    @Override
    public void setPropertiesFile(File f) {
        object2DOuterJPanel1.setPropertiesFile(f);
    }

    @Override
    public File getPropertiesFile() {
       return object2DOuterJPanel1.getPropertiesFile();
    }

    @Override
    public void saveProperties() throws IOException {
       object2DOuterJPanel1.saveProperties();
    }

    @Override
    public void restoreProperties() throws IOException {
        object2DOuterJPanel1.restoreProperties();
    }
    
}
