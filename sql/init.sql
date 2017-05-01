
CREATE DATABASE IF NOT EXISTS `distributed_test0`;
USE `distributed_test0`;


CREATE TABLE IF NOT EXISTS `distribute_job` (
  `id` varchar(36) NOT NULL DEFAULT '',
  `state` varchar(36) NOT NULL DEFAULT '',
  `last_modify_timestamp` BIGINT(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE IF NOT EXISTS `undo_job` (
  `id` varchar(36) NOT NULL DEFAULT '',
  `distribute_job_id` varchar(36) NOT NULL DEFAULT '',
  `state` varchar(36) NOT NULL DEFAULT '',
  `content` varbinary(2048) NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `distribute_job_id` (`distribute_job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE IF NOT EXISTS `user` (
  `id` varchar(36) NOT NULL DEFAULT '',
  `balance` bigint(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE DATABASE IF NOT EXISTS `distributed_test1`;
USE `distributed_test1`;


CREATE TABLE IF NOT EXISTS `distribute_job` (
  `id` varchar(36) NOT NULL DEFAULT '',
  `state` varchar(36) NOT NULL DEFAULT '',
  `last_modify_timestamp` BIGINT(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE IF NOT EXISTS `undo_job` (
  `id` varchar(36) NOT NULL DEFAULT '',
  `distribute_job_id` varchar(36) NOT NULL DEFAULT '',
  `state` varchar(36) NOT NULL DEFAULT '',
  `content` varbinary(2048) NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `distribute_job_id` (`distribute_job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS `user` (
  `id` varchar(36) NOT NULL DEFAULT '',
  `balance` bigint(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



TRUNCATE distributed_test0.user;
TRUNCATE distributed_test1.user;
TRUNCATE distributed_test0.distribute_job;
TRUNCATE distributed_test1.distribute_job;
TRUNCATE distributed_test0.undo_job;
TRUNCATE distributed_test1.undo_job;

insert into distributed_test1.user (id,balance) values (1, 1000000);
insert into distributed_test0.user (id,balance) values (2, 1000000);

