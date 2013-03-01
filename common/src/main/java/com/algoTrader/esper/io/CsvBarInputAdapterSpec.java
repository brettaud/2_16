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
package com.algoTrader.esper.io;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.algoTrader.enumeration.Duration;
import com.algoTrader.util.CustomDate;
import com.espertech.esperio.AdapterInputSource;
import com.espertech.esperio.csv.CSVInputAdapterSpec;

/**
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class CsvBarInputAdapterSpec extends CSVInputAdapterSpec {

    private File file;
    private Duration barSize;

    public CsvBarInputAdapterSpec(File file, Duration barSize) {

        super(new AdapterInputSource(file), "RawBar");

        this.file = file;
        this.barSize = barSize;

        //@formatter:off
        String[] barPropertyOrder = new String[] {
                "dateTime",
                "open",
                "high",
                "low",
                "close",
                "vol",
                "openIntrest",
                "settlement" };
        //@formatter:on

        setPropertyOrder(barPropertyOrder);

        Map<String, Object> barPropertyTypes = new HashMap<String, Object>();

        barPropertyTypes.put("dateTime", CustomDate.class);
        barPropertyTypes.put("open", BigDecimal.class);
        barPropertyTypes.put("high", BigDecimal.class);
        barPropertyTypes.put("low", BigDecimal.class);
        barPropertyTypes.put("close", BigDecimal.class);
        barPropertyTypes.put("vol", int.class);
        barPropertyTypes.put("openIntrest", int.class);
        barPropertyTypes.put("settlement", BigDecimal.class);

        setPropertyTypes(barPropertyTypes);

        setTimestampColumn("dateTime");

        setUsingExternalTimer(true);
    }

    public File getFile() {

        return this.file;
    }

    public Duration getBarSize() {
        return this.barSize;
    }
}
