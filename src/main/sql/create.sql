CREATE TABLE `page_ids` (
	`id` INT(11) NOT NULL AUTO_INCREMENT,
	PRIMARY KEY (`id`) USING BTREE
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
	`modifiedBy` VARCHAR(255) NULL DEFAULT NULL COLLATE 'latin1_swedish_ci',
	`deleted` INT(11) NOT NULL DEFAULT '0',
	PRIMARY KEY (`id`, `revision`) USING BTREE,
	UNIQUE INDEX `Uniqueness` (`site`, `namespace`, `pagename`, `validTS`) USING BTREE,
	CONSTRAINT `FK_ID` FOREIGN KEY (`id`) REFERENCES `page_ids` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
;

