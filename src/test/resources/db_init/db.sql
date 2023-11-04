--DROP TABLE IF EXISTS page;
--DROP TABLE IF EXISTS page_ids;

CREATE TABLE `page_ids` (
	`id` NUMBER(11) NOT NULL AUTO_INCREMENT,
	PRIMARY KEY (`id`)
);

CREATE TABLE `page` (
	`id` NUMBER(11) NOT NULL,
	`revision` NUMBER(11) NOT NULL,
	`site` VARCHAR(50) NOT NULL,
	`namespace` VARCHAR(50) NOT NULL DEFAULT '',
	`pagename` VARCHAR(200) NOT NULL,
	`text` TEXT NULL DEFAULT NULL,
	`title` VARCHAR(200) NULL DEFAULT NULL,
	`modified` DATETIME NULL DEFAULT current_timestamp(),
	`validTS` DATETIME NULL DEFAULT '9999-12-31 00:00:00' ON UPDATE current_timestamp(),
	`modifiedBy` VARCHAR(255) NULL DEFAULT NULL,
	`deleted` NUMBER(11) NOT NULL DEFAULT '0',
	PRIMARY KEY (`id`, `revision`),
	UNIQUE (`site`, `namespace`, `pagename`, `validTS`),
	CONSTRAINT `FK_ID` FOREIGN KEY (`id`) REFERENCES `page_ids` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- Create 3 ids
insert into page_ids () VALUES ();
insert into page_ids () VALUES ();
insert into page_ids () VALUES ();

insert into page (id, revision, site, namespace, pagename, text, validTS, deleted) VALUES(1, 1, 'site1', 'ns', 'page1', 'old text', '2023-11-04 00:00:00', 0);
insert into page (id, revision, site, namespace, pagename, text, deleted) VALUES(1, 2, 'site1', 'ns', 'page1', 'some text', 0);

select * from page;