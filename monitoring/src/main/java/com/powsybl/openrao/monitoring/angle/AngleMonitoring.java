/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.angle;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.AbstractMonitoring;
import com.powsybl.openrao.monitoring.MonitoringInput;
import com.powsybl.openrao.monitoring.SecurityStatus;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithAngleMonitoring;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleMonitoring extends AbstractMonitoring<AngleCnec> {
    public AngleMonitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        super(loadFlowProvider, loadFlowParameters);
    }

    @Override
    protected PhysicalParameter getPhysicalParameter() {
        return PhysicalParameter.ANGLE;
    }

    @Override
    protected MonitoringResult<AngleCnec> makeEmptySecureResult() {
        return new AngleMonitoringResult(Set.of(), Map.of(), SecurityStatus.SECURE);
    }

    @Override
    protected Set<AngleCnec> getCnecs(Crac crac) {
        return crac.getAngleCnecs();
    }

    @Override
    protected MonitoringResult<AngleCnec> makeMonitoringResult(Set<CnecResult<AngleCnec>> cnecResults, Map<State, Set<RemedialAction<?>>> appliedRemedialActions, SecurityStatus monitoringResultStatus) {
        return new AngleMonitoringResult(cnecResults, appliedRemedialActions, monitoringResultStatus);
    }

    @Override
    protected MonitoringResult<AngleCnec> makeFailedMonitoringResultForState(State state, String failureReason, Set<CnecResult<AngleCnec>> cnecResults) {
        BUSINESS_WARNS.warn(failureReason);
        return new AngleMonitoringResult(cnecResults, Map.of(state, Collections.emptySet()), SecurityStatus.FAILURE);
    }

    @Override
    protected CnecResult<AngleCnec> computeCnecResult(AngleCnec angleCnec, Network network, Unit unit) {
        AngleCnecHelper angleCnecHelper = new AngleCnecHelper();
        return new AngleCnecResult(angleCnec, unit, angleCnecHelper.computeValue(angleCnec, network, unit), angleCnecHelper.computeMargin(angleCnec, network, unit), angleCnecHelper.computeSecurityStatus(angleCnec, network, unit));
    }

    @Override
    protected CnecResult<AngleCnec> makeFailedCnecResult(AngleCnec angleCnec, Unit unit) {
        return new AngleCnecResult(angleCnec, unit, new AngleCnecValue(Double.NaN), Double.NaN, SecurityStatus.FAILURE);
    }

    /**
     * Main function : runs AngleMonitoring computation on all AngleCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with AngleMonitoringResult
     */
    public static RaoResult runAndUpdateRaoResult(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel, MonitoringInput<AngleCnec> monitoringInput) throws OpenRaoException {
        return new RaoResultWithAngleMonitoring(monitoringInput.getRaoResult(), new AngleMonitoring(loadFlowProvider, loadFlowParameters).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel));
    }
}
