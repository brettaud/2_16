package com.algoTrader.service.fix;

import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import quickfix.field.ClOrdID;
import quickfix.field.ContractMultiplier;
import quickfix.field.Currency;
import quickfix.field.CustomerOrFirm;
import quickfix.field.ExDestination;
import quickfix.field.HandlInst;
import quickfix.field.MaturityMonthYear;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.PutOrCall;
import quickfix.field.SecurityType;
import quickfix.field.StopPx;
import quickfix.field.StrikePrice;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelReplaceRequest;
import quickfix.fix42.OrderCancelRequest;

import com.algoTrader.entity.security.Forex;
import com.algoTrader.entity.security.Future;
import com.algoTrader.entity.security.Security;
import com.algoTrader.entity.security.Stock;
import com.algoTrader.entity.security.StockOption;
import com.algoTrader.entity.trade.LimitOrderI;
import com.algoTrader.entity.trade.SimpleOrder;
import com.algoTrader.entity.trade.StopOrderI;
import com.algoTrader.enumeration.OptionType;
import com.algoTrader.util.MyLogger;

public class FixOrderServiceImpl extends FixOrderServiceBase {

    private static Logger logger = MyLogger.getLogger(FixOrderServiceImpl.class.getName());
    private static SimpleDateFormat monthFormat = new SimpleDateFormat("yyyyMM");

    private String sessionName = "FIXIB";

    @Override
    protected void handleValidateOrder(SimpleOrder order) throws Exception {
        // to be implememented
    }

    @Override
    protected void handleSendOrder(SimpleOrder order) throws Exception {

        // use system time for orderNumber
        order.setNumber(FixIdGenerator.getInstance().getNextOrderId());

        NewOrderSingle newOrder = new NewOrderSingle();
        Security security = order.getSecurity();

        // common info
        newOrder.set(new ClOrdID(String.valueOf(order.getNumber())));
        newOrder.set(new HandlInst('2'));
        newOrder.set(new CustomerOrFirm(0));

        newOrder.set(FixUtil.getFixSymbol(security));
        newOrder.set(FixUtil.getFixSide(order.getSide()));
        newOrder.set(new OrderQty(order.getQuantity()));
        newOrder.set(FixUtil.getFixOrderType(order));
        newOrder.set(new ExDestination(FixMarketConverter.marketToString(security.getSecurityFamily().getMarket())));

        // populate security information
        if (security instanceof StockOption) {

            StockOption stockOption = (StockOption) security;

            newOrder.set(new SecurityType(SecurityType.OPTION));
            newOrder.set(new Currency(stockOption.getSecurityFamily().getCurrency().toString()));
            newOrder.set(new PutOrCall(OptionType.PUT.equals(stockOption.getType()) ? PutOrCall.PUT : PutOrCall.CALL));
            newOrder.set(new StrikePrice(stockOption.getStrike().doubleValue()));
            newOrder.set(new ContractMultiplier(stockOption.getSecurityFamily().getContractSize()));
            newOrder.set(new MaturityMonthYear(monthFormat.format(stockOption.getExpiration())));

        } else if (security instanceof Future) {

            Future future = (Future) security;

            newOrder.set(new SecurityType(SecurityType.FUTURE));
            newOrder.set(new Currency(future.getSecurityFamily().getCurrency().toString()));
            newOrder.set(new MaturityMonthYear(monthFormat.format(future.getExpiration())));

        } else if (security instanceof Forex) {

            String[] currencies = security.getSymbol().split("\\.");
            newOrder.set(new SecurityType(SecurityType.CASH));
            newOrder.set(new Currency(currencies[1]));

        } else if (security instanceof Stock) {

            Stock stock = (Stock) security;

            newOrder.set(new SecurityType(SecurityType.COMMON_STOCK));
            newOrder.set(new Currency(stock.getSecurityFamily().getCurrency().toString()));
        }

        //set the limit price if order is a limit order or stop limit order
        if (order instanceof LimitOrderI) {
            newOrder.set(new Price(((LimitOrderI) order).getLimit().doubleValue()));
        }

        //set the stop price if order is a stop order or stop limit order
        if (order instanceof StopOrderI) {
            newOrder.set(new StopPx(((StopOrderI) order).getStop().doubleValue()));
        }

        // progapate the order to all corresponding esper engines
        getOrderService().propagateOrder(order);

        // send the message to the FixClient
        getFixClient().sendMessage(newOrder, this.sessionName);

        logger.info("sent order: " + order);
    }

    @Override
    protected void handleModifyOrder(SimpleOrder order) throws Exception {

        // assign a new order number
        long origNumber = order.getNumber();
        order.setNumber(FixIdGenerator.getInstance().getNextOrderId());

        OrderCancelReplaceRequest replaceRequest = new OrderCancelReplaceRequest();

        // common info
        replaceRequest.set(new ClOrdID(String.valueOf(order.getNumber())));
        replaceRequest.set(new OrigClOrdID(String.valueOf(origNumber)));
        replaceRequest.set(new HandlInst('2'));
        replaceRequest.set(new CustomerOrFirm(0));

        replaceRequest.set(FixUtil.getFixSymbol(order.getSecurity()));
        replaceRequest.set(FixUtil.getFixSide(order.getSide()));
        replaceRequest.set(new OrderQty(order.getQuantity()));
        replaceRequest.set(FixUtil.getFixOrderType(order));

        //set the limit price if order is a limit order or stop limit order
        if (order instanceof LimitOrderI) {
            replaceRequest.set(new Price(((LimitOrderI) order).getLimit().doubleValue()));
        }

        //set the stop price if order is a stop order or stop limit order
        if (order instanceof StopOrderI) {
            replaceRequest.set(new StopPx(((StopOrderI) order).getStop().doubleValue()));
        }
        // progapate the order to all corresponding esper engines
        getOrderService().propagateOrder(order);

        // send the message to the FixClient
        getFixClient().sendMessage(replaceRequest, this.sessionName);

        logger.info("modified order: " + order);
    }

    @Override
    protected void handleCancelOrder(SimpleOrder order) throws Exception {

        OrderCancelRequest cancelRequest = new OrderCancelRequest();

        // common info
        cancelRequest.set(new ClOrdID(String.valueOf(FixIdGenerator.getInstance().getNextOrderId())));
        cancelRequest.set(new OrigClOrdID(String.valueOf(order.getNumber())));

        cancelRequest.set(FixUtil.getFixSymbol(order.getSecurity()));
        cancelRequest.set(FixUtil.getFixSide(order.getSide()));
        cancelRequest.set(new OrderQty(order.getQuantity()));

        // send the message to the FixClient
        getFixClient().sendMessage(cancelRequest, this.sessionName);

        logger.info("requested order cancallation for order: " + order);
    }
}
