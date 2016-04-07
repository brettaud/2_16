ALTER TABLE `account` MODIFY `BROKER` ENUM('IB','JPM','DC','RBS','RT','LMAX','FXCM','CNX','FTX') DEFAULT NULL;
ALTER TABLE `account` MODIFY `ORDER_SERVICE_TYPE` ENUM('IB_NATIVE','IB_FIX','JPM_FIX','DC_FIX','RT_FIX','LMAX_FIX','FXCM_FIX','CNX_FIX', 'FTX_FIX') NOT NULL;
ALTER TABLE `bar` MODIFY `FEED_TYPE` ENUM('IB','BB','DC','LMAX','FXCM','CNX','FTX') DEFAULT NULL;
ALTER TABLE `broker_parameters` MODIFY `BROKER` ENUM('IB','JPM','DC','RBS','RT','LMAX','FXCM','CNX','FTX') NOT NULL;
ALTER TABLE `generic_tick` MODIFY `FEED_TYPE` ENUM('IB','BB','DC','LMAX','FXCM','CNX','FTX') NOT NULL;
ALTER TABLE `easy_to_borrow` MODIFY `BROKER` ENUM('IB','JPM','DC','RBS','RT','LMAX','FXCM','CNX','FTX') NOT NULL;
ALTER TABLE `subscription` MODIFY `FEED_TYPE` ENUM('IB','BB','DC','LMAX','FXCM','CNX','FTX') NOT NULL;
ALTER TABLE `tick` MODIFY `FEED_TYPE` ENUM('IB','BB','DC','LMAX','FXCM','CNX','FTX') DEFAULT NULL;

ALTER TABLE `order` CHANGE COLUMN `EXT_ID` `EXT_ID` VARCHAR(100) NULL DEFAULT NULL;
ALTER TABLE `transaction` CHANGE COLUMN `EXT_ID` `EXT_ID` VARCHAR(100) NULL DEFAULT NULL;