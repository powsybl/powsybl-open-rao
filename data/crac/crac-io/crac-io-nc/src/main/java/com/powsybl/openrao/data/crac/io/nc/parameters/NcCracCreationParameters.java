/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.parameters;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class NcCracCreationParameters extends AbstractExtension<CracCreationParameters> {
    private CapacityCalculationRegion capacityCalculationRegionCode = null;
    private OffsetDateTime timestamp = null;
    private Map<String, Integer> curativeInstants = Map.of("curative 1", 300, "curative 2", 600, "curative 3", 1200);

    @Override
    public String getName() {
        return "NcCracCreatorParameters";
    }

    public CapacityCalculationRegion getCapacityCalculationRegion() {
        return capacityCalculationRegionCode;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Integer> getCurativeInstants() {
        return curativeInstants;
    }

    public void setCapacityCalculationRegion(CapacityCalculationRegion capacityCalculationRegionCode) {
        this.capacityCalculationRegionCode = capacityCalculationRegionCode;
    }

    public void setCurativeInstants(Map<String, Integer> curativeInstants) {
        this.curativeInstants = new HashMap<>(curativeInstants);
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
