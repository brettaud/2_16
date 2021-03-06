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
package ch.algotrader.entity.security;

import java.util.Date;

import ch.algotrader.entity.marketData.MarketDataEventI;
import ch.algotrader.util.DateUtil;
import ch.algotrader.visitor.SecurityVisitor;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public class FutureImpl extends Future {

    private static final long serialVersionUID = -7436972192801577685L;

    @Override
    public <R, P> R accept(SecurityVisitor<R, ? super P> visitor, P param) {

        return visitor.visitFuture(this, param);
    }

    @Override
    public double getLeverage(MarketDataEventI marketDataEvent, MarketDataEventI underlyingMarketDataEvent, Date currentTime) {

        return 1.0;
    }

    @Override
    public long getTimeToExpiration(Date dateTime) {

        return getExpiration().getTime() - dateTime.getTime();
    }

    @Override
    public int getDuration(Date dateTime) {

        FutureFamily family = (FutureFamily) this.getSecurityFamily();
        Date nextExpDate = DateUtil.getExpirationDate(family.getExpirationType(), dateTime);
        return 1 + (int) Math.round(((this.getExpiration().getTime() - nextExpDate.getTime()) / (double)family.getExpirationDistance().getValue()));
    }

    /**
     * make sure expiration is a java.util.Date and not a java.sql.TimeStamp
     */
    @Override
    public Date getExpiration() {

        Date expiration = super.getExpiration();
        if (expiration != null && expiration instanceof java.sql.Timestamp) {
            return new Date(expiration.getTime());
        } else {
            return expiration;
        }
    }
}
