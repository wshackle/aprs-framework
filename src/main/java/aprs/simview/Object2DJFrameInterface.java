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
package aprs.simview;

import aprs.database.PhysicalItem;
import crcl.base.PointType;
import crcl.base.PoseType;
import crcl.utils.XFutureVoid;
import java.io.File;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import rcs.posemath.PmCartesian;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
interface Object2DJFrameInterface {

    public List<PhysicalItem> getItems();

    public XFutureVoid setItems(List<PhysicalItem> items);

    public @Nullable
    File[] takeSnapshot(File f, PointType point, String label);

    public @Nullable
    File[] takeSnapshot(File f, PmCartesian point, String label);

    public @Nullable
    File[] takeSnapshot(File f, PoseType pose, String label);

    public @Nullable
    File[] takeSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint);

    public @Nullable
    File[] takeSnapshot(File f, PointType point, String label, int w, int h);

    public @Nullable
    File[] takeSnapshot(File f, PmCartesian point, String label, int w, int h);

    public @Nullable
    File[] takeSnapshot(File f, PoseType pose, String label, int w, int h);

    public @Nullable
    File[] takeSnapshot(File f, Collection<? extends PhysicalItem> itemsToPaint, int w, int h);

}
