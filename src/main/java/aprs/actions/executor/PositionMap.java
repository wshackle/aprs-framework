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

import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.utils.CRCLPosemath;
import static crcl.utils.CRCLPosemath.pose;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import static crcl.utils.CRCLPosemath.point;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import rcs.posemath.PmCartesian;
import rcs.posemath.Posemath;

/**
 *
 * @author shackle
 */
public class PositionMap {

    private final List<PositionMapEntry> errmapList;
    private final List<String[]> errmapStringsList;
    @Nullable private final String fileName;
    private final String[] columnHeaders;
    @Nullable private PointType lastPointIn;
    @Nullable private PointType lastPointOut;
    @Nullable private PointType lastOffset;

    public Iterable<Object[]> getTableIterable() {
        return new Iterable<Object[]>() {

            private final List<PositionMapEntry> l = new ArrayList<>(errmapList);

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

        BadErrorMapFormatException(String message) {
            super(message);
        }
    }

    private static final String[] DEFAULT_COLUMN_HEADERS = new String[]{"X", "Y", "Z", "Offset_X", "Offset_Y", "Offset_Z"};

    private PositionMap() {
        errmapList = Collections.emptyList();
        errmapStringsList = Collections.emptyList();
        fileName = null;
        columnHeaders = DEFAULT_COLUMN_HEADERS;
    }

    public PositionMap(final PositionMapEntry... pmes) {
        errmapList = Arrays.asList(pmes);
        errmapStringsList = Collections.emptyList();
        fileName = null;
        columnHeaders = DEFAULT_COLUMN_HEADERS;
    }

    public PositionMap(final List<PositionMapEntry> l) {
        errmapList = l;
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
        int labelIndex = -1;
        for (int i = 0; i < columnHeaders.length; i++) {
            switch (columnHeaders[i]) {
                case "X":
                case "X1":
                case "Xin":
                case "Robot_X":
                    robotXIndex = i;
                    break;

                case "Y":
                case "Y1":
                case "Yin":
                case "Robot_Y":
                    robotYIndex = i;
                    break;

                case "Z":
                case "Z1":
                case "Zin":
                case "Robot_Z":
                    robotZIndex = i;
                    break;

                case "Offset_X":
                case "X_Offset":
                    offsetXIndex = i;
                    break;

                case "Y_Offset":
                case "Offset_Y":
                    offsetYIndex = i;
                    break;

                case "Z_Offset":
                case "Offset_Z":
                    offsetZIndex = i;
                    break;

                case "Label":
                    labelIndex = i;
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
            double robotX = (robotXIndex >= 0 ? Double.parseDouble(a[robotXIndex]) : 0);
            double robotY = (robotYIndex >= 0 ? Double.parseDouble(a[robotYIndex]) : 0);
            double robotZ = (robotZIndex >= 0 ? Double.parseDouble(a[robotZIndex]) : 0);
            double offsetX = (offsetXIndex >= 0 ? Double.parseDouble(a[offsetXIndex]) : 0);
            double offsetY = (offsetYIndex >= 0 ? Double.parseDouble(a[offsetYIndex]) : 0);
            double offsetZ = (offsetZIndex >= 0 ? Double.parseDouble(a[offsetZIndex]) : 0);
            String label = (labelIndex >= 0 && labelIndex < a.length ? a[labelIndex] : "");
            errmapList.add(PositionMapEntry.pointOffsetLabelEntry(robotX, robotY, robotZ, offsetX, offsetY, offsetZ, label));
        }
//        errmapList = errmapStringsList.stream().map(a -> {
//            return new PositionMapEntry(Double.parseDouble(a[robotXIndex]), Double.parseDouble(a[robotYIndex]),
//                    Double.parseDouble(a[robotXIndex]), Double.parseDouble(a[robotXIndex]));
//        }).collect(Collectors.toList());
    }

    public void saveFile(File f) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("X,Y,Z,Offset_X,Offset_Y,Offset_Z");
            for (PositionMapEntry entry : this.errmapList) {
                pw.println(entry.getRobotX() + "," + entry.getRobotY() + "," + entry.getRobotZ() + "," + entry.getOffsetX() + "," + entry.getOffsetY() + "," + entry.getOffsetZ());
            }
        }
    }

    public PoseType correctPose(PoseType poseIn) {
        if (errmapList.size() < 1) {
            if (null != poseIn) {
                lastPointIn = poseIn.getPoint();
                lastPointOut = poseIn.getPoint();
            }
            return poseIn;
        }
        lastPointIn = poseIn.getPoint();
        PointType offsetPt = getOffset(
                poseIn.getPoint().getX(),
                poseIn.getPoint().getY(),
                poseIn.getPoint().getZ()
        );
        PointType pt = point(
                offsetPt.getX() + poseIn.getPoint().getX(),
                offsetPt.getY() + poseIn.getPoint().getY(),
                offsetPt.getZ() + poseIn.getPoint().getZ()
        );
        PoseType poseOut = pose(pt, poseIn.getXAxis(), poseIn.getZAxis()
        );
        lastPointOut = poseOut.getPoint();
        return poseOut;
    }

    public PointType correctPoint(PointType ptIn) {
        if (errmapList.size() < 1) {
            lastPointIn = lastPointOut = ptIn;
            return ptIn;
        }
        lastPointIn = ptIn;
        PointType offsetPt = getOffset(
                ptIn.getX(),
                ptIn.getY(),
                ptIn.getZ()
        );
        PointType pt = point(
                offsetPt.getX() + ptIn.getX(),
                offsetPt.getY() + ptIn.getY(),
                offsetPt.getZ() + ptIn.getZ()
        );
        lastPointOut = pt;
        return pt;
    }

    private static double dist(PositionMapEntry e, double x, double y) {
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
            return PositionMapEntry.pointOffsetEntryCombining((e1.getRobotX() + e2.getRobotX()) / 2.0,
                    (e1.getRobotY() + e2.getRobotY()) / 2.0,
                    (e1.getRobotZ() + e2.getRobotZ()) / 2.0,
                    (e1.getOffsetX() + e2.getOffsetX()) / 2.0,
                    (e1.getOffsetY() + e2.getOffsetY()) / 2.0,
                    (e1.getOffsetZ() + e2.getOffsetZ()) / 2.0,
                    e1, e2);
        }
        PmCartesian xy = new PmCartesian(x, y, z);
        PmCartesian diff2 = xy.subtract(c1);
        double d = Posemath.pmCartCartDot(diff, diff2) / (diff.mag() * diff.mag());
        PmCartesian center = c1.add(diff.multiply(d));
        double s1 = d;
        double s2 = (1 - d);
        return PositionMapEntry.pointOffsetEntryCombining(
                center.x, center.y, center.z,
                e1.getOffsetX() * s2 + e2.getOffsetX() * s1,
                e1.getOffsetY() * s2 + e2.getOffsetY() * s1,
                e1.getOffsetZ() * s2 + e2.getOffsetZ() * s1,
                e1, e2
        );
    }

    @Nullable
    private static PositionMapEntry combineX(@Nullable PositionMapEntry e1, @Nullable PositionMapEntry e2, double x) {
        if (null == e1) {
            if (null != e2 && Math.abs(e2.getRobotX() - x) < 1e-6) {
                return e2;
            } else {
                return null;
            }
        }
        if (null == e2) {
            if (null != e1 && Math.abs(e1.getRobotX() - x) < 1e-6) {
                return e1;
            } else {
                return null;
            }
        }
        PmCartesian c1 = new PmCartesian(e1.getRobotX(), e1.getRobotY(), e1.getRobotZ());
        PmCartesian c2 = new PmCartesian(e2.getRobotX(), e2.getRobotY(), e2.getRobotZ());
        PmCartesian diff = c2.subtract(c1);
        if (Math.abs(diff.x) < 1e-6) {
            if (Math.abs(e1.getRobotX() - x) > 1e-6) {
                return null;
//                throw new IllegalArgumentException("Can't combine two entry  with the same x  for different target x: "
//                        + "e1=" + e1 + ", "
//                        + "e2=" + e2 + ", "
//                        + "x=" + x
//                );
            }
            return PositionMapEntry.pointOffsetEntryCombining((e1.getRobotX() + e2.getRobotX()) / 2.0,
                    (e1.getRobotY() + e2.getRobotY()) / 2.0,
                    (e1.getRobotZ() + e2.getRobotZ()) / 2.0,
                    (e1.getOffsetX() + e2.getOffsetX()) / 2.0,
                    (e1.getOffsetY() + e2.getOffsetY()) / 2.0,
                    (e1.getOffsetZ() + e2.getOffsetZ()) / 2.0,
                    e1, e2);
        }
//        PmCartesian xy = new PmCartesian(x, y, z);
//        PmCartesian diff2 = xy.subtract(c1);
//        double d = Posemath.pmCartCartDot(diff, diff2) / (diff.mag() * diff.mag());

        double d = (x - c1.x) / diff.x;
        PmCartesian center = c1.add(diff.multiply(d));
        double s1 = d;
        double s2 = (1 - d);
        return PositionMapEntry.pointOffsetEntryCombining(center.x, center.y, center.z,
                e1.getOffsetX() * s2 + e2.getOffsetX() * s1,
                e1.getOffsetY() * s2 + e2.getOffsetY() * s1,
                e1.getOffsetZ() * s2 + e2.getOffsetZ() * s1,
                e1, e2
        );
    }

    @Nullable
    private static PositionMapEntry combineY(PositionMapEntry e1, PositionMapEntry e2, double y) {
        if (null == e1) {
            if (null != e2 && Math.abs(e2.getRobotY() - y) < 1e-6) {
                return e2;
            } else {
                return null;
            }
        }
        if (null == e2) {
            if (null != e1 && Math.abs(e1.getRobotY() - y) < 1e-6) {
                return e1;
            } else {
                return null;
            }
        }
        PmCartesian c1 = new PmCartesian(e1.getRobotX(), e1.getRobotY(), e1.getRobotZ());
        PmCartesian c2 = new PmCartesian(e2.getRobotX(), e2.getRobotY(), e2.getRobotZ());
        PmCartesian diff = c2.subtract(c1);
        if (Math.abs(diff.y) < 1e-6) {
            if (Math.abs(e1.getRobotY() - y) > 1e-6) {
                return null;
//                throw new IllegalArgumentException("Can't combine two entry  with the same y  for different target y: "
//                        + "e1=" + e1 + ", "
//                        + "e2=" + e2 + ", "
//                        + "y=" + y
//                );
            }
            return PositionMapEntry.pointOffsetEntryCombining((e1.getRobotX() + e2.getRobotX()) / 2.0,
                    (e1.getRobotY() + e2.getRobotY()) / 2.0,
                    (e1.getRobotZ() + e2.getRobotZ()) / 2.0,
                    (e1.getOffsetX() + e2.getOffsetX()) / 2.0,
                    (e1.getOffsetY() + e2.getOffsetY()) / 2.0,
                    (e1.getOffsetZ() + e2.getOffsetZ()) / 2.0,
                    e1, e2);
        }
//        PmCartesian xy = new PmCartesian(x, y, z);
//        PmCartesian diff2 = xy.subtract(c1);
//        double d = Posemath.pmCartCartDot(diff, diff2) / (diff.mag() * diff.mag());

        double d = (y - c1.y) / diff.y;
        PmCartesian center = c1.add(diff.multiply(d));
        double s1 = d;
        double s2 = (1 - d);
        return PositionMapEntry.pointOffsetEntryCombining(center.x, center.y, center.z,
                e1.getOffsetX() * s2 + e2.getOffsetX() * s1,
                e1.getOffsetY() * s2 + e2.getOffsetY() * s1,
                e1.getOffsetZ() * s2 + e2.getOffsetZ() * s1,
                e1, e2
        );
    }

    public PointType getOffset(double x, double y, double z) {
        return getOffsetInternal(x, y, z, 0);
    }

    private PointType getOffsetInternal(double x, double y, double z, int recurseLevel) {
        if (errmapList.size() < 1) {
            return point(0, 0, 0);
        }
        if (errmapList.size() == 1) {
            return point(errmapList.get(0).getOffsetX(), errmapList.get(0).getOffsetY(), errmapList.get(0).getOffsetZ());
        }
        PositionMapEntry e12 = findXCombo(robotY -> robotY <= y, x, y, z);
        PositionMapEntry e34 = findXCombo(robotY -> robotY >= y, x, y, z);
        if (null == e12 || null == e34) {
            List<PositionMapEntry> sortedList = new ArrayList<>(errmapList);
            sortedList.sort(Comparator.comparingDouble(em -> dist(em, x, y)));
            e12 = null;
            e34 = null;
            ILOOP:
            for (int i = 0; i < sortedList.size(); i++) {
                for (int j = i; j < sortedList.size(); j++) {
                    if (i == j) {
                        e12 = sortedList.get(i);
                        if (Math.abs(e12.getRobotX() - x) > 1e-6) {
                            e12 = null;
                            continue;
                        }
                    } else {
                        e12 = combineX(sortedList.get(i), sortedList.get(j), x);
                    }
                    if (null != e12) {
                        for (int k = i; k < sortedList.size(); k++) {
                            for (int l = k; l < sortedList.size(); l++) {
                                if (k == i && l == j) {
                                    continue;
                                }
                                if (k == l) {
                                    if (k == i) {
                                        continue;
                                    }
                                    e34 = sortedList.get(k);
                                    if (Math.abs(e34.getRobotX() - x) > 1e-6) {
                                        e34 = null;
                                        continue;
                                    }
                                } else {
                                    e34 = combineX(sortedList.get(k), sortedList.get(l), x);
                                }
                                if (null != e34
                                        && Math.abs(e34.getRobotY() - e12.getRobotY()) < 1e-6
                                        && Math.abs(e34.getRobotY() - y) > 1e-6) {
                                    e34 = null;
                                }
                                if (e34 != null) {
                                    break ILOOP;
                                }
                            }
                            if (e34 != null) {
                                break ILOOP;
                            }
                        }
                        if (e34 != null) {
                            break ILOOP;
                        }
                    }
                }
            }
            if (null == e12 || null == e34) {
                if (recurseLevel == 0) {
                    PointType p1 = getOffsetInternal(x + 000.1, y + 000.1, z, recurseLevel + 1);
                    PointType p2 = getOffsetInternal(x - 000.1, y - 000.1, z, recurseLevel + 1);
                    if (null != p1 && null != p2) {
                        return CRCLPosemath.multiply(0.5, CRCLPosemath.add(p1, p2));
                    }
                }
                throw new IllegalStateException("x=" + x + ",y=" + y + ",e12=" + e12 + ", e34=" + e34 + ", sortedList=" + sortedList);
            }
        }

        PositionMapEntry eme = combineY(e12, e34, y);
        if (null == eme) {
            throw new IllegalStateException("combineY returned null");
        }
        lastOffset = point(eme.getOffsetX(), eme.getOffsetY(), eme.getOffsetZ());
        return lastOffset;
    }

    @Nullable private PositionMapEntry findXCombo(Predicate<Double> predy, double x, double y, double z) {
        List<PositionMapEntry> yFilteredList = errmapList.stream()
                .filter(e -> predy.test(e.getRobotY()))
                .collect(Collectors.toList());
        return findXCombo(yFilteredList, x, y, z);
    }

    @SuppressWarnings("unused")
    @Nullable private PositionMapEntry findXCombo(List<PositionMapEntry> yFilteredList, double x, double y, double z) {
        if (yFilteredList.size() < 2) {
            if (yFilteredList.size() == 1 && Math.abs(yFilteredList.get(0).getRobotX() - x) < 1e-6) {
                return yFilteredList.get(0);
            }
            return null;
        } else if (yFilteredList.size() == 2) {
            return combineX(yFilteredList.get(0), yFilteredList.get(1), x);
        }
        PositionMapEntry e1 = findEntry(robotX -> robotX <= x,
                yFilteredList,
                x, y);
        PositionMapEntry e2 = findEntry(robotX -> robotX >= x,
                yFilteredList,
                x, y);
        if (e1 == null && e2 != null) {
            final double e2fx = (e2.getRobotX() + Double.MIN_NORMAL);
            e1 = findEntry(
                    robotX -> robotX > e2fx,
                    yFilteredList,
                    x, y);
        } else if (e1 != null && e2 == null) {
            final double e1fx = (e1.getRobotX() - Double.MIN_NORMAL);
            e2 = findEntry(
                    robotX -> robotX < e1fx,
                    yFilteredList,
                    x, y);
        }
        return combineX(e1, e2, x);
    }

    @Nullable private PositionMapEntry findEntry(Predicate<Double> predx, List<PositionMapEntry> yfilteredList, double x, double y) {
        PositionMapEntry e1 = yfilteredList.stream()
                .filter(e -> predx.test(e.getRobotX()))
                .min(Comparator.comparingDouble(em -> dist(em, x, y)))
                .orElse(null);
        return e1;
    }

    public List<PositionMapEntry> getErrmapList() {
        return errmapList;
    }

    public List<String[]> getErrmapStringsList() {
        return errmapStringsList;
    }

    @Nullable public String getFileName() {
        return fileName;
    }

    public String[] getColumnHeaders() {
        return columnHeaders;
    }

    @Nullable public PointType getLastPointIn() {
        return lastPointIn;
    }

    @Nullable public PointType getLastPointOut() {
        return lastPointOut;
    }

    @Nullable public PointType getLastOffset() {
        return lastOffset;
    }

    public PositionMap reverse() {
        List<PositionMapEntry> l = new ArrayList<>();
        for (PositionMapEntry entry : errmapList) {
            l.add(PositionMapEntry.pointOffsetEntry(entry.getOtherX(), entry.getOtherY(), entry.getOtherZ(), -entry.getOffsetX(), -entry.getOffsetY(), -entry.getOffsetZ()));
        }
        PositionMap rpm = new PositionMap(l);
        return rpm;
    }
}
