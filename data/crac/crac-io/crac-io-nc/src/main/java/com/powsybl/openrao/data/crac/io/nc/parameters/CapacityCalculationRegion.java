/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.parameters;

import com.powsybl.openrao.commons.OpenRaoException;

import java.util.Arrays;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum CapacityCalculationRegion {
    BALTIC("10Y1001C--00120B"),
    CENTRAL_EUROPE("10Y1001C--00145W"),
    CHANNEL("10Y1001C--000239"),
    CORE("10Y1001C--00059P"),
    HANSA("10Y1001C--00136X"),
    GREECE_ITALY("10Y1001C--00138T"),
    IRELAND_UK("10Y1001C--00022B"),
    ITALY_NORTH("10Y1001C--00137V"),
    NORDIC("10Y1001A1001A91G"),
    SELENE("10Y1001C--00139R"),
    SOUTH_WESTERN_EUROPE("10Y1001C--00095L");

    private final String eic;

    CapacityCalculationRegion(String eic) {
        this.eic = eic;
    }

    public String getEIC() {
        return eic;
    }

    public static CapacityCalculationRegion fromEIC(String eic) {
        return Arrays.stream(CapacityCalculationRegion.values())
            .filter(region -> region.getEIC().equals(eic))
            .findFirst()
            .orElseThrow(() -> new OpenRaoException("Unrecognized Capacity Calculation Region with EIC %s.".formatted(eic)));
    }
}
