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
package aprs.actions.executor;

//import static aprs.actions.executor.PositionMap.combine;
import crcl.base.PointType;
import crcl.utils.CRCLPosemath;
import static crcl.utils.CRCLPosemath.point;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import rcs.posemath.PmCartesian;
import rcs.posemath.PmException;
import rcs.posemath.PmPose;
import rcs.posemath.PmQuaternion;
import rcs.posemath.PmRotationVector;
import rcs.posemath.Posemath;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("ALL")
public class PositionMapTest {

    public PositionMapTest() {
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

    private void assertErrorMapEntryEquals(PositionMapEntry e1, PositionMapEntry e2) {
        if (e1.equals(e2)) {
            return;
        }
        assertEquals("RobotX", e1.getInputX(), e2.getInputX(), 1e-6);
        assertEquals("RobotY", e1.getInputY(), e2.getInputY(), 1e-6);
        assertEquals("OffsetX", e1.getOffsetX(), e2.getOffsetX(), 1e-6);
        assertEquals("OffsetY", e1.getOffsetY(), e2.getOffsetY(), 1e-6);
    }

    private static void assertPointEquals(PointType expectedPt, PointType actualPt) {
        if (expectedPt.equals(actualPt)) {
            return;
        }
        assertEquals("X", expectedPt.getX(), actualPt.getX(), 1e-6);
        assertEquals("Y", expectedPt.getY(), actualPt.getY(), 1e-6);
        assertEquals("Z", expectedPt.getZ(), actualPt.getZ(), 1e-6);
    }

    private static boolean checkPointEquals(PointType expectedPt, PointType actualPt) {
        if (expectedPt.equals(actualPt)) {
            return true;
        }
        return Math.abs(expectedPt.getX()- actualPt.getX()) < 1e-6
                && Math.abs(expectedPt.getY()- actualPt.getY()) < 1e-6
                && Math.abs(expectedPt.getZ()- actualPt.getZ()) < 1e-6;
                
//        assertEquals("X", expectedPt.getX(), actualPt.getX(), 1e-6);
//        assertEquals("Y", expectedPt.getY(), actualPt.getY(), 1e-6);
//        assertEquals("Z", expectedPt.getZ(), actualPt.getZ(), 1e-6);
    }
    
//    /**
//     * Test of combine method, of class
//     */
//    ///@Test
//    public void testCombine() {
//        System.out.println("combine");
//        PositionMapEntry e1 = PositionMapEntry.pointOffsetEntry(0, 0, 0, 2, 2, 0);
//        PositionMapEntry e2 = PositionMapEntry.pointOffsetEntry(0, 1, 0, 4, 4, 0);
//        double x = 0.0;
//        double y = 0.5;
//        PositionMapEntry expResult = PositionMapEntry.pointOffsetEntry(0, 0.5, 0, 3, 3, 0);
//        PositionMapEntry result = combine(e1, e2, x, y, 0);
//        assertErrorMapEntryEquals(expResult, result);
//        x = 10.0;
//        y = 0.5;
//        expResult = PositionMapEntry.pointOffsetEntry(0, 0.5, 0, 3, 3, 0);
//        result = combine(e1, e2, x, y, 0);
//        assertErrorMapEntryEquals(expResult, result);
//        x = 10.0;
//        y = 1.0;
//        expResult = PositionMapEntry.pointOffsetEntry(0, 1.0, 0, 4, 4, 0);
//        result = combine(e1, e2, x, y, 0);
//        assertErrorMapEntryEquals(expResult, result);
//
//        e1 = PositionMapEntry.pointOffsetEntry(0, 0, 0, 2, 2, 0);
//        e2 = PositionMapEntry.pointOffsetEntry(1, 0, 0, 4, 4, 0);
//        x = 0.5;
//        y = 0.0;
//        expResult = PositionMapEntry.pointOffsetEntry(0.5, 0, 0, 3, 3, 0);
//        result = combine(e1, e2, x, y, 0);
//        assertErrorMapEntryEquals(expResult, result);
//        x = 0.5;
//        y = 10.0;
//        expResult = PositionMapEntry.pointOffsetEntry(0.5, 0, 0, 3, 3, 0);
//        result = combine(e1, e2, x, y, 0);
//        assertErrorMapEntryEquals(expResult, result);
//        x = 1.0;
//        y = 10.0;
//        expResult = PositionMapEntry.pointOffsetEntry(1.0, 0, 0, 4, 4, 0);
//        result = combine(e1, e2, x, y, 0);
//        assertErrorMapEntryEquals(expResult, result);
//
//        e1 = PositionMapEntry.pointOffsetEntry(0, 0, 0, 2, 2, 0);
//        e2 = PositionMapEntry.pointOffsetEntry(1, 1, 0, 4, 4, 0);
//        x = 0.5;
//        y = 0.5;
//        expResult = PositionMapEntry.pointOffsetEntry(0.5, 0.5, 0, 3, 3, 0);
//        result = combine(e1, e2, x, y, 0);
//        assertErrorMapEntryEquals(expResult, result);
//        x = 1.0;
//        y = 1.0;
//        expResult = PositionMapEntry.pointOffsetEntry(1.0, 1.0, 0, 4, 4, 0);
//        result = combine(e1, e2, x, y, 0);
//        assertErrorMapEntryEquals(expResult, result);
//    }

    private static void checkMapPoint(PositionMap pm, PointType pt, PointType expected) {
        PointType correctedPoint = pm.correctPoint(pt);
        assertPointEquals(expected,correctedPoint);
    }
    
    private static PositionMapEntry poseCartEntry(PmPose pose, double x, double y,double  z) throws PmException {
        PmCartesian c0 = new PmCartesian(x,y,z);
        PmCartesian pc0 = new PmCartesian();
        Posemath.pmPoseCartMult(pose, c0, pc0);
        return PositionMapEntry.pointPairEntry(x, y, z, pc0.x,pc0.y,pc0.z);
    }

    private static void checkMapPosePoint(PositionMap pm, PmCartesian c, PmPose pose) throws PmException {
        PmCartesian cout = new PmCartesian();
        Posemath.pmPoseCartMult(pose, c, cout);
        PointType ptIn = CRCLPosemath.toPointType(c);
        PointType expectedPoint = CRCLPosemath.toPointType(cout);
        PointType correctedPoint = pm.correctPoint(ptIn);
        if(!checkPointEquals(expectedPoint, correctedPoint)) {
            System.out.println("ptIn="+CRCLPosemath.toString(ptIn));
            System.out.println("expectedPoint="+CRCLPosemath.toString(expectedPoint));
            System.out.println("correctedPoint="+CRCLPosemath.toString(correctedPoint));
            System.out.println("pm = " + pm);
            correctedPoint = pm.correctPoint(ptIn);
        }
        assertPointEquals(expectedPoint,correctedPoint);
    }
    
    //@Test
    public void testCorrectPoint() throws PmException {

        // x = 2*x +1 map
        PositionMap x2p1map = new PositionMap(
                PositionMapEntry.pointOffsetEntry(0, 0, 0, 1, 1, 1),
                PositionMapEntry.pointOffsetEntry(1, 0, 0, 2, 1, 1)
        );
        checkMapPoint(x2p1map, point(0, 0, 0), point(1, 1, 1));
        checkMapPoint(x2p1map, point(1, 0, 0), point(3, 1, 1));
        checkMapPoint(x2p1map, point(-1, 0, 0), point(-1, 1, 1));
        checkMapPoint(x2p1map, point(2, 0, 0), point(5, 1, 1));
        
        // x = -y, y=x map
        PositionMap xmyyxMap = new PositionMap(
                PositionMapEntry.pointPairEntry(0, 0, 0, 0, 0, 0),
                PositionMapEntry.pointPairEntry(1, 0, 0, 0, 1, 0),
                PositionMapEntry.pointPairEntry(0, 1, 0, -1, 0, 0),
                PositionMapEntry.pointPairEntry(1, 1, 0, -1, 1, 0)
        );
        checkMapPoint(xmyyxMap, point(0, 0, 0), point(0,0,0));
        checkMapPoint(xmyyxMap, point(1, 0, 0), point(0,1,0));
        checkMapPoint(xmyyxMap, point(0.5, 0.5, 0), point(-0.5,0.5,0));
        checkMapPoint(xmyyxMap, point(-1, 0, 0), point(0,-1,0));
        checkMapPoint(xmyyxMap, point(0,1,0), point(-1,0,0));
        checkMapPoint(xmyyxMap, point(0,2,0), point(-2,0,0));
        
        Random random = new Random(271043);
        for (int i = 0; i < 10; i++) {
            testRandomPose(random);
        }
        
    }

    private void testRandomPose(Random random) throws PmException {
        PmCartesian tran = new PmCartesian(random.nextDouble()*10.0,random.nextDouble()*10.0,random.nextDouble()*10.0);
        PmQuaternion quat = Posemath.toQuat(new PmRotationVector(random.nextDouble()*2*Math.PI-Math.PI, 0, 0, 1));
        PmPose pose = new PmPose(tran, quat);
        
        
        testPoseRandomMap4(pose, random);
        testPoseFixedMap4(pose, random);
        testPoseRandomMap3(pose, random);
        testPoseFixedMap3(pose, random);
    }
    private void testPoseFixedMap3(PmPose pose, Random random) throws PmException {
        PositionMap pm = new PositionMap(
                poseCartEntry(pose,0, 0, 0),
                poseCartEntry(pose,0, 1, 0),
                poseCartEntry(pose,1, 0, 0)
        );
        
        for(double x = -2; x < 2.5; x +=0.25*random.nextDouble()) {
            for(double y = -2; y < 2.5; y+= 0.25*random.nextDouble()) {
                checkMapPosePoint(pm, new PmCartesian(x, y, 0), pose);
            }
        }
        checkMapPosePoint(pm, new PmCartesian(1,2, 0), pose);
        for(double x = -2; x < 2.5; x +=0.1) {
            for(double y = -2; y < 2.5; y+= 0.1) {
                checkMapPosePoint(pm, new PmCartesian(x, y, 0), pose);
            }
        }
    }
    
    private void testPoseFixedMap4(PmPose pose, Random random) throws PmException {
        PositionMap pm = new PositionMap(
                poseCartEntry(pose,0, 0, 0),
                poseCartEntry(pose,0, 1, 0),
                poseCartEntry(pose,1, 0, 0),
                poseCartEntry(pose,1, 1, 0)
        );
        
        for(double x = -2; x < 2.5; x +=0.25*random.nextDouble()) {
            for(double y = -2; y < 2.5; y+= 0.25*random.nextDouble()) {
                checkMapPosePoint(pm, new PmCartesian(x, y, 0), pose);
            }
        }
        for(double x = -2; x < 2.5; x +=0.1) {
            for(double y = -2; y < 2.5; y+= 0.1) {
                checkMapPosePoint(pm, new PmCartesian(x, y, 0), pose);
            }
        }
    }

    private void testPoseRandomMap4(PmPose pose, Random random) throws PmException {
        PositionMap pm = new PositionMap(
                poseCartEntry(pose,0+ 0.5*random.nextDouble(), 0 + 0.5*random.nextDouble(), 0),
                poseCartEntry(pose,0+ 0.5*random.nextDouble(), 1 + 0.5*random.nextDouble(), 0),
                poseCartEntry(pose,1+ 0.5*random.nextDouble(), 0, 0),
                poseCartEntry(pose,1+ 0.5*random.nextDouble(), 1+ 0.5*random.nextDouble(), 0)
        );
        
        for(double x = -2; x < 2.5; x +=0.25*random.nextDouble()) {
            for(double y = -2; y < 2.5; y+= 0.25*random.nextDouble()) {
                checkMapPosePoint(pm, new PmCartesian(x, y, 0), pose);
            }
        }
        for(double x = -2; x < 2.5; x +=0.1) {
            for(double y = -2; y < 2.5; y+= 0.1) {
                checkMapPosePoint(pm, new PmCartesian(x, y, 0), pose);
            }
        }
    }
    
    private void testPoseRandomMap8(PmPose pose, Random random) throws PmException {
        PositionMap pm = new PositionMap(
                poseCartEntry(pose,0+ 0.5*random.nextDouble(), 0 + 0.5*random.nextDouble(), 0),
                poseCartEntry(pose,0+ 0.5*random.nextDouble(), 1 + 0.5*random.nextDouble(), 0),
                poseCartEntry(pose,1+ 0.5*random.nextDouble(), 0, 0),
                poseCartEntry(pose,1+ 0.5*random.nextDouble(), 1+ 0.5*random.nextDouble(), 0),
                poseCartEntry(pose,2+ 0.5*random.nextDouble(), 2 + 0.5*random.nextDouble(), 0),
                poseCartEntry(pose,2+ 0.5*random.nextDouble(), 2 + 0.5*random.nextDouble(), 0),
                poseCartEntry(pose,-1+ 0.5*random.nextDouble(), 0, 0),
                poseCartEntry(pose,-1+ 0.5*random.nextDouble(), -1+ 0.5*random.nextDouble(), 0)
        );
        
        for(double x = -2; x < 2.5; x +=0.25*random.nextDouble()) {
            for(double y = -2; y < 2.5; y+= 0.25*random.nextDouble()) {
                checkMapPosePoint(pm, new PmCartesian(x, y, 0), pose);
            }
        }
        for(double x = -2; x < 2.5; x +=0.1) {
            for(double y = -2; y < 2.5; y+= 0.1) {
                checkMapPosePoint(pm, new PmCartesian(x, y, 0), pose);
            }
        }
    }
    
    private void testPoseRandomMap3(PmPose pose, Random random) throws PmException {
        PositionMap pm = new PositionMap(
                poseCartEntry(pose,0+ 0.5*random.nextDouble(), 0 + 0.5*random.nextDouble(), 0),
                poseCartEntry(pose,0+ 0.5*random.nextDouble(), 1 + 0.5*random.nextDouble(), 0),
                poseCartEntry(pose,1+ 0.5*random.nextDouble(), 0, 0)
        );
        
        for(double x = -2; x < 2.5; x +=0.25*random.nextDouble()) {
            for(double y = -2; y < 2.5; y+= 0.25*random.nextDouble()) {
                checkMapPosePoint(pm, new PmCartesian(x, y, 0), pose);
            }
        }
        for(double x = -2; x < 2.5; x +=0.1) {
            for(double y = -2; y < 2.5; y+= 0.1) {
                checkMapPosePoint(pm, new PmCartesian(x, y, 0), pose);
            }
        }
    }

}
