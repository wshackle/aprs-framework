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
package aprs.misc;

import java.io.File;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Common Interface implemented by the JInternalFrame of each submodule.
 * 
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"unused", "RedundantThrows"})
public interface DisplayInterface {
   
    /**
     * Set the properties file   
     * @param propertiesFile new value of propertiesFile
     */
    public void setPropertiesFile(File propertiesFile);
    
    @Nullable public File getPropertiesFile();
    

    /**
     * Write current settings to the properties file.
     * @throws IOException if writing file fails
     */
    public void saveProperties() throws IOException;

    /**
     * Read settings from the current properties file.
     * @throws IOException if reading file fails.
     */
    public void loadProperties() throws IOException;
    
    /**
     * Close the Frame/Window associated with this interface.
     */
    public void close();
}