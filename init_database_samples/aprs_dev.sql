-- MySQL dump 10.13  Distrib 5.5.50, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: aprs-dev
-- ------------------------------------------------------
-- Server version	5.5.50-0ubuntu0.14.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ActionBase`
--

DROP TABLE IF EXISTS `ActionBase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ActionBase` (
  `ActionBaseID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasActionBase_Description` varchar(255) DEFAULT NULL,
  `hasActionBase_Effect` varchar(255) DEFAULT NULL,
  `hadByAction_Domain` varchar(255) DEFAULT NULL,
  `hasActionBase_Precondition` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ActionBaseID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasActionBase_Effect` (`hasActionBase_Effect`),
  KEY `fkhadByAction_Domain` (`hadByAction_Domain`),
  KEY `fkhasActionBase_Precondition` (`hasActionBase_Precondition`),
  CONSTRAINT `fkhadByAction_Domain` FOREIGN KEY (`hadByAction_Domain`) REFERENCES `Domain` (`_NAME`),
  CONSTRAINT `fkhasActionBase_Effect` FOREIGN KEY (`hasActionBase_Effect`) REFERENCES `Effect` (`_NAME`),
  CONSTRAINT `fkhasActionBase_Precondition` FOREIGN KEY (`hasActionBase_Precondition`) REFERENCES `Precondition` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ActionBase`
--

LOCK TABLES `ActionBase` WRITE;
/*!40000 ALTER TABLE `ActionBase` DISABLE KEYS */;
INSERT INTO `ActionBase` VALUES (26,'look-for-part','Look for a part','look-for-part-effect','kitting-domain','look-for-part-precondition'),(97,'place-part','Place a part in a kit','place-part-effect','kitting-domain','place-part-precondition'),(101,'take-part','Take a part from a parts tray','take-part-effect','kitting-domain','take-part-precondition'),(119,'create-kit','Create a kit from a kit tray','create-kit-effect','kitting-domain','create-kit-precondition'),(162,'place-kit','Place a kit in a large box with kits','place-kit-effect','kitting-domain','place-kit-precondition'),(180,'take-kit','Take a kit from the table ','take-kit-effect','kitting-domain','take-kit-precondition'),(198,'attach-endEffector','Attach an end effector to a robot','attach-endEffector-effect','kitting-domain','attach-endEffector-precondition'),(215,'detach-endEffector','Detach an end effector from a robot and place it in the end effector holder','detach-endEffector-effect','kitting-domain','detach-endEffector-precondition'),(229,'move-over-part','The robot provided with an end effector moves over a part placed in a parts tray','move-over-part-effect','kitting-domain','move-over-part-precondition'),(296,'take-kitTray','Take a kit tray from a large box with empty kit trays','take-kitTray-effect','kitting-domain','take-kitTray-precondition'),(341,'place-kitTray','Place a kit tray on the table','place-kitTray-effect','kitting-domain','place-kitTray-precondition'),(409,'move-over-kit','The robot provided with an end effector moves over a kit located on a table','move-over-kit-effect','kitting-domain','move-over-kit-precondition'),(423,'move-over-kitTray','The robot provided with an end effector moves over a kit tray located in a large box empty kit trays','move-over-kitTray-effect','kitting-domain','move-over-kitTray-precondition');
/*!40000 ALTER TABLE `ActionBase` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ActionParameterSet`
--

DROP TABLE IF EXISTS `ActionParameterSet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ActionParameterSet` (
  `ActionParameterSetID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasActionParameterSet_ActionParameter` varchar(100) NOT NULL,
  `hasActionParameterSet_ActionParameterPosition` int(11) NOT NULL,
  `hadByActionParameterSet_ActionBase` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ActionParameterSetID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByActionParameterSet_ActionBase` (`hadByActionParameterSet_ActionBase`),
  CONSTRAINT `fkhadByActionParameterSet_ActionBase` FOREIGN KEY (`hadByActionParameterSet_ActionBase`) REFERENCES `ActionBase` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ActionParameterSet`
--

LOCK TABLES `ActionParameterSet` WRITE;
/*!40000 ALTER TABLE `ActionParameterSet` DISABLE KEYS */;
INSERT INTO `ActionParameterSet` VALUES (62,'take-part-param-1','Robot',1,'take-part'),(63,'take-part-param-4','PartsTray',4,'take-part'),(64,'take-part-param-5','EndEffector',5,'take-part'),(66,'take-part-param-2','Part',2,'take-part'),(67,'take-part-param-3','StockKeepingUnit',3,'take-part'),(68,'take-part-param-6','Kit',6,'take-part'),(99,'take-kitTray-param-5','StockKeepingUnit',5,'take-kitTray'),(105,'take-kitTray-param-1','Robot',1,'take-kitTray'),(106,'take-kitTray-param-2','KitTray',2,'take-kitTray'),(107,'take-kitTray-param-3','LargeBoxWithEmptyKitTrays',3,'take-kitTray'),(108,'take-kitTray-param-4','EndEffector',4,'take-kitTray'),(115,'move-over-kit-param-1','Robot',1,'move-over-kit'),(122,'move-over-kitTray-param-1','Robot',1,'move-over-kitTray'),(123,'move-over-kitTray-param-2','EndEffector',2,'move-over-kitTray'),(124,'move-over-kitTray-param-3','LargeBoxWithEmptyKitTrays',3,'move-over-kitTray'),(127,'move-over-kitTray-param-4','StockKeepingUnit',4,'move-over-kitTray'),(128,'move-over-kitTray-param-5','KitTray',5,'move-over-kitTray'),(146,'move-over-kit-param-4','Kit',4,'move-over-kit'),(147,'move-over-kit-param-5','KitTray',5,'move-over-kit'),(148,'move-over-kit-param-2','EndEffector',2,'move-over-kit'),(151,'move-over-kit-param-3','StockKeepingUnit',3,'move-over-kit'),(190,'look-for-part-param-1','Robot',1,'look-for-part'),(192,'look-for-part-param-4','Kit',4,'look-for-part'),(193,'look-for-part-param-5','EndEffector',5,'look-for-part'),(194,'look-for-part-param-2','Part',2,'look-for-part'),(195,'look-for-part-param-3','StockKeepingUnit',3,'look-for-part'),(239,'take-kit-param-1','Robot',1,'take-kit'),(240,'take-kit-param-2','Kit',2,'take-kit'),(245,'take-kit-param-5','StockKeepingUnit',5,'take-kit'),(246,'take-kit-param-6','EndEffector',6,'take-kit'),(247,'take-kit-param-3','KitTray',3,'take-kit'),(248,'take-kit-param-4','WorkTable',4,'take-kit'),(251,'place-kitTray-param-4','EndEffector',4,'place-kitTray'),(252,'place-kitTray-param-3','WorkTable',3,'place-kitTray'),(253,'place-kitTray-param-2','KitTray',2,'place-kitTray'),(254,'place-kitTray-param-1','Robot',1,'place-kitTray'),(266,'place-part-param-1','Robot',1,'place-part'),(279,'place-part-param-6','WorkTable',6,'place-part'),(280,'place-part-param-7','PartsTray',7,'place-part'),(281,'place-part-param-2','Part',2,'place-part'),(282,'place-part-param-3','StockKeepingUnit',3,'place-part'),(283,'place-part-param-4','Kit',4,'place-part'),(284,'place-part-param-5','EndEffector',5,'place-part'),(334,'attach-endEffector-param-4','EndEffectorChangingStation',4,'attach-endEffector'),(342,'attach-endEffector-param-2','EndEffector',2,'attach-endEffector'),(343,'attach-endEffector-param-3','EndEffectorHolder',3,'attach-endEffector'),(344,'attach-endEffector-param-1','Robot',1,'attach-endEffector'),(350,'detach-endEffector-param-4','EndEffectorChangingStation',4,'detach-endEffector'),(351,'detach-endEffector-param-3','EndEffectorHolder',3,'detach-endEffector'),(353,'detach-endEffector-param-2','EndEffector',2,'detach-endEffector'),(356,'detach-endEffector-param-1','Robot',1,'detach-endEffector'),(370,'move-over-part-param-1','Robot',1,'move-over-part'),(373,'move-over-part-param-2','EndEffector',2,'move-over-part'),(379,'move-over-part-param-5','StockKeepingUnit',5,'move-over-part'),(380,'move-over-part-param-3','PartsTray',3,'move-over-part'),(382,'move-over-part-param-4','Part',4,'move-over-part'),(395,'create-kit-param-1','Kit',1,'create-kit'),(397,'create-kit-param-3','WorkTable',3,'create-kit'),(398,'create-kit-param-2','KitTray',2,'create-kit'),(399,'place-kit-param-4','LargeBoxWithKits',4,'place-kit'),(401,'place-kit-param-3','EndEffector',3,'place-kit'),(404,'place-kit-param-2','Kit',2,'place-kit'),(405,'place-kit-param-1','Robot',1,'place-kit');
/*!40000 ALTER TABLE `ActionParameterSet` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `BoxVolume`
--

DROP TABLE IF EXISTS `BoxVolume`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `BoxVolume` (
  `BoxVolumeID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasBoxVolume_MaximumPoint` varchar(255) DEFAULT NULL,
  `hasBoxVolume_MinimumPoint` varchar(255) DEFAULT NULL,
  `hadByOtherObstacle_KittingWorkstation` varchar(255) DEFAULT NULL,
  `hadByWorkVolume_Robot` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`BoxVolumeID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasBoxVolume_MaximumPoint` (`hasBoxVolume_MaximumPoint`),
  KEY `fkhasBoxVolume_MinimumPoint` (`hasBoxVolume_MinimumPoint`),
  KEY `fkhadByOtherObstacle_KittingWorkstation` (`hadByOtherObstacle_KittingWorkstation`),
  KEY `fkhadByWorkVolume_Robot` (`hadByWorkVolume_Robot`),
  CONSTRAINT `fkhadByOtherObstacle_KittingWorkstation` FOREIGN KEY (`hadByOtherObstacle_KittingWorkstation`) REFERENCES `KittingWorkstation` (`_NAME`),
  CONSTRAINT `fkhadByWorkVolume_Robot` FOREIGN KEY (`hadByWorkVolume_Robot`) REFERENCES `Robot` (`_NAME`),
  CONSTRAINT `fkhasBoxVolume_MaximumPoint` FOREIGN KEY (`hasBoxVolume_MaximumPoint`) REFERENCES `Point` (`_NAME`),
  CONSTRAINT `fkhasBoxVolume_MinimumPoint` FOREIGN KEY (`hasBoxVolume_MinimumPoint`) REFERENCES `Point` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `BoxVolume`
--

LOCK TABLES `BoxVolume` WRITE;
/*!40000 ALTER TABLE `BoxVolume` DISABLE KEYS */;
INSERT INTO `BoxVolume` VALUES (221,'box_volume_1','point_max','point_min',NULL,'robot_1');
/*!40000 ALTER TABLE `BoxVolume` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `BoxyShape`
--

DROP TABLE IF EXISTS `BoxyShape`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `BoxyShape` (
  `BoxyShapeID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasBoxyShape_HasTop` tinyint(1) NOT NULL,
  `hasBoxyShape_Width` double NOT NULL,
  `hasBoxyShape_Height` double NOT NULL,
  `hasBoxyShape_Length` double NOT NULL,
  PRIMARY KEY (`BoxyShapeID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `BoxyShape`
--

LOCK TABLES `BoxyShape` WRITE;
/*!40000 ALTER TABLE `BoxyShape` DISABLE KEYS */;
INSERT INTO `BoxyShape` VALUES (134,'shape_kit_box',0,300,300,500),(155,'shape_washerShaft_tray',0,100,20,100),(227,'shape_GearBox_top',1,95.6,24,197),(233,'shape_gear_tray',0,100,50,270),(244,'shape_GearBox_kit_tray',0,240,50,340),(269,'shape_GearBox_base',1,100,47.5,196),(286,'shape_topBase_tray',0,214,80,330),(303,'shape_gripper_holder_1',1,80,1,80),(304,'shape_gripper_holder_2',1,80,1,80),(328,'shape_changing_station_base',1,100,500,420),(336,'shape_work_table',1,800,500,1600);
/*!40000 ALTER TABLE `BoxyShape` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `CylindricalShape`
--

DROP TABLE IF EXISTS `CylindricalShape`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CylindricalShape` (
  `CylindricalShapeID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasCylindricalShape_HasTop` tinyint(1) NOT NULL,
  `hasCylindricalShape_Height` double NOT NULL,
  `hasCylindricalShape_Diameter` double NOT NULL,
  PRIMARY KEY (`CylindricalShapeID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CylindricalShape`
--

LOCK TABLES `CylindricalShape` WRITE;
/*!40000 ALTER TABLE `CylindricalShape` DISABLE KEYS */;
INSERT INTO `CylindricalShape` VALUES (69,'shape_medium_gear',1,25.5,59.7),(91,'shape_small_gear',1,21.4,43.7),(249,'shape_large_gear',1,37,81),(308,'shape_washer',1,0.343,10.16),(390,'shape_shaft',1,34.239,4.966);
/*!40000 ALTER TABLE `CylindricalShape` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `DataThing`
--

DROP TABLE IF EXISTS `DataThing`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DataThing` (
  `DataThingID` int(11) NOT NULL AUTO_INCREMENT,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`DataThingID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB AUTO_INCREMENT=427 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `DataThing`
--

LOCK TABLES `DataThing` WRITE;
/*!40000 ALTER TABLE `DataThing` DISABLE KEYS */;
INSERT INTO `DataThing` VALUES (198,'attach-endEffector'),(167,'attach-endEffector-effect'),(165,'attach-endEffector-not-1'),(170,'attach-endEffector-not-2'),(168,'attach-endEffector-not-3'),(344,'attach-endEffector-param-1'),(342,'attach-endEffector-param-2'),(343,'attach-endEffector-param-3'),(334,'attach-endEffector-param-4'),(236,'attach-endEffector-precondition'),(221,'box_volume_1'),(287,'capacity-of-kits-in-lbwk'),(39,'capacity-of-parts-in-kit'),(120,'changing_station_base_point'),(174,'changing_station_base_pose'),(371,'changing_station_base_x_axis'),(307,'changing_station_base_z_axis'),(298,'changing_station_point'),(217,'changing_station_pose'),(160,'changing_station_x_axis'),(414,'changing_station_z_axis'),(417,'ContainedIn'),(377,'ContainedInLBWK'),(119,'create-kit'),(205,'create-kit-effect'),(135,'create-kit-not-1'),(136,'create-kit-not-2'),(395,'create-kit-param-1'),(398,'create-kit-param-2'),(397,'create-kit-param-3'),(392,'create-kit-precondition'),(295,'current-quantity-of-parts-in-kit'),(215,'detach-endEffector'),(274,'detach-endEffector-effect'),(1,'detach-endEffector-effect-np-1'),(2,'detach-endEffector-effect-np-2'),(356,'detach-endEffector-param-1'),(353,'detach-endEffector-param-2'),(351,'detach-endEffector-param-3'),(350,'detach-endEffector-param-4'),(206,'detach-endEffector-precondition'),(267,'empty_kit_tray_box_point'),(316,'empty_kit_tray_box_pose'),(394,'empty_kit_tray_box_x_axis'),(117,'empty_kit_tray_box_z_axis'),(163,'empty_kit_tray_supply_point'),(204,'empty_kit_tray_supply_pose'),(83,'empty_kit_tray_supply_x_axis'),(161,'empty_kit_tray_supply_z_axis'),(149,'EndEffector-EndEffectorHolder-ContainedIn'),(178,'EndEffector-EndEffectorHolder-PartiallyIn'),(268,'EndEffector-EndEffectorHolder-SR'),(23,'EndEffector-EndEffectorHolder-SR-ContainedIn'),(355,'endEffector-has-heldObject-kit'),(152,'endEffector-has-heldObject-kitTray'),(100,'endEffector-has-heldObject-part'),(424,'endEffector-has-no-heldObject'),(416,'endEffector-has-physicalLocation-refObject-endEffectorHolder'),(421,'endEffector-has-physicalLocation-refObject-robot'),(102,'endEffector-is-for-kitTraySKU'),(141,'endEffector-is-for-partSKU'),(300,'endEffector-is-over-kit'),(408,'endEffector-is-over-kitTray'),(133,'endEffector-is-over-part'),(88,'EndEffector-Kit-SR'),(176,'EndEffector-Kit-SR-ContainedIn'),(60,'EndEffector-Kit-SR-NotUnderWithContact'),(154,'EndEffector-Kit-SR-PartiallyIn'),(271,'EndEffector-KitTray-SR-NotUnderWithContact'),(293,'EndEffector-kitTray-SR-PartiallyIn'),(138,'EndEffector-Part-SR-NotUnderWithContact'),(250,'EndEffector-Part-SR-PartiallyIn'),(228,'EndEffector-Robot-SR-InContactWith'),(294,'EndEffector-Robot-SR-NotInContactWith'),(130,'EndEffector-SolidObject-SR'),(129,'EndEffector-SolidObject-SR-NotContainedIn'),(54,'EndEffector-SolidObject-SR-NotUnderWithContact'),(376,'EndEffectorChangingStation-EndEffectorHolder-SR-InContactWith'),(289,'endEffectorChangingStation-has-endEffectorHolder'),(230,'EndEffectorHolder-EndEffectorChangingStation-SR-ContainedIn'),(36,'endEffectorHolder-has-endEffector'),(270,'endEffectorHolder-has-physicalLocation-refObject-changingStation'),(61,'final-quantity-of-parts-in-kit'),(214,'finished_kit_box_point'),(177,'finished_kit_box_pose'),(407,'finished_kit_box_x_axis'),(393,'finished_kit_box_z_axis'),(340,'finished_kit_receiver_point'),(223,'finished_kit_receiver_pose'),(47,'finished_kit_receiver_x_axis'),(264,'finished_kit_receiver_z_axis'),(232,'GearBox_base_1_point'),(18,'GearBox_base_1_pose'),(367,'GearBox_base_1_x_axis'),(28,'GearBox_base_1_z_axis'),(322,'GearBox_base_tray_point'),(71,'GearBox_base_tray_pose'),(186,'GearBox_base_tray_x_axis'),(121,'GearBox_base_tray_z_axis'),(187,'GearBox_top_1_point'),(425,'GearBox_top_1_pose'),(35,'GearBox_top_1_x_axis'),(406,'GearBox_top_1_z_axis'),(333,'GearBox_top_tray_point'),(10,'GearBox_top_tray_pose'),(324,'GearBox_top_tray_x_axis'),(231,'GearBox_top_tray_z_axis'),(410,'InContactWith'),(9,'Kit-EndEffector-SR'),(369,'Kit-EndEffector-SR-ContainedIn'),(263,'Kit-EndEffector-SR-NotUnderWithContact'),(92,'Kit-EndEffector-SR-PartiallyIn'),(242,'kit-exists'),(352,'kit-has-kitTray'),(339,'kit-has-physicalLocation-refObject-endEffector'),(387,'kit-has-physicalLocation-refObject-lbwk'),(111,'kit-has-physicalLocation-refObject-workTable'),(12,'Kit-LargeBoxWithKits-SR'),(158,'Kit-LargeBoxWithKits-SR-ContainedIn'),(140,'Kit-LargeBoxWithKits-SR-PartiallyIn'),(374,'Kit-WorkTable-SR-UnderWithContact'),(218,'kitting-domain'),(73,'kitTray-EndEffector-SR'),(365,'KitTray-endEffector-SR-NotUnderWithContact'),(301,'KitTray-endEffector-SR-PartiallyIn'),(131,'KitTray-EndEffector-SRContainedIn'),(315,'kitTray-has-physicalLocation-refObject-endEffector'),(3,'kitTray-has-physicalLocation-refObject-kit'),(164,'kitTray-has-physicalLocation-refObject-lbwekt'),(93,'kitTray-has-physicalLocation-refObject-workTable'),(276,'kitTray-has-skuObject-sku'),(126,'KitTray-LargeBoxWithEmptyKitTrays-SR-ContainedIn'),(403,'KitTray-WorkTable-SR-UnderWithContact'),(46,'kit_design_GearBox'),(314,'kit_gearbox_point'),(25,'kit_gearbox_pose'),(185,'kit_gearbox_x_axis'),(212,'kit_gearbox_z_axis'),(384,'kit_tray_1_point'),(159,'kit_tray_1_pose'),(20,'kit_tray_1_x_axis'),(21,'kit_tray_1_z_axis'),(422,'LargeBoxWithKits-Kit-SR'),(31,'LargeBoxWithKits-Kit-SR-ContainedIn'),(17,'LargeBoxWithKits-Kit-SR-PartiallyIn'),(82,'large_gear_1_point'),(78,'large_gear_1_pose'),(90,'large_gear_1_x_axis'),(103,'large_gear_1_z_axis'),(37,'large_gear_tray_point'),(191,'large_gear_tray_pose'),(327,'large_gear_tray_x_axis'),(258,'large_gear_tray_z_axis'),(89,'lbwk-has-kit'),(26,'look-for-part'),(52,'look-for-part-Decrease'),(220,'look-for-part-effect'),(391,'look-for-part-FunctionToNumberGreater'),(190,'look-for-part-param-1'),(194,'look-for-part-param-2'),(195,'look-for-part-param-3'),(192,'look-for-part-param-4'),(193,'look-for-part-param-5'),(104,'look-for-part-precondition'),(95,'medium_1_z_axis'),(59,'medium_gear_1_point'),(338,'medium_gear_1_pose'),(348,'medium_gear_1_x_axis'),(234,'medium_gear_tray_point'),(209,'medium_gear_tray_pose'),(362,'medium_gear_tray_x_axis'),(412,'medium_tray_z_axis'),(409,'move-over-kit'),(139,'move-over-kit-effect'),(115,'move-over-kit-param-1'),(148,'move-over-kit-param-2'),(151,'move-over-kit-param-3'),(146,'move-over-kit-param-4'),(147,'move-over-kit-param-5'),(116,'move-over-kit-precondition'),(423,'move-over-kitTray'),(197,'move-over-kitTray-effect'),(122,'move-over-kitTray-param-1'),(123,'move-over-kitTray-param-2'),(124,'move-over-kitTray-param-3'),(127,'move-over-kitTray-param-4'),(128,'move-over-kitTray-param-5'),(224,'move-over-kitTray-precondition'),(229,'move-over-part'),(173,'move-over-part-effect'),(370,'move-over-part-param-1'),(373,'move-over-part-param-2'),(380,'move-over-part-param-3'),(382,'move-over-part-param-4'),(379,'move-over-part-param-5'),(312,'move-over-part-precondition'),(84,'NotContainedIn'),(386,'NotInContactWith'),(114,'NotOnTopWithContact'),(94,'NotUnderWithContact'),(363,'OnTopOf'),(243,'OnTopWithContact'),(210,'part-endEffector-SR'),(24,'part-endEffector-SR-ContainedIn'),(196,'part-endEffector-SR-NotUnderWithContact'),(415,'part-endEffector-SR-PartiallyIn'),(347,'part-found-flag'),(302,'part-has-physicalLocation-refObject-endEffector'),(323,'part-has-physicalLocation-refObject-kit'),(388,'part-has-physicalLocation-refObject-partsTray'),(216,'part-has-skuObject-sku'),(326,'part-is-found'),(273,'Part-Kit-SR'),(75,'Part-Kit-SR-ContainedIn'),(157,'Part-Kit-SR-PartiallyIn'),(426,'Part-PartsTray-SR-ContainedIn'),(181,'Part-PartsTray-SR-PartiallyIn'),(8,'PartiallyIn'),(188,'PartiallyInAndInContactWith'),(65,'PartsTray-Part-SR-ContainedIn'),(272,'partsVessel-has-part'),(189,'part_gripper_holder_point'),(345,'part_gripper_holder_pose'),(309,'part_gripper_holder_x_axis'),(29,'part_gripper_holder_z_axis'),(213,'part_gripper_point'),(208,'part_gripper_pose'),(317,'part_gripper_x_axis'),(346,'part_gripper_z_axis'),(372,'part_ref_and_pose_kit_GearBox_base'),(297,'part_ref_and_pose_kit_GearBox_top'),(332,'part_ref_and_pose_kit_large_gear'),(113,'part_ref_and_pose_kit_medium_gear'),(169,'part_ref_and_pose_kit_small_gear'),(162,'place-kit'),(32,'place-kit-effect'),(411,'place-kit-FunctionToFunctionLess'),(385,'place-kit-Increase'),(292,'place-kit-not-1'),(291,'place-kit-not-2'),(290,'place-kit-not-3'),(405,'place-kit-param-1'),(404,'place-kit-param-2'),(401,'place-kit-param-3'),(399,'place-kit-param-4'),(96,'place-kit-precondition'),(341,'place-kitTray'),(418,'place-kitTray-effect'),(56,'place-kitTray-not-1'),(53,'place-kitTray-not-2'),(254,'place-kitTray-param-1'),(253,'place-kitTray-param-2'),(252,'place-kitTray-param-3'),(251,'place-kitTray-param-4'),(354,'place-kitTray-precondition'),(97,'place-part'),(257,'place-part-effect'),(337,'place-part-FunctionToFunctionLess'),(320,'place-part-Increase-1'),(321,'place-part-Increase-2'),(319,'place-part-Increase-3'),(81,'place-part-not-1'),(80,'place-part-not-2'),(266,'place-part-param-1'),(281,'place-part-param-2'),(282,'place-part-param-3'),(283,'place-part-param-4'),(284,'place-part-param-5'),(279,'place-part-param-6'),(280,'place-part-param-7'),(41,'place-part-precondition'),(306,'point_kit_GearBox_base'),(413,'point_kit_GearBox_top'),(278,'point_kit_large_gear'),(396,'point_kit_medium_gear'),(7,'point_kit_small_gear'),(419,'point_max'),(16,'point_min'),(50,'quantity-of-kits-in-lbwk'),(49,'quantity-of-kittrays-in-lbwekt'),(70,'quantity-of-parts-in-kit'),(87,'quantity-of-parts-in-partstray'),(261,'RCC8-ContainedIn'),(226,'RCC8-ContainedInLBWK'),(219,'RCC8-InContactWith'),(183,'RCC8-NotContainedIn'),(86,'RCC8-NotInContactWith'),(211,'RCC8-NotOnTopWithContact'),(72,'RCC8-NotUnderWithContact'),(132,'RCC8-OnTopOf'),(150,'RCC8-OnTopWithContact'),(30,'RCC8-PartiallyIn'),(222,'RCC8-PartiallyInAndInContactWith'),(4,'RCC8-Under'),(85,'RCC8-UnderWithContact'),(203,'relative_location_in_1'),(200,'Robot-EndEffector-SR-InContactWith'),(277,'robot-has-endEffector'),(51,'robot-has-no-endEffector'),(55,'robot_point'),(33,'robot_pose'),(259,'robot_x_axis'),(5,'robot_z_axis'),(145,'shaft_tray_point'),(11,'shaft_tray_pose'),(275,'shaft_tray_x_axis'),(207,'shaft_tray_z_axis'),(328,'shape_changing_station_base'),(269,'shape_GearBox_base'),(137,'shape_GearBox_base_stl'),(244,'shape_GearBox_kit_tray'),(227,'shape_GearBox_top'),(184,'shape_GearBox_top_stl'),(233,'shape_gear_tray'),(303,'shape_gripper_holder_1'),(304,'shape_gripper_holder_2'),(134,'shape_kit_box'),(249,'shape_large_gear'),(15,'shape_large_gear_stl'),(69,'shape_medium_gear'),(153,'shape_medium_gear_stl'),(390,'shape_shaft'),(335,'shape_shaft_stl'),(91,'shape_small_gear'),(288,'shape_small_gear_stl'),(286,'shape_topBase_tray'),(308,'shape_washer'),(155,'shape_washerShaft_tray'),(325,'shape_washer_stl'),(336,'shape_work_table'),(182,'small_1_z_axis'),(420,'small_gear_1_point'),(305,'small_gear_1_pose'),(172,'small_gear_1_x_axis'),(364,'small_gear_tray_point'),(110,'small_gear_tray_pose'),(34,'small_gear_tray_x_axis'),(79,'small_tray_z_axis'),(201,'soap'),(43,'stock_keeping_unit_GearBox_base'),(40,'stock_keeping_unit_GearBox_kit_tray'),(299,'stock_keeping_unit_GearBox_top'),(389,'stock_keeping_unit_gear_tray'),(179,'stock_keeping_unit_kit_box'),(74,'stock_keeping_unit_large_gear'),(255,'stock_keeping_unit_medium_gear'),(330,'stock_keeping_unit_shaft'),(318,'stock_keeping_unit_small_gear'),(19,'stock_keeping_unit_topBase_tray'),(235,'stock_keeping_unit_washer'),(310,'stock_keeping_unit_washerShaft_tray'),(180,'take-kit'),(262,'take-kit-effect'),(368,'take-kit-FunctionToFunctionEqual'),(357,'take-kit-not-1'),(358,'take-kit-not-2'),(359,'take-kit-not-3'),(360,'take-kit-not-4'),(239,'take-kit-param-1'),(240,'take-kit-param-2'),(247,'take-kit-param-3'),(248,'take-kit-param-4'),(245,'take-kit-param-5'),(246,'take-kit-param-6'),(199,'take-kit-precondition'),(296,'take-kitTray'),(375,'take-kitTray-Decrease'),(400,'take-kitTray-effect'),(109,'take-kitTray-FunctionToNumberGreater'),(381,'take-kitTray-not-1'),(383,'take-kitTray-not-2'),(378,'take-kitTray-not-3'),(105,'take-kitTray-param-1'),(106,'take-kitTray-param-2'),(107,'take-kitTray-param-3'),(108,'take-kitTray-param-4'),(99,'take-kitTray-param-5'),(366,'take-kitTray-precondition'),(101,'take-part'),(38,'take-part-Decrease'),(118,'take-part-effect'),(142,'take-part-effect-not-1'),(143,'take-part-effect-not-2'),(144,'take-part-effect-not-3'),(313,'take-part-FunctionToNumberGreater'),(62,'take-part-param-1'),(66,'take-part-param-2'),(67,'take-part-param-3'),(63,'take-part-param-4'),(64,'take-part-param-5'),(68,'take-part-param-6'),(42,'take-part-precondition'),(6,'tray_gripper_holder_point'),(45,'tray_gripper_holder_pose'),(13,'tray_gripper_holder_x_axis'),(77,'tray_gripper_holder_z_axis'),(98,'tray_gripper_point'),(202,'tray_gripper_pose'),(76,'tray_gripper_x_axis'),(260,'tray_gripper_z_axis'),(48,'Under'),(225,'UnderWithContact'),(22,'washer_tray_point'),(166,'washer_tray_pose'),(361,'washer_tray_x_axis'),(256,'washer_tray_z_axis'),(27,'workTable-has-no-objectOnTable'),(156,'workTable-has-objectOnTable-kit'),(265,'workTable-has-objectOnTable-kitTray'),(125,'WorkTable-Kit-SR-UnderWithContact'),(14,'WorkTable-KitTray-SR-UnderWithContact'),(57,'WorkTable-SolidObject-SR-NotUnderWithContact'),(44,'work_table_point'),(331,'work_table_pose'),(349,'work_table_x_axis'),(175,'work_table_z_axis'),(238,'x_axis_kit_GearBox_base'),(285,'x_axis_kit_GearBox_top'),(311,'x_axis_kit_large_gear'),(237,'x_axis_kit_medium_gear'),(402,'x_axis_kit_small_gear'),(329,'z_axis_kit_GearBox_base'),(241,'z_axis_kit_GearBox_top'),(112,'z_axis_kit_large_gear'),(58,'z_axis_kit_medium_gear'),(171,'z_axis_kit_small_gear');
/*!40000 ALTER TABLE `DataThing` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Decrease`
--

DROP TABLE IF EXISTS `Decrease`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Decrease` (
  `DecreaseID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByDecrease_Effect` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`DecreaseID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByDecrease_Effect` (`hadByDecrease_Effect`),
  CONSTRAINT `fkhadByDecrease_Effect` FOREIGN KEY (`hadByDecrease_Effect`) REFERENCES `Effect` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Decrease`
--

LOCK TABLES `Decrease` WRITE;
/*!40000 ALTER TABLE `Decrease` DISABLE KEYS */;
INSERT INTO `Decrease` VALUES (52,'look-for-part-Decrease','look-for-part-effect'),(375,'take-kitTray-Decrease','take-kitTray-effect'),(38,'take-part-Decrease','take-part-effect');
/*!40000 ALTER TABLE `Decrease` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary table structure for view `DirectPose`
--

DROP TABLE IF EXISTS `DirectPose`;
/*!50001 DROP VIEW IF EXISTS `DirectPose`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE TABLE `DirectPose` (
  `name` tinyint NOT NULL,
  `X` tinyint NOT NULL,
  `Y` tinyint NOT NULL,
  `Z` tinyint NOT NULL,
  `VXX` tinyint NOT NULL,
  `VXY` tinyint NOT NULL,
  `VXZ` tinyint NOT NULL,
  `VZX` tinyint NOT NULL,
  `VZY` tinyint NOT NULL,
  `VZZ` tinyint NOT NULL
) ENGINE=MyISAM */;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `Domain`
--

DROP TABLE IF EXISTS `Domain`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Domain` (
  `DomainID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`DomainID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Domain`
--

LOCK TABLES `Domain` WRITE;
/*!40000 ALTER TABLE `Domain` DISABLE KEYS */;
INSERT INTO `Domain` VALUES (218,'kitting-domain');
/*!40000 ALTER TABLE `Domain` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Effect`
--

DROP TABLE IF EXISTS `Effect`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Effect` (
  `EffectID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`EffectID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Effect`
--

LOCK TABLES `Effect` WRITE;
/*!40000 ALTER TABLE `Effect` DISABLE KEYS */;
INSERT INTO `Effect` VALUES (167,'attach-endEffector-effect'),(205,'create-kit-effect'),(274,'detach-endEffector-effect'),(220,'look-for-part-effect'),(139,'move-over-kit-effect'),(197,'move-over-kitTray-effect'),(173,'move-over-part-effect'),(32,'place-kit-effect'),(418,'place-kitTray-effect'),(257,'place-part-effect'),(262,'take-kit-effect'),(400,'take-kitTray-effect'),(118,'take-part-effect');
/*!40000 ALTER TABLE `Effect` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `EndEffector`
--

DROP TABLE IF EXISTS `EndEffector`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EndEffector` (
  `EndEffectorID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasEndEffector_Weight` double NOT NULL,
  `hasEndEffector_Description` varchar(255) NOT NULL,
  `hasEndEffector_MaximumLoadWeight` double NOT NULL,
  `hasEndEffector_HeldObject` varchar(255) DEFAULT NULL,
  `hadByEndEffector_Robot` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`EndEffectorID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasEndEffector_HeldObject` (`hasEndEffector_HeldObject`),
  KEY `fkhadByEndEffector_Robot` (`hadByEndEffector_Robot`),
  CONSTRAINT `fkhadByEndEffector_Robot` FOREIGN KEY (`hadByEndEffector_Robot`) REFERENCES `Robot` (`_NAME`),
  CONSTRAINT `fkhasEndEffector_HeldObject` FOREIGN KEY (`hasEndEffector_HeldObject`) REFERENCES `SolidObject` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `EndEffector`
--

LOCK TABLES `EndEffector` WRITE;
/*!40000 ALTER TABLE `EndEffector` DISABLE KEYS */;
INSERT INTO `EndEffector` VALUES (2,'part_gripper',0.01,'small single cup vacuum effector',0.4,NULL,NULL),(11,'tray_gripper',0.04,'large single cup vacuum effector',0.5,NULL,'robot_1');
/*!40000 ALTER TABLE `EndEffector` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `EndEffectorChangingStation`
--

DROP TABLE IF EXISTS `EndEffectorChangingStation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EndEffectorChangingStation` (
  `EndEffectorChangingStationID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasEndEffectorChangingStation_Base` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`EndEffectorChangingStationID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasEndEffectorChangingStation_Base` (`hasEndEffectorChangingStation_Base`),
  CONSTRAINT `fkhasEndEffectorChangingStation_Base` FOREIGN KEY (`hasEndEffectorChangingStation_Base`) REFERENCES `MechanicalComponent` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `EndEffectorChangingStation`
--

LOCK TABLES `EndEffectorChangingStation` WRITE;
/*!40000 ALTER TABLE `EndEffectorChangingStation` DISABLE KEYS */;
INSERT INTO `EndEffectorChangingStation` VALUES (3,'changing_station_1','changing_station_base');
/*!40000 ALTER TABLE `EndEffectorChangingStation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `EndEffectorHolder`
--

DROP TABLE IF EXISTS `EndEffectorHolder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EndEffectorHolder` (
  `EndEffectorHolderID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByEndEffectorHolder_EndEffectorChangingStation` varchar(255) DEFAULT NULL,
  `hasEndEffectorHolder_EndEffector` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`EndEffectorHolderID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByEndEffectorHolder_EndEffectorChangingStation` (`hadByEndEffectorHolder_EndEffectorChangingStation`),
  KEY `fkhasEndEffectorHolder_EndEffector` (`hasEndEffectorHolder_EndEffector`),
  CONSTRAINT `fkhadByEndEffectorHolder_EndEffectorChangingStation` FOREIGN KEY (`hadByEndEffectorHolder_EndEffectorChangingStation`) REFERENCES `EndEffectorChangingStation` (`_NAME`),
  CONSTRAINT `fkhasEndEffectorHolder_EndEffector` FOREIGN KEY (`hasEndEffectorHolder_EndEffector`) REFERENCES `EndEffector` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `EndEffectorHolder`
--

LOCK TABLES `EndEffectorHolder` WRITE;
/*!40000 ALTER TABLE `EndEffectorHolder` DISABLE KEYS */;
INSERT INTO `EndEffectorHolder` VALUES (1,'tray_gripper_holder','changing_station_1',NULL),(22,'part_gripper_holder','changing_station_1','part_gripper');
/*!40000 ALTER TABLE `EndEffectorHolder` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ExternalShape`
--

DROP TABLE IF EXISTS `ExternalShape`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ExternalShape` (
  `ExternalShapeID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasExternalShape_ModelName` varchar(255) DEFAULT NULL,
  `hasExternalShape_ModelFileName` varchar(255) NOT NULL,
  `hasExternalShape_ModelFormatName` varchar(255) NOT NULL,
  PRIMARY KEY (`ExternalShapeID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ExternalShape`
--

LOCK TABLES `ExternalShape` WRITE;
/*!40000 ALTER TABLE `ExternalShape` DISABLE KEYS */;
INSERT INTO `ExternalShape` VALUES (15,'shape_large_gear_stl',NULL,'\n        stlFiles/large_gear_course_2148A.stl\n      ','STL'),(137,'shape_GearBox_base_stl',NULL,'stlFiles/base_course_4894A.stl','STL'),(153,'shape_medium_gear_stl',NULL,'\n        stlFiles/medium_gear_course_2068A.stl\n      ','STL'),(184,'shape_GearBox_top_stl',NULL,'\n        stlFiles/top_course_3474A.stl\n      ','STL'),(288,'shape_small_gear_stl',NULL,'\n        stlFiles/small_gear_course_1864A.stl\n      ','STL'),(325,'shape_washer_stl',NULL,'\n        stlFiles/washer_course_272A.stl\n      ','STL'),(335,'shape_shaft_stl',NULL,'\n        stlFiles/shaft_course_68A.stl\n      ','STL');
/*!40000 ALTER TABLE `ExternalShape` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Function`
--

DROP TABLE IF EXISTS `Function`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Function` (
  `FunctionID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasFunction_ReferenceParameter` varchar(100) DEFAULT NULL,
  `hasFunction_TargetParameter` varchar(100) DEFAULT NULL,
  `hasFunction_Description` varchar(255) DEFAULT NULL,
  `hadByFunction_Domain` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByFunction_Domain` (`hadByFunction_Domain`),
  CONSTRAINT `fkhadByFunction_Domain` FOREIGN KEY (`hadByFunction_Domain`) REFERENCES `Domain` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Function`
--

LOCK TABLES `Function` WRITE;
/*!40000 ALTER TABLE `Function` DISABLE KEYS */;
INSERT INTO `Function` VALUES (39,'capacity-of-parts-in-kit','StockKeepingUnit','Kit','Number of Parts of a certain StockKeepingUnit that Kit can have','kitting-domain'),(49,'quantity-of-kittrays-in-lbwekt','LargeBoxWithEmptyKitTrays',NULL,'Current quantity of KitTrays in LargeBoxWithEmptyKitTrays','kitting-domain'),(50,'quantity-of-kits-in-lbwk','LargeBoxWithKits',NULL,'Quantity of Kits in LargeBoxWithKits','kitting-domain'),(61,'final-quantity-of-parts-in-kit','Kit',NULL,'Final quantity of Parts in Kit','kitting-domain'),(70,'quantity-of-parts-in-kit','StockKeepingUnit','Kit','Current quantity of Parts with StockKeepingUnit in Kit','kitting-domain'),(87,'quantity-of-parts-in-partstray','PartsTray',NULL,'Current quantity of Parts in PartsTray','kitting-domain'),(287,'capacity-of-kits-in-lbwk','LargeBoxWithKits',NULL,'Number of Kits that LargeBoxWithKits can hold','kitting-domain'),(295,'current-quantity-of-parts-in-kit','Kit',NULL,'Current quantity of Parts in Kit','kitting-domain'),(347,'part-found-flag',NULL,NULL,'Flag that tells if a Part has been found','kitting-domain');
/*!40000 ALTER TABLE `Function` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionCondition`
--

DROP TABLE IF EXISTS `FunctionCondition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionCondition` (
  `FunctionConditionID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`FunctionConditionID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionCondition`
--

LOCK TABLES `FunctionCondition` WRITE;
/*!40000 ALTER TABLE `FunctionCondition` DISABLE KEYS */;
INSERT INTO `FunctionCondition` VALUES (391,'look-for-part-FunctionToNumberGreater'),(411,'place-kit-FunctionToFunctionLess'),(337,'place-part-FunctionToFunctionLess'),(368,'take-kit-FunctionToFunctionEqual'),(109,'take-kitTray-FunctionToNumberGreater'),(313,'take-part-FunctionToNumberGreater');
/*!40000 ALTER TABLE `FunctionCondition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionOperation`
--

DROP TABLE IF EXISTS `FunctionOperation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionOperation` (
  `FunctionOperationID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasFunctionOperation_Value` int(11) NOT NULL,
  `hasFunctionOperation_Function` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionOperationID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasFunctionOperation_Function` (`hasFunctionOperation_Function`),
  CONSTRAINT `fkhasFunctionOperation_Function` FOREIGN KEY (`hasFunctionOperation_Function`) REFERENCES `Function` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionOperation`
--

LOCK TABLES `FunctionOperation` WRITE;
/*!40000 ALTER TABLE `FunctionOperation` DISABLE KEYS */;
INSERT INTO `FunctionOperation` VALUES (38,'take-part-Decrease',1,'quantity-of-parts-in-partstray'),(52,'look-for-part-Decrease',1,'part-found-flag'),(319,'place-part-Increase-3',1,'part-found-flag'),(320,'place-part-Increase-1',1,'current-quantity-of-parts-in-kit'),(321,'place-part-Increase-2',1,'quantity-of-parts-in-kit'),(375,'take-kitTray-Decrease',1,'quantity-of-kittrays-in-lbwekt'),(385,'place-kit-Increase',1,'quantity-of-kits-in-lbwk');
/*!40000 ALTER TABLE `FunctionOperation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionToFunctionCondition`
--

DROP TABLE IF EXISTS `FunctionToFunctionCondition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionToFunctionCondition` (
  `FunctionToFunctionConditionID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasFunctionToFunctionCondition_F2` varchar(255) DEFAULT NULL,
  `hasFunctionToFunctionCondition_F1` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionToFunctionConditionID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasFunctionToFunctionCondition_F2` (`hasFunctionToFunctionCondition_F2`),
  KEY `fkhasFunctionToFunctionCondition_F1` (`hasFunctionToFunctionCondition_F1`),
  CONSTRAINT `fkhasFunctionToFunctionCondition_F1` FOREIGN KEY (`hasFunctionToFunctionCondition_F1`) REFERENCES `Function` (`_NAME`),
  CONSTRAINT `fkhasFunctionToFunctionCondition_F2` FOREIGN KEY (`hasFunctionToFunctionCondition_F2`) REFERENCES `Function` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionToFunctionCondition`
--

LOCK TABLES `FunctionToFunctionCondition` WRITE;
/*!40000 ALTER TABLE `FunctionToFunctionCondition` DISABLE KEYS */;
INSERT INTO `FunctionToFunctionCondition` VALUES (337,'place-part-FunctionToFunctionLess','capacity-of-parts-in-kit','quantity-of-parts-in-kit'),(368,'take-kit-FunctionToFunctionEqual','final-quantity-of-parts-in-kit','current-quantity-of-parts-in-kit'),(411,'place-kit-FunctionToFunctionLess','capacity-of-kits-in-lbwk','quantity-of-kits-in-lbwk');
/*!40000 ALTER TABLE `FunctionToFunctionCondition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionToFunctionEqual`
--

DROP TABLE IF EXISTS `FunctionToFunctionEqual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionToFunctionEqual` (
  `FunctionToFunctionEqualID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByFunctionToFunctionEqual_Precondition` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionToFunctionEqualID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByFunctionToFunctionEqual_Precondition` (`hadByFunctionToFunctionEqual_Precondition`),
  CONSTRAINT `fkhadByFunctionToFunctionEqual_Precondition` FOREIGN KEY (`hadByFunctionToFunctionEqual_Precondition`) REFERENCES `Precondition` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionToFunctionEqual`
--

LOCK TABLES `FunctionToFunctionEqual` WRITE;
/*!40000 ALTER TABLE `FunctionToFunctionEqual` DISABLE KEYS */;
INSERT INTO `FunctionToFunctionEqual` VALUES (368,'take-kit-FunctionToFunctionEqual','take-kit-precondition');
/*!40000 ALTER TABLE `FunctionToFunctionEqual` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionToFunctionGreater`
--

DROP TABLE IF EXISTS `FunctionToFunctionGreater`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionToFunctionGreater` (
  `FunctionToFunctionGreaterID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByFunctionToFunctionGreater_Precondition` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionToFunctionGreaterID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByFunctionToFunctionGreater_Precondition` (`hadByFunctionToFunctionGreater_Precondition`),
  CONSTRAINT `fkhadByFunctionToFunctionGreater_Precondition` FOREIGN KEY (`hadByFunctionToFunctionGreater_Precondition`) REFERENCES `Precondition` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionToFunctionGreater`
--

LOCK TABLES `FunctionToFunctionGreater` WRITE;
/*!40000 ALTER TABLE `FunctionToFunctionGreater` DISABLE KEYS */;
/*!40000 ALTER TABLE `FunctionToFunctionGreater` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionToFunctionGreaterOrEqual`
--

DROP TABLE IF EXISTS `FunctionToFunctionGreaterOrEqual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionToFunctionGreaterOrEqual` (
  `FunctionToFunctionGreaterOrEqualID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByFunctionToFunctionGreaterOrEqual_Precondition` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionToFunctionGreaterOrEqualID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByFunctionToFunctionGreaterOrEqual_Precondition` (`hadByFunctionToFunctionGreaterOrEqual_Precondition`),
  CONSTRAINT `fkhadByFunctionToFunctionGreaterOrEqual_Precondition` FOREIGN KEY (`hadByFunctionToFunctionGreaterOrEqual_Precondition`) REFERENCES `Precondition` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionToFunctionGreaterOrEqual`
--

LOCK TABLES `FunctionToFunctionGreaterOrEqual` WRITE;
/*!40000 ALTER TABLE `FunctionToFunctionGreaterOrEqual` DISABLE KEYS */;
/*!40000 ALTER TABLE `FunctionToFunctionGreaterOrEqual` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionToFunctionLess`
--

DROP TABLE IF EXISTS `FunctionToFunctionLess`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionToFunctionLess` (
  `FunctionToFunctionLessID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByFunctionToFunctionLess_Precondition` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionToFunctionLessID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByFunctionToFunctionLess_Precondition` (`hadByFunctionToFunctionLess_Precondition`),
  CONSTRAINT `fkhadByFunctionToFunctionLess_Precondition` FOREIGN KEY (`hadByFunctionToFunctionLess_Precondition`) REFERENCES `Precondition` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionToFunctionLess`
--

LOCK TABLES `FunctionToFunctionLess` WRITE;
/*!40000 ALTER TABLE `FunctionToFunctionLess` DISABLE KEYS */;
INSERT INTO `FunctionToFunctionLess` VALUES (411,'place-kit-FunctionToFunctionLess','place-kit-precondition'),(337,'place-part-FunctionToFunctionLess','place-part-precondition');
/*!40000 ALTER TABLE `FunctionToFunctionLess` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionToFunctionLessOrEqual`
--

DROP TABLE IF EXISTS `FunctionToFunctionLessOrEqual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionToFunctionLessOrEqual` (
  `FunctionToFunctionLessOrEqualID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByFunctionToFunctionLessOrEqual_Precondition` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionToFunctionLessOrEqualID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByFunctionToFunctionLessOrEqual_Precondition` (`hadByFunctionToFunctionLessOrEqual_Precondition`),
  CONSTRAINT `fkhadByFunctionToFunctionLessOrEqual_Precondition` FOREIGN KEY (`hadByFunctionToFunctionLessOrEqual_Precondition`) REFERENCES `Precondition` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionToFunctionLessOrEqual`
--

LOCK TABLES `FunctionToFunctionLessOrEqual` WRITE;
/*!40000 ALTER TABLE `FunctionToFunctionLessOrEqual` DISABLE KEYS */;
/*!40000 ALTER TABLE `FunctionToFunctionLessOrEqual` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionToNumberCondition`
--

DROP TABLE IF EXISTS `FunctionToNumberCondition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionToNumberCondition` (
  `FunctionToNumberConditionID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasFunctionToNumberCondition_Number` double NOT NULL,
  `hasFunctionToNumberCondition_Function` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionToNumberConditionID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasFunctionToNumberCondition_Function` (`hasFunctionToNumberCondition_Function`),
  CONSTRAINT `fkhasFunctionToNumberCondition_Function` FOREIGN KEY (`hasFunctionToNumberCondition_Function`) REFERENCES `Function` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionToNumberCondition`
--

LOCK TABLES `FunctionToNumberCondition` WRITE;
/*!40000 ALTER TABLE `FunctionToNumberCondition` DISABLE KEYS */;
INSERT INTO `FunctionToNumberCondition` VALUES (109,'take-kitTray-FunctionToNumberGreater',0,'quantity-of-kittrays-in-lbwekt'),(313,'take-part-FunctionToNumberGreater',0,'quantity-of-parts-in-partstray'),(391,'look-for-part-FunctionToNumberGreater',0,'part-found-flag');
/*!40000 ALTER TABLE `FunctionToNumberCondition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionToNumberEqual`
--

DROP TABLE IF EXISTS `FunctionToNumberEqual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionToNumberEqual` (
  `FunctionToNumberEqualID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByFunctionToNumberEqual_Precondition` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionToNumberEqualID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByFunctionToNumberEqual_Precondition` (`hadByFunctionToNumberEqual_Precondition`),
  CONSTRAINT `fkhadByFunctionToNumberEqual_Precondition` FOREIGN KEY (`hadByFunctionToNumberEqual_Precondition`) REFERENCES `Precondition` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionToNumberEqual`
--

LOCK TABLES `FunctionToNumberEqual` WRITE;
/*!40000 ALTER TABLE `FunctionToNumberEqual` DISABLE KEYS */;
/*!40000 ALTER TABLE `FunctionToNumberEqual` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionToNumberGreater`
--

DROP TABLE IF EXISTS `FunctionToNumberGreater`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionToNumberGreater` (
  `FunctionToNumberGreaterID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByFunctionToNumberGreater_Precondition` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionToNumberGreaterID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByFunctionToNumberGreater_Precondition` (`hadByFunctionToNumberGreater_Precondition`),
  CONSTRAINT `fkhadByFunctionToNumberGreater_Precondition` FOREIGN KEY (`hadByFunctionToNumberGreater_Precondition`) REFERENCES `Precondition` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionToNumberGreater`
--

LOCK TABLES `FunctionToNumberGreater` WRITE;
/*!40000 ALTER TABLE `FunctionToNumberGreater` DISABLE KEYS */;
INSERT INTO `FunctionToNumberGreater` VALUES (391,'look-for-part-FunctionToNumberGreater','look-for-part-precondition'),(109,'take-kitTray-FunctionToNumberGreater','take-kitTray-precondition'),(313,'take-part-FunctionToNumberGreater','take-part-precondition');
/*!40000 ALTER TABLE `FunctionToNumberGreater` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionToNumberGreaterOrEqual`
--

DROP TABLE IF EXISTS `FunctionToNumberGreaterOrEqual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionToNumberGreaterOrEqual` (
  `FunctionToNumberGreaterOrEqualID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByFunctionToNumberGreaterOrEqual_Precondition` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionToNumberGreaterOrEqualID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByFunctionToNumberGreaterOrEqual_Precondition` (`hadByFunctionToNumberGreaterOrEqual_Precondition`),
  CONSTRAINT `fkhadByFunctionToNumberGreaterOrEqual_Precondition` FOREIGN KEY (`hadByFunctionToNumberGreaterOrEqual_Precondition`) REFERENCES `Precondition` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionToNumberGreaterOrEqual`
--

LOCK TABLES `FunctionToNumberGreaterOrEqual` WRITE;
/*!40000 ALTER TABLE `FunctionToNumberGreaterOrEqual` DISABLE KEYS */;
/*!40000 ALTER TABLE `FunctionToNumberGreaterOrEqual` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionToNumberLess`
--

DROP TABLE IF EXISTS `FunctionToNumberLess`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionToNumberLess` (
  `FunctionToNumberLessID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByFunctionToNumberLess_Precondition` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionToNumberLessID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByFunctionToNumberLess_Precondition` (`hadByFunctionToNumberLess_Precondition`),
  CONSTRAINT `fkhadByFunctionToNumberLess_Precondition` FOREIGN KEY (`hadByFunctionToNumberLess_Precondition`) REFERENCES `Precondition` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionToNumberLess`
--

LOCK TABLES `FunctionToNumberLess` WRITE;
/*!40000 ALTER TABLE `FunctionToNumberLess` DISABLE KEYS */;
/*!40000 ALTER TABLE `FunctionToNumberLess` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `FunctionToNumberLessOrEqual`
--

DROP TABLE IF EXISTS `FunctionToNumberLessOrEqual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionToNumberLessOrEqual` (
  `FunctionToNumberLessOrEqualID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByFunctionToNumberLessOrEqual_Precondition` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FunctionToNumberLessOrEqualID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByFunctionToNumberLessOrEqual_Precondition` (`hadByFunctionToNumberLessOrEqual_Precondition`),
  CONSTRAINT `fkhadByFunctionToNumberLessOrEqual_Precondition` FOREIGN KEY (`hadByFunctionToNumberLessOrEqual_Precondition`) REFERENCES `Precondition` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FunctionToNumberLessOrEqual`
--

LOCK TABLES `FunctionToNumberLessOrEqual` WRITE;
/*!40000 ALTER TABLE `FunctionToNumberLessOrEqual` DISABLE KEYS */;
/*!40000 ALTER TABLE `FunctionToNumberLessOrEqual` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `GripperEffector`
--

DROP TABLE IF EXISTS `GripperEffector`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GripperEffector` (
  `GripperEffectorID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`GripperEffectorID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `GripperEffector`
--

LOCK TABLES `GripperEffector` WRITE;
/*!40000 ALTER TABLE `GripperEffector` DISABLE KEYS */;
/*!40000 ALTER TABLE `GripperEffector` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Human`
--

DROP TABLE IF EXISTS `Human`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Human` (
  `HumanID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`HumanID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Human`
--

LOCK TABLES `Human` WRITE;
/*!40000 ALTER TABLE `Human` DISABLE KEYS */;
/*!40000 ALTER TABLE `Human` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Increase`
--

DROP TABLE IF EXISTS `Increase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Increase` (
  `IncreaseID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByIncrease_Effect` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`IncreaseID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByIncrease_Effect` (`hadByIncrease_Effect`),
  CONSTRAINT `fkhadByIncrease_Effect` FOREIGN KEY (`hadByIncrease_Effect`) REFERENCES `Effect` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Increase`
--

LOCK TABLES `Increase` WRITE;
/*!40000 ALTER TABLE `Increase` DISABLE KEYS */;
INSERT INTO `Increase` VALUES (385,'place-kit-Increase','place-kit-effect'),(319,'place-part-Increase-3','place-part-effect'),(320,'place-part-Increase-1','place-part-effect'),(321,'place-part-Increase-2','place-part-effect');
/*!40000 ALTER TABLE `Increase` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `IntermediateStateRelation`
--

DROP TABLE IF EXISTS `IntermediateStateRelation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `IntermediateStateRelation` (
  `IntermediateStateRelationID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasIntermediateStateRelation_RCC8StateRelation` varchar(255) DEFAULT NULL,
  `hadByIntermediateStateRelation_SOAP` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`IntermediateStateRelationID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasIntermediateStateRelation_RCC8StateRelation` (`hasIntermediateStateRelation_RCC8StateRelation`),
  KEY `fkhadByIntermediateStateRelation_SOAP` (`hadByIntermediateStateRelation_SOAP`),
  CONSTRAINT `fkhadByIntermediateStateRelation_SOAP` FOREIGN KEY (`hadByIntermediateStateRelation_SOAP`) REFERENCES `SOAP` (`_NAME`),
  CONSTRAINT `fkhasIntermediateStateRelation_RCC8StateRelation` FOREIGN KEY (`hasIntermediateStateRelation_RCC8StateRelation`) REFERENCES `RCC8StateRelation` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `IntermediateStateRelation`
--

LOCK TABLES `IntermediateStateRelation` WRITE;
/*!40000 ALTER TABLE `IntermediateStateRelation` DISABLE KEYS */;
INSERT INTO `IntermediateStateRelation` VALUES (8,'PartiallyIn','RCC8-PartiallyIn','soap'),(48,'Under','RCC8-Under','soap'),(84,'NotContainedIn','RCC8-NotContainedIn','soap'),(94,'NotUnderWithContact','RCC8-NotUnderWithContact','soap'),(114,'NotOnTopWithContact','RCC8-NotOnTopWithContact','soap'),(188,'PartiallyInAndInContactWith','RCC8-PartiallyInAndInContactWith','soap'),(225,'UnderWithContact','RCC8-UnderWithContact','soap'),(243,'OnTopWithContact','RCC8-OnTopWithContact','soap'),(363,'OnTopOf','RCC8-OnTopOf','soap'),(377,'ContainedInLBWK','RCC8-ContainedInLBWK','soap'),(386,'NotInContactWith','RCC8-NotInContactWith','soap'),(410,'InContactWith','RCC8-InContactWith','soap'),(417,'ContainedIn','RCC8-ContainedIn','soap');
/*!40000 ALTER TABLE `IntermediateStateRelation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `InternalShape`
--

DROP TABLE IF EXISTS `InternalShape`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `InternalShape` (
  `InternalShapeID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`InternalShapeID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `InternalShape`
--

LOCK TABLES `InternalShape` WRITE;
/*!40000 ALTER TABLE `InternalShape` DISABLE KEYS */;
INSERT INTO `InternalShape` VALUES (328,'shape_changing_station_base'),(269,'shape_GearBox_base'),(244,'shape_GearBox_kit_tray'),(227,'shape_GearBox_top'),(233,'shape_gear_tray'),(303,'shape_gripper_holder_1'),(304,'shape_gripper_holder_2'),(134,'shape_kit_box'),(249,'shape_large_gear'),(69,'shape_medium_gear'),(390,'shape_shaft'),(91,'shape_small_gear'),(286,'shape_topBase_tray'),(308,'shape_washer'),(155,'shape_washerShaft_tray'),(336,'shape_work_table');
/*!40000 ALTER TABLE `InternalShape` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Kit`
--

DROP TABLE IF EXISTS `Kit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Kit` (
  `KitID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasKit_Finished` tinyint(1) NOT NULL,
  `hadByKit_LargeBoxWithKits` varchar(255) DEFAULT NULL,
  `hasKit_Design` varchar(255) DEFAULT NULL,
  `hasKit_KitTray` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`KitID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByKit_LargeBoxWithKits` (`hadByKit_LargeBoxWithKits`),
  KEY `fkhasKit_Design` (`hasKit_Design`),
  KEY `fkhasKit_KitTray` (`hasKit_KitTray`),
  CONSTRAINT `fkhadByKit_LargeBoxWithKits` FOREIGN KEY (`hadByKit_LargeBoxWithKits`) REFERENCES `LargeBoxWithKits` (`_NAME`),
  CONSTRAINT `fkhasKit_Design` FOREIGN KEY (`hasKit_Design`) REFERENCES `KitDesign` (`_NAME`),
  CONSTRAINT `fkhasKit_KitTray` FOREIGN KEY (`hasKit_KitTray`) REFERENCES `KitTray` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Kit`
--

LOCK TABLES `Kit` WRITE;
/*!40000 ALTER TABLE `Kit` DISABLE KEYS */;
INSERT INTO `Kit` VALUES (9,'kit_gearbox',1,'finished_kit_receiver','kit_design_GearBox','kit_tray_1');
/*!40000 ALTER TABLE `Kit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `KitDesign`
--

DROP TABLE IF EXISTS `KitDesign`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `KitDesign` (
  `KitDesignID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByKitDesign_KittingWorkstation` varchar(255) DEFAULT NULL,
  `hasKitDesign_KitTraySku` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`KitDesignID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByKitDesign_KittingWorkstation` (`hadByKitDesign_KittingWorkstation`),
  KEY `fkhasKitDesign_KitTraySku` (`hasKitDesign_KitTraySku`),
  CONSTRAINT `fkhadByKitDesign_KittingWorkstation` FOREIGN KEY (`hadByKitDesign_KittingWorkstation`) REFERENCES `KittingWorkstation` (`_NAME`),
  CONSTRAINT `fkhasKitDesign_KitTraySku` FOREIGN KEY (`hasKitDesign_KitTraySku`) REFERENCES `StockKeepingUnit` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `KitDesign`
--

LOCK TABLES `KitDesign` WRITE;
/*!40000 ALTER TABLE `KitDesign` DISABLE KEYS */;
INSERT INTO `KitDesign` VALUES (46,'kit_design_GearBox','kitting_workstation_1','stock_keeping_unit_GearBox_kit_tray');
/*!40000 ALTER TABLE `KitDesign` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `KitTray`
--

DROP TABLE IF EXISTS `KitTray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `KitTray` (
  `KitTrayID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasKitTray_SerialNumber` varchar(100) NOT NULL,
  `hadByKitTray_LargeBoxWithEmptyKitTrays` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`KitTrayID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByKitTray_LargeBoxWithEmptyKitTrays` (`hadByKitTray_LargeBoxWithEmptyKitTrays`),
  CONSTRAINT `fkhadByKitTray_LargeBoxWithEmptyKitTrays` FOREIGN KEY (`hadByKitTray_LargeBoxWithEmptyKitTrays`) REFERENCES `LargeBoxWithEmptyKitTrays` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `KitTray`
--

LOCK TABLES `KitTray` WRITE;
/*!40000 ALTER TABLE `KitTray` DISABLE KEYS */;
INSERT INTO `KitTray` VALUES (18,'kit_tray_1','1',NULL);
/*!40000 ALTER TABLE `KitTray` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `KittingWorkstation`
--

DROP TABLE IF EXISTS `KittingWorkstation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `KittingWorkstation` (
  `KittingWorkstationID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasKittingWorkstation_LengthUnit` varchar(20) NOT NULL,
  `hasKittingWorkstation_WeightUnit` varchar(20) NOT NULL,
  `hasKittingWorkstation_AngleUnit` varchar(20) NOT NULL,
  `hasKittingWorkstation_ChangingStation` varchar(255) DEFAULT NULL,
  `hasKittingWorkstation_Robot` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`KittingWorkstationID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasKittingWorkstation_ChangingStation` (`hasKittingWorkstation_ChangingStation`),
  KEY `fkhasKittingWorkstation_Robot` (`hasKittingWorkstation_Robot`),
  CONSTRAINT `fkhasKittingWorkstation_ChangingStation` FOREIGN KEY (`hasKittingWorkstation_ChangingStation`) REFERENCES `EndEffectorChangingStation` (`_NAME`),
  CONSTRAINT `fkhasKittingWorkstation_Robot` FOREIGN KEY (`hasKittingWorkstation_Robot`) REFERENCES `Robot` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `KittingWorkstation`
--

LOCK TABLES `KittingWorkstation` WRITE;
/*!40000 ALTER TABLE `KittingWorkstation` DISABLE KEYS */;
INSERT INTO `KittingWorkstation` VALUES (21,'kitting_workstation_1','millimeter','kilogram','degree','changing_station_1','robot_1');
/*!40000 ALTER TABLE `KittingWorkstation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `LargeBoxWithEmptyKitTrays`
--

DROP TABLE IF EXISTS `LargeBoxWithEmptyKitTrays`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LargeBoxWithEmptyKitTrays` (
  `LargeBoxWithEmptyKitTraysID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasLargeBoxWithEmptyKitTrays_LargeContainer` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`LargeBoxWithEmptyKitTraysID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasLargeBoxWithEmptyKitTrays_LargeContainer` (`hasLargeBoxWithEmptyKitTrays_LargeContainer`),
  CONSTRAINT `fkhasLargeBoxWithEmptyKitTrays_LargeContainer` FOREIGN KEY (`hasLargeBoxWithEmptyKitTrays_LargeContainer`) REFERENCES `LargeContainer` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `LargeBoxWithEmptyKitTrays`
--

LOCK TABLES `LargeBoxWithEmptyKitTrays` WRITE;
/*!40000 ALTER TABLE `LargeBoxWithEmptyKitTrays` DISABLE KEYS */;
INSERT INTO `LargeBoxWithEmptyKitTrays` VALUES (19,'empty_kit_tray_supply','empty_kit_tray_box');
/*!40000 ALTER TABLE `LargeBoxWithEmptyKitTrays` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `LargeBoxWithKits`
--

DROP TABLE IF EXISTS `LargeBoxWithKits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LargeBoxWithKits` (
  `LargeBoxWithKitsID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasLargeBoxWithKits_Capacity` int(11) NOT NULL,
  `hasLargeBoxWithKits_LargeContainer` varchar(255) DEFAULT NULL,
  `hasLargeBoxWithKits_KitDesign` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`LargeBoxWithKitsID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasLargeBoxWithKits_LargeContainer` (`hasLargeBoxWithKits_LargeContainer`),
  KEY `fkhasLargeBoxWithKits_KitDesign` (`hasLargeBoxWithKits_KitDesign`),
  CONSTRAINT `fkhasLargeBoxWithKits_KitDesign` FOREIGN KEY (`hasLargeBoxWithKits_KitDesign`) REFERENCES `KitDesign` (`_NAME`),
  CONSTRAINT `fkhasLargeBoxWithKits_LargeContainer` FOREIGN KEY (`hasLargeBoxWithKits_LargeContainer`) REFERENCES `LargeContainer` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `LargeBoxWithKits`
--

LOCK TABLES `LargeBoxWithKits` WRITE;
/*!40000 ALTER TABLE `LargeBoxWithKits` DISABLE KEYS */;
INSERT INTO `LargeBoxWithKits` VALUES (26,'finished_kit_receiver',12,'finished_kit_box','kit_design_GearBox');
/*!40000 ALTER TABLE `LargeBoxWithKits` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `LargeContainer`
--

DROP TABLE IF EXISTS `LargeContainer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LargeContainer` (
  `LargeContainerID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasLargeContainer_SerialNumber` varchar(100) NOT NULL,
  PRIMARY KEY (`LargeContainerID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `LargeContainer`
--

LOCK TABLES `LargeContainer` WRITE;
/*!40000 ALTER TABLE `LargeContainer` DISABLE KEYS */;
INSERT INTO `LargeContainer` VALUES (24,'empty_kit_tray_box','4'),(25,'finished_kit_box','5');
/*!40000 ALTER TABLE `LargeContainer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `MechanicalComponent`
--

DROP TABLE IF EXISTS `MechanicalComponent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `MechanicalComponent` (
  `MechanicalComponentID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`MechanicalComponentID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `MechanicalComponent`
--

LOCK TABLES `MechanicalComponent` WRITE;
/*!40000 ALTER TABLE `MechanicalComponent` DISABLE KEYS */;
INSERT INTO `MechanicalComponent` VALUES (4,'changing_station_base');
/*!40000 ALTER TABLE `MechanicalComponent` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `NegativePredicate`
--

DROP TABLE IF EXISTS `NegativePredicate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `NegativePredicate` (
  `NegativePredicateID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadByNegativePredicate_Effect` varchar(255) DEFAULT NULL,
  `hasNegativePredicate_PositivePredicate` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`NegativePredicateID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByNegativePredicate_Effect` (`hadByNegativePredicate_Effect`),
  KEY `fkhasNegativePredicate_PositivePredicate` (`hasNegativePredicate_PositivePredicate`),
  CONSTRAINT `fkhadByNegativePredicate_Effect` FOREIGN KEY (`hadByNegativePredicate_Effect`) REFERENCES `Effect` (`_NAME`),
  CONSTRAINT `fkhasNegativePredicate_PositivePredicate` FOREIGN KEY (`hasNegativePredicate_PositivePredicate`) REFERENCES `PositivePredicate` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `NegativePredicate`
--

LOCK TABLES `NegativePredicate` WRITE;
/*!40000 ALTER TABLE `NegativePredicate` DISABLE KEYS */;
INSERT INTO `NegativePredicate` VALUES (1,'detach-endEffector-effect-np-1','detach-endEffector-effect','robot-has-endEffector'),(2,'detach-endEffector-effect-np-2','detach-endEffector-effect','endEffector-has-physicalLocation-refObject-robot'),(53,'place-kitTray-not-2','place-kitTray-effect','endEffector-has-heldObject-kitTray'),(56,'place-kitTray-not-1','place-kitTray-effect','kitTray-has-physicalLocation-refObject-endEffector'),(80,'place-part-not-2','place-part-effect','part-has-physicalLocation-refObject-endEffector'),(81,'place-part-not-1','place-part-effect','endEffector-has-heldObject-part'),(135,'create-kit-not-1','create-kit-effect','workTable-has-objectOnTable-kitTray'),(136,'create-kit-not-2','create-kit-effect','kitTray-has-physicalLocation-refObject-workTable'),(142,'take-part-effect-not-1','take-part-effect','endEffector-has-no-heldObject'),(143,'take-part-effect-not-2','take-part-effect','part-has-physicalLocation-refObject-partsTray'),(144,'take-part-effect-not-3','take-part-effect','endEffector-is-over-part'),(165,'attach-endEffector-not-1','attach-endEffector-effect','endEffector-has-physicalLocation-refObject-endEffectorHolder'),(168,'attach-endEffector-not-3','attach-endEffector-effect','robot-has-no-endEffector'),(170,'attach-endEffector-not-2','attach-endEffector-effect','endEffectorHolder-has-endEffector'),(290,'place-kit-not-3','place-kit-effect','kit-has-physicalLocation-refObject-endEffector'),(291,'place-kit-not-2','place-kit-effect','kit-exists'),(292,'place-kit-not-1','place-kit-effect','endEffector-has-heldObject-kit'),(357,'take-kit-not-1','take-kit-effect','workTable-has-objectOnTable-kit'),(358,'take-kit-not-2','take-kit-effect','kit-has-physicalLocation-refObject-workTable'),(359,'take-kit-not-3','take-kit-effect','endEffector-has-no-heldObject'),(360,'take-kit-not-4','take-kit-effect','endEffector-is-over-kit'),(378,'take-kitTray-not-3','take-kitTray-effect','endEffector-is-over-kitTray'),(381,'take-kitTray-not-1','take-kitTray-effect','kitTray-has-physicalLocation-refObject-lbwekt'),(383,'take-kitTray-not-2','take-kitTray-effect','endEffector-has-no-heldObject');
/*!40000 ALTER TABLE `NegativePredicate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `NoSkuObject`
--

DROP TABLE IF EXISTS `NoSkuObject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `NoSkuObject` (
  `NoSkuObjectID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasNoSkuObject_ExternalShape` varchar(255) DEFAULT NULL,
  `hasNoSkuObject_InternalShape` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`NoSkuObjectID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasNoSkuObject_ExternalShape` (`hasNoSkuObject_ExternalShape`),
  KEY `fkhasNoSkuObject_InternalShape` (`hasNoSkuObject_InternalShape`),
  CONSTRAINT `fkhasNoSkuObject_ExternalShape` FOREIGN KEY (`hasNoSkuObject_ExternalShape`) REFERENCES `ExternalShape` (`_NAME`),
  CONSTRAINT `fkhasNoSkuObject_InternalShape` FOREIGN KEY (`hasNoSkuObject_InternalShape`) REFERENCES `InternalShape` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `NoSkuObject`
--

LOCK TABLES `NoSkuObject` WRITE;
/*!40000 ALTER TABLE `NoSkuObject` DISABLE KEYS */;
INSERT INTO `NoSkuObject` VALUES (1,'tray_gripper_holder',NULL,'shape_gripper_holder_2'),(2,'part_gripper',NULL,NULL),(3,'changing_station_1',NULL,NULL),(4,'changing_station_base',NULL,'shape_changing_station_base'),(9,'kit_gearbox',NULL,NULL),(11,'tray_gripper',NULL,NULL),(14,'robot_1',NULL,NULL),(19,'empty_kit_tray_supply',NULL,NULL),(21,'kitting_workstation_1',NULL,NULL),(22,'part_gripper_holder',NULL,'shape_gripper_holder_1'),(23,'work_table_1',NULL,'shape_work_table'),(26,'finished_kit_receiver',NULL,NULL);
/*!40000 ALTER TABLE `NoSkuObject` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Part`
--

DROP TABLE IF EXISTS `Part`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Part` (
  `PartID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasPart_SerialNumber` varchar(100) NOT NULL,
  `hadByPart_Kit` varchar(255) DEFAULT NULL,
  `hadByPart_PartsVessel` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PartID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadByPart_Kit` (`hadByPart_Kit`),
  KEY `fkhadByPart_PartsVessel` (`hadByPart_PartsVessel`),
  CONSTRAINT `fkhadByPart_Kit` FOREIGN KEY (`hadByPart_Kit`) REFERENCES `Kit` (`_NAME`),
  CONSTRAINT `fkhadByPart_PartsVessel` FOREIGN KEY (`hadByPart_PartsVessel`) REFERENCES `PartsVessel` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Part`
--

LOCK TABLES `Part` WRITE;
/*!40000 ALTER TABLE `Part` DISABLE KEYS */;
INSERT INTO `Part` VALUES (5,'GearBox_top_1','1','kit_gearbox',NULL),(7,'GearBox_base_1','1','kit_gearbox',NULL),(8,'large_gear_1','1','kit_gearbox',NULL),(13,'medium_gear_1','1','kit_gearbox',NULL),(16,'small_gear_1','1','kit_gearbox',NULL);
/*!40000 ALTER TABLE `Part` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PartRefAndPose`
--

DROP TABLE IF EXISTS `PartRefAndPose`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PartRefAndPose` (
  `PartRefAndPoseID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasPartRefAndPose_XAxis` varchar(255) DEFAULT NULL,
  `hasPartRefAndPose_ZAxis` varchar(255) DEFAULT NULL,
  `hasPartRefAndPose_Sku` varchar(255) DEFAULT NULL,
  `hadByPartRefAndPose_KitDesign` varchar(255) DEFAULT NULL,
  `hasPartRefAndPose_Point` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PartRefAndPoseID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasPartRefAndPose_XAxis` (`hasPartRefAndPose_XAxis`),
  KEY `fkhasPartRefAndPose_ZAxis` (`hasPartRefAndPose_ZAxis`),
  KEY `fkhasPartRefAndPose_Sku` (`hasPartRefAndPose_Sku`),
  KEY `fkhadByPartRefAndPose_KitDesign` (`hadByPartRefAndPose_KitDesign`),
  KEY `fkhasPartRefAndPose_Point` (`hasPartRefAndPose_Point`),
  CONSTRAINT `fkhadByPartRefAndPose_KitDesign` FOREIGN KEY (`hadByPartRefAndPose_KitDesign`) REFERENCES `KitDesign` (`_NAME`),
  CONSTRAINT `fkhasPartRefAndPose_Point` FOREIGN KEY (`hasPartRefAndPose_Point`) REFERENCES `Point` (`_NAME`),
  CONSTRAINT `fkhasPartRefAndPose_Sku` FOREIGN KEY (`hasPartRefAndPose_Sku`) REFERENCES `StockKeepingUnit` (`_NAME`),
  CONSTRAINT `fkhasPartRefAndPose_XAxis` FOREIGN KEY (`hasPartRefAndPose_XAxis`) REFERENCES `Vector` (`_NAME`),
  CONSTRAINT `fkhasPartRefAndPose_ZAxis` FOREIGN KEY (`hasPartRefAndPose_ZAxis`) REFERENCES `Vector` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PartRefAndPose`
--

LOCK TABLES `PartRefAndPose` WRITE;
/*!40000 ALTER TABLE `PartRefAndPose` DISABLE KEYS */;
INSERT INTO `PartRefAndPose` VALUES (113,'part_ref_and_pose_kit_medium_gear','x_axis_kit_medium_gear','z_axis_kit_medium_gear','stock_keeping_unit_medium_gear','kit_design_GearBox','point_kit_medium_gear'),(169,'part_ref_and_pose_kit_small_gear','x_axis_kit_small_gear','z_axis_kit_small_gear','stock_keeping_unit_small_gear','kit_design_GearBox','point_kit_small_gear'),(297,'part_ref_and_pose_kit_GearBox_top','x_axis_kit_GearBox_top','z_axis_kit_GearBox_top','stock_keeping_unit_GearBox_top','kit_design_GearBox','point_kit_GearBox_top'),(332,'part_ref_and_pose_kit_large_gear','x_axis_kit_large_gear','z_axis_kit_large_gear','stock_keeping_unit_large_gear','kit_design_GearBox','point_kit_large_gear'),(372,'part_ref_and_pose_kit_GearBox_base','x_axis_kit_GearBox_base','z_axis_kit_GearBox_base','stock_keeping_unit_GearBox_base','kit_design_GearBox','point_kit_GearBox_base');
/*!40000 ALTER TABLE `PartRefAndPose` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PartsBin`
--

DROP TABLE IF EXISTS `PartsBin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PartsBin` (
  `PartsBinID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`PartsBinID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PartsBin`
--

LOCK TABLES `PartsBin` WRITE;
/*!40000 ALTER TABLE `PartsBin` DISABLE KEYS */;
/*!40000 ALTER TABLE `PartsBin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PartsTray`
--

DROP TABLE IF EXISTS `PartsTray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PartsTray` (
  `PartsTrayID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`PartsTrayID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PartsTray`
--

LOCK TABLES `PartsTray` WRITE;
/*!40000 ALTER TABLE `PartsTray` DISABLE KEYS */;
INSERT INTO `PartsTray` VALUES (17,'GearBox_base_tray'),(15,'GearBox_top_tray'),(6,'large_gear_tray'),(20,'medium_gear_tray'),(10,'shaft_tray'),(27,'small_gear_tray'),(12,'washer_tray');
/*!40000 ALTER TABLE `PartsTray` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PartsVessel`
--

DROP TABLE IF EXISTS `PartsVessel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PartsVessel` (
  `PartsVesselID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasPartsVessel_PartQuantity` int(11) NOT NULL,
  `hasPartsVessel_SerialNumber` varchar(100) NOT NULL,
  `hasPartsVessel_PartSku` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PartsVesselID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasPartsVessel_PartSku` (`hasPartsVessel_PartSku`),
  CONSTRAINT `fkhasPartsVessel_PartSku` FOREIGN KEY (`hasPartsVessel_PartSku`) REFERENCES `StockKeepingUnit` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PartsVessel`
--

LOCK TABLES `PartsVessel` WRITE;
/*!40000 ALTER TABLE `PartsVessel` DISABLE KEYS */;
INSERT INTO `PartsVessel` VALUES (6,'large_gear_tray',3,'9','stock_keeping_unit_large_gear'),(10,'shaft_tray',12,'9','stock_keeping_unit_shaft'),(12,'washer_tray',12,'9','stock_keeping_unit_washer'),(15,'GearBox_top_tray',3,'9','stock_keeping_unit_GearBox_top'),(17,'GearBox_base_tray',3,'8','stock_keeping_unit_GearBox_base'),(20,'medium_gear_tray',3,'9','stock_keeping_unit_medium_gear'),(27,'small_gear_tray',3,'9','stock_keeping_unit_small_gear');
/*!40000 ALTER TABLE `PartsVessel` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PhysicalLocation`
--

DROP TABLE IF EXISTS `PhysicalLocation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalLocation` (
  `PhysicalLocationID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasPhysicalLocation_Timestamp` varchar(255) DEFAULT NULL,
  `hadBySecondaryLocation_SolidObject` varchar(255) DEFAULT NULL,
  `hasPhysicalLocation_RefObject` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PhysicalLocationID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadBySecondaryLocation_SolidObject` (`hadBySecondaryLocation_SolidObject`),
  KEY `fkhasPhysicalLocation_RefObject` (`hasPhysicalLocation_RefObject`),
  CONSTRAINT `fkhadBySecondaryLocation_SolidObject` FOREIGN KEY (`hadBySecondaryLocation_SolidObject`) REFERENCES `SolidObject` (`_NAME`),
  CONSTRAINT `fkhasPhysicalLocation_RefObject` FOREIGN KEY (`hasPhysicalLocation_RefObject`) REFERENCES `SolidObject` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PhysicalLocation`
--

LOCK TABLES `PhysicalLocation` WRITE;
/*!40000 ALTER TABLE `PhysicalLocation` DISABLE KEYS */;
INSERT INTO `PhysicalLocation` VALUES (10,'GearBox_top_tray_pose',NULL,NULL,'kitting_workstation_1'),(11,'shaft_tray_pose',NULL,NULL,'kitting_workstation_1'),(18,'GearBox_base_1_pose',NULL,NULL,'kit_gearbox'),(25,'kit_gearbox_pose',NULL,NULL,'finished_kit_receiver'),(33,'robot_pose',NULL,NULL,'kitting_workstation_1'),(45,'tray_gripper_holder_pose',NULL,NULL,'changing_station_1'),(71,'GearBox_base_tray_pose',NULL,NULL,'kitting_workstation_1'),(78,'large_gear_1_pose',NULL,NULL,'kit_gearbox'),(110,'small_gear_tray_pose',NULL,NULL,'kitting_workstation_1'),(159,'kit_tray_1_pose',NULL,NULL,'kit_gearbox'),(166,'washer_tray_pose',NULL,NULL,'kitting_workstation_1'),(174,'changing_station_base_pose',NULL,NULL,'changing_station_1'),(177,'finished_kit_box_pose',NULL,NULL,'finished_kit_receiver'),(191,'large_gear_tray_pose',NULL,NULL,'kitting_workstation_1'),(202,'tray_gripper_pose',NULL,NULL,'robot_1'),(203,'relative_location_in_1',NULL,NULL,'kitting_workstation_1'),(204,'empty_kit_tray_supply_pose',NULL,NULL,'kitting_workstation_1'),(208,'part_gripper_pose',NULL,NULL,'part_gripper_holder'),(209,'medium_gear_tray_pose',NULL,NULL,'kitting_workstation_1'),(217,'changing_station_pose',NULL,NULL,'kitting_workstation_1'),(223,'finished_kit_receiver_pose',NULL,NULL,'kitting_workstation_1'),(305,'small_gear_1_pose',NULL,NULL,'kit_gearbox'),(316,'empty_kit_tray_box_pose',NULL,NULL,'empty_kit_tray_supply'),(331,'work_table_pose',NULL,NULL,'kitting_workstation_1'),(338,'medium_gear_1_pose',NULL,NULL,'kit_gearbox'),(345,'part_gripper_holder_pose',NULL,NULL,'changing_station_1'),(425,'GearBox_top_1_pose',NULL,NULL,'kit_gearbox');
/*!40000 ALTER TABLE `PhysicalLocation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Point`
--

DROP TABLE IF EXISTS `Point`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Point` (
  `PointID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasPoint_Z` double NOT NULL,
  `hasPoint_X` double NOT NULL,
  `hasPoint_Y` double NOT NULL,
  PRIMARY KEY (`PointID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Point`
--

LOCK TABLES `Point` WRITE;
/*!40000 ALTER TABLE `Point` DISABLE KEYS */;
INSERT INTO `Point` VALUES (6,'tray_gripper_holder_point',500,-250,0),(7,'point_kit_small_gear',0.02,-18,85),(16,'point_min',0,-1000,-1000),(22,'washer_tray_point',0,4200,2000),(37,'large_gear_tray_point',0,3600,2000),(44,'work_table_point',0,4000,1000),(55,'robot_point',2000,200,0),(59,'medium_gear_1_point',0,275,-100),(82,'large_gear_1_point',0,354.2056074766355,-223.94080996884736),(98,'tray_gripper_point',0,0,0),(120,'changing_station_base_point',0,0,0),(145,'shaft_tray_point',0,4400,2000),(163,'empty_kit_tray_supply_point',0,1500,1000),(187,'GearBox_top_1_point',0.02,110,0),(189,'part_gripper_holder_point',500,250,0),(213,'part_gripper_point',0,0,0),(214,'finished_kit_box_point',0.02,0,0),(232,'GearBox_base_1_point',0.02,-110,0),(234,'medium_gear_tray_point',0,3800,2000),(267,'empty_kit_tray_box_point',1,0,0),(278,'point_kit_large_gear',0.02,0,-60),(298,'changing_station_point',0,5500,1000),(306,'point_kit_GearBox_base',0.02,-110,0),(314,'kit_gearbox_point',0,0,0),(322,'GearBox_base_tray_point',0,4750,2000),(333,'GearBox_top_tray_point',0,3250,2000),(340,'finished_kit_receiver_point',0,2500,1000),(364,'small_gear_tray_point',0,4000,2000),(384,'kit_tray_1_point',0,0,0),(396,'point_kit_medium_gear',0.02,-10,20),(413,'point_kit_GearBox_top',0.02,110,0),(419,'point_max',2000,8000,3000),(420,'small_gear_1_point',0,275,-75);
/*!40000 ALTER TABLE `Point` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PoseLocation`
--

DROP TABLE IF EXISTS `PoseLocation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PoseLocation` (
  `PoseLocationID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasPoseLocation_OrientationStandardDeviation` double DEFAULT NULL,
  `hasPoseLocation_PositionStandardDeviation` double DEFAULT NULL,
  `hasPoseLocation_ZAxis` varchar(255) DEFAULT NULL,
  `hasPoseLocation_XAxis` varchar(255) DEFAULT NULL,
  `hasPoseLocation_Point` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PoseLocationID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasPoseLocation_ZAxis` (`hasPoseLocation_ZAxis`),
  KEY `fkhasPoseLocation_XAxis` (`hasPoseLocation_XAxis`),
  KEY `fkhasPoseLocation_Point` (`hasPoseLocation_Point`),
  CONSTRAINT `fkhasPoseLocation_Point` FOREIGN KEY (`hasPoseLocation_Point`) REFERENCES `Point` (`_NAME`),
  CONSTRAINT `fkhasPoseLocation_XAxis` FOREIGN KEY (`hasPoseLocation_XAxis`) REFERENCES `Vector` (`_NAME`),
  CONSTRAINT `fkhasPoseLocation_ZAxis` FOREIGN KEY (`hasPoseLocation_ZAxis`) REFERENCES `Vector` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PoseLocation`
--

LOCK TABLES `PoseLocation` WRITE;
/*!40000 ALTER TABLE `PoseLocation` DISABLE KEYS */;
INSERT INTO `PoseLocation` VALUES (10,'GearBox_top_tray_pose',NULL,NULL,'GearBox_top_tray_z_axis','GearBox_top_tray_x_axis','GearBox_top_tray_point'),(11,'shaft_tray_pose',NULL,NULL,'shaft_tray_z_axis','shaft_tray_x_axis','shaft_tray_point'),(18,'GearBox_base_1_pose',NULL,NULL,'GearBox_base_1_z_axis','GearBox_base_1_x_axis','GearBox_base_1_point'),(25,'kit_gearbox_pose',NULL,NULL,'kit_gearbox_z_axis','kit_gearbox_x_axis','kit_gearbox_point'),(33,'robot_pose',NULL,NULL,'robot_z_axis','robot_x_axis','robot_point'),(45,'tray_gripper_holder_pose',NULL,NULL,'tray_gripper_holder_z_axis','tray_gripper_holder_x_axis','tray_gripper_holder_point'),(71,'GearBox_base_tray_pose',NULL,NULL,'GearBox_base_tray_z_axis','GearBox_base_tray_x_axis','GearBox_base_tray_point'),(78,'large_gear_1_pose',NULL,NULL,'large_gear_1_z_axis','large_gear_1_x_axis','large_gear_1_point'),(110,'small_gear_tray_pose',NULL,NULL,'small_tray_z_axis','small_gear_tray_x_axis','small_gear_tray_point'),(159,'kit_tray_1_pose',NULL,NULL,'kit_tray_1_z_axis','kit_tray_1_x_axis','kit_tray_1_point'),(166,'washer_tray_pose',NULL,NULL,'washer_tray_z_axis','washer_tray_x_axis','washer_tray_point'),(174,'changing_station_base_pose',NULL,NULL,'changing_station_base_z_axis','changing_station_base_x_axis','changing_station_base_point'),(177,'finished_kit_box_pose',NULL,NULL,'finished_kit_box_z_axis','finished_kit_box_x_axis','finished_kit_box_point'),(191,'large_gear_tray_pose',NULL,NULL,'large_gear_tray_z_axis','large_gear_tray_x_axis','large_gear_tray_point'),(202,'tray_gripper_pose',NULL,NULL,'tray_gripper_z_axis','tray_gripper_x_axis','tray_gripper_point'),(204,'empty_kit_tray_supply_pose',NULL,NULL,'empty_kit_tray_supply_z_axis','empty_kit_tray_supply_x_axis','empty_kit_tray_supply_point'),(208,'part_gripper_pose',NULL,NULL,'part_gripper_z_axis','part_gripper_x_axis','part_gripper_point'),(209,'medium_gear_tray_pose',NULL,NULL,'medium_tray_z_axis','medium_gear_tray_x_axis','medium_gear_tray_point'),(217,'changing_station_pose',NULL,NULL,'changing_station_z_axis','changing_station_x_axis','changing_station_point'),(223,'finished_kit_receiver_pose',NULL,NULL,'finished_kit_receiver_z_axis','finished_kit_receiver_x_axis','finished_kit_receiver_point'),(305,'small_gear_1_pose',NULL,NULL,'small_1_z_axis','small_gear_1_x_axis','small_gear_1_point'),(316,'empty_kit_tray_box_pose',NULL,NULL,'empty_kit_tray_box_z_axis','empty_kit_tray_box_x_axis','empty_kit_tray_box_point'),(331,'work_table_pose',NULL,NULL,'work_table_z_axis','work_table_x_axis','work_table_point'),(338,'medium_gear_1_pose',NULL,NULL,'medium_1_z_axis','medium_gear_1_x_axis','medium_gear_1_point'),(345,'part_gripper_holder_pose',NULL,NULL,'part_gripper_holder_z_axis','part_gripper_holder_x_axis','part_gripper_holder_point'),(425,'GearBox_top_1_pose',NULL,NULL,'GearBox_top_1_z_axis','GearBox_top_1_x_axis','GearBox_top_1_point');
/*!40000 ALTER TABLE `PoseLocation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PoseLocationIn`
--

DROP TABLE IF EXISTS `PoseLocationIn`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PoseLocationIn` (
  `PoseLocationInID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`PoseLocationInID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PoseLocationIn`
--

LOCK TABLES `PoseLocationIn` WRITE;
/*!40000 ALTER TABLE `PoseLocationIn` DISABLE KEYS */;
INSERT INTO `PoseLocationIn` VALUES (18,'GearBox_base_1_pose'),(425,'GearBox_top_1_pose'),(78,'large_gear_1_pose'),(338,'medium_gear_1_pose'),(305,'small_gear_1_pose');
/*!40000 ALTER TABLE `PoseLocationIn` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PoseLocationOn`
--

DROP TABLE IF EXISTS `PoseLocationOn`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PoseLocationOn` (
  `PoseLocationOnID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`PoseLocationOnID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PoseLocationOn`
--

LOCK TABLES `PoseLocationOn` WRITE;
/*!40000 ALTER TABLE `PoseLocationOn` DISABLE KEYS */;
/*!40000 ALTER TABLE `PoseLocationOn` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PoseOnlyLocation`
--

DROP TABLE IF EXISTS `PoseOnlyLocation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PoseOnlyLocation` (
  `PoseOnlyLocationID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`PoseOnlyLocationID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PoseOnlyLocation`
--

LOCK TABLES `PoseOnlyLocation` WRITE;
/*!40000 ALTER TABLE `PoseOnlyLocation` DISABLE KEYS */;
INSERT INTO `PoseOnlyLocation` VALUES (174,'changing_station_base_pose'),(217,'changing_station_pose'),(316,'empty_kit_tray_box_pose'),(204,'empty_kit_tray_supply_pose'),(177,'finished_kit_box_pose'),(223,'finished_kit_receiver_pose'),(71,'GearBox_base_tray_pose'),(10,'GearBox_top_tray_pose'),(25,'kit_gearbox_pose'),(159,'kit_tray_1_pose'),(191,'large_gear_tray_pose'),(209,'medium_gear_tray_pose'),(345,'part_gripper_holder_pose'),(208,'part_gripper_pose'),(33,'robot_pose'),(11,'shaft_tray_pose'),(110,'small_gear_tray_pose'),(45,'tray_gripper_holder_pose'),(202,'tray_gripper_pose'),(166,'washer_tray_pose'),(331,'work_table_pose');
/*!40000 ALTER TABLE `PoseOnlyLocation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PositivePredicate`
--

DROP TABLE IF EXISTS `PositivePredicate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PositivePredicate` (
  `PositivePredicateID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasPositivePredicate_TargetParameter` varchar(100) DEFAULT NULL,
  `hasPositivePredicate_Description` varchar(255) DEFAULT NULL,
  `hasPositivePredicate_ReferenceParameter` varchar(100) NOT NULL,
  `hasPositivePredicate_PredicateStateRelationOR` varchar(255) DEFAULT NULL,
  `hadByPositivePredicate_Domain` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PositivePredicateID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasPositivePredicate_PredicateStateRelationOR` (`hasPositivePredicate_PredicateStateRelationOR`),
  KEY `fkhadByPositivePredicate_Domain` (`hadByPositivePredicate_Domain`),
  CONSTRAINT `fkhadByPositivePredicate_Domain` FOREIGN KEY (`hadByPositivePredicate_Domain`) REFERENCES `Domain` (`_NAME`),
  CONSTRAINT `fkhasPositivePredicate_PredicateStateRelationOR` FOREIGN KEY (`hasPositivePredicate_PredicateStateRelationOR`) REFERENCES `PredicateStateRelationOr` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PositivePredicate`
--

LOCK TABLES `PositivePredicate` WRITE;
/*!40000 ALTER TABLE `PositivePredicate` DISABLE KEYS */;
INSERT INTO `PositivePredicate` VALUES (3,'kitTray-has-physicalLocation-refObject-kit','Kit','KitTray is associated to Kit','KitTray',NULL,'kitting-domain'),(27,'workTable-has-no-objectOnTable',NULL,'WorkTable has nothing on top of it','WorkTable',NULL,'kitting-domain'),(36,'endEffectorHolder-has-endEffector','EndEffector','EndEffectorHolder is holding EndEffector','EndEffectorHolder',NULL,'kitting-domain'),(51,'robot-has-no-endEffector',NULL,'Robot has no EndEffector','Robot',NULL,'kitting-domain'),(89,'lbwk-has-kit','Kit','LargeBoxWithKits contains Kit','LargeBoxWithKits','LargeBoxWithKits-Kit-SR','kitting-domain'),(93,'kitTray-has-physicalLocation-refObject-workTable','WorkTable','KitTray is on WorkTable','KitTray',NULL,'kitting-domain'),(100,'endEffector-has-heldObject-part','Part','EndEffector is holding Part','EndEffector',NULL,'kitting-domain'),(102,'endEffector-is-for-kitTraySKU','StockKeepingUnit','EndEffector can handle KitTrays with StockKeepingUnit','EndEffector',NULL,'kitting-domain'),(111,'kit-has-physicalLocation-refObject-workTable','WorkTable','Kit is on WorkTable','Kit',NULL,'kitting-domain'),(133,'endEffector-is-over-part','Part','The end effector is attached to the robot and is over a part located in a parts bin','EndEffector',NULL,'kitting-domain'),(141,'endEffector-is-for-partSKU','StockKeepingUnit','EndEffector can hold Parts with StockKeepingUnit','EndEffector',NULL,'kitting-domain'),(152,'endEffector-has-heldObject-kitTray','KitTray','EndEffector is holding KitTray','EndEffector',NULL,'kitting-domain'),(156,'workTable-has-objectOnTable-kit','Kit','WorkTable has Kit on its top','WorkTable',NULL,'kitting-domain'),(164,'kitTray-has-physicalLocation-refObject-lbwekt','LargeBoxWithEmptyKitTrays','KitTray is located in LargeBoxWithEmptyKitTrays','KitTray',NULL,'kitting-domain'),(216,'part-has-skuObject-sku','StockKeepingUnit','Part has StockKeepingUnit','Part',NULL,'kitting-domain'),(242,'kit-exists',NULL,'Kit exists','Kit',NULL,'kitting-domain'),(265,'workTable-has-objectOnTable-kitTray','KitTray','WorkTable has KitTray on its top','WorkTable',NULL,'kitting-domain'),(270,'endEffectorHolder-has-physicalLocation-refObject-changingStation','EndEffectorChangingStation','EndEffectorHolder is located in EndEffectorChangingStation','EndEffectorHolder',NULL,'kitting-domain'),(272,'partsVessel-has-part','Part','PartsTray contains Part','PartsTray',NULL,'kitting-domain'),(276,'kitTray-has-skuObject-sku','StockKeepingUnit','KiTray has StockKeepingUnit','KitTray',NULL,'kitting-domain'),(277,'robot-has-endEffector','EndEffector','Robot is equipped with EndEffector','Robot',NULL,'kitting-domain'),(289,'endEffectorChangingStation-has-endEffectorHolder','EndEffectorHolder','endEffectorChangingStation contains EndEffectorHolder','EndEffectorChangingStation',NULL,'kitting-domain'),(300,'endEffector-is-over-kit','Kit','The end effector is attached to the robot and is over a kit located on a table','EndEffector',NULL,'kitting-domain'),(302,'part-has-physicalLocation-refObject-endEffector','EndEffector','Part is held by EndEffector','Part','part-endEffector-SR','kitting-domain'),(315,'kitTray-has-physicalLocation-refObject-endEffector','EndEffector','KitTray is held by EndEffector','KitTray','kitTray-EndEffector-SR','kitting-domain'),(323,'part-has-physicalLocation-refObject-kit','Kit','Part is in Kit','Part','Part-Kit-SR','kitting-domain'),(326,'part-is-found',NULL,'Part is found','Part',NULL,'kitting-domain'),(339,'kit-has-physicalLocation-refObject-endEffector','EndEffector','Kit is held by EndEffector','Kit','Kit-EndEffector-SR','kitting-domain'),(352,'kit-has-kitTray','KitTray','Kit is associated with KitTray','Kit',NULL,'kitting-domain'),(355,'endEffector-has-heldObject-kit','Kit','EndEffector is holding Kit','EndEffector','EndEffector-Kit-SR','kitting-domain'),(387,'kit-has-physicalLocation-refObject-lbwk','LargeBoxWithKits','Kit is located in LargeBoxWithKits','Kit','Kit-LargeBoxWithKits-SR','kitting-domain'),(388,'part-has-physicalLocation-refObject-partsTray','PartsTray','Part is in PartsTray','Part',NULL,'kitting-domain'),(408,'endEffector-is-over-kitTray','KitTray','The end effector is attached to the robot and is over a kitTray located in a LargeBoxWithKitTrays','EndEffector',NULL,'kitting-domain'),(416,'endEffector-has-physicalLocation-refObject-endEffectorHolder','EndEffectorHolder','EndEffector is located in EndEffectorHolder','EndEffector','EndEffector-EndEffectorHolder-SR','kitting-domain'),(421,'endEffector-has-physicalLocation-refObject-robot','Robot','EndEffector is attached to Robot','EndEffector',NULL,'kitting-domain'),(424,'endEffector-has-no-heldObject',NULL,'EndEffector is not holding any object','EndEffector','EndEffector-SolidObject-SR','kitting-domain');
/*!40000 ALTER TABLE `PositivePredicate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Precondition`
--

DROP TABLE IF EXISTS `Precondition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Precondition` (
  `PreconditionID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`PreconditionID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Precondition`
--

LOCK TABLES `Precondition` WRITE;
/*!40000 ALTER TABLE `Precondition` DISABLE KEYS */;
INSERT INTO `Precondition` VALUES (236,'attach-endEffector-precondition'),(392,'create-kit-precondition'),(206,'detach-endEffector-precondition'),(104,'look-for-part-precondition'),(116,'move-over-kit-precondition'),(224,'move-over-kitTray-precondition'),(312,'move-over-part-precondition'),(96,'place-kit-precondition'),(354,'place-kitTray-precondition'),(41,'place-part-precondition'),(199,'take-kit-precondition'),(366,'take-kitTray-precondition'),(42,'take-part-precondition');
/*!40000 ALTER TABLE `Precondition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PredicateStateRelation`
--

DROP TABLE IF EXISTS `PredicateStateRelation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PredicateStateRelation` (
  `PredicateStateRelationID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasPredicateStateRelation_ReferenceParameter` varchar(100) NOT NULL,
  `hasPredicateStateRelation_TargetParameter` varchar(100) DEFAULT NULL,
  `hasPredicateStateRelation_IntermediateStateRelation` varchar(255) DEFAULT NULL,
  `hadByPredicateStateRelation_PredicateStateRelationOr` varchar(255) DEFAULT NULL,
  `hadByPredicateStateRelation_PositivePredicate` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PredicateStateRelationID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasPredicateStateRelation_IntermediateStateRelation` (`hasPredicateStateRelation_IntermediateStateRelation`),
  KEY `fkhadByPredicateStateRelation_PredicateStateRelationOr` (`hadByPredicateStateRelation_PredicateStateRelationOr`),
  KEY `fkhadByPredicateStateRelation_PositivePredicate` (`hadByPredicateStateRelation_PositivePredicate`),
  CONSTRAINT `fkhadByPredicateStateRelation_PositivePredicate` FOREIGN KEY (`hadByPredicateStateRelation_PositivePredicate`) REFERENCES `PositivePredicate` (`_NAME`),
  CONSTRAINT `fkhadByPredicateStateRelation_PredicateStateRelationOr` FOREIGN KEY (`hadByPredicateStateRelation_PredicateStateRelationOr`) REFERENCES `PredicateStateRelationOr` (`_NAME`),
  CONSTRAINT `fkhasPredicateStateRelation_IntermediateStateRelation` FOREIGN KEY (`hasPredicateStateRelation_IntermediateStateRelation`) REFERENCES `IntermediateStateRelation` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PredicateStateRelation`
--

LOCK TABLES `PredicateStateRelation` WRITE;
/*!40000 ALTER TABLE `PredicateStateRelation` DISABLE KEYS */;
INSERT INTO `PredicateStateRelation` VALUES (14,'WorkTable-KitTray-SR-UnderWithContact','WorkTable','KitTray','UnderWithContact',NULL,'workTable-has-objectOnTable-kitTray'),(17,'LargeBoxWithKits-Kit-SR-PartiallyIn','Kit','LargeBoxWithKits','PartiallyIn','LargeBoxWithKits-Kit-SR',NULL),(23,'EndEffector-EndEffectorHolder-SR-ContainedIn','EndEffector','EndEffectorHolder','ContainedIn',NULL,'endEffectorHolder-has-endEffector'),(24,'part-endEffector-SR-ContainedIn','Part','EndEffector','ContainedIn','part-endEffector-SR',NULL),(31,'LargeBoxWithKits-Kit-SR-ContainedIn','Kit','LargeBoxWithKits','ContainedIn','LargeBoxWithKits-Kit-SR',NULL),(54,'EndEffector-SolidObject-SR-NotUnderWithContact','SolidObject','EndEffector','NotUnderWithContact','EndEffector-SolidObject-SR',NULL),(57,'WorkTable-SolidObject-SR-NotUnderWithContact','WorkTable','SolidObject','NotUnderWithContact',NULL,'workTable-has-no-objectOnTable'),(60,'EndEffector-Kit-SR-NotUnderWithContact','SolidObject','Kit','NotUnderWithContact',NULL,'endEffector-has-heldObject-kit'),(65,'PartsTray-Part-SR-ContainedIn','Part','PartsTray','ContainedIn',NULL,'partsVessel-has-part'),(75,'Part-Kit-SR-ContainedIn','Part','Kit','ContainedIn','Part-Kit-SR',NULL),(92,'Kit-EndEffector-SR-PartiallyIn','Kit','EndEffector','PartiallyIn','Kit-EndEffector-SR',NULL),(125,'WorkTable-Kit-SR-UnderWithContact','WorkTable','Kit','UnderWithContact',NULL,'workTable-has-objectOnTable-kit'),(126,'KitTray-LargeBoxWithEmptyKitTrays-SR-ContainedIn','KitTray','LargeBoxWithEmptyKitTrays','ContainedIn',NULL,'kitTray-has-physicalLocation-refObject-lbwekt'),(129,'EndEffector-SolidObject-SR-NotContainedIn','SolidObject','EndEffector','NotContainedIn','EndEffector-SolidObject-SR',NULL),(131,'KitTray-EndEffector-SRContainedIn','KitTray','EndEffector','ContainedIn','kitTray-EndEffector-SR',NULL),(138,'EndEffector-Part-SR-NotUnderWithContact','Part','KitTray','NotUnderWithContact',NULL,'endEffector-has-heldObject-part'),(140,'Kit-LargeBoxWithKits-SR-PartiallyIn','Kit','LargeBoxWithKits','PartiallyIn','Kit-LargeBoxWithKits-SR',NULL),(149,'EndEffector-EndEffectorHolder-ContainedIn','EndEffector','EndEffectorHolder','ContainedIn','EndEffector-EndEffectorHolder-SR',NULL),(154,'EndEffector-Kit-SR-PartiallyIn','Kit','EndEffector','PartiallyIn','EndEffector-Kit-SR',NULL),(157,'Part-Kit-SR-PartiallyIn','Part','Kit','PartiallyIn','Part-Kit-SR',NULL),(158,'Kit-LargeBoxWithKits-SR-ContainedIn','Kit','LargeBoxWithKits','ContainedIn','Kit-LargeBoxWithKits-SR',NULL),(176,'EndEffector-Kit-SR-ContainedIn','Kit','EndEffector','ContainedIn','EndEffector-Kit-SR',NULL),(178,'EndEffector-EndEffectorHolder-PartiallyIn','EndEffector','EndEffectorHolder','PartiallyIn','EndEffector-EndEffectorHolder-SR',NULL),(181,'Part-PartsTray-SR-PartiallyIn','Part','PartsTray','PartiallyIn',NULL,'part-has-physicalLocation-refObject-partsTray'),(196,'part-endEffector-SR-NotUnderWithContact','SolidObject','Part','NotUnderWithContact',NULL,'part-has-physicalLocation-refObject-endEffector'),(200,'Robot-EndEffector-SR-InContactWith','EndEffector','Robot','InContactWith',NULL,'robot-has-endEffector'),(228,'EndEffector-Robot-SR-InContactWith','EndEffector','Robot','InContactWith',NULL,'endEffector-has-physicalLocation-refObject-robot'),(230,'EndEffectorHolder-EndEffectorChangingStation-SR-ContainedIn','EndEffectorHolder','EndEffectorChangingStation','ContainedIn',NULL,'endEffectorHolder-has-physicalLocation-refObject-changingStation'),(250,'EndEffector-Part-SR-PartiallyIn','Part','EndEffector','PartiallyIn',NULL,'endEffector-has-heldObject-part'),(263,'Kit-EndEffector-SR-NotUnderWithContact','SolidObject','Kit','NotUnderWithContact',NULL,'kit-has-physicalLocation-refObject-endEffector'),(271,'EndEffector-KitTray-SR-NotUnderWithContact','SolidObject','KitTray','NotUnderWithContact',NULL,'endEffector-has-heldObject-kitTray'),(293,'EndEffector-kitTray-SR-PartiallyIn','KitTray','EndEffector','PartiallyIn',NULL,'endEffector-has-heldObject-kitTray'),(294,'EndEffector-Robot-SR-NotInContactWith','EndEffector','Robot','NotInContactWith',NULL,'robot-has-no-endEffector'),(301,'KitTray-endEffector-SR-PartiallyIn','KitTray','EndEffector','PartiallyIn','kitTray-EndEffector-SR',NULL),(365,'KitTray-endEffector-SR-NotUnderWithContact','SolidObject','KitTray','NotUnderWithContact',NULL,'kitTray-has-physicalLocation-refObject-endEffector'),(369,'Kit-EndEffector-SR-ContainedIn','Kit','EndEffector','ContainedIn','Kit-EndEffector-SR',NULL),(374,'Kit-WorkTable-SR-UnderWithContact','Kit','WorkTable','UnderWithContact',NULL,'kit-has-physicalLocation-refObject-workTable'),(376,'EndEffectorChangingStation-EndEffectorHolder-SR-InContactWith','EndEffectorHolder','EndEffectorChangingStation','InContactWith',NULL,'endEffectorChangingStation-has-endEffectorHolder'),(403,'KitTray-WorkTable-SR-UnderWithContact','KitTray','WorkTable','UnderWithContact',NULL,'kitTray-has-physicalLocation-refObject-workTable'),(415,'part-endEffector-SR-PartiallyIn','Part','EndEffector','PartiallyIn','part-endEffector-SR',NULL),(426,'Part-PartsTray-SR-ContainedIn','Part','PartsTray','ContainedIn',NULL,'part-has-physicalLocation-refObject-partsTray');
/*!40000 ALTER TABLE `PredicateStateRelation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PredicateStateRelationOr`
--

DROP TABLE IF EXISTS `PredicateStateRelationOr`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PredicateStateRelationOr` (
  `PredicateStateRelationOrID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`PredicateStateRelationOrID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PredicateStateRelationOr`
--

LOCK TABLES `PredicateStateRelationOr` WRITE;
/*!40000 ALTER TABLE `PredicateStateRelationOr` DISABLE KEYS */;
INSERT INTO `PredicateStateRelationOr` VALUES (268,'EndEffector-EndEffectorHolder-SR'),(88,'EndEffector-Kit-SR'),(130,'EndEffector-SolidObject-SR'),(9,'Kit-EndEffector-SR'),(12,'Kit-LargeBoxWithKits-SR'),(73,'kitTray-EndEffector-SR'),(422,'LargeBoxWithKits-Kit-SR'),(210,'part-endEffector-SR'),(273,'Part-Kit-SR');
/*!40000 ALTER TABLE `PredicateStateRelationOr` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `RCC8StateRelation`
--

DROP TABLE IF EXISTS `RCC8StateRelation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RCC8StateRelation` (
  `RCC8StateRelationID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasRCC8StateRelation_RCC8Set` varchar(255) NOT NULL,
  PRIMARY KEY (`RCC8StateRelationID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RCC8StateRelation`
--

LOCK TABLES `RCC8StateRelation` WRITE;
/*!40000 ALTER TABLE `RCC8StateRelation` DISABLE KEYS */;
INSERT INTO `RCC8StateRelation` VALUES (4,'RCC8-Under','Smaller-Z and (X-EC or X-NTPP or X-TPP or X-PO or X-NTPPi or X-TPPi) and (Y-EC or Y-NTPP or Y-TPP or Y-PO or Y-NTPPi or Y-TPPi)'),(30,'RCC8-PartiallyIn','(Z-Plus and (Z-NTPP or Z-NTPPi or Z-PO or Z-TPP or Z-TPPi)) and (X-NTPP or X-NTPPi or X-TPP or X-TPPi) and (Y-NTPP or Y-NTPPi or Y-TPP or Y-TPPi)'),(72,'RCC8-NotUnderWithContact','not(Z-EC and Z-Minus and (X-NTPP or X-NTPPi or X-PO or X-TPP or X-TPPi) and (Y-NTPP or Y-NTPPi or Y-PO or Y-TPP or Y-TPPi))'),(85,'RCC8-UnderWithContact','Z-EC and Z-Minus and (X-NTPP or X-NTPPi or X-PO or X-TPP or X-TPPi) and (Y-NTPP or Y-NTPPi or Y-PO or Y-TPP or Y-TPPi)'),(86,'RCC8-NotInContactWith','not(X-EC or Y-EC or Z-EC)'),(132,'RCC8-OnTopOf','Greater-Z and (X-EQ or X-NTPP or X-TPP or X-PO or X-NTPPi  or X-TPPi) and (Y-EQ or Y-NTPP or Y-TPP or Y-PO or Y-NTPPi or Y-TPPi)'),(150,'RCC8-OnTopWithContact','Z-EC and Z-Plus and (X-NTPP or X-NTPPi or X-PO or X-TPP or X-TPPi) and (Y-NTPP or Y-NTPPi or Y-PO or Y-TPP or Y-TPPi)'),(183,'RCC8-NotContainedIn','not((X-TPP or X-NTPP) and (Y-TPP or Y-NTPP) and (Z-TPP or Z-NTPP))'),(211,'RCC8-NotOnTopWithContact','not(Z-EC and Z-Plus and (X-NTPP or X-NTPPi or X-PO or X-TPP or X-TPPi) and (Y-NTPP or Y-NTPPi or Y-PO or Y-TPP or Y-TPPi))'),(219,'RCC8-InContactWith','X-EC or Y-EC or Z-EC'),(222,'RCC8-PartiallyInAndInContactWith','((Z-Plus and (Z-NTPP or Z-NTPPi or Z-PO or Z-TPP or Z-TPPi)) and (X-NTPP or X-NTPPi or X-TPP or X-TPPi) and (Y-NTPP or Y-NTPPi or Y-TPP or Y-TPPi)) and (X-EC or Y-EC or Z-EC)'),(226,'RCC8-ContainedInLBWK','(X-TPP or X-NTPP) and (Y-TPP or Y-NTPP) and (Z-TPP or Z-NTPP)'),(261,'RCC8-ContainedIn','(X-TPP or X-NTPP) and (Y-TPP or Y-NTPP) and (Z-TPP or Z-NTPP)');
/*!40000 ALTER TABLE `RCC8StateRelation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `RelativeLocation`
--

DROP TABLE IF EXISTS `RelativeLocation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RelativeLocation` (
  `RelativeLocationID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasRelativeLocation_Description` varchar(255) NOT NULL,
  PRIMARY KEY (`RelativeLocationID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RelativeLocation`
--

LOCK TABLES `RelativeLocation` WRITE;
/*!40000 ALTER TABLE `RelativeLocation` DISABLE KEYS */;
INSERT INTO `RelativeLocation` VALUES (203,'relative_location_in_1','kitting_workstation_1 is in kitting_workstation_1');
/*!40000 ALTER TABLE `RelativeLocation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `RelativeLocationIn`
--

DROP TABLE IF EXISTS `RelativeLocationIn`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RelativeLocationIn` (
  `RelativeLocationInID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`RelativeLocationInID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RelativeLocationIn`
--

LOCK TABLES `RelativeLocationIn` WRITE;
/*!40000 ALTER TABLE `RelativeLocationIn` DISABLE KEYS */;
INSERT INTO `RelativeLocationIn` VALUES (203,'relative_location_in_1');
/*!40000 ALTER TABLE `RelativeLocationIn` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `RelativeLocationOn`
--

DROP TABLE IF EXISTS `RelativeLocationOn`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RelativeLocationOn` (
  `RelativeLocationOnID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`RelativeLocationOnID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RelativeLocationOn`
--

LOCK TABLES `RelativeLocationOn` WRITE;
/*!40000 ALTER TABLE `RelativeLocationOn` DISABLE KEYS */;
/*!40000 ALTER TABLE `RelativeLocationOn` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Robot`
--

DROP TABLE IF EXISTS `Robot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Robot` (
  `RobotID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasRobot_MaximumLoadWeight` double NOT NULL,
  `hasRobot_Description` varchar(255) NOT NULL,
  PRIMARY KEY (`RobotID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Robot`
--

LOCK TABLES `Robot` WRITE;
/*!40000 ALTER TABLE `Robot` DISABLE KEYS */;
INSERT INTO `Robot` VALUES (14,'robot_1',7,'this is the robot');
/*!40000 ALTER TABLE `Robot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SOAP`
--

DROP TABLE IF EXISTS `SOAP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SOAP` (
  `SOAPID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasSOAP_Domain` varchar(255) DEFAULT NULL,
  `hasSOAP_KittingWorkstation` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`SOAPID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasSOAP_Domain` (`hasSOAP_Domain`),
  KEY `fkhasSOAP_KittingWorkstation` (`hasSOAP_KittingWorkstation`),
  CONSTRAINT `fkhasSOAP_Domain` FOREIGN KEY (`hasSOAP_Domain`) REFERENCES `Domain` (`_NAME`),
  CONSTRAINT `fkhasSOAP_KittingWorkstation` FOREIGN KEY (`hasSOAP_KittingWorkstation`) REFERENCES `KittingWorkstation` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SOAP`
--

LOCK TABLES `SOAP` WRITE;
/*!40000 ALTER TABLE `SOAP` DISABLE KEYS */;
INSERT INTO `SOAP` VALUES (201,'soap','kitting-domain','kitting_workstation_1');
/*!40000 ALTER TABLE `SOAP` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ShapeDesign`
--

DROP TABLE IF EXISTS `ShapeDesign`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ShapeDesign` (
  `ShapeDesignID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasShapeDesign_Description` varchar(255) NOT NULL,
  `hasShapeDesign_GraspPose` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ShapeDesignID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasShapeDesign_GraspPose` (`hasShapeDesign_GraspPose`),
  CONSTRAINT `fkhasShapeDesign_GraspPose` FOREIGN KEY (`hasShapeDesign_GraspPose`) REFERENCES `PoseLocation` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ShapeDesign`
--

LOCK TABLES `ShapeDesign` WRITE;
/*!40000 ALTER TABLE `ShapeDesign` DISABLE KEYS */;
INSERT INTO `ShapeDesign` VALUES (15,'shape_large_gear_stl','stl file with shape of large gear',NULL),(69,'shape_medium_gear','Shape of bounding cylinder of medium gear',NULL),(91,'shape_small_gear','Shape of bounding cylinder of small gear',NULL),(134,'shape_kit_box','Shape of boxes to hold kits and kit trays',NULL),(137,'shape_GearBox_base_stl','stl file with shape of base of GearBox',NULL),(153,'shape_medium_gear_stl','stl file with shape of medium gear',NULL),(155,'shape_washerShaft_tray','\n        Shape of PartsTrays for washer and shaft of GearBox\n      ',NULL),(184,'shape_GearBox_top_stl','stl file with shape of top of GearBox',NULL),(227,'shape_GearBox_top','Shape of top of GearBox',NULL),(233,'shape_gear_tray','Shape of PartsTrays for gears of GearBox',NULL),(244,'shape_GearBox_kit_tray','Shape of KitTrays for GearBox',NULL),(249,'shape_large_gear','Shape of bounding cylinder of large gear',NULL),(269,'shape_GearBox_base','Shape of GearBox base',NULL),(286,'shape_topBase_tray','\n        Shape of PartsTrays for top and base of GearBox\n      ',NULL),(288,'shape_small_gear_stl','stl file with shape of small gear',NULL),(303,'shape_gripper_holder_1','Shape of gripper holder 1',NULL),(304,'shape_gripper_holder_2','Shape of gripper holder 2',NULL),(308,'shape_washer','Shape of bounding cylinder of washer',NULL),(325,'shape_washer_stl','stl file with shape of washer',NULL),(328,'shape_changing_station_base','Shape of base of changing station',NULL),(335,'shape_shaft_stl','stl file with shape of shaft',NULL),(336,'shape_work_table','Shape of the work table',NULL),(390,'shape_shaft','Shape of bounding cylinder of shaft',NULL);
/*!40000 ALTER TABLE `ShapeDesign` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SkuObject`
--

DROP TABLE IF EXISTS `SkuObject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SkuObject` (
  `SkuObjectID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasSkuObject_Sku` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`SkuObjectID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasSkuObject_Sku` (`hasSkuObject_Sku`),
  CONSTRAINT `fkhasSkuObject_Sku` FOREIGN KEY (`hasSkuObject_Sku`) REFERENCES `StockKeepingUnit` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SkuObject`
--

LOCK TABLES `SkuObject` WRITE;
/*!40000 ALTER TABLE `SkuObject` DISABLE KEYS */;
INSERT INTO `SkuObject` VALUES (7,'GearBox_base_1','stock_keeping_unit_GearBox_base'),(18,'kit_tray_1','stock_keeping_unit_GearBox_kit_tray'),(5,'GearBox_top_1','stock_keeping_unit_GearBox_top'),(6,'large_gear_tray','stock_keeping_unit_gear_tray'),(20,'medium_gear_tray','stock_keeping_unit_gear_tray'),(27,'small_gear_tray','stock_keeping_unit_gear_tray'),(24,'empty_kit_tray_box','stock_keeping_unit_kit_box'),(25,'finished_kit_box','stock_keeping_unit_kit_box'),(8,'large_gear_1','stock_keeping_unit_large_gear'),(13,'medium_gear_1','stock_keeping_unit_medium_gear'),(16,'small_gear_1','stock_keeping_unit_small_gear'),(15,'GearBox_top_tray','stock_keeping_unit_topBase_tray'),(17,'GearBox_base_tray','stock_keeping_unit_topBase_tray'),(10,'shaft_tray','stock_keeping_unit_washerShaft_tray'),(12,'washer_tray','stock_keeping_unit_washerShaft_tray');
/*!40000 ALTER TABLE `SkuObject` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Slot`
--

DROP TABLE IF EXISTS `Slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Slot` (
  `SlotID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hadBySlot_Kit` varchar(255) DEFAULT NULL,
  `hasSlot_Part` varchar(255) DEFAULT NULL,
  `hasSlot_PartRefAndPose` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`SlotID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadBySlot_Kit` (`hadBySlot_Kit`),
  KEY `fkhasSlot_Part` (`hasSlot_Part`),
  KEY `fkhasSlot_PartRefAndPose` (`hasSlot_PartRefAndPose`),
  CONSTRAINT `fkhadBySlot_Kit` FOREIGN KEY (`hadBySlot_Kit`) REFERENCES `Kit` (`_NAME`),
  CONSTRAINT `fkhasSlot_Part` FOREIGN KEY (`hasSlot_Part`) REFERENCES `Part` (`_NAME`),
  CONSTRAINT `fkhasSlot_PartRefAndPose` FOREIGN KEY (`hasSlot_PartRefAndPose`) REFERENCES `PartRefAndPose` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Slot`
--

LOCK TABLES `Slot` WRITE;
/*!40000 ALTER TABLE `Slot` DISABLE KEYS */;
/*!40000 ALTER TABLE `Slot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SolidObject`
--

DROP TABLE IF EXISTS `SolidObject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SolidObject` (
  `SolidObjectID` int(11) NOT NULL AUTO_INCREMENT,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasSolidObject_PrimaryLocation` varchar(255) DEFAULT NULL,
  `hadByObjectOnTable_WorkTable` varchar(255) DEFAULT NULL,
  `hadByObject_KittingWorkstation` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`SolidObjectID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhasSolidObject_PrimaryLocation` (`hasSolidObject_PrimaryLocation`),
  KEY `fkhadByObjectOnTable_WorkTable` (`hadByObjectOnTable_WorkTable`),
  KEY `fkhadByObject_KittingWorkstation` (`hadByObject_KittingWorkstation`),
  CONSTRAINT `fkhadByObjectOnTable_WorkTable` FOREIGN KEY (`hadByObjectOnTable_WorkTable`) REFERENCES `WorkTable` (`_NAME`),
  CONSTRAINT `fkhadByObject_KittingWorkstation` FOREIGN KEY (`hadByObject_KittingWorkstation`) REFERENCES `KittingWorkstation` (`_NAME`),
  CONSTRAINT `fkhasSolidObject_PrimaryLocation` FOREIGN KEY (`hasSolidObject_PrimaryLocation`) REFERENCES `PhysicalLocation` (`_NAME`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SolidObject`
--

LOCK TABLES `SolidObject` WRITE;
/*!40000 ALTER TABLE `SolidObject` DISABLE KEYS */;
INSERT INTO `SolidObject` VALUES (1,'tray_gripper_holder','tray_gripper_holder_pose',NULL,NULL),(2,'part_gripper','part_gripper_pose',NULL,NULL),(3,'changing_station_1','changing_station_pose',NULL,NULL),(4,'changing_station_base','changing_station_base_pose',NULL,NULL),(5,'GearBox_top_1','GearBox_top_1_pose',NULL,NULL),(6,'large_gear_tray','large_gear_tray_pose',NULL,'kitting_workstation_1'),(7,'GearBox_base_1','GearBox_base_1_pose',NULL,NULL),(8,'large_gear_1','large_gear_1_pose',NULL,NULL),(9,'kit_gearbox','kit_gearbox_pose',NULL,NULL),(10,'shaft_tray','shaft_tray_pose',NULL,'kitting_workstation_1'),(11,'tray_gripper','tray_gripper_pose',NULL,NULL),(12,'washer_tray','washer_tray_pose',NULL,'kitting_workstation_1'),(13,'medium_gear_1','medium_gear_1_pose',NULL,NULL),(14,'robot_1','robot_pose',NULL,NULL),(15,'GearBox_top_tray','GearBox_top_tray_pose',NULL,'kitting_workstation_1'),(16,'small_gear_1','small_gear_1_pose',NULL,NULL),(17,'GearBox_base_tray','GearBox_base_tray_pose',NULL,'kitting_workstation_1'),(18,'kit_tray_1','kit_tray_1_pose',NULL,NULL),(19,'empty_kit_tray_supply','empty_kit_tray_supply_pose',NULL,'kitting_workstation_1'),(20,'medium_gear_tray','medium_gear_tray_pose',NULL,'kitting_workstation_1'),(21,'kitting_workstation_1','relative_location_in_1',NULL,NULL),(22,'part_gripper_holder','part_gripper_holder_pose',NULL,NULL),(23,'work_table_1','work_table_pose',NULL,'kitting_workstation_1'),(24,'empty_kit_tray_box','empty_kit_tray_box_pose',NULL,NULL),(25,'finished_kit_box','finished_kit_box_pose',NULL,NULL),(26,'finished_kit_receiver','finished_kit_receiver_pose',NULL,'kitting_workstation_1'),(27,'small_gear_tray','small_gear_tray_pose',NULL,'kitting_workstation_1');
/*!40000 ALTER TABLE `SolidObject` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `StockKeepingUnit`
--

DROP TABLE IF EXISTS `StockKeepingUnit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `StockKeepingUnit` (
  `StockKeepingUnitID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasStockKeepingUnit_Description` varchar(255) NOT NULL,
  `hasStockKeepingUnit_Weight` double NOT NULL,
  `hadBySku_KittingWorkstation` varchar(255) DEFAULT NULL,
  `hasStockKeepingUnit_ExternalShape` varchar(255) DEFAULT NULL,
  `hasStockKeepingUnit_InternalShape` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`StockKeepingUnitID`,`_NAME`),
  KEY `_NAME` (`_NAME`),
  KEY `fkhadBySku_KittingWorkstation` (`hadBySku_KittingWorkstation`),
  KEY `fkhasStockKeepingUnit_ExternalShape` (`hasStockKeepingUnit_ExternalShape`),
  KEY `fkhasStockKeepingUnit_InternalShape` (`hasStockKeepingUnit_InternalShape`),
  CONSTRAINT `fkhadBySku_KittingWorkstation` FOREIGN KEY (`hadBySku_KittingWorkstation`) REFERENCES `KittingWorkstation` (`_NAME`),
  CONSTRAINT `fkhasStockKeepingUnit_ExternalShape` FOREIGN KEY (`hasStockKeepingUnit_ExternalShape`) REFERENCES `ExternalShape` (`_NAME`),
  CONSTRAINT `fkhasStockKeepingUnit_InternalShape` FOREIGN KEY (`hasStockKeepingUnit_InternalShape`) REFERENCES `InternalShape` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `StockKeepingUnit`
--

LOCK TABLES `StockKeepingUnit` WRITE;
/*!40000 ALTER TABLE `StockKeepingUnit` DISABLE KEYS */;
INSERT INTO `StockKeepingUnit` VALUES (19,'stock_keeping_unit_topBase_tray','SKU for PartsTrays for top and base of GearBox',0.2,'kitting_workstation_1',NULL,'shape_topBase_tray'),(40,'stock_keeping_unit_GearBox_kit_tray','SKU for KitTrays for GearBox',0.2,'kitting_workstation_1',NULL,'shape_GearBox_kit_tray'),(43,'stock_keeping_unit_GearBox_base','SKU for base of GearBox',0.05,'kitting_workstation_1','shape_GearBox_base_stl','shape_GearBox_base'),(74,'stock_keeping_unit_large_gear','SKU for large gear of GearBox',0.2,'kitting_workstation_1','shape_large_gear_stl','shape_large_gear'),(179,'stock_keeping_unit_kit_box','SKU for Boxes for Kits and KitTrays',1.2,'kitting_workstation_1',NULL,'shape_kit_box'),(235,'stock_keeping_unit_washer','SKU for washer of GearBox',0.001,'kitting_workstation_1','shape_washer_stl','shape_washer'),(255,'stock_keeping_unit_medium_gear','SKU for medium gear of GearBox',0.15,'kitting_workstation_1','shape_medium_gear_stl','shape_medium_gear'),(299,'stock_keeping_unit_GearBox_top','SKU for top of GearBox',0.05,'kitting_workstation_1','shape_GearBox_top_stl','shape_GearBox_top'),(310,'stock_keeping_unit_washerShaft_tray','\n      SKU for PartsTrays for washer and shaft of GearBox\n    ',0.1,'kitting_workstation_1',NULL,'shape_washerShaft_tray'),(318,'stock_keeping_unit_small_gear','SKU for small gear of GearBox',0.1,'kitting_workstation_1','shape_small_gear_stl','shape_small_gear'),(330,'stock_keeping_unit_shaft','SKU for shaft of GearBox',0.005,'kitting_workstation_1','shape_shaft_stl','shape_shaft'),(389,'stock_keeping_unit_gear_tray','SKU for PartsTrays for gears of GearBox',0.2,'kitting_workstation_1',NULL,'shape_gear_tray');
/*!40000 ALTER TABLE `StockKeepingUnit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `VacuumEffector`
--

DROP TABLE IF EXISTS `VacuumEffector`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `VacuumEffector` (
  `VacuumEffectorID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasVacuumEffector_Length` double NOT NULL,
  `hasVacuumEffector_CupDiameter` double NOT NULL,
  PRIMARY KEY (`VacuumEffectorID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `VacuumEffector`
--

LOCK TABLES `VacuumEffector` WRITE;
/*!40000 ALTER TABLE `VacuumEffector` DISABLE KEYS */;
INSERT INTO `VacuumEffector` VALUES (2,'part_gripper',25,40),(11,'tray_gripper',100,80);
/*!40000 ALTER TABLE `VacuumEffector` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `VacuumEffectorMultiCup`
--

DROP TABLE IF EXISTS `VacuumEffectorMultiCup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `VacuumEffectorMultiCup` (
  `VacuumEffectorMultiCupID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasVacuumEffectorMultiCup_ArrayRadius` double NOT NULL,
  `hasVacuumEffectorMultiCup_ArrayNumber` int(11) NOT NULL,
  PRIMARY KEY (`VacuumEffectorMultiCupID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `VacuumEffectorMultiCup`
--

LOCK TABLES `VacuumEffectorMultiCup` WRITE;
/*!40000 ALTER TABLE `VacuumEffectorMultiCup` DISABLE KEYS */;
/*!40000 ALTER TABLE `VacuumEffectorMultiCup` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `VacuumEffectorSingleCup`
--

DROP TABLE IF EXISTS `VacuumEffectorSingleCup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `VacuumEffectorSingleCup` (
  `VacuumEffectorSingleCupID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`VacuumEffectorSingleCupID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `VacuumEffectorSingleCup`
--

LOCK TABLES `VacuumEffectorSingleCup` WRITE;
/*!40000 ALTER TABLE `VacuumEffectorSingleCup` DISABLE KEYS */;
INSERT INTO `VacuumEffectorSingleCup` VALUES (2,'part_gripper'),(11,'tray_gripper');
/*!40000 ALTER TABLE `VacuumEffectorSingleCup` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Vector`
--

DROP TABLE IF EXISTS `Vector`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Vector` (
  `VectorID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  `hasVector_I` double NOT NULL,
  `hasVector_J` double NOT NULL,
  `hasVector_K` double NOT NULL,
  PRIMARY KEY (`VectorID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Vector`
--

LOCK TABLES `Vector` WRITE;
/*!40000 ALTER TABLE `Vector` DISABLE KEYS */;
INSERT INTO `Vector` VALUES (5,'robot_z_axis',0,0,-1),(13,'tray_gripper_holder_x_axis',1,0,0),(20,'kit_tray_1_x_axis',1,0,0),(21,'kit_tray_1_z_axis',0,0,1),(28,'GearBox_base_1_z_axis',0,0,1),(29,'part_gripper_holder_z_axis',0,0,1),(34,'small_gear_tray_x_axis',0,1,0),(35,'GearBox_top_1_x_axis',0,1,0),(47,'finished_kit_receiver_x_axis',1,0,0),(58,'z_axis_kit_medium_gear',0,0,1),(76,'tray_gripper_x_axis',1,0,0),(77,'tray_gripper_holder_z_axis',0,0,1),(79,'small_tray_z_axis',0,0,1),(83,'empty_kit_tray_supply_x_axis',1,0,0),(90,'large_gear_1_x_axis',1,0,0),(95,'medium_1_z_axis',0,0,1),(103,'large_gear_1_z_axis',0,0,1),(112,'z_axis_kit_large_gear',0,0,1),(117,'empty_kit_tray_box_z_axis',0,0,1),(121,'GearBox_base_tray_z_axis',0,0,1),(160,'changing_station_x_axis',1,0,0),(161,'empty_kit_tray_supply_z_axis',0,0,1),(171,'z_axis_kit_small_gear',0,0,1),(172,'small_gear_1_x_axis',1,0,0),(175,'work_table_z_axis',0,0,1),(182,'small_1_z_axis',0,0,1),(185,'kit_gearbox_x_axis',1,0,0),(186,'GearBox_base_tray_x_axis',1,0,0),(207,'shaft_tray_z_axis',0,0,1),(212,'kit_gearbox_z_axis',0,0,1),(231,'GearBox_top_tray_z_axis',0,0,1),(237,'x_axis_kit_medium_gear',1,0,0),(238,'x_axis_kit_GearBox_base',0,1,0),(241,'z_axis_kit_GearBox_top',0,0,1),(256,'washer_tray_z_axis',0,0,1),(258,'large_gear_tray_z_axis',0,0,1),(259,'robot_x_axis',1,0,0),(260,'tray_gripper_z_axis',0,0,1),(264,'finished_kit_receiver_z_axis',0,0,1),(275,'shaft_tray_x_axis',1,0,0),(285,'x_axis_kit_GearBox_top',0,1,0),(307,'changing_station_base_z_axis',0,0,1),(309,'part_gripper_holder_x_axis',1,0,0),(311,'x_axis_kit_large_gear',1,0,0),(317,'part_gripper_x_axis',1,0,0),(324,'GearBox_top_tray_x_axis',1,0,0),(327,'large_gear_tray_x_axis',0,1,0),(329,'z_axis_kit_GearBox_base',0,0,1),(346,'part_gripper_z_axis',0,0,-1),(348,'medium_gear_1_x_axis',1,0,0),(349,'work_table_x_axis',1,0,0),(361,'washer_tray_x_axis',1,0,0),(362,'medium_gear_tray_x_axis',0,1,0),(367,'GearBox_base_1_x_axis',0,1,0),(371,'changing_station_base_x_axis',1,0,0),(393,'finished_kit_box_z_axis',0,0,1),(394,'empty_kit_tray_box_x_axis',1,0,0),(402,'x_axis_kit_small_gear',1,0,0),(406,'GearBox_top_1_z_axis',0,0,1),(407,'finished_kit_box_x_axis',1,0,0),(412,'medium_tray_z_axis',0,0,1),(414,'changing_station_z_axis',0,0,1);
/*!40000 ALTER TABLE `Vector` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `WorkTable`
--

DROP TABLE IF EXISTS `WorkTable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `WorkTable` (
  `WorkTableID` int(11) NOT NULL,
  `_NAME` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`WorkTableID`,`_NAME`),
  KEY `_NAME` (`_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `WorkTable`
--

LOCK TABLES `WorkTable` WRITE;
/*!40000 ALTER TABLE `WorkTable` DISABLE KEYS */;
INSERT INTO `WorkTable` VALUES (23,'work_table_1');
/*!40000 ALTER TABLE `WorkTable` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hadByEndEffector_StockKeepingUnit`
--

DROP TABLE IF EXISTS `hadByEndEffector_StockKeepingUnit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hadByEndEffector_StockKeepingUnit` (
  `EndEffectorID` int(11) NOT NULL,
  `StockKeepingUnitID` int(11) NOT NULL,
  PRIMARY KEY (`EndEffectorID`,`StockKeepingUnitID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hadByEndEffector_StockKeepingUnit`
--

LOCK TABLES `hadByEndEffector_StockKeepingUnit` WRITE;
/*!40000 ALTER TABLE `hadByEndEffector_StockKeepingUnit` DISABLE KEYS */;
/*!40000 ALTER TABLE `hadByEndEffector_StockKeepingUnit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hasDomain_RequirementValue`
--

DROP TABLE IF EXISTS `hasDomain_RequirementValue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hasDomain_RequirementValue` (
  `DomainID` int(11) NOT NULL,
  `hasDomain_Requirement` varchar(100) NOT NULL,
  PRIMARY KEY (`DomainID`,`hasDomain_Requirement`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hasDomain_RequirementValue`
--

LOCK TABLES `hasDomain_RequirementValue` WRITE;
/*!40000 ALTER TABLE `hasDomain_RequirementValue` DISABLE KEYS */;
INSERT INTO `hasDomain_RequirementValue` VALUES (218,'action-costs'),(218,'derived-predicates'),(218,'equality'),(218,'fluents'),(218,'strips'),(218,'typing');
/*!40000 ALTER TABLE `hasDomain_RequirementValue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hasDomain_VariableValue`
--

DROP TABLE IF EXISTS `hasDomain_VariableValue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hasDomain_VariableValue` (
  `DomainID` int(11) NOT NULL,
  `hasDomain_Variable` varchar(100) NOT NULL,
  PRIMARY KEY (`DomainID`,`hasDomain_Variable`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hasDomain_VariableValue`
--

LOCK TABLES `hasDomain_VariableValue` WRITE;
/*!40000 ALTER TABLE `hasDomain_VariableValue` DISABLE KEYS */;
INSERT INTO `hasDomain_VariableValue` VALUES (218,'EndEffector'),(218,'EndEffectorChangingStation'),(218,'EndEffectorHolder'),(218,'Kit'),(218,'KitTray'),(218,'LargeBoxWithEmptyKitTrays'),(218,'LargeBoxWithKits'),(218,'Part'),(218,'PartsTray'),(218,'Robot'),(218,'StockKeepingUnit'),(218,'WorkTable');
/*!40000 ALTER TABLE `hasDomain_VariableValue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hasEffect_PositivePredicate`
--

DROP TABLE IF EXISTS `hasEffect_PositivePredicate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hasEffect_PositivePredicate` (
  `EffectID` int(11) NOT NULL,
  `PositivePredicateID` int(11) NOT NULL,
  PRIMARY KEY (`EffectID`,`PositivePredicateID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hasEffect_PositivePredicate`
--

LOCK TABLES `hasEffect_PositivePredicate` WRITE;
/*!40000 ALTER TABLE `hasEffect_PositivePredicate` DISABLE KEYS */;
INSERT INTO `hasEffect_PositivePredicate` VALUES (32,89),(32,387),(32,424),(118,100),(118,302),(139,300),(167,277),(167,421),(167,424),(173,133),(197,408),(205,111),(205,156),(205,242),(220,326),(257,323),(257,424),(262,339),(262,355),(274,36),(274,51),(274,416),(400,152),(400,315),(418,93),(418,265),(418,424);
/*!40000 ALTER TABLE `hasEffect_PositivePredicate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hasPrecondition_PositivePredicate`
--

DROP TABLE IF EXISTS `hasPrecondition_PositivePredicate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hasPrecondition_PositivePredicate` (
  `PreconditionID` int(11) NOT NULL,
  `PositivePredicateID` int(11) NOT NULL,
  PRIMARY KEY (`PreconditionID`,`PositivePredicateID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hasPrecondition_PositivePredicate`
--

LOCK TABLES `hasPrecondition_PositivePredicate` WRITE;
/*!40000 ALTER TABLE `hasPrecondition_PositivePredicate` DISABLE KEYS */;
INSERT INTO `hasPrecondition_PositivePredicate` VALUES (41,100),(41,216),(41,242),(41,277),(41,302),(41,421),(42,133),(42,141),(42,216),(42,242),(42,272),(42,277),(42,326),(42,388),(42,421),(42,424),(96,277),(96,339),(96,355),(96,421),(104,141),(104,216),(104,242),(104,277),(104,421),(104,424),(116,3),(116,102),(116,242),(116,276),(116,277),(116,352),(116,421),(116,424),(199,102),(199,242),(199,276),(199,277),(199,300),(199,352),(199,421),(199,424),(206,270),(206,277),(206,289),(206,421),(206,424),(224,102),(224,164),(224,276),(224,277),(224,421),(224,424),(236,36),(236,51),(236,270),(236,289),(236,416),(312,141),(312,216),(312,272),(312,277),(312,326),(312,388),(312,421),(312,424),(354,152),(354,277),(354,315),(354,421),(366,102),(366,164),(366,276),(366,277),(366,408),(366,421),(366,424),(392,3),(392,93),(392,265),(392,352);
/*!40000 ALTER TABLE `hasPrecondition_PositivePredicate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Final view structure for view `DirectPose`
--

/*!50001 DROP TABLE IF EXISTS `DirectPose`*/;
/*!50001 DROP VIEW IF EXISTS `DirectPose`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `DirectPose` AS select `SO`.`_NAME` AS `name`,`PT`.`hasPoint_X` AS `X`,`PT`.`hasPoint_Y` AS `Y`,`PT`.`hasPoint_Z` AS `Z`,`VX`.`hasVector_I` AS `VXX`,`VX`.`hasVector_J` AS `VXY`,`VX`.`hasVector_K` AS `VXZ`,`VZ`.`hasVector_I` AS `VZX`,`VZ`.`hasVector_J` AS `VZY`,`VZ`.`hasVector_K` AS `VZZ` from ((((`SolidObject` `SO` join `PoseLocation` `PL`) join `Point` `PT`) join `Vector` `VX`) join `Vector` `VZ`) where ((`SO`.`hasSolidObject_PrimaryLocation` = `PL`.`_NAME`) and (`PT`.`_NAME` = `PL`.`hasPoseLocation_Point`) and (`VX`.`_NAME` = `PL`.`hasPoseLocation_XAxis`) and (`VZ`.`_NAME` = `PL`.`hasPoseLocation_ZAxis`)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-09-14 14:31:00
