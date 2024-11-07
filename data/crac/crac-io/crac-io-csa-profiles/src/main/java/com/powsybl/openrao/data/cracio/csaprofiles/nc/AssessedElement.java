/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.nc;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracUtils;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record AssessedElement(String mrid, Boolean inBaseCase, String name, String assessedSystemOperator, String conductingEquipment, String operationalLimit, Boolean isCombinableWithContingency, Boolean normalEnabled, String securedForRegion, String scannedForRegion, Double flowReliabilityMargin, String overlappingZone) implements NCObject {
    public String getUniqueName() {
        return CsaProfileCracUtils.createElementName(name(), assessedSystemOperator()).orElse(mrid());
    }
}
