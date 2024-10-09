/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cim.parameters;

import com.powsybl.openrao.commons.OpenRaoException;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Contains relevant information that allows the creation of VoltageCnecs in the CimCracCreator
 * (as VoltageCnecs are not defined inside the CIM CRAC document)
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageCnecsCreationParameters {

    private Map<String, VoltageMonitoredContingenciesAndThresholds> monitoredStatesAndThresholds;
    private Set<String> monitoredNetworkElements;

    public VoltageCnecsCreationParameters(Map<String, VoltageMonitoredContingenciesAndThresholds> monitoredStatesAndThresholds, Set<String> monitoredNetworkElements) {
        Objects.requireNonNull(monitoredStatesAndThresholds);
        Objects.requireNonNull(monitoredNetworkElements);
        if (monitoredStatesAndThresholds.isEmpty() || monitoredNetworkElements.isEmpty()) {
            throw new OpenRaoException("At least one monitored element and one monitored state with thresholds should be defined.");
        }
        this.monitoredStatesAndThresholds = monitoredStatesAndThresholds;
        this.monitoredNetworkElements = monitoredNetworkElements;
    }

    public Map<String, VoltageMonitoredContingenciesAndThresholds> getMonitoredStatesAndThresholds() {
        return monitoredStatesAndThresholds;
    }

    public Set<String> getMonitoredNetworkElements() {
        return monitoredNetworkElements;
    }
}
