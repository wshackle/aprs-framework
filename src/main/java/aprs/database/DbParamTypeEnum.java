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
package aprs.database;

/**
 * An enumeration of the various parameter/result types that can be used in a 
 *  database query through the DatabasePoseUpdater or QuerySet.
 * 
 * The same type might be a used as a parameter to one query and one of the
 * results from another query.
 * 
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("unused")
public enum DbParamTypeEnum {
    TYPE, NAME, X, Y, Z, VXI, VXJ, VXK, VZI, VZJ, VZK,
    SLOT_DESIGN_ID,
    SLOT_ID,
    PART_DESIGN_NAME,
    TRAY_DESIGN_NAME,
    X_SLOT_OFFSET,
    Y_SLOT_OFFSET,
    VISIONCYCLE,
    PART_IN_DESIGN_COUNT,
    NODE_ID,
    TRAY_COMPLETE,
    EXTERNAL_SHAPE,
    EXTERNAL_SHAPE_MODEL_FILE_NAME,
    EXTERNAL_SHAPE_MODEL_FORMAT_NAME,
    SLOT_OCCUPIED,
    SKU_NAME,
    MAX_VISIONCYCLE,
    PRP_NAME,
    TRAY_NAME,
    DIAMETER
}
