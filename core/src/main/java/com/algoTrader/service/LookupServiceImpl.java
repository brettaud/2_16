package com.algoTrader.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.IllegalStateException;

import org.apache.commons.collections.map.MultiValueMap;
import org.springframework.beans.factory.annotation.Value;

import com.algoTrader.entity.Position;
import com.algoTrader.entity.PositionDao;
import com.algoTrader.entity.Strategy;
import com.algoTrader.entity.StrategyImpl;
import com.algoTrader.entity.Subscription;
import com.algoTrader.entity.Transaction;
import com.algoTrader.entity.TransactionDao;
import com.algoTrader.entity.marketData.Bar;
import com.algoTrader.entity.marketData.Tick;
import com.algoTrader.entity.security.Combination;
import com.algoTrader.entity.security.Component;
import com.algoTrader.entity.security.Future;
import com.algoTrader.entity.security.FutureFamily;
import com.algoTrader.entity.security.Security;
import com.algoTrader.entity.security.SecurityFamily;
import com.algoTrader.entity.security.StockOption;
import com.algoTrader.entity.security.StockOptionFamily;
import com.algoTrader.entity.strategy.CashBalance;
import com.algoTrader.entity.strategy.DefaultOrderPreference;
import com.algoTrader.entity.strategy.Measurement;
import com.algoTrader.entity.strategy.OrderPreference;
import com.algoTrader.entity.trade.Order;
import com.algoTrader.enumeration.Currency;
import com.algoTrader.enumeration.OptionType;
import com.algoTrader.util.DateUtil;
import com.algoTrader.util.HibernateUtil;
import com.algoTrader.util.PositionUtil;
import com.algoTrader.util.metric.MetricsUtil;
import com.algoTrader.vo.OrderStatusVO;
import com.algoTrader.vo.PositionVO;
import com.algoTrader.vo.RawBarVO;
import com.algoTrader.vo.RawTickVO;
import com.algoTrader.vo.TransactionVO;

@SuppressWarnings("unchecked")
public class LookupServiceImpl extends LookupServiceBase {

    private @Value("${simulation}") boolean simulation;
    private @Value("${statement.simulateStockOptions}") boolean simulateStockOptions;
    private @Value("${statement.simulateFuturesByUnderlying}") boolean simulateFuturesByUnderlying;
    private @Value("${statement.simulateFuturesByGenericFutures}") boolean simulateFuturesByGenericFutures;
    private @Value("${misc.transactionDisplayCount}") int transactionDisplayCount;
    private @Value("${misc.intervalDays}") int intervalDays;

    @Override
    protected Collection<Security> handleGetAllSecurities() throws Exception {

        return getSecurityDao().loadAll();
    }

    @Override
    protected Security handleGetSecurity(int id) throws java.lang.Exception {

        return getSecurityDao().get(id);
    }

    @Override
    protected Security handleGetSecurityByIsin(String isin) throws Exception {

        return getSecurityDao().findByIsin(isin);
    }

    @Override
    protected Security handleGetSecurityInclFamilyAndUnderlying(int securityId) throws Exception {

        return getSecurityDao().findByIdInclFamilyAndUnderlying(securityId);
    }

    @Override
    protected Security handleGetSecurityInitialized(int id) throws java.lang.Exception {

        Security security = getSecurityDao().get(id);

        // initialize the security
        security.initialize();

        return security;
    }

    @Override
    protected Security handleGetSecurityInclComponentsInitialized(int id) throws java.lang.Exception {

        Security security = getSecurityDao().get(id);

        if (security != null) {

            // initialize the security
            security.initialize();

            // initialize components
            security.getComponentsInitialized();
        }

        return security;
    }

    @Override
    protected List<Security> handleGetSecuritiesByIds(Collection<Integer> ids) throws Exception {

        return getSecurityDao().findByIds(ids);
    }

    @Override
    protected Collection<Security> handleGetSubscribedSecuritiesByStrategyAndComponent(String strategyName, int securityId) throws Exception {

        return getSecurityDao().findSubscribedByStrategyAndComponent(strategyName, securityId);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Collection<Security> handleGetSubscribedSecuritiesByStrategyAndComponentClass(String strategyName, final Class type) throws Exception {

        int discriminator = HibernateUtil.getDisriminatorValue(getSessionFactory(), type);
        return getSecurityDao().findSubscribedByStrategyAndComponentClass(strategyName, discriminator);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Collection<Security> handleGetSubscribedSecuritiesByStrategyAndComponentClassWithZeroQty(String strategyName, final Class type) throws Exception {

        int discriminator = HibernateUtil.getDisriminatorValue(getSessionFactory(), type);
        return getSecurityDao().findSubscribedByStrategyAndComponentClassWithZeroQty(strategyName, discriminator);
    }

    @Override
    protected List<Security> handleGetSubscribedSecuritiesForAutoActivateStrategiesInclFamily() throws Exception {

        return getSecurityDao().findSubscribedForAutoActivateStrategiesInclFamily();
    }

    @Override
    protected StockOption handleGetStockOptionByMinExpirationAndMinStrikeDistance(int underlyingId, Date targetExpirationDate, BigDecimal underlyingSpot,
            OptionType optionType) throws Exception {


        List<StockOption> list = getStockOptionDao().findByMinExpirationAndMinStrikeDistance(1, 1, underlyingId, targetExpirationDate, underlyingSpot, optionType);

        StockOption stockOption = null;
        if (!list.isEmpty()) {
            stockOption = list.get(0);
        }

        // if no future was found, create it if simulating options
        if (this.simulation && this.simulateStockOptions) {

            StockOptionFamily family = getStockOptionFamilyDao().findByUnderlying(underlyingId);
            if ((stockOption == null) || Math.abs(stockOption.getStrike().doubleValue() - underlyingSpot.doubleValue()) > family.getStrikeDistance()) {

                stockOption = getStockOptionService().createDummyStockOption(family.getId(), targetExpirationDate, underlyingSpot, optionType);
            }
        }

        if (stockOption == null) {
            throw new LookupServiceException("no stockOption available for expiration " + targetExpirationDate + " strike " + underlyingSpot + " type " + optionType);
        } else {
            return stockOption;
        }
    }

    @Override
    protected StockOption handleGetStockOptionByMinExpirationAndMinStrikeDistanceWithTicks(int underlyingId, Date targetExpirationDate,
            BigDecimal underlyingSpot, OptionType optionType, Date date) throws Exception {

        List<StockOption> list = getStockOptionDao().findByMinExpirationAndMinStrikeDistanceWithTicks(1, 1, underlyingId, targetExpirationDate, underlyingSpot, optionType, date);

        if (!list.isEmpty()) {
            return list.get(0);
        } else {
            return null;
        }
    }

    @Override
    protected StockOption handleGetStockOptionByMinExpirationAndStrikeLimit(int underlyingId, Date targetExpirationDate, BigDecimal underlyingSpot,
            OptionType optionType)
            throws Exception {

        StockOptionFamily family = getStockOptionFamilyDao().findByUnderlying(underlyingId);

        List<StockOption> list = getStockOptionDao().findByMinExpirationAndStrikeLimit(1, 1, underlyingId, targetExpirationDate, underlyingSpot, optionType);

        StockOption stockOption = null;
        if (!list.isEmpty()) {
            stockOption = list.get(0);
        }

        // if no future was found, create it if simulating options
        if (this.simulation && this.simulateStockOptions) {
            if ((stockOption == null) || Math.abs(stockOption.getStrike().doubleValue() - underlyingSpot.doubleValue()) > family.getStrikeDistance()) {

                stockOption = getStockOptionService().createDummyStockOption(family.getId(), targetExpirationDate, underlyingSpot, optionType);
            }
        }

        if (stockOption == null) {
            throw new LookupServiceException("no stockOption available for expiration " + targetExpirationDate + " strike " + underlyingSpot + " type "
                    + optionType);
        } else {
            return stockOption;
        }
    }

    @Override
    protected StockOption handleGetStockOptionByMinExpirationAndStrikeLimitWithTicks(int underlyingId, Date targetExpirationDate, BigDecimal underlyingSpot,
            OptionType optionType,
            Date date) throws Exception {

        List<StockOption> list = getStockOptionDao().findByMinExpirationAndStrikeLimitWithTicks(1, 1, underlyingId, targetExpirationDate, underlyingSpot, optionType, date);

        if (!list.isEmpty()) {
            return list.get(0);
        } else {
            return null;
        }
    }


    @Override
    protected List<StockOption> handleGetSubscribedStockOptions() throws Exception {

        return getStockOptionDao().findSubscribedStockOptions();
    }

    @Override
    protected Future handleGetFutureByMinExpiration(int futureFamilyId, Date expirationDate) throws Exception {

        List<Future> list = getFutureDao().findByMinExpiration(1, 1, futureFamilyId, expirationDate);

        Future future = null;
        if (!list.isEmpty()) {
            future = list.get(0);
        }

        // if no future was found, create the missing part of the future-chain
        if (this.simulation && future == null && (this.simulateFuturesByUnderlying || this.simulateFuturesByGenericFutures)) {

            getFutureService().createDummyFutures(futureFamilyId);

            list = getFutureDao().findByMinExpiration(1, 1, futureFamilyId, expirationDate);

            if (!list.isEmpty()) {
                future = list.get(0);
            }
        }

        if (future == null) {
            throw new LookupServiceException("no future available for expiration " + expirationDate);
        } else {
            return future;
        }

    }

    @Override
    protected Future handleGetFutureByExpiration(int futureFamilyId, Date expirationDate) throws Exception {

        Future future = getFutureDao().findByExpirationInclSecurityFamily(futureFamilyId, expirationDate);

        // if no future was found, create the missing part of the future-chain
        if (this.simulation && future == null && (this.simulateFuturesByUnderlying || this.simulateFuturesByGenericFutures)) {

            getFutureService().createDummyFutures(futureFamilyId);
            future = getFutureDao().findByExpirationInclSecurityFamily(futureFamilyId, expirationDate);
        }

        if (future == null) {
            throw new LookupServiceException("no future available targetExpiration " + expirationDate);
        } else {

            return future;
        }
    }

    @Override
    protected Future handleGetFutureByDuration(int futureFamilyId, Date targetExpirationDate, int duration) throws Exception {

        FutureFamily futureFamily = getFutureFamilyDao().get(futureFamilyId);

        Date expirationDate = DateUtil.getExpirationDateNMonths(futureFamily.getExpirationType(), targetExpirationDate, duration);
        Future future = getFutureDao().findByExpirationInclSecurityFamily(futureFamilyId, expirationDate);

        // if no future was found, create the missing part of the future-chain
        if (this.simulation && future == null && (this.simulateFuturesByUnderlying || this.simulateFuturesByGenericFutures)) {

            getFutureService().createDummyFutures(futureFamily.getId());
            future = getFutureDao().findByExpirationInclSecurityFamily(futureFamilyId, expirationDate);
        }

        if (future == null) {
            throw new LookupServiceException("no future available targetExpiration " + targetExpirationDate + " and duration " + duration);
        } else {
            return future;
        }
    }

    @Override
    protected List<Future> handleGetFuturesByMinExpiration(int futureFamilyId, Date minExpirationDate) throws Exception {

        return getFutureDao().findByMinExpiration(futureFamilyId, minExpirationDate);
    }

    @Override
    protected List<Future> handleGetSubscribedFutures() throws Exception {

        return getFutureDao().findSubscribedFutures();
    }

    @Override
    protected Subscription handleGetSubscription(String strategyName, int securityId) throws Exception {

        return getSubscriptionDao().findByStrategyAndSecurity(strategyName, securityId);
    }

    @Override
    protected List<Subscription> handleGetSubscriptionsByStrategyInclComponents(String strategyName) throws Exception {

        List<Subscription> subscriptions = getSubscriptionDao().findByStrategy(strategyName);

        // initialize components
        for (Subscription subscription : subscriptions) {
            subscription.getSecurity().getComponentsInitialized();
        }

        return subscriptions;
    }

    @Override
    protected List<Subscription> handleGetSubscriptionsForAutoActivateStrategiesInclComponents() throws Exception {

        List<Subscription> subscriptions = getSubscriptionDao().findForAutoActivateStrategies();

        // initialize components
        for (Subscription subscription : subscriptions) {
            subscription.getSecurity().getComponentsInitialized();
        }

        return subscriptions;
    }

    @Override
    protected Collection<Subscription> handleGetNonPositionSubscriptions(String strategyName) throws Exception {

        return getSubscriptionDao().findNonPositionSubscriptions(strategyName);
    }

    @Override
    protected Collection<Strategy> handleGetAllStrategies() throws Exception {

        return getStrategyDao().loadAll();
    }

    @Override
    protected Strategy handleGetStrategy(int id) throws java.lang.Exception {

        return getStrategyDao().get(id);
    }

    @Override
    protected Strategy handleGetStrategyByName(String name) throws Exception {

        return getStrategyDao().findByName(name);
    }

    @Override
    protected List<Strategy> handleGetAutoActivateStrategies() throws Exception {

        return getStrategyDao().findAutoActivateStrategies();
    }

    @Override
    protected SecurityFamily handleGetSecurityFamily(int id) throws Exception {

        return getSecurityFamilyDao().get(id);
    }

    @Override
    protected StockOptionFamily handleGetStockOptionFamilyByUnderlying(int id) throws Exception {

        return getStockOptionFamilyDao().findByUnderlying(id);
    }

    @Override
    protected FutureFamily handleGetFutureFamilyByUnderlying(int id) throws Exception {

        return getFutureFamilyDao().findByUnderlying(id);
    }

    @Override
    protected Collection<Position> handleGetAllPositions() throws Exception {

        return getPositionDao().loadAll();
    }

    @Override
    protected Position handleGetPosition(int id) throws java.lang.Exception {

        return getPositionDao().get(id);
    }

    @Override
    protected Position handleGetPositionInclSecurityAndSecurityFamily(int id) throws Exception {

        return getPositionDao().findByIdInclSecurityAndSecurityFamily(id);
    }

    @Override
    protected List<Position> handleGetPositionsByStrategy(String strategyName) throws Exception {

        return getPositionDao().findByStrategy(strategyName);
    }

    @Override
    protected Position handleGetPositionBySecurityAndStrategy(int securityId, String strategyName) throws Exception {

        return getPositionDao().findBySecurityAndStrategy(securityId, strategyName);
    }

    @Override
    protected List<Position> handleGetOpenPositions() throws Exception {

        return getPositionDao().findOpenPositions();
    }

    @Override
    protected List<Position> handleGetOpenTradeablePositions() throws Exception {

        return getPositionDao().findOpenTradeablePositions();
    }

    @Override
    protected List<Position> handleGetOpenPositionsByStrategy(String strategyName) throws Exception {

        return getPositionDao().findOpenPositionsByStrategy(strategyName);
    }

    @Override
    protected List<Position> handleGetOpenTradeablePositionsByStrategy(String strategyName) throws Exception {

        return getPositionDao().findOpenTradeablePositionsByStrategy(strategyName);
    }

    @Override
    protected List<Position> handleGetOpenPositionsBySecurityId(int securityId) throws Exception {

        return getPositionDao().findOpenPositionsBySecurityId(securityId);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected List<Position> handleGetOpenPositionsByStrategyAndType(String strategyName, final Class type) throws Exception {

        int discriminator = HibernateUtil.getDisriminatorValue(getSessionFactory(), type);
        return getPositionDao().findOpenPositionsByStrategyAndType(strategyName, discriminator);
    }

    @Override
    protected List<Position> handleGetOpenPositionsByStrategyAndSecurityFamily(String strategyName, int securityFamilyId) throws Exception {

        return getPositionDao().findOpenPositionsByStrategyAndSecurityFamily(strategyName, securityFamilyId);
    }

    @Override
    protected List<Position> handleGetOpenFXPositions() throws Exception {

        return getPositionDao().findOpenFXPositions();
    }

    @Override
    protected List<Position> handleGetOpenFXPositionsByStrategy(String strategyName) throws Exception {

        return getPositionDao().findOpenFXPositionsByStrategy(strategyName);
    }

    @Override
    protected List<PositionVO> handleGetPositionsVO(String strategyName, boolean displayClosedPositions) throws Exception {

        if (strategyName.equals(StrategyImpl.BASE)) {
            if (displayClosedPositions) {
                return (List<PositionVO>) getPositionDao().loadAll(PositionDao.TRANSFORM_POSITIONVO);
            } else {
                return (List<PositionVO>) getPositionDao().findOpenPositions(PositionDao.TRANSFORM_POSITIONVO);
            }
        } else {
            if (displayClosedPositions) {
                return (List<PositionVO>) getPositionDao().findByStrategy(PositionDao.TRANSFORM_POSITIONVO, strategyName);
            } else {
                return (List<PositionVO>) getPositionDao().findOpenPositionsByStrategy(PositionDao.TRANSFORM_POSITIONVO, strategyName);
            }
        }
    }

    @Override
    protected double handleGetPositionMarketPriceByDate(Security security, Date date) throws Exception {

        List<Tick> ticks = getTickDao().findTicksAfterDate(1, 1, security.getId(), date, this.intervalDays);
        if (ticks.isEmpty()) {
            throw new IllegalStateException("not tick available for " + security);
        } else {
            Tick tick = ticks.get(0);
            return tick.getSettlement().doubleValue();
        }
    }

    @Override
    protected double handleGetPositionAveragePriceByDate(Security security, Date date) throws Exception {

        Collection<Transaction> transactions = getTransactionDao().findBySecurityAndDate(security.getId(), date);
        return PositionUtil.getAveragePrice(security, transactions, false);
    }

    @Override
    protected Collection<Transaction> handleGetAllTransactions() throws Exception {

        return getTransactionDao().loadAll();
    }

    @Override
    protected List<Transaction> handleGetAllTrades() throws Exception {

        return getTransactionDao().findAllTradesInclSecurity();
    }

    @Override
    protected List<Transaction> handleGetAllCashFlows() throws Exception {

        return getTransactionDao().findAllCashflows();
    }

    @Override
    protected Transaction handleGetTransaction(int id) throws java.lang.Exception {

        return getTransactionDao().get(id);
    }

    @Override
    protected List<TransactionVO> handleGetTransactionsVO(String strategyName) throws Exception {

        if (strategyName.equals(StrategyImpl.BASE)) {
            return (List<TransactionVO>) getTransactionDao().findTransactionsDesc(TransactionDao.TRANSFORM_TRANSACTIONVO, 1, this.transactionDisplayCount);
        } else {
            return (List<TransactionVO>) getTransactionDao().findTransactionsByStrategyDesc(TransactionDao.TRANSFORM_TRANSACTIONVO, 1, this.transactionDisplayCount, strategyName);
        }
    }

    @Override
    protected Order handleGetOpenOrderByIntId(int intId) throws Exception {

        return getOrderDao().findOpenOrderByIntId(intId);
    }

    @Override
    protected Collection<OrderStatusVO> handleGetOpenOrdersVO(String strategyName) throws Exception {

        if (strategyName.equals(StrategyImpl.BASE)) {
            return getOrderStatusDao().findAllOrderStati();
        } else {
            return getOrderStatusDao().findOrderStatiByStrategy(strategyName);
        }

    }

    @Override
    protected OrderPreference handleGetOrderPreferenceByName(String name) throws Exception {

        return getOrderPreferenceDao().findByName(name);
    }

    @Override
    protected OrderPreference handleGetOrderPreferenceByStrategyAndSecurityFamily(String strategyName, int securityFamilyId) throws Exception {

        DefaultOrderPreference orderPreference = getDefaultOrderPreferenceDao().findByStrategyAndSecurityFamilyInclOrderPreference(strategyName, securityFamilyId);
        if (orderPreference != null) {
            return orderPreference.getOrderPreference();
        } else {
            throw new IllegalStateException("no default order preference defined for securityFamilyId " + securityFamilyId + " and " + strategyName);
        }
    }

    @Override
    protected Tick handleGetLastTick(int securityId) throws Exception {

        List<Tick> list = getTickDao().findTicksBeforeDate(1, 1, securityId, DateUtil.getCurrentEPTime(), this.intervalDays);

        Tick tick = null;
        if (!list.isEmpty()) {
            tick = list.get(0);
            tick.getSecurity().initialize();
        }

        return tick;
    }

    @Override
    protected List<Tick> handleGetTicksBeforeDate(int securityId, Date maxDate) throws Exception {

        return getTickDao().findTicksBeforeDate(securityId, maxDate, this.intervalDays);
    }

    @Override
    protected List<Tick> handleGetTicksAfterDate(int securityId, Date minDate) throws Exception {

        return getTickDao().findTicksAfterDate(securityId, minDate, this.intervalDays);
    }

    @Override
    protected List<Tick> handleGetDailyTicksBeforeTime(int securityId, Date maxDate, Date time) {

        List<Integer> ids = getTickDao().findDailyTickIdsBeforeTime(securityId, maxDate, time);
        if (ids.size() > 0) {
            return getTickDao().findByIdsInclSecurityAndUnderlying(ids);
        } else {
            return new ArrayList<Tick>();
        }
    }

    @Override
    protected List<Tick> handleGetDailyTicksAfterTime(int securityId, Date maxDate, Date time) {

        List<Integer> ids = getTickDao().findDailyTickIdsAfterTime(securityId, maxDate, time);
        if (ids.size() > 0) {
            return getTickDao().findByIdsInclSecurityAndUnderlying(ids);
        } else {
            return new ArrayList<Tick>();
        }
    }

    @Override
    protected List<Bar> handleGetDailyBarsFromTicks(int securityId, Date fromDate, Date toDate) {

        return getTickDao().findDailyBars(securityId, fromDate, toDate);
    }

    @Override
    protected List<Tick> handleGetSubscribedTicksByTimePeriod(Date startDate, Date endDate) throws Exception {

        return getTickDao().findSubscribedByTimePeriod(startDate, endDate);
    }

    @Override
    protected Tick handleGetTickByDateAndSecurityInclSecurityInitialized(Date date, int securityId) {

        Tick tick = getTickDao().findByDateAndSecurity(date, securityId);

        // initialize the security
        if (tick != null) {
            tick.getSecurityInitialized().initialize();
        }

        return tick;
    }

    @Override
    protected double handleGetForexRateDouble(Currency baseCurrency, Currency transactionCurrency) throws Exception {

        return getForexDao().getRateDouble(baseCurrency, transactionCurrency);
    }

    @Override
    protected double handleGetForexRateDoubleByDate(Currency baseCurrency, Currency transactionCurrency, Date date) throws Exception {

        return getForexDao().getRateDoubleByDate(baseCurrency, transactionCurrency, date);
    }

    @Override
    protected Collection<Currency> handleGetHeldCurrencies() throws Exception {

        return getCashBalanceDao().findHeldCurrencies();
    }

    @Override
    protected Collection<CashBalance> handleGetCashBalancesByStrategy(String strategyName) throws Exception {

        return getCashBalanceDao().findCashBalancesByStrategy(strategyName);
    }

    @Override
    protected Collection<Combination> handleGetAllCombinations() throws Exception {

        return getCombinationDao().loadAll();
    }

    @Override
    protected Collection<Combination> handleGetSubscribedCombinationsByStrategy(String strategyName) throws Exception {

        return getCombinationDao().findSubscribedByStrategy(strategyName);
    }

    @Override
    protected Collection<Combination> handleGetSubscribedCombinationsByStrategyAndUnderlying(String strategyName, int underlyingId) throws Exception {

        return getCombinationDao().findSubscribedByStrategyAndUnderlying(strategyName, underlyingId);
    }

    @Override
    protected Collection<Component> handleGetSubscribedComponentsByStrategy(String strategyName) throws Exception {

        return getComponentDao().findSubscribedByStrategyInclSecurity(strategyName);
    }

    @Override
    protected Collection<Component> handleGetSubscribedComponentsBySecurity(int securityId) throws Exception {

        return getComponentDao().findSubscribedBySecurityInclSecurity(securityId);
    }

    @Override
    protected Collection<Component> handleGetSubscribedComponentsByStrategyAndSecurity(String strategyName, int securityId) throws Exception {

        return getComponentDao().findSubscribedByStrategyAndSecurityInclSecurity(strategyName, securityId);
    }

    @Override
    protected Map<Date, Object> handleGetMeasurementsBeforeDate(String strategyName, String name, Date date) throws Exception {

        List<Measurement> measurements = getMeasurementDao().findMeasurementsBeforeDate(strategyName, name, date);

        return getValuesByDate(measurements);
    }

    @Override
    protected Map<Date, Map<String, Object>> handleGetAllMeasurementsBeforeDate(String strategyName, Date date) throws Exception {

        List<Measurement> measurements = getMeasurementDao().findAllMeasurementsBeforeDate(strategyName, date);

        return getNameValuePairsByDate(measurements);
    }

    @Override
    protected Map<Date, Object> handleGetMeasurementsAfterDate(String strategyName, String name, Date date) throws Exception {

        List<Measurement> measurements = getMeasurementDao().findMeasurementsAfterDate(strategyName, name, date);

        return getValuesByDate(measurements);
    }

    @Override
    protected Map<Date, Map<String, Object>> handleGetAllMeasurementsAfterDate(String strategyName, Date date) throws Exception {

        List<Measurement> measurements = getMeasurementDao().findAllMeasurementsAfterDate(strategyName, date);

        return getNameValuePairsByDate(measurements);
    }

    @Override
    protected Object handleGetMeasurementForMaxDate(String strategyName, String name, Date maxDate) throws Exception {

        List<Measurement> list = getMeasurementDao().findMeasurementsBeforeDate(1, 1, strategyName, name, maxDate);

        if (!list.isEmpty()) {
            return list.get(0).getValue();
        } else {
            return null;
        }
    }

    @Override
    protected Object handleGetMeasurementForMinDate(String strategyName, String name, Date minDate) throws Exception {

        List<Measurement> list = getMeasurementDao().findMeasurementsAfterDate(1, 1, strategyName, name, minDate);

        if (!list.isEmpty()) {
            return list.get(0).getValue();
        } else {
            return null;
        }
    }

    @Override
    protected Tick handleGetTickFromRawTick(RawTickVO rawTick) {

        long beforeCompleteRaw = System.nanoTime();
        Tick tick = getTickDao().rawTickVOToEntity(rawTick);
        long afterCompleteRawT = System.nanoTime();

        MetricsUtil.account("LookupService.getMarketDataEventFromRaw", (afterCompleteRawT - beforeCompleteRaw));

        return tick;
    }

    @Override
    protected Bar handleGetBarFromRawBar(RawBarVO barVO) {

        long beforeCompleteRaw = System.nanoTime();
        Bar bar = getBarDao().rawBarVOToEntity(barVO);
        long afterCompleteRawT = System.nanoTime();

        MetricsUtil.account("LookupService.getMarketDataEventFromRaw", (afterCompleteRawT - beforeCompleteRaw));

        return bar;
    }

    @Override
    protected Date handleGetCurrentDBTime() throws Exception {

        return getStrategyDao().findCurrentDBTime();
    }

    private Map<Date, Object> getValuesByDate(List<Measurement> measurements) {

        Map<Date, Object> valuesByDate = new HashMap<Date, Object>();
        for (Measurement measurement : measurements) {
            valuesByDate.put(measurement.getDate(), measurement.getValue());
        }

        return valuesByDate;
    }

    private Map<Date, Map<String, Object>> getNameValuePairsByDate(List<Measurement> measurements) {

        // group Measurements by date
        MultiValueMap measurementsByDate = new MultiValueMap();
        for (Measurement measurement : measurements) {
            measurementsByDate.put(measurement.getDate(), measurement);
        }

        // create a nameValuePair Map per date
        Map<Date, Map<String, Object>> nameValuePairsByDate = new HashMap<Date, Map<String, Object>>();
        for (Date dt : (Set<Date>) measurementsByDate.keySet()) {

            Map<String, Object> nameValuePairs = new HashMap<String, Object>();
            for (Measurement measurement : (Collection<Measurement>) measurementsByDate.get(dt)) {
                nameValuePairs.put(measurement.getName(), measurement.getValue());
            }
            nameValuePairsByDate.put(dt, nameValuePairs);
        }

        return nameValuePairsByDate;
    }
}
