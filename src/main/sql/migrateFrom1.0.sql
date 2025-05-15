-- Remove existing foreign keys to userRecord
ALTER TABLE mediaRecord DROP FOREIGN KEY FKmr_uploadedBy;
ALTER TABLE page DROP FOREIGN KEY FK_MODIFIEDBY;
ALTER TABLE pageLock DROP FOREIGN KEY FK_LOCK_OWNER;
ALTER TABLE userRole DROP FOREIGN KEY FK_USER;
ALTER TABLE verificationToken DROP FOREIGN KEY verificationToken_userRecord_FK;

-- Add new Id column to userRecord
ALTER TABLE userRecord DROP PRIMARY KEY;
ALTER TABLE userRecord ADD userId INT auto_increment NOT NULL PRIMARY KEY FIRST;
CREATE UNIQUE INDEX userRecord_userName_IDX USING BTREE ON userRecord (userName);


-- Reestablish keys for mediaRecord
ALTER TABLE mediaRecord CHANGE uploadedBy uploadedByBack varchar(150) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL;
ALTER TABLE mediaRecord ADD uploadedBy INT NULL AFTER site;

UPDATE mediaRecord mr INNER JOIN userRecord ur ON mr.uploadedByBack = ur.userName SET uploadedBy = ur.userId;
ALTER TABLE mediaRecord DROP COLUMN uploadedByBack;
ALTER TABLE mediaRecord ADD CONSTRAINT mediaRecord_userRecord_FK FOREIGN KEY (uploadedBy) REFERENCES userRecord(userId) ON DELETE SET NULL ON UPDATE CASCADE;

-- Reestablish keys for page
ALTER TABLE page CHANGE modifiedBy modifiedByBack varchar(255) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL NULL;
ALTER TABLE page ADD modifiedBy INT NULL  AFTER modifiedByBack;

UPDATE page p INNER JOIN userRecord ur ON p.modifiedByBack = ur.userName SET modifiedBy = ur.userId, validTS=validTS;

ALTER TABLE page DROP COLUMN modifiedByBack;
ALTER TABLE page ADD CONSTRAINT page_userRecord_FK FOREIGN KEY (modifiedBy) REFERENCES userRecord(userId) ON DELETE SET NULL ON UPDATE CASCADE;

-- Reestablish keys for userRole
ALTER TABLE userRole ADD userId INT NULL AFTER id;

UPDATE userRole r INNER JOIN userRecord ur ON r.userName = ur.userName SET r.userId = ur.userId;

ALTER TABLE userRole DROP INDEX FK_USER;
ALTER TABLE userRole DROP COLUMN userName;
ALTER TABLE userRole ADD CONSTRAINT userRole_userRecord_FK FOREIGN KEY (userId) REFERENCES userRecord(userId) ON DELETE CASCADE ON UPDATE CASCADE;


-- These can just delete existing records first
-- Reestablish keys for pageLock
DELETE FROM pageLock;
ALTER TABLE pageLock DROP COLUMN owner;
ALTER TABLE pageLock ADD owner INT NOT NULL;
ALTER TABLE pageLock ADD CONSTRAINT pageLock_userRecord_FK FOREIGN KEY (owner) REFERENCES userRecord(userId) ON DELETE CASCADE ON UPDATE CASCADE;


-- Reestablish keys for verificationToken
DELETE FROM verificationToken;
ALTER TABLE verificationToken DROP COLUMN `user`;
ALTER TABLE verificationToken ADD `user` INT NULL AFTER id;
ALTER TABLE verificationToken ADD CONSTRAINT verificationToken_userRecord_FK FOREIGN KEY (`user`) REFERENCES userRecord(userId) ON DELETE CASCADE ON UPDATE CASCADE;

--Set keys for mediaHistory
ALTER TABLE mediaHistory CHANGE uploadedBy uploadedByBack varchar(150) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL;
ALTER TABLE mediaHistory ADD uploadedBy INT NULL AFTER fileName;

UPDATE mediaHistory mh INNER JOIN userRecord ur ON mh.uploadedByBack = ur.userName SET mh.uploadedBy = ur.userId;

ALTER TABLE mediaHistory ADD CONSTRAINT mediaHistory_userRecord_FK FOREIGN KEY (uploadedBy) REFERENCES userRecord(userId) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE mediaHistory DROP COLUMN uploadedByBack;


-- User new ActionTypeTable in media History
CREATE TABLE `activityType` (
  `activityTypeId` int(11) NOT NULL,
  `activityName` varchar(100) NOT NULL,
  `simpleName` varchar(100) DEFAULT NULL,
  `fullDesc` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`activityTypeId`),
  UNIQUE KEY `activityType_unique` (`activityName`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
INSERT INTO activityType (activityTypeId,activityName,simpleName,fullDesc) VALUES
	 (10,'createPage','Created','Create Page'),
	 (20,'modifyPage','Modified','Modify Page'),
	 (30,'deletePage','Deleted','Delete Page'),
	 (40,'movePage','Moved','Move Page'),
	 (50,'uploadMedia','Uploaded','Upload Media'),
	 (60,'replaceMedia','Replaced','Replace Media'),
	 (70,'deleteMedia','Deleted','Delete Media'),
	 (80,'moveMedia','Moved','Move Media'),
	 (90,'createUser','Created','Create User'),
	 (100,'deleteUser','Deleted','Delete User');
INSERT INTO activityType (activityTypeId,activityName,simpleName,fullDesc) VALUES
	 (110,'changeRoles','Changed','Change Roles'),
	 (120,'changeSettings','Changed','Change Settings');

ALTER TABLE mediaHistory CHANGE `action` actionBack varchar(100) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL;
ALTER TABLE mediaHistory ADD `action` INT NULL AFTER uploadedBy;

UPDATE mediaHistory SET action=50 WHERE actionBack='Uploaded';
UPDATE mediaHistory SET action=60 WHERE actionBack='Replaced';
UPDATE mediaHistory SET action=70 WHERE actionBack='Deleted';

ALTER TABLE mediaHistory DROP FOREIGN KEY mediaHistory_enumAction_FK;
ALTER TABLE mediaHistory DROP COLUMN actionBack;
ALTER TABLE mediaHistory ADD CONSTRAINT mediaHistory_activityType_FK FOREIGN KEY (`action`) REFERENCES activityType(activityTypeId) ON DELETE RESTRICT ON UPDATE CASCADE;

DROP TABLE enumAction;

