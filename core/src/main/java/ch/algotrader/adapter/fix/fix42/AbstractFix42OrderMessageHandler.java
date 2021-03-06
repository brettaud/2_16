/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2015 AlgoTrader GmbH - All rights reserved
 *
 * All information contained herein is, and remains the property of AlgoTrader GmbH.
 * The intellectual and technical concepts contained herein are proprietary to
 * AlgoTrader GmbH. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from AlgoTrader GmbH
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * AlgoTrader GmbH
 * Aeschstrasse 6
 * 8834 Schindellegi
 ***********************************************************************************/
package ch.algotrader.adapter.fix.fix42;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.algotrader.entity.trade.Fill;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.entity.trade.OrderStatus;
import ch.algotrader.enumeration.Status;
import ch.algotrader.service.OrderExecutionService;
import quickfix.FieldNotFound;
import quickfix.SessionID;
import quickfix.field.CxlRejReason;
import quickfix.field.ExecTransType;
import quickfix.field.ExecType;
import quickfix.field.MsgSeqNum;
import quickfix.field.Text;
import quickfix.field.TransactTime;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.OrderCancelReject;

/**
 * Abstract FIX/4.2 order message handler implementing generic functionality common to all broker specific
 * interfaces..
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public abstract class AbstractFix42OrderMessageHandler extends AbstractFix42MessageHandler {

    private static final Logger LOGGER = LogManager.getLogger(AbstractFix42OrderMessageHandler.class);

    private final OrderExecutionService orderExecutionService;

    protected AbstractFix42OrderMessageHandler(final OrderExecutionService orderExecutionService) {
        this.orderExecutionService = orderExecutionService;
    }

    protected abstract boolean discardReport(ExecutionReport executionReport) throws FieldNotFound;

    protected abstract void handleStatus(ExecutionReport executionReport) throws FieldNotFound;

    protected abstract void handleExternal(ExecutionReport executionReport) throws FieldNotFound;

    protected abstract void handleUnknown(ExecutionReport executionReport) throws FieldNotFound;

    protected abstract void handleRestated(ExecutionReport executionReport, Order order) throws FieldNotFound;

    protected abstract boolean isOrderRejected(ExecutionReport executionReport) throws FieldNotFound;

    protected abstract boolean isOrderReplaced(ExecutionReport executionReport) throws FieldNotFound;

    protected abstract boolean isOrderRestated(ExecutionReport executionReport) throws FieldNotFound;

    protected abstract OrderStatus createStatus(ExecutionReport executionReport, Order order) throws FieldNotFound;

    protected abstract Fill createFill(ExecutionReport executionReport, Order order) throws FieldNotFound;

    protected abstract String getDefaultBroker();

    public void onMessage(ExecutionReport executionReport, SessionID sessionID) throws FieldNotFound {

        if (discardReport(executionReport)) {

            return;
        }

        // check ExecTransType
        if (executionReport.isSetExecTransType()) {
            char execTransType = executionReport.getExecTransType().getValue();
            switch (execTransType) {
                case ExecTransType.NEW:
                    break;
                case ExecTransType.STATUS:
                    handleStatus(executionReport);
                    return;
                default:
                    throw new UnsupportedOperationException("Unsupported ExecTransType " + execTransType);
            }
        }

        String orderIntId;
        ExecType execType = executionReport.getExecType();
        if (execType.getValue() == ExecType.CANCELED) {
            if (executionReport.isSetOrigClOrdID()) {
                orderIntId = executionReport.getOrigClOrdID().getValue();
            } else {
                String orderExtId = executionReport.getOrderID().getValue();
                orderIntId = this.orderExecutionService.lookupIntId(orderExtId);
            }
        } else {
            if (executionReport.isSetClOrdID()) {
                orderIntId = executionReport.getClOrdID().getValue();
            } else {
                String orderExtId = executionReport.getOrderID().getValue();
                orderIntId = this.orderExecutionService.lookupIntId(orderExtId);
            }
        }
        if (orderIntId == null) {

            handleUnknown(executionReport);
            return;
        }

        Order order = this.orderExecutionService.getOpenOrderByIntId(orderIntId);
        if (order == null) {

            handleUnknown(executionReport);
            return;
        }

        if (isOrderRejected(executionReport)) {

            if (LOGGER.isErrorEnabled()) {

                StringBuilder buf = new StringBuilder();
                String intId = executionReport.getClOrdID().getValue();
                buf.append("Order with IntID ").append(intId).append(" has been rejected");
                if (executionReport.isSetField(Text.FIELD)) {

                    buf.append("; reason given: ").append(executionReport.getText().getValue());
                }
                LOGGER.error(buf.toString());
            }

            OrderStatus orderStatus = OrderStatus.Factory.newInstance();
            orderStatus.setStatus(Status.REJECTED);
            orderStatus.setIntId(orderIntId);
            orderStatus.setSequenceNumber(executionReport.getHeader().getInt(MsgSeqNum.FIELD));
            orderStatus.setOrder(order);
            if (executionReport.isSetField(TransactTime.FIELD)) {

                orderStatus.setExtDateTime(executionReport.getTransactTime().getValue());
            }
            if (executionReport.isSetField(Text.FIELD)) {

                orderStatus.setReason(executionReport.getText().getValue());
            }

            this.orderExecutionService.handleOrderStatus(orderStatus);

            return;
        }

        if (isOrderRestated(executionReport)) {

            handleRestated(executionReport, order);
            return;
        }

        if (isOrderReplaced(executionReport)) {

            // Send status report for replaced order
            String oldIntId = executionReport.getOrigClOrdID().getValue();
            Order oldOrder = this.orderExecutionService.getOpenOrderByIntId(oldIntId);

            if (oldOrder != null) {

                OrderStatus orderStatus = createStatus(executionReport, oldOrder);
                orderStatus.setStatus(Status.CANCELED);
                orderStatus.setExtId(null);
                this.orderExecutionService.handleOrderStatus(orderStatus);
            }
        }

        OrderStatus orderStatus = createStatus(executionReport, order);

        this.orderExecutionService.handleOrderStatus(orderStatus);

        Fill fill = createFill(executionReport, order);
        if (fill != null) {
            this.orderExecutionService.handleFill(fill);
        }
    }

    public void onMessage(final OrderCancelReject reject, final SessionID sessionID) throws FieldNotFound {

        int value =  reject.isSetCxlRejReason() ? reject.getCxlRejReason().getValue() : CxlRejReason.OTHER;
        String intId = reject.isSetClOrdID() ? reject.getClOrdID().getValue() : null;
        String originalIntId = reject.isSetOrigClOrdID() ? reject.getOrigClOrdID().getValue() : null;

        if (value == CxlRejReason.TOO_LATE_TO_CANCEL) {
            if (LOGGER.isInfoEnabled()) {

                StringBuilder buf = new StringBuilder();
                buf.append("Order has already been filled");
                buf.append(" [order intId: ").append(intId).append("]");
                buf.append(" [original order intId: ").append(originalIntId).append("]");
                LOGGER.info(buf.toString());
            }
        } else {
            if (LOGGER.isErrorEnabled()) {

                StringBuilder buf = new StringBuilder();
                buf.append("Order cancel/replace has been rejected");
                buf.append(" [order IntID: ").append(intId).append("]");
                buf.append(" [original order IntID: ").append(originalIntId).append("]");
                if (reject.isSetField(Text.FIELD)) {
                    String text = reject.getText().getValue();
                    buf.append(": ").append(text);
                }
                LOGGER.error(buf.toString());
            }

            Order order = intId != null ? this.orderExecutionService.getOpenOrderByIntId(intId) : null;
            if (order != null) {
                OrderStatus orderStatus = OrderStatus.Factory.newInstance();
                orderStatus.setStatus(Status.REJECTED);
                orderStatus.setIntId(intId);
                orderStatus.setSequenceNumber(reject.getHeader().getInt(MsgSeqNum.FIELD));
                orderStatus.setOrder(order);
                if (reject.isSetField(TransactTime.FIELD)) {

                    orderStatus.setExtDateTime(reject.getTransactTime().getValue());
                }
                if (reject.isSetField(Text.FIELD)) {

                    orderStatus.setReason(reject.getText().getValue());
                }
                this.orderExecutionService.handleOrderStatus(orderStatus);

            } else {
                order = originalIntId != null ? this.orderExecutionService.getOpenOrderByIntId(originalIntId) : null;
                if (order != null) {
                    OrderStatus orderStatus = OrderStatus.Factory.newInstance();
                    orderStatus.setStatus(Status.REJECTED);
                    orderStatus.setIntId(originalIntId);
                    orderStatus.setSequenceNumber(reject.getHeader().getInt(MsgSeqNum.FIELD));
                    orderStatus.setOrder(order);
                    if (reject.isSetField(TransactTime.FIELD)) {

                        orderStatus.setExtDateTime(reject.getTransactTime().getValue());
                    }
                    if (reject.isSetField(Text.FIELD)) {

                        orderStatus.setReason(reject.getText().getValue());
                    }
                    this.orderExecutionService.handleOrderStatus(orderStatus);

                }
            }
        }

    }

}
