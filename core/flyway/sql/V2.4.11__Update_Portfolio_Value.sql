ALTER TABLE `portfolio_value` MODIFY `NET_LIQ_VALUE` decimal(15,6) NOT NULL AFTER `DATE_TIME`;
ALTER TABLE `portfolio_value` CHANGE `SECURITIES_CURRENT_VALUE` `MARKET_VALUE` decimal(15,6) NOT NULL;
ALTER TABLE `portfolio_value` ADD `REALIZED_P_L` decimal(15,6) NOT NULL AFTER `MARKET_VALUE`;
ALTER TABLE `portfolio_value` ADD `UNREALIZED_P_L` decimal(15,6) NOT NULL AFTER `REALIZED_P_L`;
ALTER TABLE `portfolio_value` ADD `OPEN_POSITIONS` int(11) NOT NULL AFTER `CASH_BALANCE`;