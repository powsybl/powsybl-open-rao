/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowRunParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.AngleResult;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import com.powsybl.openrao.data.raoresult.api.extension.FlowResult;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.data.raoresult.api.extension.VoltageResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.powsybl.openrao.monitoring.angle.AngleMonitoring;
import com.powsybl.openrao.monitoring.results.AngleMonitoringResultAdapter;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultHelper;
import com.powsybl.openrao.monitoring.results.VoltageMonitoringResultAdapter;
import com.powsybl.openrao.monitoring.voltage.VoltageMonitoring;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.jspecify.annotations.NonNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
        final MonitoringResult angleMonitoringResult = new AngleMonitoring(
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
        final MonitoringResult angleMonitoringResult = new AngleMonitoring(
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
        final MonitoringResult voltageMonitoringResult = new VoltageMonitoring(loadFlowProvider,
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

    // POST-PROCESSING

    /**
     * Extends the content of the original RaoResult with additional angle monitoring results.
     *
     * @param angleMonitoringResult the result obtained after performing angle monitoring computations
     * @param monitoringInput       the input parameters used for the monitoring process
     * @param raoParameters         the parameters used for Rao optimization
     * @return an enhanced RaoResult that incorporates the post-processed angle monitoring results
     */
    public static RaoResult postProcessAngleMonitoringResult(MonitoringResult angleMonitoringResult, MonitoringInput monitoringInput, RaoParameters raoParameters) {
        return postProcessMonitoringResult(
            angleMonitoringResult,
            monitoringInput,
            raoParameters,
            PhysicalParameter.ANGLE,
            AngleResult.class,
            AngleMonitoringResultAdapter::convertToAngleExtension
        );
    }

    /**
     * Extends the content of the original RaoResult with additional voltage monitoring results.
     *
     * @param voltageMonitoringResult the result obtained after performing voltage monitoring computations
     * @param monitoringInput         the input parameters used for the monitoring process
     * @param raoParameters           the parameters used for Rao optimization
     * @return an enhanced RaoResult that incorporates the post-processed voltage monitoring results
     */
    public static RaoResult postProcessVoltageMonitoringResult(MonitoringResult voltageMonitoringResult, MonitoringInput monitoringInput, RaoParameters raoParameters) {
        return postProcessMonitoringResult(
            voltageMonitoringResult,
            monitoringInput,
            raoParameters,
            PhysicalParameter.VOLTAGE,
            VoltageResult.class,
            VoltageMonitoringResultAdapter::convertToVoltageExtension
        );
    }

    private static <E extends Extension<RaoResult>> RaoResult postProcessMonitoringResult(MonitoringResult monitoringResult,
                                                                                          MonitoringInput monitoringInput,
                                                                                          RaoParameters raoParameters,
                                                                                          PhysicalParameter physicalParameter,
                                                                                          Class<E> extensionClass,
                                                                                          Function<MonitoringResult, E> extensionAdapter) {
        monitoringInput.getNetwork().getVariantManager().setWorkingVariant("InitialState");
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        monitoringResult.getAppliedRas().forEach(
            (state, remedialActions) -> remedialActions.stream()
                .filter(NetworkAction.class::isInstance)
                .map(NetworkAction.class::cast)
                .forEach(networkAction -> appliedRemedialActions.addAppliedNetworkAction(state, networkAction))
        );

        RaoResult raoResultWithMonitoring;
        if (monitoringInput.needToRecomputeFlows()) {
            raoResultWithMonitoring = RaoResultHelper.addAppliedRemedialActions(
                monitoringInput.getRaoResult(),
                monitoringInput.getCrac(),
                monitoringInput.getNetwork(),
                appliedRemedialActions,
                new EnumMap<>(Map.of(physicalParameter, monitoringResult.getStatus() == Cnec.SecurityStatus.SECURE)),
                raoParameters,
                ReportNode.NO_OP
            );
        } else {
            raoResultWithMonitoring = includeMonitoringRemedialActions(monitoringResult, monitoringInput, physicalParameter);
        }

        if (raoResultWithMonitoring.getExtension(Metadata.class) == null) {
            raoResultWithMonitoring.addExtension(Metadata.class, Metadata.builder()
                .addComputationStatus(
                    monitoringInput.getCrac().getPreventiveState(),
                    monitoringResult.getStatus() == Cnec.SecurityStatus.FAILURE
                        ? ComputationStatus.FAILURE
                        : ComputationStatus.DEFAULT
                )
                .build());
        }

        Metadata metadata = raoResultWithMonitoring.getExtension(Metadata.class);
        if (metadata != null) {
            raoResultWithMonitoring.removeExtension(Metadata.class);
            Optional<String> executionDetails = metadata.getExecutionDetails();
            if (executionDetails.isPresent()) {
                metadata = new Metadata(metadata, executionDetails.get()
                    + " and went through %s monitoring".formatted(physicalParameter.toString().toLowerCase())
                );
            } else {
                metadata = new Metadata(metadata, "RAO went through %s monitoring".formatted(physicalParameter.toString().toLowerCase()));
            }
            raoResultWithMonitoring.addExtension(Metadata.class, metadata);
        }

        raoResultWithMonitoring.addExtension(
            extensionClass,
            extensionAdapter.apply(monitoringResult)
        );
        return raoResultWithMonitoring;
    }

    private static @NonNull RaoResult includeMonitoringRemedialActions(MonitoringResult monitoringResult, MonitoringInput monitoringInput, PhysicalParameter physicalParameter) {
        RaoResult raoResultWithMonitoring;
        RaoResultImpl.RaoResultImplBuilder builder = RaoResultImpl.builder(monitoringInput.getCrac())
            .withSecurityStatus(PhysicalParameter.FLOW, monitoringInput.getRaoResult().isSecure(PhysicalParameter.FLOW))
            .withSecurityStatus(physicalParameter, monitoringResult.getStatus() == Cnec.SecurityStatus.SECURE);

        if (physicalParameter == PhysicalParameter.ANGLE && monitoringInput.getRaoResult().getExtension(VoltageResult.class) != null) {
            builder.withSecurityStatus(PhysicalParameter.VOLTAGE, monitoringInput.getRaoResult().isSecure(PhysicalParameter.VOLTAGE));
        } else if (physicalParameter == PhysicalParameter.VOLTAGE && monitoringInput.getRaoResult().getExtension(AngleResult.class) != null) {
            builder.withSecurityStatus(PhysicalParameter.ANGLE, monitoringInput.getRaoResult().isSecure(PhysicalParameter.ANGLE));
        }

        monitoringInput.getCrac().getStates().forEach(
            state -> {
                monitoringInput.getRaoResult().getActivatedNetworkActionsDuringState(state)
                    .forEach(networkAction -> builder.addNetworkActionActivation(networkAction, state));
                monitoringInput.getRaoResult().getActivatedRangeActionsDuringState(state)
                    .forEach(rangeAction -> builder.addRangeActionActivation(
                        rangeAction, state, monitoringInput.getRaoResult().getOptimizedSetPointOnState(state, rangeAction)));
                monitoringResult.getAppliedRas(state).forEach(
                    remedialAction -> builder.addNetworkActionActivation((NetworkAction) remedialAction, state));
            }
        );
        raoResultWithMonitoring = builder.build();

        // clone extensions

        FlowResult flowResult = monitoringInput.getRaoResult().getExtension(FlowResult.class);
        if (flowResult != null) {
            raoResultWithMonitoring.addExtension(FlowResult.class, new FlowResult(flowResult));
        }

        AngleResult angleResult = monitoringInput.getRaoResult().getExtension(AngleResult.class);
        if (angleResult != null) {
            raoResultWithMonitoring.addExtension(AngleResult.class, new AngleResult(angleResult));
        }

        VoltageResult voltageResult = monitoringInput.getRaoResult().getExtension(VoltageResult.class);
        if (voltageResult != null) {
            raoResultWithMonitoring.addExtension(VoltageResult.class, new VoltageResult(voltageResult));
        }

        CostResult costResult = monitoringInput.getRaoResult().getExtension(CostResult.class);
        if (costResult != null) {
            raoResultWithMonitoring.addExtension(CostResult.class, new CostResult(costResult));
        }

        Metadata metadata = monitoringInput.getRaoResult().getExtension(Metadata.class);
        if (metadata != null) {
            Metadata.MetadataBuilder metadataBuilder = Metadata.builder();
            metadata.getExecutionDetails().ifPresent(metadataBuilder::withExecutionDetails);
            monitoringInput.getCrac().getStates().forEach(
                state -> metadataBuilder.addComputationStatus(state, getComputationStatus(state, metadata, monitoringResult)));
            raoResultWithMonitoring.addExtension(Metadata.class, new Metadata(metadata, metadata.getExecutionDetails().orElse(null)));
        }
        return raoResultWithMonitoring;
    }

    private static ComputationStatus getComputationStatus(State state, Metadata metadata, MonitoringResult monitoringResult) {
        if (monitoringResult.getStatus() == Cnec.SecurityStatus.FAILURE) {
            return ComputationStatus.FAILURE;
        }
        return metadata.getComputationStatus(state);
    }
}
