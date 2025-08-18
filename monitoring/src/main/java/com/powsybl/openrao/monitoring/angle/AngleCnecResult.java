/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.angle;

import com.powsybl.openrao.commons.MeasurementRounding;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.monitoring.api.SecurityStatus;
import com.powsybl.openrao.monitoring.results.AbstractCnecResult;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleCnecResult extends AbstractCnecResult<AngleCnec> {
    public AngleCnecResult(AngleCnec angleCnec, Unit unit, AngleCnecValue value, double margin, SecurityStatus securityStatus) {
        super(angleCnec, unit, value, margin, securityStatus);
    }

    @Override
    public String print() {
        AngleCnecValue angleCnecValue = (AngleCnecValue) value;
        return String.format("AngleCnec %s (with importing network element %s and exporting network element %s) at state %s has an angle of %s°.",
            cnec.getId(),
            cnec.getImportingNetworkElement().getId(),
            cnec.getExportingNetworkElement().getId(),
            cnec.getState().getId(),
            MeasurementRounding.roundValueBasedOnMargin(angleCnecValue.value(), margin, 2).doubleValue());
    }
}
