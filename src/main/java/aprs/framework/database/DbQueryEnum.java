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
package aprs.framework.database;

/**
 * An enumeration of the various queries that can be used
 * through the DatabasePoseUpdater or QuerySet.
 * 
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public enum DbQueryEnum {
    GET_SINGLE_POSE,
    SET_SINGLE_POSE,
    GET_ALL_POSE,
    GET_SINGLE_TRAY_SLOT_DESIGN,
    SET_SINGLE_TRAY_SLOT_DESIGN,
    NEW_SINGLE_TRAY_SLOT_DESIGN,
    GET_ALL_TRAY_SLOT_DESIGNS,
    DELETE_SINGLE_POSE,
    SET_SINGLE_PT_POSE,
    SET_SINGLE_KT_POSE,
    GET_TRAY_SLOTS,
    PRE_VISION_CLEAN_DB,
    GET_PARTDESIGN_PART_COUNT,
    GET_ALL_PARTS_IN_KT,
    GET_ALL_PARTS_IN_PT,
    GET_PARTSTRAYS,
    GET_SLOTS,
    GET_TRAY_SLOTS_FROM_KIT_SKU,
    GET_ALL_NEW_POSE
    ;
}
