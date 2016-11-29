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
package aprs.framework.pddl.executor;

import static aprs.framework.pddl.executor.ErrorMap.combine;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author shackle
 */
public class ErrorMapTest {

    public ErrorMapTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private void assertErrorMapEntryEquals(ErrorMapEntry e1, ErrorMapEntry e2) {
        if(e1.equals(e2)) {
            return;
        }
        assertEquals("RobotX",e1.getRobotX(), e2.getRobotX(), 1e-6);
        assertEquals("RobotY",e1.getRobotY(), e2.getRobotY(), 1e-6);
        assertEquals("OffsetX",e1.getOffsetX(), e2.getOffsetX(), 1e-6);
        assertEquals("OffsetY",e1.getOffsetY(), e2.getOffsetY(), 1e-6);
    }
    /**
     * Test of combine method, of class 
     */
    @Test
    public void testCombine() {
        System.out.println("combine");
        ErrorMapEntry e1 = new ErrorMapEntry(0, 0, 2, 2);
        ErrorMapEntry e2 = new ErrorMapEntry(0, 1, 4, 4);
        double x = 0.0;
        double y = 0.5;
        ErrorMapEntry expResult = new ErrorMapEntry(0, 0.5, 3, 3);
        ErrorMapEntry result = ErrorMap.combine(e1, e2, x, y);
        assertErrorMapEntryEquals(expResult, result);
        x = 10.0;
        y = 0.5;
        expResult = new ErrorMapEntry(0, 0.5, 3, 3);
        result = combine(e1, e2, x, y);
        assertErrorMapEntryEquals(expResult, result);
        x = 10.0;
        y = 1.0;
        expResult = new ErrorMapEntry(0, 1.0, 4, 4);
        result = combine(e1, e2, x, y);
        assertErrorMapEntryEquals(expResult, result);
        
        e1 = new ErrorMapEntry(0, 0, 2, 2);
        e2 = new ErrorMapEntry(1, 0, 4, 4);
        x = 0.5;
        y = 0.0;
        expResult = new ErrorMapEntry(0.5, 0, 3, 3);
        result = combine(e1, e2, x, y);
        assertErrorMapEntryEquals(expResult, result);
        x = 0.5;
        y = 10.0;
        expResult = new ErrorMapEntry(0.5,0, 3, 3);
        result = combine(e1, e2, x, y);
        assertErrorMapEntryEquals(expResult, result);
        x = 1.0;
        y = 10.0;
        expResult = new ErrorMapEntry(1.0,0, 4, 4);
        result = combine(e1, e2, x, y);
        assertErrorMapEntryEquals(expResult, result);
        
        e1 = new ErrorMapEntry(0, 0, 2, 2);
        e2 = new ErrorMapEntry(1, 1, 4, 4);
        x = 0.5;
        y = 0.5;
        expResult = new ErrorMapEntry(0.5, 0.5, 3, 3);
        result = combine(e1, e2, x, y);
        assertErrorMapEntryEquals(expResult, result);
        x = 1.0;
        y = 1.0;
        expResult = new ErrorMapEntry(1.0,1.0, 4, 4);
        result = combine(e1, e2, x, y);
        assertErrorMapEntryEquals(expResult, result);
    }

}
