/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.action.Action;
import com.powsybl.action.BoundaryLineAction;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.LoadAction;
import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowRunParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.angle.AngleMonitoring;
import com.powsybl.openrao.data.raoresult.api.extension.AngleResult;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.data.raoresult.api.extension.VoltageResult;
import com.powsybl.openrao.monitoring.redispatching.RedispatchAction;
import com.powsybl.openrao.monitoring.results.AngleMonitoringResultAdapter;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithAngleMonitoring;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import com.powsybl.openrao.monitoring.voltage.VoltageMonitoring;
import com.powsybl.openrao.monitoring.results.VoltageMonitoringResultAdapter;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.searchtreerao.networkpool.AbstractNetworkPool;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.util.RaoResultHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class Monitoring {

    private final String loadFlowProvider;
    private final LoadFlowRunParameters loadFlowRunParameters;

    /**
     * @deprecated use {@link AngleMonitoring} or {@link VoltageMonitoring} instead
     */
    @Deprecated(since = "7.5.0")
    public Monitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        this.loadFlowProvider = loadFlowProvider;
        this.loadFlowRunParameters = new LoadFlowRunParameters().setParameters(loadFlowParameters);
    }

    /**
     * The computation manager can be used by the caller to execute actions before and/or after running the loadflow.
     * In particular, GridCapa relies on it to inject task-id in the MDC in order to bind logs with tasks.
     *
     * @deprecated use {@link AngleMonitoring} or {@link VoltageMonitoring} instead
     */
    @Deprecated(since = "7.5.0")
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
    @Deprecated(since = "7.5.0")
    public static RaoResult runAngleAndUpdateRaoResult(String loadFlowProvider,
                                                       RaoParameters raoParameters,
                                                       int numberOfLoadFlowsInParallel,
                                                       MonitoringInput monitoringInput) throws OpenRaoException {
        final MonitoringResult angleMonitoringResult = new Monitoring(
            loadFlowProvider,
            LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters()
        ).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return postProcessAngleMonitoringResult(angleMonitoringResult, monitoringInput, raoParameters);
    }

    /**
     * The computation manager can be used by the caller to execute actions before and/or after running the loadflow.
     * In particular, GridCapa relies on it to inject task-id in the MDC in order to bind logs with tasks.
     *
     * @deprecated use AngleMonitoring.runAndUpdateRaoResult instead
     */
    @Deprecated(since = "7.5.0")
    public static RaoResult runAngleAndUpdateRaoResult(String loadFlowProvider,
                                                       RaoParameters raoParameters,
                                                       ComputationManager computationManager,
                                                       int numberOfLoadFlowsInParallel,
                                                       MonitoringInput monitoringInput) throws OpenRaoException {
        final MonitoringResult angleMonitoringResult = new Monitoring(
            loadFlowProvider,
            LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters(),
            computationManager
        ).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return postProcessAngleMonitoringResult(angleMonitoringResult, monitoringInput, raoParameters);
    }

    /**
     * Main function : runs VoltageMonitoring computation on all VoltageCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with VoltageMonitoringResult
     *
     * @deprecated use VoltageMonitoring.runAndUpdateRaoResult instead
     */
    @Deprecated(since = "7.5.0")
    public static RaoResult runVoltageAndUpdateRaoResult(String loadFlowProvider,
                                                         RaoParameters raoParameters,
                                                         int numberOfLoadFlowsInParallel,
                                                         MonitoringInput monitoringInput) {
        final MonitoringResult voltageMonitoringResult = new Monitoring(loadFlowProvider,
            LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters()
        ).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return postProcessVoltageMonitoringResult(voltageMonitoringResult, monitoringInput, raoParameters);
    }

    /**
     * The computation manager can be used by the caller to execute actions before and/or after running the loadflow.
     * In particular, GridCapa relies on it to inject task-id in the MDC in order to bind logs with tasks.
     *
     * @deprecated use VoltageMonitoring.runAndUpdateRaoResult instead
     */
    @Deprecated(since = "7.5.0")
    public static RaoResult runVoltageAndUpdateRaoResult(String loadFlowProvider,
                                                         RaoParameters raoParameters,
                                                         ComputationManager computationManager,
                                                         int numberOfLoadFlowsInParallel,
                                                         MonitoringInput monitoringInput) {
        final MonitoringResult voltageMonitoringResult = new VoltageMonitoring(
            loadFlowProvider,
            LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters(),
            computationManager
        ).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return postProcessVoltageMonitoringResult(voltageMonitoringResult, monitoringInput, raoParameters);
    }
}
