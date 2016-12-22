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
import static crcl.utils.CRCLPosemath.pose;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import static crcl.utils.CRCLPosemath.point;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import rcs.posemath.PmCartesian;
import rcs.posemath.Posemath;

/**
 *
 * @author shackle
 */
public class PositionMap {

    private final List<PositionMapEntry> errmapList;
    private final List<String[]> errmapStringsList;
    private final String fileName;
    private final String[] columnHeaders;
    private PoseType lastPoseIn;
    private PoseType lastPoseOut;
    private PointType lastOffset;

    public Iterable<Object[]> getTableIterable() {
        return new Iterable<Object[]>() {

            private final List<PositionMapEntry> l = new ArrayList(errmapList);

            @Override
            public Iterator<Object[]> iterator() {

                return new Iterator<Object[]>() {

                    int index = 0;

                    @Override
                    public boolean hasNext() {
                        return index < l.size();
                    }

                    @Override
                    public Object[] next() {
                        PositionMapEntry entry = l.get(index);
                        ++index;
                        return new Object[]{entry.getRobotX(), entry.getRobotY(), entry.getOffsetZ(), entry.getOffsetX(), entry.getOffsetY(), entry.getOffsetZ()};
                    }
                };
            }
        };
    }

    public static class BadErrorMapFormatException extends Exception {

        public BadErrorMapFormatException(String message) {
            super(message);
        }
    }

    public static final String DEFAULT_COLUMN_HEADERS[] = new String[]{"X", "Y", "Z", "Offset_X", "Offset_Y", "Offset_Z"};

    private PositionMap() {
        errmapList = Collections.emptyList();
        errmapStringsList = Collections.emptyList();
        fileName = null;
        columnHeaders = DEFAULT_COLUMN_HEADERS;
    }

    public static PositionMap emptyPositionMap() {
        return new PositionMap();
    }

    public PositionMap(File f) throws IOException, BadErrorMapFormatException {
        errmapStringsList = Files.lines(f.toPath()).map(l -> l.split(",")).collect(Collectors.toList());
        columnHeaders = errmapStringsList.get(0);

        fileName = f.getCanonicalPath();
        int robotXIndex = -1;
        int robotYIndex = -1;
        int robotZIndex = -1;
        int offsetXIndex = -1;
        int offsetYIndex = -1;
        int offsetZIndex = -1;
        for (int i = 0; i < columnHeaders.length; i++) {
            switch (columnHeaders[i]) {
                case "X":
                case "Xin":
                case "Robot_X":
                    robotXIndex = i;
                    break;

                case "Y":
                case "Yin":
                case "Robot_Y":
                    robotYIndex = i;
                    break;

                case "Z":
                case "Zin":
                case "Robot_Z":
                    robotZIndex = i;
                    break;

                case "Offset_X":
                    offsetXIndex = i;
                    break;

                case "Offset_Y":
                    offsetYIndex = i;
                    break;

                case "Offset_Z":
                    offsetZIndex = i;
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
            double robotX = (robotXIndex >= 0 ? Double.valueOf(a[robotXIndex]) : 0);
            double robotY = (robotYIndex >= 0 ? Double.valueOf(a[robotYIndex]) : 0);
            double robotZ = (robotZIndex >= 0 ? Double.valueOf(a[robotZIndex]) : 0);
            double offsetX = (offsetXIndex >= 0 ? Double.valueOf(a[offsetXIndex]) : 0);
            double offsetY = (offsetYIndex >= 0 ? Double.valueOf(a[offsetYIndex]) : 0);
            double offsetZ = (offsetZIndex >= 0 ? Double.valueOf(a[offsetZIndex]) : 0);

            errmapList.add(new PositionMapEntry(robotX, robotY, robotZ, offsetX, offsetY, offsetZ));
        }
//        errmapList = errmapStringsList.stream().map(a -> {
//            return new PositionMapEntry(Double.valueOf(a[robotXIndex]), Double.valueOf(a[robotYIndex]),
//                    Double.valueOf(a[robotXIndex]), Double.valueOf(a[robotXIndex]));
//        }).collect(Collectors.toList());
    }

    public void saveFile(File f) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("X,Y,Z,Offset_X,Offset_Y,Offset_Z");
            for (int i = 0; i < this.errmapList.size(); i++) {
                PositionMapEntry entry = this.errmapList.get(i);
                pw.println(entry.getRobotX() + "," + entry.getRobotY() + "," + entry.getRobotZ() + "," + entry.getOffsetX() + "," + entry.getOffsetY() + "," + entry.getOffsetZ());
            }
        }
    }

    public PoseType correctPose(PoseType poseIn) {
        lastPoseIn = poseIn;
        PointType offsetPt = getOffset(poseIn.getPoint().getX().doubleValue(),
                poseIn.getPoint().getY().doubleValue(),
                poseIn.getPoint().getZ().doubleValue());
        PoseType poseOut = pose(point(offsetPt.getX().add(poseIn.getPoint().getX()),
                offsetPt.getY().add(poseIn.getPoint().getY()),
                poseIn.getPoint().getZ()), poseIn.getXAxis(), poseIn.getZAxis()
        );
        lastPoseOut = poseOut;
        return poseOut;
    }

    private double dist(PositionMapEntry e, double x, double y) {
        double dx = e.getRobotX() - x;
        double dy = e.getRobotY() - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    static public PositionMapEntry combine(PositionMapEntry e1, PositionMapEntry e2, double x, double y, double z) {
        if (null == e1) {
            return e2;
        }
        if (null == e2) {
            return e1;
        }
        PmCartesian c1 = new PmCartesian(e1.getRobotX(), e1.getRobotY(), e1.getRobotZ());
        PmCartesian c2 = new PmCartesian(e2.getRobotX(), e2.getRobotY(), e2.getRobotZ());
        PmCartesian diff = c2.subtract(c1);
        if (diff.mag() < 1e-6) {
            return new PositionMapEntry((e1.getRobotX() + e2.getRobotX()) / 2.0,
                    (e1.getRobotY() + e2.getRobotY()) / 2.0,
                    (e1.getRobotZ() + e2.getRobotZ()) / 2.0,
                    (e1.getOffsetX() + e2.getOffsetX()) / 2.0,
                    (e1.getOffsetY() + e2.getOffsetY()) / 2.0,
                    (e1.getOffsetZ() + e2.getOffsetZ()) / 2.0);
        }
        PmCartesian xy = new PmCartesian(x, y, z);
        PmCartesian diff2 = xy.subtract(c1);
        double d = Posemath.pmCartCartDot(diff, diff2) / (diff.mag() * diff.mag());
        PmCartesian center = c1.add(diff.multiply(d));
        double s1 = d;
        double s2 = (1 - d);
        return new PositionMapEntry(center.x, center.y, center.z,
                e1.getOffsetX() * s2 + e2.getOffsetX() * s1,
                e1.getOffsetY() * s2 + e2.getOffsetY() * s1,
                e1.getOffsetZ() * s2 + e2.getOffsetZ() * s1
        );
    }

   

    public PointType getOffset(double x, double y, double z) {
        PositionMapEntry e1 = errmapList.stream()
                .filter(e -> e.getRobotX() <= x && e.getRobotY() <= y)
                .min((em1, em2) -> Double.compare(dist(em1, x, y), dist(em2, x, y)))
                .orElse(null);

        PositionMapEntry e2 = errmapList.stream()
                .filter(e -> e.getRobotX() >= x && e.getRobotY() <= y)
                .min((em1, em2) -> Double.compare(dist(em1, x, y), dist(em2, x, y)))
                .orElse(null);

        PositionMapEntry e12 = combine(e1, e2, x, y, z);

        PositionMapEntry e3 = errmapList.stream()
                .filter(e -> e.getRobotX() <= x && e.getRobotY() >= y)
                .min((em1, em2) -> Double.compare(dist(em1, x, y), dist(em2, x, y)))
                .orElse(null);

        PositionMapEntry e4 = errmapList.stream()
                .filter(e -> e.getRobotX() >= x && e.getRobotY() >= y)
                .min((em1, em2) -> Double.compare(dist(em1, x, y), dist(em2, x, y)))
                .orElse(null);
        PositionMapEntry e34 = combine(e3, e4, x, y, z);

        PositionMapEntry eme = combine(e12, e34, x, y, z);
        lastOffset = point(eme.getOffsetX(), eme.getOffsetY(), eme.getOffsetZ());
        return lastOffset;
    }

    public List<PositionMapEntry> getErrmapList() {
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
