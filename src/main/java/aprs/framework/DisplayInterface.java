/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aprs.framework;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public interface DisplayInterface {
    
    public File getPropertiesFile();
    public void setPropertiesFile(File propertiesFile);
    public void saveProperties() throws IOException;
    public void loadProperties() throws IOException;
}
