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
package ch.algotrader.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IWorkingSet;

/**
 * Wizard configuration
 *
 * @author <a href="mailto:akhikhl@gmail.com">Andrey Hihlovskiy</a>
 *
 * @version $Revision$ $Date$
 */
final class WizardConfig {
    IDatabaseModel databaseModel;
    IPath path;
    List<IWorkingSet> workingSets = new ArrayList<IWorkingSet>();
    String groupId;
    String artifactId;
    String version;
    String packageName;
    String serviceName;
    MarketDataType dataSetType;
    String dataSet;
    String databaseName;
    boolean updateDatabase;
}
