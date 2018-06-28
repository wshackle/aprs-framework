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
package aprs.pddl_planner;

import aprs.actions.executor.ExecutorJInternalFrame;
import aprs.misc.DisplayInterface;
import java.io.File;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PddlPlannerJInternalFrame extends javax.swing.JInternalFrame implements DisplayInterface{

    /**
     * Creates new form PddlPlannerJInternalFrame
     */
    @SuppressWarnings("initialization")
    public PddlPlannerJInternalFrame() {
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

        pddlPlannerJPanel1 = new aprs.pddl_planner.PddlPlannerJPanel();

        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("PDDL Planner");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pddlPlannerJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pddlPlannerJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.pddl_planner.PddlPlannerJPanel pddlPlannerJPanel1;
    // End of variables declaration//GEN-END:variables


    @Override
    public void setPropertiesFile(File propertiesFile) {
        pddlPlannerJPanel1.setPropertiesFile(propertiesFile);
    }

    @Override
    public void saveProperties() {
        pddlPlannerJPanel1.saveProperties();
    }

    @Override
    public void loadProperties() throws IOException {
        pddlPlannerJPanel1.loadProperties();
    }

    public void setActionsToCrclJInternalFrame1(@Nullable ExecutorJInternalFrame actionsToCrclJInternalFrame1) {
        this.pddlPlannerJPanel1.setActionsToCrclJInternalFrame1(actionsToCrclJInternalFrame1);
    }
    
    @SuppressWarnings("RedundantThrows")
    @Override
    public void close() {
        pddlPlannerJPanel1.close();
        this.setVisible(false);
    }

    @Override
    @Nullable public File getPropertiesFile() {
        return pddlPlannerJPanel1.getPropertiesFile();
    }
}