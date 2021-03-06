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
package ch.algotrader.esper.io;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.espertech.esperio.csv.BasicTypeCoercer;

import ch.algotrader.util.DateTimeLegacy;
import ch.algotrader.util.DateTimeUtil;

/**
 */
public class CvsTypeCoercer extends BasicTypeCoercer {

    private final Set<String> dateProperties;

    public CvsTypeCoercer() {
        this.dateProperties = new HashSet<>();
    }

    @Override
    public void setPropertyTypes(final Map<String, Object> propertyTypes) {

        this.dateProperties.clear();
        for (Map.Entry<String, Object> entry: propertyTypes.entrySet()) {
            String propertyName = entry.getKey();
            Class<?> clazz = (Class<?>) entry.getValue();
            if (clazz == Date.class) {
                this.dateProperties.add(propertyName);
            }
        }
        super.setPropertyTypes(propertyTypes);
    }

    @Override
    public Object coerce(final String property, final String source) throws Exception {
        if (this.dateProperties.contains(property)) {
            if (source == null || source.isEmpty()) {
                return new Date();
            } else {
                try {
                    long timestamp = Long.parseLong(source);
                    return new Date(timestamp);
                } catch (NumberFormatException ex) {
                    LocalDateTime localDateTime = DateTimeUtil.parseLocalDateTime(source);
                    return DateTimeLegacy.toLocalDateTime(localDateTime);
                }
            }
        }
        return super.coerce(property, source);
    }

}
