/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class UnitConverter {

    private UnitConverter() {
    }

    public static double getFlowUnitMultiplier(double nominalVoltage, Unit unitFrom, Unit unitTo) {
        if (unitFrom == unitTo) {
            return 1;
        }
        if (unitFrom == Unit.MEGAWATT && unitTo == Unit.AMPERE) {
            return 1000 / (nominalVoltage * Math.sqrt(3));
        } else if (unitFrom == Unit.AMPERE && unitTo == Unit.MEGAWATT) {
            return nominalVoltage * Math.sqrt(3) / 1000;
        } else {
            throw new OpenRaoException("Only conversions between MW and A are supported.");
        }
    }

    public static double convertAToPercentImax(double valueInA, double fmax) {
        return valueInA / fmax;
    }

    public static double convertPercentImaxToA(double valueInPercent, double fmax) {
        return valueInPercent * fmax;
    }

    public static double convertMWToA(double valueInMW, double nominalVoltage) {
        return valueInMW * getFlowUnitMultiplier(nominalVoltage, Unit.MEGAWATT, Unit.AMPERE);
    }

    public static double convertAToMW(double valueInA, double nominalVoltage) {
        return valueInA * getFlowUnitMultiplier(nominalVoltage, Unit.AMPERE, Unit.MEGAWATT);
    }
}
