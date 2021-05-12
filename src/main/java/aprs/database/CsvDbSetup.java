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

import static aprs.database.DbParamTypeEnum.DIAMETER;
import static aprs.database.DbParamTypeEnum.EXTERNAL_SHAPE_MODEL_FILE_NAME;
import static aprs.database.DbParamTypeEnum.EXTERNAL_SHAPE_MODEL_FORMAT_NAME;
import static aprs.database.DbParamTypeEnum.MAX_VISIONCYCLE;
import static aprs.database.DbParamTypeEnum.NAME;
import static aprs.database.DbParamTypeEnum.NODE_ID;
import static aprs.database.DbParamTypeEnum.PART_DESIGN_NAME;
import static aprs.database.DbParamTypeEnum.PRP_NAME;
import static aprs.database.DbParamTypeEnum.SKU_NAME;
import static aprs.database.DbParamTypeEnum.SLOT_DESIGN_ID;
import static aprs.database.DbParamTypeEnum.SLOT_ID;
import static aprs.database.DbParamTypeEnum.SLOT_OCCUPIED;
import static aprs.database.DbParamTypeEnum.TRAY_COMPLETE;
import static aprs.database.DbParamTypeEnum.TRAY_DESIGN_NAME;
import static aprs.database.DbParamTypeEnum.TRAY_NAME;
import static aprs.database.DbParamTypeEnum.VISIONCYCLE;
import static aprs.database.DbParamTypeEnum.VXI;
import static aprs.database.DbParamTypeEnum.VXJ;
import static aprs.database.DbParamTypeEnum.VXK;
import static aprs.database.DbParamTypeEnum.VZI;
import static aprs.database.DbParamTypeEnum.VZJ;
import static aprs.database.DbParamTypeEnum.VZK;
import static aprs.database.DbParamTypeEnum.X;
import static aprs.database.DbParamTypeEnum.X_SLOT_OFFSET;
import static aprs.database.DbParamTypeEnum.Y;
import static aprs.database.DbParamTypeEnum.Y_SLOT_OFFSET;
import static aprs.database.DbParamTypeEnum.Z;
import static aprs.database.DbQueryEnum.DELETE_SINGLE_POSE;
import static aprs.database.DbQueryEnum.GET_ALL_NEW_POSE;
import static aprs.database.DbQueryEnum.GET_ALL_PARTS_IN_KT;
import static aprs.database.DbQueryEnum.GET_ALL_PARTS_IN_PT;
import static aprs.database.DbQueryEnum.GET_ALL_POSE;
import static aprs.database.DbQueryEnum.GET_ALL_TRAY_SLOT_DESIGNS;
import static aprs.database.DbQueryEnum.GET_PARTDESIGN_PART_COUNT;
import static aprs.database.DbQueryEnum.GET_PARTSTRAYS;
import static aprs.database.DbQueryEnum.GET_SINGLE_POSE;
import static aprs.database.DbQueryEnum.GET_SINGLE_TRAY_SLOT_DESIGN;
import static aprs.database.DbQueryEnum.GET_SLOTS;
import static aprs.database.DbQueryEnum.GET_TRAY_SLOTS;
import static aprs.database.DbQueryEnum.GET_TRAY_SLOTS_FROM_KIT_SKU;
import static aprs.database.DbQueryEnum.NEW_SINGLE_TRAY_SLOT_DESIGN;
import static aprs.database.DbQueryEnum.PRE_VISION_CLEAN_DB;
import static aprs.database.DbQueryEnum.SET_SINGLE_KT_POSE;
import static aprs.database.DbQueryEnum.SET_SINGLE_POSE;
import static aprs.database.DbQueryEnum.SET_SINGLE_PT_POSE;
import static aprs.database.DbQueryEnum.SET_SINGLE_TRAY_SLOT_DESIGN;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 *
 * @author shackle
 */
public class CsvDbSetup implements DbSetup {

    public CsvDbSetup() throws IOException {

//        this.queriesMap = DbSetupBuilder.readResourceQueriesDirectory("aprs/database/neo4j/v2/");
//        File tmpFile = File.createTempFile("queriesMap", ".java");
//        System.out.println("tmpFile = " + tmpFile);
////                DbParamTypeEnum[] params=new DbParamTypeEnum[] {NAME, X, Y, Z, VXI, VXJ, VXK, VZI, VZJ, VZK, SKU_NAME, VISIONCYCLE};
//
//        try (PrintStream ps = new PrintStream(new FileOutputStream(tmpFile))) {
//            ps.println();
//            ps.println("\t\tMap<DbParamTypeEnum, String> results;");
//            ps.println();
//            for (DbQueryEnum qtype : DbQueryEnum.values()) {
//                DbQueryInfo info = this.queriesMap.get(qtype);
//                String query = info.getQuery();
//                DbParamTypeEnum[] params = info.getParams();
//                Map<DbParamTypeEnum, String> results = info.getResults();
//                ps.println("\t\t//"+qtype);
//                ps.println("\t\tresults = new EnumMap<>(DbParamTypeEnum.class);");
//                for(DbParamTypeEnum resultPType : results.keySet()) {
//                    String resultsName = results.get(resultPType);
//                    ps.println("\t\tresults.put("+resultPType+",\""+resultsName+"\");");
//                }
//                String origText = info.getOrigText();
//                String resourceName = info.getResourceName();
//                final String paramsString = Arrays.toString(params);
////                ps.println("\t\tString query=\"" + query + "\";");
////                ps.println("\t\tDbParamTypeEnum[] params= new DbParamTypeEnum[]{" + Arrays.toString(params) + "};");
//                ps.println("\t\tqueriesMap.put(" + qtype + ",");
//                ps.println("\t\t\tnew DbQueryInfo(");
//                ps.println("\t\t\t\t/*query= */ \"" + query.replace("\n", "\\n").replace("\r", "\\r") + "\",");
//                ps.println("\t\t\t\t/*params= */ new DbParamTypeEnum[] {" + paramsString.substring(1, paramsString.length() - 1) + "},");
//                ps.println("\t\t\t\t/*results= */ results,");
//                ps.println("\t\t\t\t/*origText= */ \"" + origText.replace("\n", "\\n").replace("\r", "\\r") + "\",");
//                ps.println("\t\t\t\t/*resourceName= */ \"" + resourceName + "\"");
//                ps.println("\t\t\t\t));");
//                ps.println();
//            }
//        }
//    }
//    
//    private void initQueriesMap() {
        queriesMap = new EnumMap<>(DbQueryEnum.class);
        Map<DbParamTypeEnum, String> results;

        results = new EnumMap<>(DbParamTypeEnum.class);
        results.put(NAME, "name");
        results.put(X, "x");
        results.put(Y, "y");
        results.put(Z, "z");
        results.put(VXI, "vxi");
        results.put(VXJ, "vxj");
        results.put(VXK, "vxk");
        results.put(VZI, "vxi");
        results.put(VZJ, "vxj");
        results.put(VZK, "vxk");
        results.put(VISIONCYCLE, "visioncycle");
        results.put(MAX_VISIONCYCLE, "maxvisioncycle");
        queriesMap.put(GET_SINGLE_POSE,
                new DbQueryInfo(
                        /*query= */"MATCH (o)\nwith max(o.visioncycle) as maxvisioncycle\nMATCH pointpath=(object { name:{1} } )-[:hasSolidObject_PrimaryLocation]->(n)-[r2]->(pose:Pose)- [r1:hasPose_Point] -> (p:Point),\nxaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\nzaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\nreturn object.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,p.hasPoint_Z as z, xaxis.hasVector_I as vxi,xaxis.hasVector_J as vxj,xaxis.hasVector_K as vxk, zaxis.hasVector_I as vzi,zaxis.hasVector_J as vzj,zaxis.hasVector_K as vzk, object.visioncycle as visioncycle, maxvisioncycle\n",
                        /*params= */ new DbParamTypeEnum[]{NAME},
                        /*results= */ results,
                        /*origText= */ "#params=NAME,\r\n#results={NAME=name,X=x,Y=y,Z=z,VXI=vxi,VXJ=vxj,VXK=vxk,VZI=vxi,VZJ=vxj,VZK=vxk,VISIONCYCLE=visioncycle,MAX_VISIONCYCLE=maxvisioncycle}\r\nMATCH (o)\r\nwith max(o.visioncycle) as maxvisioncycle\r\nMATCH pointpath=(object { name:{1} } )-[:hasSolidObject_PrimaryLocation]->(n)-[r2]->(pose:Pose)- [r1:hasPose_Point] -> (p:Point),\r\nxaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\r\nzaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\r\nreturn object.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,p.hasPoint_Z as z, xaxis.hasVector_I as vxi,xaxis.hasVector_J as vxj,xaxis.hasVector_K as vxk, zaxis.hasVector_I as vzi,zaxis.hasVector_J as vzj,zaxis.hasVector_K as vzk, object.visioncycle as visioncycle, maxvisioncycle\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/get_single_pose.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        queriesMap.put(SET_SINGLE_POSE,
                new DbQueryInfo(
                        /*query= */"MERGE (thing:Part { name:{1} } )\nmerge (thing) - [:hasSkuObject_Sku] -> (sku:StockKeepingUnit { name:{11} } )\nmerge (thing) -[:hasSolidObject_PrimaryLocation] -> () -[:hasPoseLocation_Pose] -> (pose:Pose)\nmerge (pose) - [:hasPose_Point] -> (pt:Point)\nmerge (pose) - [:hasPose_XAxis] -> (xaxis:Vector)\nmerge (pose) - [:hasPose_ZAxis] -> (zaxis:Vector)\nset pt.hasPoint_X= {2},pt.hasPoint_Y= {3},pt.hasPoint_Z= {4},pt.visioncycle= {12}\nset thing.visioncycle={12}\nset xaxis.hasVector_I={5}, xaxis.hasVector_J={6}, xaxis.hasVector_K={7}\nset zaxis.hasVector_I={8}, zaxis.hasVector_J={9}, zaxis.hasVector_K={10}\nreturn count(pt) + count(xaxis) + count(zaxis)\n",
                        /*params= */ new DbParamTypeEnum[]{NAME, X, Y, Z, VXI, VXJ, VXK, VZI, VZJ, VZK, SKU_NAME, VISIONCYCLE},
                        /*results= */ results,
                        /*origText= */ "#params=NAME{1}, X{2}, Y{3}, Z{4}, VXI{5},VXJ{6},VXK{7},VZI{8},VZJ{9},VZK{10},SKU_NAME{11},VISIONCYCLE{12}\r\n#results=\r\nMERGE (thing:Part { name:{1} } )\r\nmerge (thing) - [:hasSkuObject_Sku] -> (sku:StockKeepingUnit { name:{11} } )\r\nmerge (thing) -[:hasSolidObject_PrimaryLocation] -> () -[:hasPoseLocation_Pose] -> (pose:Pose)\r\nmerge (pose) - [:hasPose_Point] -> (pt:Point)\r\nmerge (pose) - [:hasPose_XAxis] -> (xaxis:Vector)\r\nmerge (pose) - [:hasPose_ZAxis] -> (zaxis:Vector)\r\nset pt.hasPoint_X= {2},pt.hasPoint_Y= {3},pt.hasPoint_Z= {4},pt.visioncycle= {12}\r\nset thing.visioncycle={12}\r\nset xaxis.hasVector_I={5}, xaxis.hasVector_J={6}, xaxis.hasVector_K={7}\r\nset zaxis.hasVector_I={8}, zaxis.hasVector_J={9}, zaxis.hasVector_K={10}\r\nreturn count(pt) + count(xaxis) + count(zaxis)\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/set_single_pose.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        results.put(NAME, "name");
        results.put(X, "x");
        results.put(Y, "y");
        results.put(Z, "z");
        results.put(VXI, "vxi");
        results.put(VXJ, "vxj");
        results.put(VZK, "vxk");
        results.put(VISIONCYCLE, "visioncycle");
        queriesMap.put(GET_ALL_POSE,
                new DbQueryInfo(
                        /*query= */"MATCH pointpath=(source)<-[:hasSkuObject_Sku]-(object)-[:hasSolidObject_PrimaryLocation]->(n)-[r2]->(pose:Pose)- [r1:hasPose_Point] -> (p:Point),\nxaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\nzaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\nreturn object.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,p.hasPoint_Z as z, xaxis.hasVector_I as vxi,xaxis.hasVector_J as vxj,xaxis.hasVector_K as vxk, zaxis.hasVector_I as vzi,zaxis.hasVector_J as vzj,zaxis.hasVector_K as vzk, object.visioncycle as visioncycle\n",
                        /*params= */ new DbParamTypeEnum[]{},
                        /*results= */ results,
                        /*origText= */ "#results={NAME=name,X=x,Y=y,Z=z,VXI=vxi,VXJ=vxj,VZK=vxk,VXI=vxi,VXJ=vxj,VZK=vxk,VISIONCYCLE=visioncycle}\r\nMATCH pointpath=(source)<-[:hasSkuObject_Sku]-(object)-[:hasSolidObject_PrimaryLocation]->(n)-[r2]->(pose:Pose)- [r1:hasPose_Point] -> (p:Point),\r\nxaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\r\nzaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\r\nreturn object.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,p.hasPoint_Z as z, xaxis.hasVector_I as vxi,xaxis.hasVector_J as vxj,xaxis.hasVector_K as vxk, zaxis.hasVector_I as vzi,zaxis.hasVector_J as vzj,zaxis.hasVector_K as vzk, object.visioncycle as visioncycle\r\n\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/get_all_pose.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        results.put(SLOT_DESIGN_ID, "1");
        results.put(PART_DESIGN_NAME, "2");
        results.put(TRAY_DESIGN_NAME, "3");
        results.put(X_SLOT_OFFSET, "4");
        results.put(Y_SLOT_OFFSET, "5");
        queriesMap.put(GET_SINGLE_TRAY_SLOT_DESIGN,
                new DbQueryInfo(
                        /*query= */"This is a stub which will not work.\n",
                        /*params= */ new DbParamTypeEnum[]{NAME},
                        /*results= */ results,
                        /*origText= */ "#params=NAME,\r\n#results={SLOT_DESIGN_ID=1,PART_DESIGN_NAME=2,TRAY_DESIGN_NAME=3,X_SLOT_OFFSET=4,Y_SLOT_OFFSET=5}\r\nThis is a stub which will not work.\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/get_single_tray_slot_design.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        queriesMap.put(SET_SINGLE_TRAY_SLOT_DESIGN,
                new DbQueryInfo(
                        /*query= */"This is a stub which will not work.\n",
                        /*params= */ new DbParamTypeEnum[]{SLOT_DESIGN_ID, PART_DESIGN_NAME, TRAY_DESIGN_NAME, X_SLOT_OFFSET, Y_SLOT_OFFSET},
                        /*results= */ results,
                        /*origText= */ "#params= SLOT_DESIGN_ID(1),PART_DESIGN_NAME(2), TRAY_DESIGN_NAME(2),X_SLOT_OFFSET(4),Y_SLOT_OFFSET(5)\r\n#results=\r\nThis is a stub which will not work.\r\n\r\n\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/set_single_tray_slot_design.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        queriesMap.put(NEW_SINGLE_TRAY_SLOT_DESIGN,
                new DbQueryInfo(
                        /*query= */"This is a stub which will not work.\n",
                        /*params= */ new DbParamTypeEnum[]{SLOT_DESIGN_ID, PART_DESIGN_NAME, TRAY_DESIGN_NAME, X_SLOT_OFFSET, Y_SLOT_OFFSET},
                        /*results= */ results,
                        /*origText= */ "#params= SLOT_DESIGN_ID(1),PART_DESIGN_NAME(2), TRAY_DESIGN_NAME(2),X_SLOT_OFFSET(4),Y_SLOT_OFFSET(5)\r\n#results=\r\nThis is a stub which will not work.\r\n\r\n\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/new_single_tray_slot_design.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        results.put(SLOT_DESIGN_ID, "1");
        results.put(PART_DESIGN_NAME, "2");
        results.put(TRAY_DESIGN_NAME, "3");
        results.put(X_SLOT_OFFSET, "4");
        results.put(Y_SLOT_OFFSET, "5");
        queriesMap.put(GET_ALL_TRAY_SLOT_DESIGNS,
                new DbQueryInfo(
                        /*query= */"This is a stub which will not work.\n",
                        /*params= */ new DbParamTypeEnum[]{},
                        /*results= */ results,
                        /*origText= */ "#params=\r\n#results={SLOT_DESIGN_ID=1,PART_DESIGN_NAME=2,TRAY_DESIGN_NAME=3,X_SLOT_OFFSET=4,Y_SLOT_OFFSET=5}\r\nThis is a stub which will not work.\r\n\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/get_all_tray_slot_designs.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        queriesMap.put(DELETE_SINGLE_POSE,
                new DbQueryInfo(
                        /*query= */"match (n { name:{1} } ) -[r] - () delete n,r\n",
                        /*params= */ new DbParamTypeEnum[]{NAME},
                        /*results= */ results,
                        /*origText= */ "#params=NAME,\r\n#results=\r\nmatch (n { name:{1} } ) -[r] - () delete n,r\r\n\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/delete_single_pose.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        queriesMap.put(SET_SINGLE_PT_POSE,
                new DbQueryInfo(
                        /*query= */"MERGE (thing:PartsTray { name:{1} } )\nmerge (thing) - [:hasSkuObject_Sku] -> (sku:StockKeepingUnit { name:{11} } )\nmerge (thing) -[:hasSolidObject_PrimaryLocation] -> () -[:hasPoseLocation_Pose] -> (pose:Pose)\nmerge (pose) - [:hasPose_Point] -> (pt:Point)\nmerge (pose) - [:hasPose_XAxis] -> (xaxis:Vector)\nmerge (pose) - [:hasPose_ZAxis] -> (zaxis:Vector)\nset pt.hasPoint_X= {2},pt.hasPoint_Y= {3},pt.hasPoint_Z= {4},pt.visioncycle= {12}\nset thing.visioncycle={12}\nset xaxis.hasVector_I={5}, xaxis.hasVector_J={6}, xaxis.hasVector_K={7}\nset zaxis.hasVector_I={8}, zaxis.hasVector_J={9}, zaxis.hasVector_K={10}\nreturn count(pt) + count(xaxis) + count(zaxis)\n",
                        /*params= */ new DbParamTypeEnum[]{NAME, X, Y, Z, VXI, VXJ, VXK, VZI, VZJ, VZK, SKU_NAME},
                        /*results= */ results,
                        /*origText= */ "#params=NAME{1}, X{2}, Y{3}, Z{4}, VXI{5},VXJ{6},VXK{7},VZI{8},VZJ{9},VZK{10},SKU_NAME{11}\r\n#results=\r\nMERGE (thing:PartsTray { name:{1} } )\r\nmerge (thing) - [:hasSkuObject_Sku] -> (sku:StockKeepingUnit { name:{11} } )\r\nmerge (thing) -[:hasSolidObject_PrimaryLocation] -> () -[:hasPoseLocation_Pose] -> (pose:Pose)\r\nmerge (pose) - [:hasPose_Point] -> (pt:Point)\r\nmerge (pose) - [:hasPose_XAxis] -> (xaxis:Vector)\r\nmerge (pose) - [:hasPose_ZAxis] -> (zaxis:Vector)\r\nset pt.hasPoint_X= {2},pt.hasPoint_Y= {3},pt.hasPoint_Z= {4},pt.visioncycle= {12}\r\nset thing.visioncycle={12}\r\nset xaxis.hasVector_I={5}, xaxis.hasVector_J={6}, xaxis.hasVector_K={7}\r\nset zaxis.hasVector_I={8}, zaxis.hasVector_J={9}, zaxis.hasVector_K={10}\r\nreturn count(pt) + count(xaxis) + count(zaxis)\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/set_single_pt_pose.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        queriesMap.put(SET_SINGLE_KT_POSE,
                new DbQueryInfo(
                        /*query= */"MERGE (thing:PartsTray { name:{1} } )\nmerge (thing) - [:hasSkuObject_Sku] -> (sku:StockKeepingUnit { name:{11} } )\nmerge (thing) -[:hasSolidObject_PrimaryLocation] -> () -[:hasPoseLocation_Pose] -> (pose:Pose)\nmerge (pose) - [:hasPose_Point] -> (pt:Point)\nmerge (pose) - [:hasPose_XAxis] -> (xaxis:Vector)\nmerge (pose) - [:hasPose_ZAxis] -> (zaxis:Vector)\nset pt.hasPoint_X= {2},pt.hasPoint_Y= {3},pt.hasPoint_Z= {4},pt.visioncycle= {12}\nset thing.visioncycle={12}\nset xaxis.hasVector_I={5}, xaxis.hasVector_J={6}, xaxis.hasVector_K={7}\nset zaxis.hasVector_I={8}, zaxis.hasVector_J={9}, zaxis.hasVector_K={10}\nreturn count(pt) + count(xaxis) + count(zaxis)\n",
                        /*params= */ new DbParamTypeEnum[]{NAME, X, Y, Z, VXI, VXJ, VXK, VZI, VZJ, VZK, SKU_NAME},
                        /*results= */ results,
                        /*origText= */ "#params=NAME{1}, X{2}, Y{3}, Z{4}, VXI{5},VXJ{6},VXK{7},VZI{8},VZJ{9},VZK{10},SKU_NAME{11}\r\n#results=\r\nMERGE (thing:PartsTray { name:{1} } )\r\nmerge (thing) - [:hasSkuObject_Sku] -> (sku:StockKeepingUnit { name:{11} } )\r\nmerge (thing) -[:hasSolidObject_PrimaryLocation] -> () -[:hasPoseLocation_Pose] -> (pose:Pose)\r\nmerge (pose) - [:hasPose_Point] -> (pt:Point)\r\nmerge (pose) - [:hasPose_XAxis] -> (xaxis:Vector)\r\nmerge (pose) - [:hasPose_ZAxis] -> (zaxis:Vector)\r\nset pt.hasPoint_X= {2},pt.hasPoint_Y= {3},pt.hasPoint_Z= {4},pt.visioncycle= {12}\r\nset thing.visioncycle={12}\r\nset xaxis.hasVector_I={5}, xaxis.hasVector_J={6}, xaxis.hasVector_K={7}\r\nset zaxis.hasVector_I={8}, zaxis.hasVector_J={9}, zaxis.hasVector_K={10}\r\nreturn count(pt) + count(xaxis) + count(zaxis)\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/set_single_kt_pose.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        results.put(NAME, "name");
        results.put(X, "x");
        results.put(Y, "y");
        results.put(SKU_NAME, "sku_name");
        results.put(PRP_NAME, "prp_name");
        results.put(TRAY_NAME, "tray_name");
        results.put(DIAMETER, "diameter");
        queriesMap.put(GET_TRAY_SLOTS,
                new DbQueryInfo(
                        /*query= */"match pointpath =(p:PartsTray)-[:hasPartsTray_Slot]-(s:Slot)-[:hasSlot_PartRefAndPose]-(prp:PartRefAndPose)-[:hasPartRefAndPose_Pose]-(pose:Pose)-[:hasPose_Point]-(point:Point), \nskupath = (prp) - [:hasPartRefAndPose_Sku] -> (sku:StockKeepingUnit)\nwhere p.name={1}\noptional match sku - [:hasStockKeepingUnit_InternalShape] -> (shape:CylindricalShape) \nreturn s.name as name, point.hasPoint_X as x, point.hasPoint_Y as y, sku.name as sku_name, prp.name as prp_name, p.name as tray_name,shape.hasCylindricalShape_Diameter as diameter\n",
                        /*params= */ new DbParamTypeEnum[]{NAME},
                        /*results= */ results,
                        /*origText= */ "#params= NAME{1}\r\n#results={NAME=name,X=x,Y=y,SKU_NAME=sku_name,PRP_NAME=prp_name,TRAY_NAME=tray_name,DIAMETER=diameter}\r\nmatch pointpath =(p:PartsTray)-[:hasPartsTray_Slot]-(s:Slot)-[:hasSlot_PartRefAndPose]-(prp:PartRefAndPose)-[:hasPartRefAndPose_Pose]-(pose:Pose)-[:hasPose_Point]-(point:Point), \r\nskupath = (prp) - [:hasPartRefAndPose_Sku] -> (sku:StockKeepingUnit)\r\nwhere p.name={1}\r\noptional match sku - [:hasStockKeepingUnit_InternalShape] -> (shape:CylindricalShape) \r\nreturn s.name as name, point.hasPoint_X as x, point.hasPoint_Y as y, sku.name as sku_name, prp.name as prp_name, p.name as tray_name,shape.hasCylindricalShape_Diameter as diameter\r\n\r\n\r\n\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/get_tray_slots.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        queriesMap.put(PRE_VISION_CLEAN_DB,
                new DbQueryInfo(
                        /*query= */"match (n) where n.visioncycle > 0\nset n.visioncycle=0\n",
                        /*params= */ new DbParamTypeEnum[]{},
                        /*results= */ results,
                        /*origText= */ "#params= \r\n#results=\r\nmatch (n) where n.visioncycle > 0\r\nset n.visioncycle=0\r\n\r\n\r\n\r\n\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/pre_vision_clean_db.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        queriesMap.put(GET_PARTDESIGN_PART_COUNT,
                new DbQueryInfo(
                        /*query= */"MATCH (n:StockKeepingUnit)-[:hasPartsTrayDesign_VesselSku]-(pd:PartsTrayDesign)-[hasPartsTrayDesign_PartRefAndPose]-(rap:PartRefAndPose) \nwhere n.name={1} RETURN count(rap)\n",
                        /*params= */ new DbParamTypeEnum[]{NAME},
                        /*results= */ results,
                        /*origText= */ "#params=NAME{1}\r\nMATCH (n:StockKeepingUnit)-[:hasPartsTrayDesign_VesselSku]-(pd:PartsTrayDesign)-[hasPartsTrayDesign_PartRefAndPose]-(rap:PartRefAndPose) \r\nwhere n.name={1} RETURN count(rap)\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/get_partdesign_part_count.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        queriesMap.put(GET_ALL_PARTS_IN_KT,
                new DbQueryInfo(
                        /*query= */"match (v) with max(v.visioncycle)-3 as maxv\nmatch partinkt = (n:Part) where n.name STARTS WITH {1} and n.visioncycle > maxv RETURN n.name as name\n",
                        /*params= */ new DbParamTypeEnum[]{NAME},
                        /*results= */ results,
                        /*origText= */ "#params=NAME(1),\r\n#results={PART_IN_KIT}\r\nmatch (v) with max(v.visioncycle)-3 as maxv\r\nmatch partinkt = (n:Part) where n.name STARTS WITH {1} and n.visioncycle > maxv RETURN n.name as name\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/get_all_parts_in_kt.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        queriesMap.put(GET_ALL_PARTS_IN_PT,
                new DbQueryInfo(
                        /*query= */"match (v) with max(v.visioncycle)-3 as maxv\nmatch partinpt = (n:Part) where n.name STARTS WITH {1} and n.visioncycle > maxv RETURN n.name as name\n",
                        /*params= */ new DbParamTypeEnum[]{NAME},
                        /*results= */ results,
                        /*origText= */ "#params=NAME(1),\r\n#results={NAME}\r\nmatch (v) with max(v.visioncycle)-3 as maxv\r\nmatch partinpt = (n:Part) where n.name STARTS WITH {1} and n.visioncycle > maxv RETURN n.name as name\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/get_all_parts_in_pt.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        results.put(NAME, "name");
        results.put(TRAY_DESIGN_NAME, "design");
        results.put(NODE_ID, "id");
        results.put(TRAY_COMPLETE, "complete");
        results.put(EXTERNAL_SHAPE_MODEL_FILE_NAME, "external_shape_model_file_name");
        results.put(EXTERNAL_SHAPE_MODEL_FORMAT_NAME, "external_shape_model_format_name");
        results.put(SKU_NAME, "sku_name");
        queriesMap.put(GET_PARTSTRAYS,
                new DbQueryInfo(
                        /*query= */"match pointpath=(n:StockKeepingUnit)-[:hasSkuObject_Sku]-(pt:PartsTray),\nexternalshape=(n)-[:hasStockKeepingUnit_ExternalShape]-(xshape),\ndesign=(pt)-[:hasPartsTray_Design]->(d:PartsTrayDesign)\nwhere n.name={1}\nreturn pt.name as name, n.name as sku_name, ID(pt) as id, d.name as design, pt.hasPartsTray_Complete as complete\n",
                        /*params= */ new DbParamTypeEnum[]{NAME},
                        /*results= */ results,
                        /*origText= */ "#params=NAME,\r\n#results={NAME=name,EXTERNAL_SHAPE_MODEL_FILE_NAME=external_shape_model_file_name, EXTERNAL_SHAPE_MODEL_FORMAT_NAME=external_shape_model_format_name, SKU_NAME=sku_name, NODE_ID=id, TRAY_DESIGN_NAME=design,TRAY_COMPLETE=complete,}\r\nmatch pointpath=(n:StockKeepingUnit)-[:hasSkuObject_Sku]-(pt:PartsTray),\r\nexternalshape=(n)-[:hasStockKeepingUnit_ExternalShape]-(xshape),\r\ndesign=(pt)-[:hasPartsTray_Design]->(d:PartsTrayDesign)\r\nwhere n.name={1}\r\nreturn pt.name as name, n.name as sku_name, ID(pt) as id, d.name as design, pt.hasPartsTray_Complete as complete\r\n\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/get_partstrays.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        results.put(NAME, "name");
        results.put(X, "x");
        results.put(Y, "y");
        results.put(Z, "z");
        results.put(SLOT_ID, "slot_id");
        results.put(EXTERNAL_SHAPE_MODEL_FILE_NAME, "external_shape_model_file_name");
        results.put(EXTERNAL_SHAPE_MODEL_FORMAT_NAME, "external_shape_model_format_name");
        results.put(SLOT_OCCUPIED, "slot_occupied");
        results.put(SKU_NAME, "sku_name");
        queriesMap.put(GET_SLOTS,
                new DbQueryInfo(
                        /*query= */"match pointpath =(p:PartsTray)-[:hasPartsTray_Slot]-(s:Slot)-[:hasSlot_PartRefAndPose]-(prp:PartRefAndPose)-[:hasPartRefAndPose_Pose]-(pose:Pose)-[:hasPose_Point]-(point:Point), \nprpstuff=(prp) - [:hasPartRefAndPose_Sku] -> (sku:StockKeepingUnit)-[:hasStockKeepingUnit_ExternalShape]-(xshape:ExternalShape)\nwhere p.name={1}\nreturn s.name as name, s.hasSlot_IsOccupied as slot_occupied, s.hasSlot_ID as slot_id, sku.name as sku_name, point.hasPoint_X as x, point.hasPoint_Y as y, point.hasPoint_Z as z\n",
                        /*params= */ new DbParamTypeEnum[]{NAME},
                        /*results= */ results,
                        /*origText= */ "#params= NAME{1}\r\n#results={NAME=name,SLOT_OCCUPIED=slot_occupied,SLOT_ID=slot_id,EXTERNAL_SHAPE_MODEL_FILE_NAME=external_shape_model_file_name, EXTERNAL_SHAPE_MODEL_FORMAT_NAME=external_shape_model_format_name ,SKU_NAME=sku_name,X=x, Y=y, Z=z}\r\nmatch pointpath =(p:PartsTray)-[:hasPartsTray_Slot]-(s:Slot)-[:hasSlot_PartRefAndPose]-(prp:PartRefAndPose)-[:hasPartRefAndPose_Pose]-(pose:Pose)-[:hasPose_Point]-(point:Point), \r\nprpstuff=(prp) - [:hasPartRefAndPose_Sku] -> (sku:StockKeepingUnit)-[:hasStockKeepingUnit_ExternalShape]-(xshape:ExternalShape)\r\nwhere p.name={1}\r\nreturn s.name as name, s.hasSlot_IsOccupied as slot_occupied, s.hasSlot_ID as slot_id, sku.name as sku_name, point.hasPoint_X as x, point.hasPoint_Y as y, point.hasPoint_Z as z\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/get_slots.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        results.put(NAME, "name");
        results.put(X, "x");
        results.put(Y, "y");
        results.put(NODE_ID, "nodeid");
        results.put(SKU_NAME, "skuname");
        queriesMap.put(GET_TRAY_SLOTS_FROM_KIT_SKU,
                new DbQueryInfo(
                        /*query= */"match pointpath=(n:StockKeepingUnit)-[:hasSkuObject_Sku]-(p:PartsTray)-[:hasPartsTray_Slot]-(s:Slot)-[:hasSlot_PartRefAndPose]-(prp:PartRefAndPose)-[:hasPartRefAndPose_Pose]-(pose:Pose)-[:hasPose_Point]-(point:Point),\npartsku=(prp)-[:hasPartRefAndPose_Sku]->(sku:StockKeepingUnit)\nwhere n.name={1}\nreturn distinct ID(s) as nodeid, s.name as name, sku.name as skuname, point.hasPoint_X as x, point.hasPoint_Y as y\n",
                        /*params= */ new DbParamTypeEnum[]{NAME},
                        /*results= */ results,
                        /*origText= */ "#params= NAME{1}\r\n#results={NODE_ID=nodeid, NAME=name,SKU_NAME=skuname,X=x,Y=y,}\r\nmatch pointpath=(n:StockKeepingUnit)-[:hasSkuObject_Sku]-(p:PartsTray)-[:hasPartsTray_Slot]-(s:Slot)-[:hasSlot_PartRefAndPose]-(prp:PartRefAndPose)-[:hasPartRefAndPose_Pose]-(pose:Pose)-[:hasPose_Point]-(point:Point),\r\npartsku=(prp)-[:hasPartRefAndPose_Sku]->(sku:StockKeepingUnit)\r\nwhere n.name={1}\r\nreturn distinct ID(s) as nodeid, s.name as name, sku.name as skuname, point.hasPoint_X as x, point.hasPoint_Y as y\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/get_tray_slots_from_kit_sku.txt"
                ));
        results = new EnumMap<>(DbParamTypeEnum.class);
        results.put(NAME, "name");
        results.put(X, "x");
        results.put(Y, "y");
        results.put(Z, "z");
        results.put(VXI, "vxi");
        results.put(VXJ, "vxj");
        results.put(VZK, "vxk");
        results.put(VISIONCYCLE, "visioncycle");
        results.put(SKU_NAME, "sku_name");
        queriesMap.put(GET_ALL_NEW_POSE,
                new DbQueryInfo(
                        /*query= */"MATCH (o)\nwith max(o.visioncycle) as maxvisioncycle\nMATCH pointpath=(sku:StockKeepingUnit)<-[:hasSkuObject_Sku]-(object)-[:hasSolidObject_PrimaryLocation]->(n)-[r2]->(pose:Pose)- [r1:hasPose_Point] -> (p:Point),\nxaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\nzaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\nwhere object.visioncycle > maxvisioncycle -2 \nreturn object.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,p.hasPoint_Z as z, xaxis.hasVector_I as vxi,xaxis.hasVector_J as vxj,xaxis.hasVector_K as vxk, zaxis.hasVector_I as vzi,zaxis.hasVector_J as vzj,zaxis.hasVector_K as vzk, object.visioncycle as visioncycle, maxvisioncycle,sku.name as sku_name\n",
                        /*params= */ new DbParamTypeEnum[]{},
                        /*results= */ results,
                        /*origText= */ "#results={NAME=name,X=x,Y=y,Z=z,VXI=vxi,VXJ=vxj,VZK=vxk,VXI=vxi,VXJ=vxj,VZK=vxk,VISIONCYCLE=visioncycle,SKU_NAME=sku_name}\r\nMATCH (o)\r\nwith max(o.visioncycle) as maxvisioncycle\r\nMATCH pointpath=(sku:StockKeepingUnit)<-[:hasSkuObject_Sku]-(object)-[:hasSolidObject_PrimaryLocation]->(n)-[r2]->(pose:Pose)- [r1:hasPose_Point] -> (p:Point),\r\nxaxispath= pose - [r3:hasPose_XAxis] -> (xaxis:Vector),\r\nzaxispath= pose - [r4:hasPose_ZAxis] -> (zaxis:Vector)\r\nwhere object.visioncycle > maxvisioncycle -2 \r\nreturn object.name as name,p.hasPoint_X as x,p.hasPoint_Y as y,p.hasPoint_Z as z, xaxis.hasVector_I as vxi,xaxis.hasVector_J as vxj,xaxis.hasVector_K as vxk, zaxis.hasVector_I as vzi,zaxis.hasVector_J as vzj,zaxis.hasVector_K as vzk, object.visioncycle as visioncycle, maxvisioncycle,sku.name as sku_name\r\n\r\n",
                        /*resourceName= */ "aprs/database/neo4j/v2/get_all_new_pose.txt"
                ));

    }

    @Override
    public DbType getDbType() {
        return DbType.NONE;
    }

    @Override
    public String getHost() {
        return "";
    }

    @Override
    public int getPort() {
        return -1;
    }

    @Override
    public String getDbName() {
        return "";
    }

    @Override
    public String getDbUser() {
        return "";
    }

    @Override
    public char[] getDbPassword() {
        return "".toCharArray();
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    private final Map<DbQueryEnum, DbQueryInfo> queriesMap;

    @Override
    public Map<DbQueryEnum, DbQueryInfo> getQueriesMap() {
        return queriesMap;
    }

    @Override
    public boolean isInternalQueriesResourceDir() {
        return false;
    }

    @Override
    public String getQueriesDir() {
        return "";
    }

    @Override
    public boolean isDebug() {
        return false;
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public String getStartScript() {
        return "";
    }
}
