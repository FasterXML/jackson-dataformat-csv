CREATE DATABASE  IF NOT EXISTS `csvparser` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `csvparser`;
-- MySQL dump 10.13  Distrib 5.6.17, for Win64 (x86_64)
--
-- Host: localhost    Database: csvparser
-- ------------------------------------------------------
-- Server version	5.6.15-log

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
-- Table structure for table `mapperdetails`
--

DROP TABLE IF EXISTS `mapperdetails`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mapperdetails` (
  `MapperDetailsID` mediumint(11) NOT NULL AUTO_INCREMENT,
  `MasterHeaderID` mediumint(11) NOT NULL,
  `row` int(11) NOT NULL,
  `columnName` varchar(100) NOT NULL,
  `value` varchar(250) NOT NULL,
  `altForeignKey` mediumint(11) DEFAULT NULL,
  PRIMARY KEY (`MapperDetailsID`),
  KEY `idx_mastermapperdet` (`MasterHeaderID`),
  CONSTRAINT `fk_MapperDetails_MasterHeader1` FOREIGN KEY (`MasterHeaderID`) REFERENCES `masterheader` (`masterHeaderID`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mapperdetails`
--

LOCK TABLES `mapperdetails` WRITE;
/*!40000 ALTER TABLE `mapperdetails` DISABLE KEYS */;
/*!40000 ALTER TABLE `mapperdetails` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mastercolumnmapper`
--

DROP TABLE IF EXISTS `mastercolumnmapper`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mastercolumnmapper` (
  `ColumnMapID` mediumint(11) NOT NULL AUTO_INCREMENT,
  `CSVAttrName` varchar(100) NOT NULL,
  `TableAttrName` varchar(45) NOT NULL,
  `AttrDataType` varchar(15) DEFAULT NULL,
  `idMasterMapper` mediumint(11) NOT NULL,
  `isKeyAttr` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`ColumnMapID`),
  KEY `fk_MasterColumnMapper_MasterMapper1_idx` (`idMasterMapper`),
  KEY `fk_MasterColumnMapper_ValidDataTypes1_idx` (`AttrDataType`),
  CONSTRAINT `fk_MasterColumnMapper_MasterMapper1` FOREIGN KEY (`idMasterMapper`) REFERENCES `mastermapper` (`idMasterMapper`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_MasterColumnMapper_ValidDataTypes1` FOREIGN KEY (`AttrDataType`) REFERENCES `validdatatypes` (`validDataTypes`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=309 DEFAULT CHARSET=utf8 COMMENT='numer';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mastercolumnmapper`
--


--
-- Table structure for table `masterheader`
--

DROP TABLE IF EXISTS `masterheader`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `masterheader` (
  `masterHeaderID` mediumint(11) NOT NULL AUTO_INCREMENT,
  `idMasterMapper` mediumint(11) NOT NULL,
  `fileImportDate` date DEFAULT NULL,
  `filename` varchar(45) DEFAULT NULL,
  `content` longblob,
  `processCSVFlag` bit(1) NOT NULL DEFAULT b'0',
  `processTableFlag` bit(1) NOT NULL DEFAULT b'0',
  `processCSVHeaderFlag` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`masterHeaderID`),
  KEY `fk_MasterHeader_MasterMapper1_idx` (`idMasterMapper`),
  CONSTRAINT `fk_MasterHeader_MasterMapper1` FOREIGN KEY (`idMasterMapper`) REFERENCES `mastermapper` (`idMasterMapper`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8 COMMENT='				';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `masterheader`
--

--
-- Table structure for table `mastermapper`
--

DROP TABLE IF EXISTS `mastermapper`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mastermapper` (
  `idMasterMapper` mediumint(11) NOT NULL AUTO_INCREMENT,
  `MapName` varchar(45) NOT NULL,
  `TargetTable` varchar(255) NOT NULL,
  `MapperDesc` text,
  `VendorID` mediumint(11) DEFAULT NULL,
  PRIMARY KEY (`idMasterMapper`),
  KEY `fk_MasterMapper_vendor1_idx` (`VendorID`),
  CONSTRAINT `fk_MasterMapper_vendor1` FOREIGN KEY (`VendorID`) REFERENCES `vendor` (`idvendor`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mastermapper`
--

LOCK TABLES `mastermapper` WRITE;
/*!40000 ALTER TABLE `mastermapper` DISABLE KEYS */;
INSERT INTO `mastermapper` VALUES (1,'Inventory FBA','http://localhost:8080/KahunaService/rest/el-local/data1/v1/main:inventoryfba','Amazon FBA Inventory file',1),(2,'Inventory Monthly','Inventory',NULL,1),(3,'Inventory Health','http://localhost:8080/KahunaService/rest/el-local/data1/v1/main:inventory','this is the big one',NULL),(4,'ScanPower','http://localhost:8080/KahunaService/rest/el-local/data1/v1/main%3Ascanpower',NULL,NULL);
/*!40000 ALTER TABLE `mastermapper` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `validdatatypes`
--

DROP TABLE IF EXISTS `validdatatypes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `validdatatypes` (
  `validDataTypes` varchar(15) NOT NULL,
  PRIMARY KEY (`validDataTypes`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `validdatatypes`
--

LOCK TABLES `validdatatypes` WRITE;
/*!40000 ALTER TABLE `validdatatypes` DISABLE KEYS */;
INSERT INTO `validdatatypes` VALUES ('date'),('MMddYYY'),('money'),('number'),('text'),('yyyyMMdd');
/*!40000 ALTER TABLE `validdatatypes` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-03-11 11:58:41
