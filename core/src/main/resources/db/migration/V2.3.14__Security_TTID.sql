ALTER TABLE `security`
  ADD `TTID` varchar(255) DEFAULT NULL AFTER `LMAXID`,
  ADD UNIQUE KEY `LMAXID_UNIQUE` (`LMAXID`),
  ADD UNIQUE KEY `TTID_UNIQUE` (`TTID`);