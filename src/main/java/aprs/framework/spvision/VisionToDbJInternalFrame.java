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

package aprs.framework.spvision;

import aprs.framework.database.DbSetupPublisher;
import aprs.framework.database.DbType;
import aprs.framework.database.DetectedItem;
import aprs.framework.database.Main;
import aprs.framework.database.PoseQueryElem;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class VisionToDbJInternalFrame extends javax.swing.JInternalFrame implements VisionToDBJFrameInterface {

    /**
     * Creates new form VisionToDbJInternalFrame
     */
    public VisionToDbJInternalFrame() {
        initComponents();
        Main.setDisplayInterface(this);
    }
    
    public DbSetupPublisher getDbSetupPublisher() {
        return this.visionToDBJPanel.getDbSetupPublisher();
    }
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        visionToDBJPanel = new aprs.framework.spvision.VisionToDBJPanel();

        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("[Object SP] Vision To Database");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1004, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(visionToDBJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 746, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(visionToDBJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 722, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public void setAquiring(String s) {
        this.visionToDBJPanel.setAquiring(s);
    }

    public void updateInfo(List<DetectedItem> _list, String line) {
        this.visionToDBJPanel.updateInfo(_list, line);
    }
    
    public void updataPoseQueryInfo(final List<PoseQueryElem> _list) {
        this.visionToDBJPanel.updataPoseQueryInfo(_list);
    }
    
    public boolean isDebug() {
        return this.visionToDBJPanel.isDebug();
    }
    
    public void addLogMessage(String stmnt) {
        this.visionToDBJPanel.addLogMessage(stmnt);
    }
    
    public void setVisionConnected(boolean _val) {
        this.visionToDBJPanel.setVisionConnected(_val);
    }
    
    public void setDBConnected(boolean _val) {
        this.visionToDBJPanel.setDBConnected(_val);
    }
    
     public void setLastCommand(String c) {
         this.visionToDBJPanel.setLastCommand(c);
     }
     
     public void setCommandConnected(boolean _val) {
         this.visionToDBJPanel.setCommandConnected(_val);
     }
     
     public void startCommand(Map<String, String> argsMap) {
         Main.startCommand(argsMap);
     }

     public void startVision(Map<String, String> argsMap) {
         Main.startVision(argsMap);
     }
     
     public Map<String,String> getArgsMap() {
         return Main.getArgsMap();
     }
     
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private aprs.framework.spvision.VisionToDBJPanel visionToDBJPanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void updateFromArgs(Map<String, String> _argsMap) {
        this.visionToDBJPanel.updateFromArgs(_argsMap);
    }

    @Override
    public void setPropertiesFile(File f) {
        this.visionToDBJPanel.setPropertiesFile(f);
    }

    @Override
    public File getPropertiesFile() {
        return this.visionToDBJPanel.getPropertiesFile();
    }

    @Override
    public void saveProperties() {
       this.visionToDBJPanel.saveProperties();
    }

    @Override
    public void restoreProperties() {
        this.visionToDBJPanel.restoreProperties();
    }

    @Override
    public Map<String, String> updateArgsMap() {
        return visionToDBJPanel.updateArgsMap();
    }

    @Override
    public void addLogMessage(Exception exception) {
        visionToDBJPanel.addLogMessage(exception);
    }

    @Override
    public Connection getSqlConnection() {
        return visionToDBJPanel.getSqlConnection();
    }

    @Override
    public DbType getDbType() {
        return visionToDBJPanel.getDbType();
    }

    @Override
    public void setSqlConnection(Connection connection, DbType dbtype) throws SQLException {
        visionToDBJPanel.setSqlConnection(connection, dbtype);
    }

    @Override
    public Callable<DbSetupPublisher> getDbSetupSupplier() {
        return visionToDBJPanel.getDbSetupSupplier();
    }

    @Override
    public void setDbSetupSupplier(Callable<DbSetupPublisher> dbSetupSupplier) {
        visionToDBJPanel.setDbSetupSupplier(dbSetupSupplier);
    }

    @Override
    public void connectVision() {
        visionToDBJPanel.connectVision();
    }
}
