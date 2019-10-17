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
 * for its use by output parties, and makes no guarantees, expressed or 
 * implied, about its quality, reliability, or any output characteristic. 
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
import diagapplet.plotter.plotterJFrame;
import java.io.FileWriter;
import java.io.PrintWriter;
import static java.util.Objects.requireNonNull;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import rcs.posemath.PmCartesian;
import rcs.posemath.Posemath;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class PositionMap {

    private final List<PositionMapEntry> errmapList;
    private final List<String[]> errmapStringsList;

    private @Nullable
    final String fileName;

    private final String[] columnHeaders;

    private @Nullable
    PointType lastPointIn;
    private @Nullable
    PointType lastPointOut;
    private @Nullable
    PointType lastOffset;

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
                        return new Object[]{entry.getInputX(), entry.getInputY(), entry.getInputZ(), entry.getOutputX(), entry.getOutputY(), entry.getOutputZ(), entry.getOffsetX(), entry.getOffsetY(), entry.getOffsetZ(), entry.getLabel()};
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

    public void plot() {
        double minx = Double.POSITIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;
        double minz = Double.POSITIVE_INFINITY;
        double maxx = Double.NEGATIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;
        double maxz = Double.NEGATIVE_INFINITY;

        for (PositionMapEntry entry : errmapList) {
            minx = Math.min(minx, entry.getInputX());
            miny = Math.min(miny, entry.getInputY());
            minz = Math.min(minz, entry.getInputZ());
            maxx = Math.max(maxx, entry.getInputX());
            maxy = Math.max(maxy, entry.getInputY());
            maxz = Math.max(maxz, entry.getInputZ());
        }
        final double xdiff = Math.abs(minx - maxx);
        final double startx = minx - xdiff / 2;
        final double ydiff = Math.abs(miny - maxy);
        final double starty = miny - ydiff / 2;
        final double z = (maxz + minz) / 2;

        try {
            diagapplet.plotter.plotterJFrame plotterFrame = new diagapplet.plotter.plotterJFrame();
            plotXFirst(startx, xdiff, starty, ydiff, z, plotterFrame);
            plotYFirst(startx, xdiff, starty, ydiff, z, plotterFrame);
            plotInput(z, plotterFrame);
            plotOutput(z, plotterFrame);
            plotInputMid(z, plotterFrame);
            plotOutputMid(z, plotterFrame);
            plotterFrame.SetEqualizeAxis(true);
            plotterFrame.setVisible(true);
        } catch (Exception ex) {
            Logger.getLogger(PositionMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void plotXFirst(final double startx, final double xdiff, final double starty, final double ydiff, final double z, plotterJFrame plotterFrame) {
        double xA[] = new double[64];
        double yA[] = new double[64];
        double xInA[] = new double[64];
        double yInA[] = new double[64];
        for (int i = 0; i < 8; i++) {
            double x = startx + i * (xdiff / 4);
            for (int j = 0; j < 8; j++) {
                double y = starty + j * (ydiff / 4);
                xInA[i * 8 + j] = x;
                yInA[i * 8 + j] = y;
                PointType pointin = CRCLPosemath.point(x, y, z);
                PointType pointOut = this.correctPoint(pointin);
                xA[i * 8 + j] = pointOut.getX();
                yA[i * 8 + j] = pointOut.getY();
            }
        }
        plotterFrame.LoadXYDoubleArrays("XFirstIn", xInA, yInA);
        plotterFrame.LoadXYDoubleArrays("correctedPointsXFirst", xA, yA);
    }

    private void plotYFirst(final double startx, final double xdiff, final double starty, final double ydiff, final double z, plotterJFrame plotterFrame) {
        double xA[] = new double[64];
        double yA[] = new double[64];
        double xInA[] = new double[64];
        double yInA[] = new double[64];
        for (int i = 0; i < 8; i++) {
            double y = starty + i * (ydiff / 4);
            for (int j = 0; j < 8; j++) {
                double x = startx + j * (xdiff / 4);
                xInA[i * 8 + j] = x;
                yInA[i * 8 + j] = y;
                PointType pointin = CRCLPosemath.point(x, y, z);
                PointType pointOut = this.correctPoint(pointin);
                xA[i * 8 + j] = pointOut.getX();
                yA[i * 8 + j] = pointOut.getY();
            }
        }
        plotterFrame.LoadXYDoubleArrays("YFirstIn", xInA, yInA);
        plotterFrame.LoadXYDoubleArrays("correctedPointsYFirst", xA, yA);
    }

    private void plotInput(final double z, plotterJFrame plotterFrame) {
        double xA[] = new double[errmapList.size() + 1];
        double yA[] = new double[errmapList.size() + 1];
        double xCA[] = new double[errmapList.size() + 1];
        double yCA[] = new double[errmapList.size() + 1];
        for (int i = 0; i < errmapList.size(); i++) {
            PositionMapEntry entry = errmapList.get(i);
            double x = entry.getInputX();
            double y = entry.getInputY();
            xA[i] = x;
            yA[i] = y;
            PointType pointin = CRCLPosemath.point(x, y, z);
            PointType pointOut = this.correctPoint(pointin);
            xCA[i] = pointOut.getX();
            yCA[i] = pointOut.getY();
        }
        xA[errmapList.size()] = xA[0];
        yA[errmapList.size()] = yA[0];
        xCA[errmapList.size()] = xCA[0];
        yCA[errmapList.size()] = yCA[0];
        plotterFrame.LoadXYDoubleArrays("input", xA, yA);
        plotterFrame.LoadXYDoubleArrays("correctedInput", xCA, yCA);
    }

    private void plotInputMid(final double z, plotterJFrame plotterFrame) {
        double xA[] = new double[errmapList.size()+1];
        double yA[] = new double[errmapList.size()+1];
        double xCA[] = new double[errmapList.size()+1];
        double yCA[] = new double[errmapList.size()+1];
        PositionMapEntry lastEntry = errmapList.get(0);
        for (int i = 0; i < errmapList.size(); i++) {
            int nextIndex = i+1;
            if(nextIndex >= errmapList.size()) {
                nextIndex = 0;
            }
            PositionMapEntry entry = errmapList.get(nextIndex);
            double x = (entry.getInputX() + lastEntry.getInputX()) / 2.0;
            double y = (entry.getInputY() + lastEntry.getInputY()) / 2.0;
            xA[i] = x;
            yA[i] = y;
            PointType pointin = CRCLPosemath.point(x, y, z);
            PointType pointOut = this.correctPoint(pointin);
            xCA[i] = pointOut.getX();
            yCA[i] = pointOut.getY();
            lastEntry = entry;
        }
        xA[errmapList.size()] = xA[0];
        yA[errmapList.size()] = yA[0];
        xCA[errmapList.size()] = xCA[0];
        yCA[errmapList.size()] = yCA[0];
        plotterFrame.LoadXYDoubleArrays("inputMid", xA, yA);
        plotterFrame.LoadXYDoubleArrays("correctedInputMid", xCA, yCA);
    }

    private void plotOutputMid(final double z, plotterJFrame plotterFrame) {
        double xA[] = new double[errmapList.size()+1];
        double yA[] = new double[errmapList.size()+1];
        double xRA[] = new double[errmapList.size()+1];
        double yRA[] = new double[errmapList.size()+1];
        PositionMapEntry lastEntry = errmapList.get(0);
        PositionMap reverse = this.reverse();
        for (int i = 0; i < errmapList.size(); i++) {
            int nextIndex = i+1;
            if(nextIndex >= errmapList.size()) {
                nextIndex = 0;
            }
            PositionMapEntry entry = errmapList.get(nextIndex);
            double x = (entry.getOutputX() + lastEntry.getOutputX()) / 2.0;
            double y = (entry.getOutputY() + lastEntry.getOutputY()) / 2.0;
            xA[i] = x;
            yA[i] = y;
            PointType pointin = CRCLPosemath.point(x, y, z);
            PointType pointOut = reverse.correctPoint(pointin);
            xRA[i] = pointOut.getX();
            yRA[i] = pointOut.getY();
            lastEntry = entry;
        }
        xA[errmapList.size()] = xA[0];
        yA[errmapList.size()] = yA[0];
        xRA[errmapList.size()] = xRA[0];
        yRA[errmapList.size()] = yRA[0];
        plotterFrame.LoadXYDoubleArrays("outputMid", xA, yA);
        plotterFrame.LoadXYDoubleArrays("ReverseOutputMid", xRA, yRA);
    }

    private void plotOutput(final double z, plotterJFrame plotterFrame) {
        double xA[] = new double[errmapList.size() + 1];
        double yA[] = new double[errmapList.size() + 1];
        double xRA[] = new double[errmapList.size() + 1];
        double yRA[] = new double[errmapList.size() + 1];
        PositionMap reverse = this.reverse();
        for (int i = 0; i < errmapList.size(); i++) {
            PositionMapEntry entry = errmapList.get(i);
            double x = entry.getOutputX();
            double y = entry.getOutputY();
            xA[i] = x;
            yA[i] = y;
            PointType pointin = CRCLPosemath.point(x, y, z);
            PointType pointOut = reverse.correctPoint(pointin);
            xRA[i] = pointOut.getX();
            yRA[i] = pointOut.getY();
        }
        xA[errmapList.size()] = xA[0];
        yA[errmapList.size()] = yA[0];
        xRA[errmapList.size()] = xRA[0];
        yRA[errmapList.size()] = yRA[0];

        plotterFrame.LoadXYDoubleArrays("output", xA, yA);
        plotterFrame.LoadXYDoubleArrays("ReverseOutput", xRA, yRA);
    }

    public PositionMap(File f) throws IOException, BadErrorMapFormatException {
        errmapStringsList
                = Files
                        .lines(f.toPath())
                        .map(l -> l.split(","))
                        .collect(Collectors.toList());
        columnHeaders = errmapStringsList.get(0);

        fileName = f.getCanonicalPath();
        int inputXIndex = -1;
        int inputYIndex = -1;
        int inputZIndex = -1;
        int offsetXIndex = -1;
        int offsetYIndex = -1;
        int offsetZIndex = -1;
        int outputXIndex = -1;
        int outputYIndex = -1;
        int outputZIndex = -1;
        int labelIndex = -1;
        for (int i = 0; i < columnHeaders.length; i++) {
            switch (columnHeaders[i]) {
                case "X":
                case "X1":
                case "Xin":
                case "X_in":
                case "Input_X":
                case "Robot_X":
                    inputXIndex = i;
                    break;

                case "Y":
                case "Y1":
                case "Yin":
                case "Y_in":
                case "Input_Y":
                case "Robot_Y":
                    inputYIndex = i;
                    break;

                case "Z":
                case "Z1":
                case "Zin":
                case "Z_in":
                case "Input_Z":
                case "Robot_Z":
                    inputZIndex = i;
                    break;

                case "Output_X":
                case "Db_X":
                case "X_Output":
                    outputXIndex = i;
                    break;

                case "Y_Output":
                case "Db_Y":
                case "Output_Y":
                    outputYIndex = i;
                    break;

                case "Z_Output":
                case "Db_Z":
                case "Output_Z":
                    outputZIndex = i;
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
                case "PartName":
                    labelIndex = i;
                    break;
            }
        }
        if (inputXIndex < 0) {
            throw new BadErrorMapFormatException("Couldn't find inputXIndex");
        }
        if (inputYIndex < 0) {
            throw new BadErrorMapFormatException("Couldn't find inputYIndex");
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
            double inputX = (inputXIndex >= 0 ? Double.parseDouble(a[inputXIndex]) : 0);
            double inputY = (inputYIndex >= 0 ? Double.parseDouble(a[inputYIndex]) : 0);
            double inputZ = (inputZIndex >= 0 ? Double.parseDouble(a[inputZIndex]) : 0);

            double offsetX = (offsetXIndex >= 0 ? Double.parseDouble(a[offsetXIndex]) : 0);
            double offsetY = (offsetYIndex >= 0 ? Double.parseDouble(a[offsetYIndex]) : 0);
            double offsetZ = (offsetZIndex >= 0 ? Double.parseDouble(a[offsetZIndex]) : 0);
            double outputX = (outputXIndex >= 0 ? Double.parseDouble(a[outputXIndex]) : inputX + offsetX);
            double outputY = (outputYIndex >= 0 ? Double.parseDouble(a[outputYIndex]) : inputY + offsetY);
            double outputZ = (outputZIndex >= 0 ? Double.parseDouble(a[outputZIndex]) : inputZ + offsetZ);
            if (Math.abs(inputX + offsetX - outputX) > 0.001) {
                System.err.println("(inputX+offsetX-outputX)=" + (inputX + offsetX - outputX) + ", inputX=" + inputX + ",offsetX=" + offsetX + ",outputX=" + outputX);
            }
            if (Math.abs(inputY + offsetY - outputY) > 0.001) {
                System.err.println("(inputY+offsetY-outputY)=" + (inputY + offsetY - outputY) + ", inputY=" + inputY + ",offsetY=" + offsetY + ",outputY=" + outputY);
            }
            if (Math.abs(inputZ + offsetZ - outputZ) > 0.001) {
                System.err.println("(inputZ+offsetZ-outputZ)=" + (inputZ + offsetZ - outputZ) + ", inputZ=" + inputZ + ",offsetZ=" + offsetZ + ",outputZ=" + outputZ);
            }
            String label = (labelIndex >= 0 && labelIndex < a.length ? a[labelIndex] : "");
            errmapList.add(PositionMapEntry.pointOffsetLabelEntry(inputX, inputY, inputZ, offsetX, offsetY, offsetZ, label));
        }
    }

    public void saveFile(File f) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("X,Y,Z,Offset_X,Offset_Y,Offset_Z");
            for (PositionMapEntry entry : this.errmapList) {
                pw.println(entry.getInputX() + "," + entry.getInputY() + "," + entry.getInputZ() + "," + entry.getOffsetX() + "," + entry.getOffsetY() + "," + entry.getOffsetZ());
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
        PointType poseInPoint = requireNonNull(poseIn.getPoint(), "poseIn.getPoint()");
        lastPointIn = poseInPoint;
        PointType offsetPt = getOffset(
                poseInPoint.getX(),
                poseInPoint.getY(),
                poseInPoint.getZ()
        );
        PointType pt = point(
                offsetPt.getX() + poseInPoint.getX(),
                offsetPt.getY() + poseInPoint.getY(),
                offsetPt.getZ() + poseInPoint.getZ()
        );
        PoseType poseOut = pose(pt,
                requireNonNull(poseIn.getXAxis(), "poseIn.getXAxis()"),
                requireNonNull(poseIn.getZAxis(), "poseIn.getZAxis()")
        );
        lastPointOut = poseOut.getPoint();
        return poseOut;
    }

    public PointType correctPoint(PointType ptIn) {
        if (errmapList.size() < 1) {
            lastPointIn = lastPointOut = ptIn;
            return ptIn;
        }
        if (!Double.isFinite(ptIn.getX())) {
            throw new IllegalArgumentException("ptIn.getX()=" + ptIn.getX() + ", ptIn=" + ptIn);
        }
        if (!Double.isFinite(ptIn.getY())) {
            throw new IllegalArgumentException("ptIn.getY()=" + ptIn.getY() + ", ptIn=" + ptIn);
        }
        if (!Double.isFinite(ptIn.getZ())) {
            throw new IllegalArgumentException("ptIn.getZ()=" + ptIn.getZ() + ", ptIn=" + ptIn);
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
        double dx = e.getInputX() - x;
        double dy = e.getInputY() - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    static public PositionMapEntry combine(PositionMapEntry e1, PositionMapEntry e2, double x, double y, double z) {
        if (null == e1) {
            return e2;
        }
        if (null == e2) {
            return e1;
        }
        PmCartesian c1 = new PmCartesian(e1.getInputX(), e1.getInputY(), e1.getInputZ());
        PmCartesian c2 = new PmCartesian(e2.getInputX(), e2.getInputY(), e2.getInputZ());
        PmCartesian diff = c2.subtract(c1);
        if (diff.mag() < 1e-6) {
            return PositionMapEntry.pointOffsetEntryCombining((e1.getInputX() + e2.getInputX()) / 2.0,
                    (e1.getInputY() + e2.getInputY()) / 2.0,
                    (e1.getInputZ() + e2.getInputZ()) / 2.0,
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

    private static @Nullable
    PositionMapEntry combineX(
            @Nullable PositionMapEntry e1,
            @Nullable PositionMapEntry e2,
            double x) {
        if (null == e1) {
            if (null != e2 && Math.abs(e2.getInputX() - x) < 1e-6) {
                return e2;
            } else {
                return null;
            }
        }
        if (null == e2) {
            if (null != e1 && Math.abs(e1.getInputX() - x) < 1e-6) {
                return e1;
            } else {
                return null;
            }
        }
        PmCartesian c1 = new PmCartesian(e1.getInputX(), e1.getInputY(), e1.getInputZ());
        PmCartesian c2 = new PmCartesian(e2.getInputX(), e2.getInputY(), e2.getInputZ());
        PmCartesian diff = c2.subtract(c1);
        if (Math.abs(diff.x) < 1e-6) {
            if (Math.abs(e1.getInputX() - x) > 1e-6) {
                return null;
            }
            return PositionMapEntry.pointOffsetEntryCombining((e1.getInputX() + e2.getInputX()) / 2.0,
                    (e1.getInputY() + e2.getInputY()) / 2.0,
                    (e1.getInputZ() + e2.getInputZ()) / 2.0,
                    (e1.getOffsetX() + e2.getOffsetX()) / 2.0,
                    (e1.getOffsetY() + e2.getOffsetY()) / 2.0,
                    (e1.getOffsetZ() + e2.getOffsetZ()) / 2.0,
                    e1, e2);
        }

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

    private static @Nullable
    PositionMapEntry combineY(PositionMapEntry e1, PositionMapEntry e2, double y) {
        if (null == e1) {
            if (null != e2 && Math.abs(e2.getInputY() - y) < 1e-6) {
                return e2;
            } else {
                return null;
            }
        }
        if (null == e2) {
            if (null != e1 && Math.abs(e1.getInputY() - y) < 1e-6) {
                return e1;
            } else {
                return null;
            }
        }
        PmCartesian c1 = new PmCartesian(e1.getInputX(), e1.getInputY(), e1.getInputZ());
        PmCartesian c2 = new PmCartesian(e2.getInputX(), e2.getInputY(), e2.getInputZ());
        PmCartesian diff = c2.subtract(c1);
        if (Math.abs(diff.y) < 1e-6) {
            if (Math.abs(e1.getInputY() - y) > 1e-6) {
                return null;
            }
            return PositionMapEntry.pointOffsetEntryCombining(
                    (e1.getInputX() + e2.getInputX()) / 2.0,
                    (e1.getInputY() + e2.getInputY()) / 2.0,
                    (e1.getInputZ() + e2.getInputZ()) / 2.0,
                    (e1.getOffsetX() + e2.getOffsetX()) / 2.0,
                    (e1.getOffsetY() + e2.getOffsetY()) / 2.0,
                    (e1.getOffsetZ() + e2.getOffsetZ()) / 2.0,
                    e1, e2);
        }

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
        PositionMapEntry e12 = findXCombo(inputY -> inputY <= y, x, y, z);
        PositionMapEntry e34 = findXCombo(inputY -> inputY >= y, x, y, z);
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
                        if (Math.abs(e12.getInputX() - x) > 1e-6) {
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
                                    if (Math.abs(e34.getInputX() - x) > 1e-6) {
                                        e34 = null;
                                        continue;
                                    }
                                } else {
                                    e34 = combineX(sortedList.get(k), sortedList.get(l), x);
                                }
                                if (null != e34
                                        && Math.abs(e34.getInputY() - e12.getInputY()) < 1e-6
                                        && Math.abs(e34.getInputY() - y) > 1e-6) {
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

    private @Nullable
    PositionMapEntry findXCombo(Predicate<Double> predy, double x, double y, double z) {
        List<PositionMapEntry> yFilteredList = errmapList.stream()
                .filter(e -> predy.test(e.getInputY()))
                .collect(Collectors.toList());
        return findXCombo(yFilteredList, x, y, z);
    }

    @SuppressWarnings("unused")
    private @Nullable
    PositionMapEntry findXCombo(List<PositionMapEntry> yFilteredList, double x, double y, double z) {
        if (yFilteredList.size() < 2) {
            if (yFilteredList.size() == 1 && Math.abs(yFilteredList.get(0).getInputX() - x) < 1e-6) {
                return yFilteredList.get(0);
            }
            return null;
        } else if (yFilteredList.size() == 2) {
            return combineX(yFilteredList.get(0), yFilteredList.get(1), x);
        }
        PositionMapEntry e1 = findEntry(inputX -> inputX <= x,
                yFilteredList,
                x, y);
        PositionMapEntry e2 = findEntry(inputX -> inputX >= x,
                yFilteredList,
                x, y);
        if (e1 == null && e2 != null) {
            final double e2fx = (e2.getInputX() + Double.MIN_NORMAL);
            e1 = findEntry(
                    inputX -> inputX > e2fx,
                    yFilteredList,
                    x, y);
        } else if (e1 != null && e2 == null) {
            final double e1fx = (e1.getInputX() - Double.MIN_NORMAL);
            e2 = findEntry(
                    inputX -> inputX < e1fx,
                    yFilteredList,
                    x, y);
        }
        return combineX(e1, e2, x);
    }

    private @Nullable
    PositionMapEntry findEntry(Predicate<Double> predx, List<PositionMapEntry> yfilteredList, double x, double y) {
        PositionMapEntry e1 = yfilteredList.stream()
                .filter(e -> predx.test(e.getInputX()))
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

    public @Nullable
    String getFileName() {
        return fileName;
    }

    public String[] getColumnHeaders() {
        return columnHeaders;
    }

    public @Nullable
    PointType getLastPointIn() {
        return lastPointIn;
    }

    public @Nullable
    PointType getLastPointOut() {
        return lastPointOut;
    }

    public @Nullable
    PointType getLastOffset() {
        return lastOffset;
    }

    public PositionMap reverse() {
        List<PositionMapEntry> l = new ArrayList<>();
        for (PositionMapEntry entry : errmapList) {
            l.add(PositionMapEntry.pointOffsetEntry(entry.getOutputX(), entry.getOutputY(), entry.getOutputZ(), -entry.getOffsetX(), -entry.getOffsetY(), -entry.getOffsetZ()));
        }
        PositionMap rpm = new PositionMap(l);
        return rpm;
    }
}
