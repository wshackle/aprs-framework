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
package aprs.database;

import aprs.system.AprsSystem;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import org.checkerframework.checker.guieffect.qual.UIEffect;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class DbSetupJInternalFrame extends javax.swing.JInternalFrame {

    /**
     * Creates new form DbSetupJInternalFrame
     */
    @SuppressWarnings({"initialization","nullness"})
    @Deprecated
    @UIEffect
    public DbSetupJInternalFrame() {
        this(null);
    }
    
    /**
     * Creates new form DbSetupJInternalFrame
     */
    @SuppressWarnings("initialization")
    @UIEffect
    public DbSetupJInternalFrame(AprsSystem aprsSystem) {
        dbSetupJPanel1 = new aprs.database.DbSetupJPanel(aprsSystem);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Database Setup");
        add(dbSetupJPanel1);
        pack();
    }

    /**
     * Load the most recent settings file.
     */
    @UIEffect
    public void loadRecentSettings() {
        dbSetupJPanel1.loadRecentSettings();
    }

   
    /**
     * Get name of a script file to execute to start the database server if
     * available
     *
     * @return name of start script file
     */
    public String getStartScript() {
        return dbSetupJPanel1.getStartScript();
    }

    /**
     * Get a source for future database setup information.
     *
     * @return database setup publisher
     */
    public DbSetupPublisher getDbSetupPublisher() {
        return this.dbSetupJPanel1;
    }

    /**
     * Set the properties file
     *
     * @param f new value of propertiesFile
     */
    public void setPropertiesFile(File f) {
        dbSetupJPanel1.setPropertiesFile(f);
    }

    /**
     * Get the current properties file.
     *
     * @return properties file
     */
    public @Nullable
    File getPropertiesFile() {
        return dbSetupJPanel1.getPropertiesFile();
    }

    private final aprs.database.DbSetupJPanel dbSetupJPanel1;
}
