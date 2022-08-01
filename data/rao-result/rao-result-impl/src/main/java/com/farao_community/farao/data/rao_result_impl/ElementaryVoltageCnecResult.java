/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class ElementaryVoltageCnecResult {

    private double voltage = Double.NaN;
    private double margin = Double.NaN;
    private static final String VOLTAGE_IN_KILOVOLT = "Voltage results are only available in KILOVOLT";

    public double getVoltage(Unit unit) {
        if (unit.equals(Unit.KILOVOLT)) {
            return voltage;
        } else {
            throw new FaraoException(VOLTAGE_IN_KILOVOLT);
        }
    }

    public double getMargin(Unit unit) {
        if (unit.equals(Unit.KILOVOLT)) {
            return margin;
        } else {
            throw new FaraoException(VOLTAGE_IN_KILOVOLT);
        }
    }

    public void setVoltage(double voltage, Unit unit) {
        if (unit.equals(Unit.KILOVOLT)) {
            this.voltage = voltage;
        } else {
            throw new FaraoException(VOLTAGE_IN_KILOVOLT);
        }
    }

    public void setMargin(double margin, Unit unit) {
        if (unit.equals(Unit.KILOVOLT)) {
            this.margin = margin;
        } else {
            throw new FaraoException(VOLTAGE_IN_KILOVOLT);
        }
    }
}
