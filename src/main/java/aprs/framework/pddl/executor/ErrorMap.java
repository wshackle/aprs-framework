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

import crcl.base.PointType;
import crcl.base.PoseType;
import static crcl.utils.CRCLPosemath.point;
import static crcl.utils.CRCLPosemath.pose;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.table.DefaultTableModel;
import rcs.posemath.PmCartesian;
import rcs.posemath.Posemath;

/**
 *
 * @author shackle
 */
public class ErrorMap {
    
    private final List<ErrorMapEntry> errmapList;
    private final List<String[]> errmapStringsList;
    private final String fileName;
    private final String []columnHeaders;
    private PoseType lastPoseIn;
    private PoseType lastPoseOut;
    private PointType lastOffset;
    
    public static class BadErrorMapFormatException extends Exception {

        public BadErrorMapFormatException(String message) {
            super(message);
        }
    }
    
    
    public ErrorMap(File f) throws IOException, BadErrorMapFormatException {
        errmapStringsList = Files.lines(f.toPath()).map(l -> l.split(",")).collect(Collectors.toList());
        columnHeaders = errmapStringsList.get(0);
        
        fileName = f.getCanonicalPath();
        int robotXIndex = -1;
        int robotYIndex = -1;
        int offsetXIndex = -1;
        int offsetYIndex = -1;
        for (int i = 0; i <columnHeaders.length; i++) {
            switch (columnHeaders[i]) {
                case "Robot_X":
                    robotXIndex = i;
                    break;

                case "Robot_Y":
                    robotYIndex = i;
                    break;

                case "Offset_X":
                    offsetXIndex = i;
                    break;

                case "Offset_Y":
                    offsetYIndex = i;
                    break;
            }
        }
        if (robotXIndex < 0) {
            throw new BadErrorMapFormatException("Couldn't find robotXIndex");
        }
        if (robotYIndex < 0) {
            throw new BadErrorMapFormatException("Couldn't find robotYIndex");
        }

        if (offsetXIndex < 0) {
            throw new BadErrorMapFormatException("Couldn't find offsetXIndex");
        }
        if (offsetYIndex < 0) {
            throw new BadErrorMapFormatException("Couldn't find offsetYIndex");
        }
        errmapList = new ArrayList<>();
        for (int i = 1; i < errmapStringsList.size(); i++) {
            String a[] = errmapStringsList.get(i);
            errmapList.add(new ErrorMapEntry(Double.valueOf(a[robotXIndex]), Double.valueOf(a[robotYIndex]),
                    Double.valueOf(a[offsetXIndex]), Double.valueOf(a[offsetYIndex])));
        }
//        errmapList = errmapStringsList.stream().map(a -> {
//            return new ErrorMapEntry(Double.valueOf(a[robotXIndex]), Double.valueOf(a[robotYIndex]),
//                    Double.valueOf(a[robotXIndex]), Double.valueOf(a[robotXIndex]));
//        }).collect(Collectors.toList());
    }

    public PoseType correctPose(PoseType poseIn) {
        lastPoseIn = poseIn;
        PointType offsetPt = getOffset(poseIn.getPoint().getX().doubleValue(),
                poseIn.getPoint().getY().doubleValue());
        PoseType poseOut = pose(point(offsetPt.getX().add(poseIn.getPoint().getX()),
                offsetPt.getY().add(poseIn.getPoint().getY()),
                poseIn.getPoint().getZ()), poseIn.getXAxis(), poseIn.getZAxis()
        );
        lastPoseOut = poseOut;
        return poseOut;
    }
    
    private double dist(ErrorMapEntry e, double x, double y) {
        double dx = e.getRobotX() - x;
        double dy = e.getRobotY() - y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    static public ErrorMapEntry combine(ErrorMapEntry e1, ErrorMapEntry e2, double x, double y) {
        if (null == e1) {
            return e2;
        }
        if (null == e2) {
            return e1;
        }
        PmCartesian c1 = new PmCartesian(e1.getRobotX(), e1.getRobotY(), 0);
        PmCartesian c2 = new PmCartesian(e2.getRobotX(), e2.getRobotY(), 0);
        PmCartesian diff = c2.subtract(c1);
        if (diff.mag() < 1e-6) {
            return new ErrorMapEntry((e1.getRobotX() + e2.getRobotX()) / 2.0,
                    (e1.getRobotY() + e2.getRobotY()) / 2.0,
                    (e1.getOffsetX() + e2.getOffsetX()) / 2.0,
                    (e1.getOffsetY() + e2.getOffsetY()) / 2.0);
        }
        PmCartesian xy = new PmCartesian(x, y, 0);
        PmCartesian diff2 = xy.subtract(c1);
        double d = Posemath.pmCartCartDot(diff, diff2) / (diff.mag()*diff.mag());
        PmCartesian center = c1.add(diff.multiply(d));
        double s1 = d;
        double s2 = (1-d);
        return new ErrorMapEntry(center.x, center.y,
                e1.getOffsetX() * s2 + e2.getOffsetX() * s1,
                e1.getOffsetY()* s2 + e2.getOffsetY() * s1);
    }
    
    public PointType getOffset(double x, double y) {
        ErrorMapEntry e1 = errmapList.stream()
                .filter(e -> e.getRobotX() <= x && e.getRobotY() <= y)
                .min((em1, em2) -> Double.compare(dist(em1, x, y), dist(em2, x, y)))
                .orElse(null);

        ErrorMapEntry e2 = errmapList.stream()
                .filter(e -> e.getRobotX() >= x && e.getRobotY() <= y)
                .min((em1, em2) -> Double.compare(dist(em1, x, y), dist(em2, x, y)))
                .orElse(null);

        ErrorMapEntry e12 = combine(e1, e2, x, y);

        ErrorMapEntry e3 = errmapList.stream()
                .filter(e -> e.getRobotX() <= x && e.getRobotY() >= y)
                .min((em1, em2) -> Double.compare(dist(em1, x, y), dist(em2, x, y)))
                .orElse(null);

        ErrorMapEntry e4 = errmapList.stream()
                .filter(e -> e.getRobotX() >= x && e.getRobotY() >= y)
                .min((em1, em2) -> Double.compare(dist(em1, x, y), dist(em2, x, y)))
                .orElse(null);
        ErrorMapEntry e34 = combine(e3, e4, x, y);

        ErrorMapEntry eme = combine(e12, e34, x, y);
        lastOffset = point(eme.getOffsetX(), eme.getOffsetY(), 0);
        return lastOffset;
    }
    
    public List<ErrorMapEntry> getErrmapList() {
        return errmapList;
    }

    public List<String[]> getErrmapStringsList() {
        return errmapStringsList;
    }

    public String getFileName() {
        return fileName;
    }

    public String[] getColumnHeaders() {
        return columnHeaders;
    }

    public PoseType getLastPoseIn() {
        return lastPoseIn;
    }

    public PoseType getLastPoseOut() {
        return lastPoseOut;
    }

    public PointType getLastOffset() {
        return lastOffset;
    }
    
}
