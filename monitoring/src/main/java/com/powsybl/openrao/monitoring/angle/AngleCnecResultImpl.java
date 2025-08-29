/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.angle;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.MeasurementRounding;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.monitoring.SecurityStatus;
import com.powsybl.openrao.monitoring.results.AbstractCnecResult;
import com.powsybl.openrao.monitoring.results.AngleCnecResult;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleCnecResultImpl extends AbstractCnecResult<AngleCnec> implements AngleCnecResult {
    private final Double angle;

    public AngleCnecResultImpl(AngleCnec angleCnec, Double angle, double margin, SecurityStatus securityStatus) {
        super(angleCnec, margin, securityStatus);
        this.angle = angle;
    }

    @Override
    public String print() {
        return String.format("AngleCnec %s (with importing network element %s and exporting network element %s) at state %s has an angle of %s°.",
            cnec.getId(),
            cnec.getImportingNetworkElement().getId(),
            cnec.getExportingNetworkElement().getId(),
            cnec.getState().getId(),
            MeasurementRounding.roundValueBasedOnMargin(angle, margin, 2).doubleValue());
    }

    @Override
    public Double getAngle() {
        return angle;
    }

    public static AngleCnecResult compute(AngleCnec angleCnec, Network network, Unit unit) {
        return new AngleCnecResultImpl(angleCnec, AngleCnecDataCalculator.computeAngle(angleCnec, network, unit), AngleCnecDataCalculator.computeMargin(angleCnec, network, unit), AngleCnecDataCalculator.computeSecurityStatus(angleCnec, network, unit));
    }

    public static AngleCnecResult failed(AngleCnec angleCnec) {
        return new AngleCnecResultImpl(angleCnec, Double.NaN, Double.NaN, SecurityStatus.FAILURE);
    }
}
