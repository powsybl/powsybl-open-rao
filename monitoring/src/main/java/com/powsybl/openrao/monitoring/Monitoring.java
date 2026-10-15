/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */

import com.powsybl.computation.ComputationManager;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowRunParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.angle.AngleMonitoring;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithAngleMonitoring;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import com.powsybl.openrao.monitoring.voltage.VoltageMonitoring;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class Monitoring {

    private final String loadFlowProvider;
    private final LoadFlowRunParameters loadFlowRunParameters;
    Map<PhysicalParameter, Unit> parameterToUnitMap = new EnumMap<>(PhysicalParameter.class);

    /**
     * @deprecated
     * @param loadFlowProvider
     * @param loadFlowParameters
     */
    @Deprecated(since = "7.4.0")
    public Monitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        this.loadFlowProvider = loadFlowProvider;
        this.loadFlowRunParameters = new LoadFlowRunParameters().setParameters(loadFlowParameters);
        parameterToUnitMap.put(PhysicalParameter.ANGLE, Unit.DEGREE);
        parameterToUnitMap.put(PhysicalParameter.VOLTAGE, Unit.KILOVOLT);
    }

    /**
     * The computation manager can be used by the caller to execute actions before and/or after running the loadflow.
     * In particular, GridCapa relies on it to inject task-id in the MDC in order to bind logs with tasks.
     *
     * @deprecated
     */
    @Deprecated(since = "7.4.0")
    public Monitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters, ComputationManager computationManager) {
        this(loadFlowProvider, loadFlowParameters);
        this.loadFlowRunParameters.setComputationManager(computationManager);
    }

    /**
     * Main function : runs AngleMonitoring computation on all AngleCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with AngleMonitoringResult
     *
     * @deprecated use AngleMonitoring.runAndUpdateRaoResult instead
     */
    @Deprecated(since = "7.4.0")
    public static RaoResult runAngleAndUpdateRaoResult(String loadFlowProvider,
                                                       LoadFlowParameters loadFlowParameters,
                                                       int numberOfLoadFlowsInParallel,
                                                       MonitoringInput monitoringInput) throws OpenRaoException {
        final MonitoringResult angleMonitoringResult = new AngleMonitoring(loadFlowProvider, loadFlowParameters).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithAngleMonitoring(monitoringInput.getRaoResult(), angleMonitoringResult);
    }

    /**
     * The computation manager can be used by the caller to execute actions before and/or after running the loadflow.
     * In particular, GridCapa relies on it to inject task-id in the MDC in order to bind logs with tasks.
     *
     * @deprecated use AngleMonitoring.runAndUpdateRaoResult instead
     */
    @Deprecated(since = "7.4.0")
    public static RaoResult runAngleAndUpdateRaoResult(String loadFlowProvider,
                                                       LoadFlowParameters loadFlowParameters,
                                                       ComputationManager computationManager,
                                                       int numberOfLoadFlowsInParallel,
                                                       MonitoringInput monitoringInput) throws OpenRaoException {
        final MonitoringResult angleMonitoringResult = new AngleMonitoring(loadFlowProvider, loadFlowParameters, computationManager).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithAngleMonitoring(monitoringInput.getRaoResult(), angleMonitoringResult);
    }

    /**
     * Main function : runs VoltageMonitoring computation on all VoltageCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with VoltageMonitoringResult
     *
     * @deprecated use VoltageMonitoring.runAndUpdateRaoResult instead
     */
    @Deprecated(since = "7.4.0")
    public static RaoResult runVoltageAndUpdateRaoResult(String loadFlowProvider,
                                                         LoadFlowParameters loadFlowParameters,
                                                         int numberOfLoadFlowsInParallel,
                                                         MonitoringInput monitoringInput) {
        final MonitoringResult voltageMonitoringResult = new VoltageMonitoring(loadFlowProvider, loadFlowParameters).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithVoltageMonitoring(monitoringInput.getRaoResult(), voltageMonitoringResult);
    }

    /**
     * The computation manager can be used by the caller to execute actions before and/or after running the loadflow.
     * In particular, GridCapa relies on it to inject task-id in the MDC in order to bind logs with tasks.
     *
     * @deprecated use VoltageMonitoring.runAndUpdateRaoResult instead
     */
    @Deprecated(since = "7.4.0")
    public static RaoResult runVoltageAndUpdateRaoResult(String loadFlowProvider,
                                                         LoadFlowParameters loadFlowParameters,
                                                         ComputationManager computationManager,
                                                         int numberOfLoadFlowsInParallel,
                                                         MonitoringInput monitoringInput) {
        final MonitoringResult voltageMonitoringResult = new VoltageMonitoring(
            loadFlowProvider,
            loadFlowParameters,
            computationManager
        ).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithVoltageMonitoring(monitoringInput.getRaoResult(), voltageMonitoringResult);
    }
}
