/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltage;

import com.powsybl.openrao.commons.MeasurementRounding;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.monitoring.api.SecurityStatus;
import com.powsybl.openrao.monitoring.results.AbstractCnecResult;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class VoltageCnecResult extends AbstractCnecResult<VoltageCnec> {
    public VoltageCnecResult(VoltageCnec voltageCnec, Unit unit, VoltageCnecValue value, double margin, SecurityStatus securityStatus) {
        super(voltageCnec, unit, value, margin, securityStatus);
    }

    @Override
    public String print() {
        VoltageCnecValue voltageValue = (VoltageCnecValue) value;
        return String.format("Network element %s at state %s has a min voltage of %s kV and a max voltage of %s kV.",
            cnec.getNetworkElement().getId(),
            cnec.getState().getId(),
            MeasurementRounding.roundValueBasedOnMargin(voltageValue.minValue(), margin, 2).doubleValue(),
            MeasurementRounding.roundValueBasedOnMargin(voltageValue.maxValue(), margin, 2).doubleValue());

    }
}
