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
	PRIMARY KEY (`name`) USING BTREE,
	UNIQUE INDEX `HostnameIdx` (`hostname`) USING BTREE
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

INSERT INTO `sites` (`name`, `hostname`) VALUES ('default', '*');

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
	`modifiedBy` VARCHAR(255) NULL DEFAULT NULL COLLATE 'latin1_swedish_ci',
	`deleted` INT(11) NOT NULL DEFAULT '0',
	PRIMARY KEY (`id`, `revision`) USING BTREE,
	UNIQUE INDEX `Uniqueness` (`site`, `namespace`, `pagename`, `validTS`) USING BTREE,
	CONSTRAINT `FK_ID` FOREIGN KEY (`id`) REFERENCES `lazerwiki`.`page_ids` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
   	CONSTRAINT `FK_MODIFIEDBY` FOREIGN KEY (`modifiedBy`) REFERENCES `lazerwiki`.`userRecord` (`userName`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  	CONSTRAINT `FK_SITE` FOREIGN KEY (`site`) REFERENCES `lazerwiki`.`sites` (`name`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

CREATE TABLE `userRecord` (
	`userName` VARCHAR(150) NOT NULL COLLATE 'latin1_swedish_ci',
	`passwordHash` VARCHAR(75) NOT NULL COLLATE 'latin1_swedish_ci',
	PRIMARY KEY (`userName`) USING BTREE
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;


CREATE TABLE `userRole` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	`userName` VARCHAR(150) NOT NULL COLLATE 'latin1_swedish_ci',
	`role` VARCHAR(10) NOT NULL COLLATE 'latin1_swedish_ci',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `FK_USER` (`userName`) USING BTREE,
	CONSTRAINT `FK_USER` FOREIGN KEY (`userName`) REFERENCES `lazerwiki`.`userRecord` (`userName`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;


CREATE TABLE `mediaRecord` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	`fileName` VARCHAR(150) NOT NULL COLLATE 'latin1_swedish_ci',
	`site` VARCHAR(50) NOT NULL COLLATE 'latin1_swedish_ci',
	`uploadedBy` VARCHAR(150) NOT NULL COLLATE 'latin1_swedish_ci',
	`fileSize` INT(11) NOT NULL DEFAULT '0',
	`height` INT(11) NOT NULL DEFAULT '0',
	`width` INT(11) NOT NULL DEFAULT '0',
	PRIMARY KEY (`id`) USING BTREE,
	UNIQUE INDEX `UNQ_mr` (`fileName`, `site`) USING BTREE,
	INDEX `FKmr_site` (`site`) USING BTREE,
	INDEX `FKmr_uploadedBy` (`uploadedBy`) USING BTREE,
	CONSTRAINT `FKmr_site` FOREIGN KEY (`site`) REFERENCES `lazerwiki`.`sites` (`name`) ON UPDATE RESTRICT ON DELETE RESTRICT,
	CONSTRAINT `FKmr_uploadedBy` FOREIGN KEY (`uploadedBy`) REFERENCES `lazerwiki`.`userRecord` (`userName`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
AUTO_INCREMENT=2
;
