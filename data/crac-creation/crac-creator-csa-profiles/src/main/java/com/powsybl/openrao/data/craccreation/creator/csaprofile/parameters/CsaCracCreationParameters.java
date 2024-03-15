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
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class CsaCracCreationParameters extends AbstractExtension<CracCreationParameters> {
    private String capacityCalculationRegionEicCode = "10Y1001C–00095L";

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
}
