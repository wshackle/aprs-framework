-- MySQL dump 10.13  Distrib 5.5.50, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: simple_aprs
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
-- Table structure for table `Poses`
--

DROP TABLE IF EXISTS `Poses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Poses` (
  `Name` varchar(80) NOT NULL,
  `X` double NOT NULL DEFAULT '0',
  `Y` double NOT NULL DEFAULT '0',
  `Z` double NOT NULL DEFAULT '0',
  `VXI` double NOT NULL DEFAULT '1',
  `VXJ` double NOT NULL DEFAULT '0',
  `VXK` double NOT NULL DEFAULT '0',
  `VZI` double NOT NULL DEFAULT '0',
  `VZJ` double NOT NULL DEFAULT '0',
  `VZK` double NOT NULL DEFAULT '1',
  PRIMARY KEY (`Name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Poses`
--

LOCK TABLES `Poses` WRITE;
/*!40000 ALTER TABLE `Poses` DISABLE KEYS */;
INSERT INTO `Poses` VALUES ('kit_tray_a_1',600,125,0,1,0,0,0,0,1),('kit_tray_b_1',600,0,0,1,0,0,0,0,1),('kit_tray_c_1',650,-200,0,1,0,0,0,0,1),('large_gear_1',325.79872204472844,-201.27795527156547,0,1,0,0,0,0,1),('large_gear_2',384.16932907348246,33.210862619808324,0,1,0,0,0,0,1),('medium_gear_1',259.37699680511184,-103.6581469648562,0,1,0,0,0,0,1),('medium_gear_2',412.34824281150156,81.51757188498402,0,1,0,0,0,0,1),('part_tray_a_1',250,-250,0,1,0,0,0,0,1),('part_tray_b_1',300,-125,0,1,0,0,0,0,1),('part_tray_c_1',400,125,0,1,0,0,0,0,1),('small_gear_1',275,-75,0,1,0,0,0,0,1),('small_gear_2',380,115,0,1,0,0,0,0,1);
/*!40000 ALTER TABLE `Poses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SlotContents`
--

DROP TABLE IF EXISTS `SlotContents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SlotContents` (
  `PartDesignName` varchar(80) NOT NULL,
  `Quantity` int(11) NOT NULL DEFAULT '0',
  `SlotDesignID` int(11) NOT NULL,
  `TrayInstance` varchar(80) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SlotContents`
--

LOCK TABLES `SlotContents` WRITE;
/*!40000 ALTER TABLE `SlotContents` DISABLE KEYS */;
/*!40000 ALTER TABLE `SlotContents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SlotDesigns`
--

DROP TABLE IF EXISTS `SlotDesigns`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SlotDesigns` (
  `ID` int(11) NOT NULL,
  `TrayDesignName` varchar(80) NOT NULL,
  `PartDesignName` varchar(80) NOT NULL,
  `X_OFFSET` double NOT NULL DEFAULT '0',
  `Y_OFFSET` double NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SlotDesigns`
--

LOCK TABLES `SlotDesigns` WRITE;
/*!40000 ALTER TABLE `SlotDesigns` DISABLE KEYS */;
/*!40000 ALTER TABLE `SlotDesigns` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-09-14 14:30:24
