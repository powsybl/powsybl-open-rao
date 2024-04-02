/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.parameters;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class CsaCracCreationParameters extends AbstractExtension<CracCreationParameters> {
    private String capacityCalculationRegionEicCode = "10Y1001C--00095L"; // swe as default
    private Integer spsMaxTimeToImplementThresholdInSeconds = 0;

    @Override
    public String getName() {
        return "CsaCracCreatorParameters";
    }

    public String getCapacityCalculationRegionEicCode() {
        return capacityCalculationRegionEicCode;
    }

    public void setCapacityCalculationRegionEicCode(String capacityCalculationRegionEicCode) {
        this.capacityCalculationRegionEicCode = capacityCalculationRegionEicCode;
    }

    public Integer getSpsMaxTimeToImplementThresholdInSeconds() {
        return spsMaxTimeToImplementThresholdInSeconds;
    }

    public void setSpsMaxTimeToImplementThresholdInSeconds(Integer spsMaxTimeToImplementThresholdInSeconds) {
        this.spsMaxTimeToImplementThresholdInSeconds = spsMaxTimeToImplementThresholdInSeconds;
    }
}
