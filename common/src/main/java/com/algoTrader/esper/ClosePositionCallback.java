/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2013 Flury Trading - All rights reserved
 *
 * All information contained herein is, and remains the property of Flury Trading.
 * The intellectual and technical concepts contained herein are proprietary to
 * Flury Trading. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from Flury Trading
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * Flury Trading
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package com.algoTrader.esper;

import org.apache.log4j.Logger;

import com.algoTrader.util.MyLogger;
import com.algoTrader.util.metric.MetricsUtil;
import com.algoTrader.vo.ClosePositionVO;

public abstract class ClosePositionCallback {

    private static Logger logger = MyLogger.getLogger(ClosePositionCallback.class.getName());

    public void update(ClosePositionVO positionVO) throws Exception {

        // get the statement alias based on all security ids
        String alias = "ON_CLOSE_POSITION_" + positionVO.getSecurityId();

        // undeploy the statement
        EsperManager.undeployStatement(positionVO.getStrategy(), alias);

        long startTime = System.nanoTime();
        logger.debug("onClosePosition start " + positionVO.getSecurityId());

        // call orderCompleted
        onClosePosition(positionVO);

        logger.debug("onClosePosition end " + positionVO.getSecurityId());

        MetricsUtil.accountEnd("ClosePositionCallback." + positionVO.getStrategy(), startTime);
    }

    public abstract void onClosePosition(ClosePositionVO positionVO) throws Exception;
}
