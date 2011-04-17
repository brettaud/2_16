package com.algoTrader.entity;

import com.algoTrader.future.FutureUtil;


public class FutureImpl extends Future {

    private static final long serialVersionUID = -7436972192801577685L;

    public double getLeverage() {

        return 1.0;
    }

    public double getMargin() {

        return FutureUtil.getMaintenanceMargin(this) * getSecurityFamily().getContractSize();
    }
}