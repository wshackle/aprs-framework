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
package aprs.conveyor;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings({"nullness","initialization"})
public class JModTest {

    protected static ModbusTCPMaster master;

    private static final Logger logger = Logger.getLogger(JModTest.class.getName());

    public static void main(String[] args) {
        try {
            master = new ModbusTCPMaster("192.168.1.50");
            master.connect();
            master.writeSingleRegister(0x8001, new SimpleRegister(1)); // set direction forward
            master.writeSingleRegister(0x8002, new SimpleRegister(1000)); // set the speed, 0 = off, 32768 = max
            master.writeSingleRegister(0x8000, new SimpleRegister(1)); // make it go
            Thread.sleep(500);
            master.writeSingleRegister(0x8000, new SimpleRegister(0)); // make it go
            
            System.out.println("connected");
//            logger.info("Read coil 1 status [192.168.1.50:502] - %b", new Object[]{ master.readCoils(0, 1).getBit(0)});
        } catch (Exception e) {
            logger.log(SEVERE,"Cannot initialise tests", e);
        } finally {
            try {
                master.writeSingleRegister(0x8000, new SimpleRegister(0)); // make it not go
            } catch (ModbusException ex) {
                java.util.logging.Logger.getLogger(JModTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (master != null) {
                master.disconnect();
            }
        }
    }
}
