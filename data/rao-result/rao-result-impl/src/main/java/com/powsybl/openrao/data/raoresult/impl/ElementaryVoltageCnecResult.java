/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.commons.MinOrMax;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class ElementaryVoltageCnecResult {

    private double minVoltage = Double.NaN;
    private double maxVoltage = Double.NaN;
    private double margin = Double.NaN;

    private void checkUnit(Unit unit) {
        if (!unit.equals(Unit.KILOVOLT)) {
            throw new OpenRaoException("Voltage results are only available in KILOVOLT");
        }
    }

    public double getVoltage(MinOrMax minOrMax, Unit unit) {
        checkUnit(unit);
        return minOrMax.equals(MinOrMax.MAX) ? maxVoltage : minVoltage;
    }

    public double getMargin(Unit unit) {
        checkUnit(unit);
        return margin;
    }

    public void setMinVoltage(double voltage, Unit unit) {
        checkUnit(unit);
        this.minVoltage = voltage;
    }

    public void setMaxVoltage(double voltage, Unit unit) {
        checkUnit(unit);
        this.maxVoltage = voltage;
    }

    public void setMargin(double margin, Unit unit) {
        checkUnit(unit);
        this.margin = margin;
    }
}
