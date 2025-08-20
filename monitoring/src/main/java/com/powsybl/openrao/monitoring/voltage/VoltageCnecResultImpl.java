/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltage;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.MeasurementRounding;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.monitoring.SecurityStatus;
import com.powsybl.openrao.monitoring.results.AbstractCnecResult;
import com.powsybl.openrao.monitoring.results.VoltageCnecResult;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class VoltageCnecResultImpl extends AbstractCnecResult<VoltageCnec> implements VoltageCnecResult {
    private final Double minVoltage;
    private final Double maxVoltage;

    public VoltageCnecResultImpl(VoltageCnec voltageCnec, Double minVoltage, Double maxVoltage, double margin, SecurityStatus securityStatus) {
        super(voltageCnec, margin, securityStatus);
        this.minVoltage = minVoltage;
        this.maxVoltage = maxVoltage;
    }

    @Override
    public String print() {
        return String.format("Network element %s at state %s has a min voltage of %s kV and a max voltage of %s kV.",
            cnec.getNetworkElement().getId(),
            cnec.getState().getId(),
            MeasurementRounding.roundValueBasedOnMargin(minVoltage, margin, 2).doubleValue(),
            MeasurementRounding.roundValueBasedOnMargin(maxVoltage, margin, 2).doubleValue());

    }

    @Override
    public Double getMinVoltage() {
        return minVoltage;
    }

    @Override
    public Double getMaxVoltage() {
        return maxVoltage;
    }

    public static VoltageCnecResult compute(VoltageCnec voltageCnec, Network network, Unit unit) {
        return new VoltageCnecResultImpl(voltageCnec, VoltageCnecDataCalculator.computeMinVoltage(voltageCnec, network, unit), VoltageCnecDataCalculator.computeMaxVoltage(voltageCnec, network, unit), VoltageCnecDataCalculator.computeMargin(voltageCnec, network, unit), VoltageCnecDataCalculator.computeSecurityStatus(voltageCnec, network, unit));
    }

    public static VoltageCnecResult failed(VoltageCnec voltageCnec) {
        return new VoltageCnecResultImpl(voltageCnec, Double.NaN, Double.NaN, Double.NaN, SecurityStatus.FAILURE);
    }
}
