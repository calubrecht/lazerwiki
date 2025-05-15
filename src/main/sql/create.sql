CREATE TABLE `page_ids` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	PRIMARY KEY (`id`) USING BTREE
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

CREATE TABLE `sites` (
	`name` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`hostname` VARCHAR(200) NOT NULL COLLATE 'latin1_swedish_ci',
	`siteName` VARCHAR(200) NULL DEFAULT NULL COLLATE 'latin1_swedish_ci',
	`settings` JSON NOT NULL DEFAULT '{}' COLLATE 'utf8mb4_bin',
	PRIMARY KEY (`name`) USING BTREE,

	UNIQUE INDEX `HostnameIdx` (`hostname`) USING BTREE,
	UNIQUE INDEX `SitenameIdx` (`siteName`) USING BTREE,
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

INSERT INTO `sites` (`name`, `hostname`) VALUES ('default', '*');

CREATE TABLE `userRecord` (
	`userId` INT(11) NOT NULL AUTO_INCREMENT,
	`userName` VARCHAR(150) NOT NULL COLLATE 'latin1_swedish_ci',
	`passwordHash` VARCHAR(75) NOT NULL COLLATE 'latin1_swedish_ci',
	`settings` JSON NOT NULL DEFAULT '{}',
	PRIMARY KEY (`userId`) USING BTREE,
  UNIQUE KEY `userRecord_userName_IDX` (`userName`) USING BTREE
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

CREATE TABLE `userRole` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
  `userId` INT(11),
	`role` VARCHAR(150) NOT NULL COLLATE 'latin1_swedish_ci',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `FK_USER` (`userName`) USING BTREE,
  CONSTRAINT userRole_userRecord_FK FOREIGN KEY (userId) REFERENCES userRecord(userId) ON DELETE CASCADE ON UPDATE CASCADE;
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

CREATE TABLE `page` (
	`id` INT(11) NOT NULL,
	`revision` INT(11) NOT NULL,
	`site` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`namespace` VARCHAR(50) NOT NULL DEFAULT '' COLLATE 'latin1_swedish_ci',
	`pagename` VARCHAR(200) NOT NULL COLLATE 'latin1_swedish_ci',
	`text` TEXT NULL DEFAULT NULL COLLATE 'latin1_swedish_ci',
	`title` VARCHAR(200) NULL DEFAULT NULL COLLATE 'latin1_swedish_ci',
	`modified` DATETIME NULL DEFAULT current_timestamp(),
	`validTS` DATETIME NULL DEFAULT '9999-12-31 00:00:00' ON UPDATE current_timestamp(),
  `modifiedBy` INT NULL,
	`deleted` INT(11) NOT NULL DEFAULT '0',
	PRIMARY KEY (`id`, `revision`) USING BTREE,
	UNIQUE INDEX `Uniqueness` (`site`, `namespace`, `pagename`, `validTS`) USING BTREE,
	CONSTRAINT `FK_ID` FOREIGN KEY (`id`) REFERENCES `page_ids` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT page_userRecord_FK FOREIGN KEY (modifiedBy) REFERENCES userRecord(userId) ON DELETE SET NULL ON UPDATE CASCADE;
  CONSTRAINT `FK_SITE` FOREIGN KEY (`site`) REFERENCES sites (`name`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

CREATE TABLE `pageLock` (
 	`site` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
 	`namespace` VARCHAR(50) NOT NULL DEFAULT '' COLLATE 'latin1_swedish_ci',
 	`pagename` VARCHAR(200) NOT NULL COLLATE 'latin1_swedish_ci',
 	`lockTime` DATETIME NOT NULL,
 	`owner` INT(11) NOT NULL,
 	`lockId` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
 	PRIMARY KEY (`site`, `namespace`, `pagename`) USING BTREE,
 	UNIQUE INDEX `Uniqueness` (`site`, `namespace`, `pagename`) USING BTREE,
 	INDEX `pageLockId` (`lockId`) USING BTREE,
  CONSTRAINT pageLock_userRecord_FK FOREIGN KEY (owner) REFERENCES userRecord(userId) ON DELETE CASCADE ON UPDATE CASCADE;
  CONSTRAINT `FK_LOCK_SITE` FOREIGN KEY (`site`) REFERENCES `sites` (`name`) ON UPDATE RESTRICT ON DELETE RESTRICT
 )
 COLLATE='latin1_swedish_ci'
 ENGINE=InnoDB
 ;

CREATE TABLE `mediaRecord` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	`fileName` VARCHAR(150) NOT NULL COLLATE 'latin1_swedish_ci',
    `namespace` varchar(50) NOT NULL DEFAULT '',
	`site` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`uploadedBy` INT(11) NULL,
	`fileSize` INT(11) NOT NULL DEFAULT '0',
	`height` INT(11) NOT NULL DEFAULT '0',
	`width` INT(11) NOT NULL DEFAULT '0',
	PRIMARY KEY (`id`) USING BTREE,
	UNIQUE INDEX `UNQ_mr` (`fileName`, `site`,`namespace`) USING BTREE,
	INDEX `FKmr_site` (`site`) USING BTREE,
	INDEX `FKmr_uploadedBy` (`uploadedBy`) USING BTREE,
	CONSTRAINT `FKmr_site` FOREIGN KEY (`site`) REFERENCES `sites` (`name`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT mediaRecord_userRecord_FK FOREIGN KEY (uploadedBy) REFERENCES userRecord(userId) ON DELETE SET NULL ON UPDATE CASCADE;
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

CREATE TABLE enumAction (
	`Action` varchar(100) NOT NULL,
	CONSTRAINT enumAction_pk PRIMARY KEY (`Action`)
)
ENGINE=InnoDB
DEFAULT CHARSET=latin1
COLLATE=latin1_swedish_ci;

INSERT INTO enumAction (Action) values ('Created');
INSERT INTO enumAction (Action) values ('Modified');
INSERT INTO enumAction (Action) values ('Uploaded');
INSERT INTO enumAction (Action) values ('Replaced');
INSERT INTO enumAction (Action) values ('Deleted');


CREATE TABLE mediaHistory (
	id INT UNSIGNED auto_increment NOT NULL,
	site varchar(50) NOT NULL,
	namespace VARCHAR(50) NOT NULL,
	fileName varchar(150) NOT NULL,
	uploadedBy varchar(150) NOT NULL,
	`action` varchar(100) NOT NULL,
	ts DATETIME DEFAULT current_timestamp() NOT NULL,
	INDEX mediaHistory_site_IDX (site,ts) USING BTREE,
	CONSTRAINT mediaHistory_pk PRIMARY KEY (id),
	CONSTRAINT mediaHistory_sites_FK FOREIGN KEY (site) REFERENCES sites(hostname) ON DELETE RESTRICT ON UPDATE RESTRICT,
	CONSTRAINT mediaHistory_enumAction_FK FOREIGN KEY (`action`) REFERENCES enumAction(`Action`) ON DELETE RESTRICT ON UPDATE RESTRICT

)
ENGINE=InnoDB
DEFAULT CHARSET=latin1
COLLATE=latin1_swedish_ci;



CREATE TABLE `ns_restriction_types` (
	`type` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	PRIMARY KEY (`type`) USING BTREE
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

INSERT INTO `ns_restriction_types` (`type`) VALUES
	('OPEN'),
	('READ_RESTRICTED'),
	('WRITE_RESTRICTED');


CREATE TABLE `namespace` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	`site` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`namespace` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`restriction_type` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	PRIMARY KEY (`id`) USING BTREE,
	UNIQUE INDEX `NS_UNIQUE` (`site`, `namespace`) USING BTREE,
	INDEX `NS_RESTRICTION_FK` (`restriction_type`) USING BTREE,
	CONSTRAINT `NS_RESTRICTION_FK` FOREIGN KEY (`restriction_type`) REFERENCES `ns_restriction_types` (`type`) ON UPDATE RESTRICT ON DELETE RESTRICT,
	CONSTRAINT `NS_SITE_FK` FOREIGN KEY (`site`) REFERENCES `sites` (`name`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

CREATE VIEW knownNamespaces as select distinct site, namespace from `page` union select distinct site, namespace from `mediarecord` union select distinct site, namespace AS `namespace` from `namespace`;

CREATE TABLE `tag` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	`pageId` INT(11) NOT NULL DEFAULT '0',
	`revision` INT(11) NOT NULL DEFAULT '0',
	`tag` VARCHAR(50) NOT NULL DEFAULT '0' COLLATE 'latin1_swedish_ci',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `tag_id` (`tag`) USING BTREE,
	INDEX `tag_id_revision_FK` (`pageId`, `revision`) USING BTREE,
	CONSTRAINT `tag_pageid_revision_FK` FOREIGN KEY (`pageId`, `revision`) REFERENCES `page` (`id`, `revision`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;


CREATE VIEW activeTags as select distinct `p`.`site` AS `site`,`t`.`tag` AS `tag` from (`tag` `t` join `page` `p` on(`p`.`id` = `t`.`pageId` and `p`.`revision` = `t`.`revision`)) where `p`.`deleted` = 0 and `p`.`validTS` = '9999-12-31 00:00:00';


CREATE TABLE `links` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	`site` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`sourcePageNS` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`sourcePageName` VARCHAR(200) NOT NULL COLLATE 'latin1_swedish_ci',
	`targetPageNS` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`targetPageName` VARCHAR(200) NOT NULL COLLATE 'latin1_swedish_ci',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `linkSourceKey` (`site`, `sourcePageNS`, `sourcePageName`) USING BTREE,
	INDEX `linkTargetKey` (`site`, `targetPageNS`, `targetPageName`) USING BTREE,
	CONSTRAINT `linksSiteFK` FOREIGN KEY (`site`) REFERENCES `sites` (`name`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

CREATE TABLE `linkOverrides` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	`site` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`sourcePageNS` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`sourcePageName` VARCHAR(200) NOT NULL COLLATE 'latin1_swedish_ci',
	`targetPageNS` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`targetPageName` VARCHAR(200) NOT NULL COLLATE 'latin1_swedish_ci',
	`newTargetPageNS` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
    `newTargetPageName` VARCHAR(200) NOT NULL COLLATE 'latin1_swedish_ci',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `linkOWSourceKey` (`site`, `sourcePageNS`, `sourcePageName`) USING BTREE,
	INDEX `linkOWTargetKey` (`site`, `targetPageNS`, `targetPageName`) USING BTREE,
	CONSTRAINT `linksOWSiteFK` FOREIGN KEY (`site`) REFERENCES `sites` (`name`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

CREATE TABLE `imageRefs` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	`site` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`sourcePageNS` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`sourcePageName` VARCHAR(200) NOT NULL COLLATE 'latin1_swedish_ci',
	`imageNS` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`imageRef` VARCHAR(200) NOT NULL COLLATE 'latin1_swedish_ci',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `imageRefSourceKey` (`site`, `sourcePageNS`, `sourcePageName`) USING BTREE,
	CONSTRAINT `imageRefSiteFK` FOREIGN KEY (`site`) REFERENCES `sites` (`name`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

CREATE TABLE `pageCache` (
	`site` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`namespace` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`pageName` VARCHAR(200) NOT NULL COLLATE 'latin1_swedish_ci',
	`title` VARCHAR(200) NULL DEFAULT NULL COLLATE 'latin1_swedish_ci',
	`renderedCache` LONGTEXT NOT NULL COLLATE 'latin1_swedish_ci',
	`plaintextCache` LONGTEXT NOT NULL COLLATE 'latin1_swedish_ci',
	`source` LONGTEXT NULL DEFAULT NULL COLLATE 'latin1_swedish_ci',
	`useCache` BIT(1) NOT NULL DEFAULT b'1',
	PRIMARY KEY (`site`, `namespace`, `pageName`) USING BTREE,
	FULLTEXT INDEX `PageCachePlaintextSearch` (`plaintextCache`),
	FULLTEXT INDEX `PageCachePageNameSearch` (`title`, `pageName`),
	CONSTRAINT `FK_pageCacheSite` FOREIGN KEY (`site`) REFERENCES `sites` (`name`) ON UPDATE RESTRICT ON DELETE RESTRICT,
	CONSTRAINT `FK_pageCache_page` FOREIGN KEY (`site`, `namespace`, `pageName`) REFERENCES `page` (`site`, `namespace`, `pagename`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;


CREATE TABLE `globalSettings` (
	`id` int NOT NULL ,
	`settings` JSON NOT NULL DEFAULT '{}' COLLATE 'utf8mb4_bin',
	PRIMARY KEY (`id`) USING BTREE
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

INSERT INTO `globalSettings` (`id`) VALUES (1);

CREATE TABLE `verificationToken` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `user` INT NULL,
  `token` varchar(10) NOT NULL COLLATE 'latin1_swedish_ci',
  `purpose` enum('VERIFY_EMAIL','RESET_PASSWORD') NOT NULL,
  `data` varchar(100) DEFAULT NULL,
  `expiry` datetime NOT NULL DEFAULT (current_timestamp() + interval 15 minute),
  PRIMARY KEY (`id`),
  KEY `verificationToken_userRecord_FK` (`user`),
  CONSTRAINT verificationToken_userRecord_FK FOREIGN KEY (`user`) REFERENCES userRecord(userId) ON DELETE CASCADE ON UPDATE CASCADE;
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
