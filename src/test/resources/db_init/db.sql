--DROP TABLE IF EXISTS page;
--DROP TABLE IF EXISTS page_ids;

CREATE TABLE `page_ids` (
	`id` NUMBER(11) NOT NULL AUTO_INCREMENT,
	PRIMARY KEY (`id`)
);

CREATE TABLE `sites` (
	`name` VARCHAR(50) NOT NULL,
	`hostname` VARCHAR(200) NOT NULL,
	`siteName` VARCHAR(200) NULL DEFAULT NULL,
	`settings` VARCHAR(200) NOT NULL DEFAULT '{}',
	PRIMARY KEY (`name`),
	UNIQUE (`hostname`)
);

INSERT INTO `sites` (`name`, `hostname`) VALUES ('default', '*');
INSERT INTO `sites` (`name`, `hostname`) VALUES ('site1', 'site1.com');

CREATE TABLE userRecord (
   userId NUMBER(11) NOT NULL  AUTO_INCREMENT,
   userName VARCHAR(150) NOT NULL,
   passwordHash VARCHAR(50) NOT NULL,
   `settings` VARCHAR(200) NOT NULL DEFAULT '{}',
   PRIMARY KEY (`userId`)
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
	`modifiedBy` NUMBER(11) NULL DEFAULT NULL,
	`deleted` NUMBER(11) NOT NULL DEFAULT '0',
	PRIMARY KEY (`id`, `revision`),
	UNIQUE (`site`, `namespace`, `pagename`, `validTS`),
	CONSTRAINT `FK_ID` FOREIGN KEY (`id`) REFERENCES `page_ids` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
	CONSTRAINT `FK_USERID` FOREIGN KEY (`modifiedBy`) REFERENCES `userRecord` (`userId`) ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE TABLE userRole (
  `id` NUMBER(11) NOT NULL AUTO_INCREMENT,
  userId NUMBER(11) NOT NULL,
  role VARCHAR(10) NOT NULL,
  	PRIMARY KEY (`id`),
  	CONSTRAINT `FK_USER` FOREIGN KEY (`userId`) REFERENCES userRecord (`userId`) ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- Create 3 ids
insert into page_ids () VALUES ();
insert into page_ids () VALUES ();
insert into page_ids () VALUES ();
insert into page_ids () VALUES ();
insert into page_ids () VALUES ();
insert into page_ids () VALUES ();
insert into page_ids () VALUES ();

insert into userRecord (userId, userName, passwordHash) VALUES (1, 'Bob', '');

insert into page (id, revision, site, namespace, pagename, text, title, validTS, deleted) VALUES(1, 1, 'site1', 'ns', 'page1', 'old text', 'Page Title', '2023-11-04 00:00:00', 0);
insert into page (id, revision, site, namespace, pagename, text, title, deleted) VALUES(1, 2, 'site1', 'ns', 'page1', 'some text', 'Page Title', 0);
insert into page (id, revision, site, namespace, pagename, text, title, validTS, deleted) VALUES(3, 1, 'site1', 'ns', 'deletedPage', 'some text', 'Page Title', '2023-12-03 16:00:00',0);
insert into page (id, revision, site, namespace, pagename, text, title, deleted) VALUES(3, 2, 'site1', 'ns', 'deletedPage', '', '', 1);
insert into page (id, revision, site, namespace, pagename, text, title, deleted) VALUES(4, 1, 'site1', 'ns', 'page2', 'othertext', 'Page Title', 0);
insert into page (id, revision, site, namespace, pagename, text, title, deleted) VALUES(5, 1, 'site2', 'ns2', 'pagens2', '', '', 0);
insert into page (id, revision, site, namespace, pagename, text, title, deleted) VALUES(6, 1, 'site2', 'ns2', 'pagens2a', '', '', 0);
insert into page (id, revision, site, namespace, pagename, text, title, deleted) VALUES(7, 1, 'site2', 'ns5', 'pagens5', '', '', 0);

insert into userRole (userId, role) VALUES (1, 'Admin');
insert into userRole (userId, role) VALUES (1, 'User');

CREATE TABLE `links` (
	`id` NUMBER(11) NOT NULL AUTO_INCREMENT,
	`site` VARCHAR(50) NOT NULL ,
	`sourcePageNS` VARCHAR(50) NOT NULL,
	`sourcePageName` VARCHAR(200) NOT NULL,
    `targetPageNS` VARCHAR(50) NOT NULL,
    `targetPageName` VARCHAR(200) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX (`site`, `sourcePageNS`, `sourcePageName`) ,
	INDEX (`site`, `targetPageNS`, `targetPageName`),
	CONSTRAINT `linkSiteFK` FOREIGN KEY (`site`) REFERENCES `sites` (`name`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
;

insert into links (site, sourcePageNS, sourcePageName, targetPageNS, targetPageName) values ('default', '', '', '', 'page1');
insert into links (site, sourcePageNS, sourcePageName, targetPageNS, targetPageName) values ('default', '', '', '', 'page2');
insert into links (site, sourcePageNS, sourcePageName, targetPageNS, targetPageName) values ('site1', '', '', '', 'page3');
insert into links (site, sourcePageNS, sourcePageName, targetPageNS, targetPageName) values ('default', '', 'page1', '', 'page2');
insert into links (site, sourcePageNS, sourcePageName, targetPageNS, targetPageName) values ('default', '', 'page1', 'ns', 'nsPage');

CREATE TABLE `imageRefs` (
	`id` NUMBER(11) NOT NULL AUTO_INCREMENT,
	`site` VARCHAR(50) NOT NULL ,
	`sourcePageNS` VARCHAR(50) NOT NULL,
	`sourcePageName` VARCHAR(200) NOT NULL,
    `imageNS` VARCHAR(50) NOT NULL,
    `imageRef` VARCHAR(200) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX (`site`, `sourcePageNS`, `sourcePageName`),
	CONSTRAINT `imageRefSiteFK` FOREIGN KEY (`site`) REFERENCES `sites` (`name`) ON UPDATE RESTRICT ON DELETE RESTRICT
)
;

insert into imageRefs (site, sourcePageNS, sourcePageName, imageNS, imageRef) values ('site1', 'ns1', 'page1', '', 'image1.jpg');
insert into imageRefs (site, sourcePageNS, sourcePageName, imageNS, imageRef) values ('site1', 'ns1', 'page1', '', 'image2.jpg');
insert into imageRefs (site, sourcePageNS, sourcePageName, imageNS, imageRef) values ('site1', 'ns2', 'page2', '', 'image1.jpg');
insert into imageRefs (site, sourcePageNS, sourcePageName, imageNS, imageRef) values ('site1', 'ns1', 'page3', '', 'image3.jpg');
insert into imageRefs (site, sourcePageNS, sourcePageName, imageNS, imageRef) values ('site1', 'ns1', 'page4', '', 'image3.jpg');

CREATE TABLE mediaHistory (
	id NUMBER(11) NOT NULL AUTO_INCREMENT,
	site varchar(50) NOT NULL,
	namespace VARCHAR(50) NOT NULL,
	fileName varchar(150) NOT NULL,
	uploadedBy NUMBER(11) NOT NULL,
	`action` varchar(100) NOT NULL,
	ts DATETIME DEFAULT current_timestamp(),
	PRIMARY KEY (`id`),
	INDEX (site,ts)
)
;

insert into mediaHistory (site, namespace, fileName, uploadedBy, `action`) values ('site1', 'ns1', 'img1.jpg', 1, 'Uploaded');
insert into mediaHistory (site, namespace, fileName, uploadedBy, `action`) values ('site1', 'ns1', 'img1.jpg', 1, 'Replaced');
insert into mediaHistory (site, namespace, fileName, uploadedBy, `action`) values ('site2', 'ns3', 'img2.jpg', 1, 'Uploaded');
insert into mediaHistory (site, namespace, fileName, uploadedBy, `action`) values ('site1', 'ns4', 'img3.jpg', 1, 'Uploaded');

CREATE TABLE `globalSettings` (
	`id` NUMBER(11) NOT NULL ,
	`settings` VARCHAR(200) NOT NULL DEFAULT '{}',
	PRIMARY KEY (`id`)
)
;


INSERT INTO `globalSettings` (`id`, `settings`) VALUES (1, '{"Setting1": "Value1"}');

CREATE TABLE `verificationToken` (
  `id` NUMBER(10) NOT NULL ,
  `user` varchar(150) NOT NULL,
  `token` varchar(10) NOT NULL,
  `purpose` varchar(10) NOT NULL,
  `data` varchar(100) ,
  `expiry` DATETIME ,
  PRIMARY KEY (`id`)
) ;



CREATE TABLE `tag` (
	`id` NUMBER(11) NOT NULL,
	`pageId` NUMBER(11) NOT NULL DEFAULT '0',
	`revision` NUMBER(11) NOT NULL DEFAULT '0',
	`tag` VARCHAR(50) NOT NULL,
	PRIMARY KEY (`id`)
);

insert into tag (id, pageId, revision, tag) VALUES(1, 6, 1, 'bigTag');