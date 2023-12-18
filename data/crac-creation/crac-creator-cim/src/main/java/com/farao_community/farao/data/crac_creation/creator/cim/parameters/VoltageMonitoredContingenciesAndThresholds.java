/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_creation.creator.cim.parameters;

import com.powsybl.open_rao.commons.FaraoException;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageMonitoredContingenciesAndThresholds {
    private Set<String> contingencyNames;
    private Map<Double, VoltageThreshold> thresholdPerNominalV;

    public VoltageMonitoredContingenciesAndThresholds(Set<String> contingencyNames, Map<Double, VoltageThreshold> thresholdPerNominalV) {
        Objects.requireNonNull(thresholdPerNominalV);
        this.contingencyNames = contingencyNames;
        if (thresholdPerNominalV.isEmpty()) {
            throw new FaraoException("At least one threshold should be defined.");
        }
        this.thresholdPerNominalV = thresholdPerNominalV;
    }

    public Set<String> getContingencyNames() {
        return contingencyNames;
    }

    public Map<Double, VoltageThreshold> getThresholdPerNominalV() {
        return thresholdPerNominalV;
    }
}
