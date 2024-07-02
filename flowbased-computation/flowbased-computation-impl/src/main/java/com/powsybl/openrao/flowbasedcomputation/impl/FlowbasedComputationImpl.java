/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.flowbasedcomputation.impl;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.flowbaseddomain.*;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.flowbasedcomputation.*;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityInterface;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Flowbased computation implementation
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(FlowbasedComputationProvider.class)
public class FlowbasedComputationImpl implements FlowbasedComputationProvider {

    private static final String INITIAL_STATE_WITH_PRA = "InitialStateWithPra";

    @Override
    public String getName() {
        return "SimpleIterativeFlowBased";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<FlowbasedComputationResult> run(Network network, Crac crac, RaoResult raoResult, ZonalData<SensitivityVariableSet> glsk, FlowbasedComputationParameters parameters, ReportNode rootReportNode) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(crac);
        Objects.requireNonNull(glsk);
        Objects.requireNonNull(parameters);
        ReportNode reportNode = FlowbasedComputationReports.reportFlowbasedComputation(rootReportNode);

        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();

        if (raoResult == null) {
            FlowbasedComputationReports.reportNullRaoResult(reportNode);
            crac.getStates().forEach(state -> {
                if (state.getInstant().isCurative()) {
                    appliedRemedialActions.addAppliedNetworkActions(state, findAllAvailableRemedialActionsForState(crac, state, reportNode));
                }
            });
        } else {
            FlowbasedComputationReports.reportNotNullRaoResult(reportNode);
            crac.getStates().forEach(state -> {
                if (state.getInstant().isCurative()) {
                    appliedRemedialActions.addAppliedNetworkActions(state, findAppliedNetworkActionsForState(raoResult, state, crac.getNetworkActions()));
                    appliedRemedialActions.addAppliedRangeActions(state, findAppliedRangeActionsForState(raoResult, state));
                }
            });
        }

        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder(reportNode)
                .withSensitivityProviderName(parameters.getSensitivityProvider())
                .withParameters(parameters.getSensitivityAnalysisParameters())
                .withPtdfSensitivities(glsk, crac.getFlowCnecs(), Collections.singleton(Unit.MEGAWATT))
                .withAppliedRemedialActions(appliedRemedialActions)
                .withOutageInstant(crac.getOutageInstant())
                .build();

        // Preventive perimeter
        String initialNetworkId = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().cloneVariant(initialNetworkId, INITIAL_STATE_WITH_PRA);
        network.getVariantManager().setWorkingVariant(INITIAL_STATE_WITH_PRA);
        applyPreventiveRemedialActions(raoResult, crac, network, reportNode);
        SystematicSensitivityResult result = systematicSensitivityInterface.run(network, reportNode);
        FlowbasedComputationResult flowBasedComputationResult = new FlowbasedComputationResultImpl(FlowbasedComputationResult.Status.SUCCESS, buildFlowbasedDomain(crac, glsk, result));

        // Restore initial variant at the end of the computation
        network.getVariantManager().setWorkingVariant(initialNetworkId);
        network.getVariantManager().removeVariant(INITIAL_STATE_WITH_PRA);

        return CompletableFuture.completedFuture(flowBasedComputationResult);
    }

    private void applyPreventiveRemedialActions(RaoResult raoResult, Crac crac, Network network, ReportNode reportNode) {
        if (raoResult == null) {
            FlowbasedComputationReports.reportNullRaoResult(reportNode);
            crac.getNetworkActions().forEach(na -> {
                UsageMethod usageMethod = na.getUsageMethod(crac.getPreventiveState());
                if (usageMethod.equals(UsageMethod.AVAILABLE)) {
                    FlowbasedComputationReports.reportRemedialActionAppliedEvenIfConditionNotChecked(reportNode);
                    na.apply(network);
                }
            });
        } else {
            FlowbasedComputationReports.reportNotNullRaoResult(reportNode);
            crac.getNetworkActions().forEach(na -> {
                if (raoResult.isActivated(crac.getPreventiveState(), na)) {
                    na.apply(network);
                }
            });
            raoResult.getOptimizedSetPointsOnState(crac.getPreventiveState()).forEach((ra, setpoint) -> ra.apply(network, setpoint));
        }
    }

    private DataDomain buildFlowbasedDomain(Crac crac, ZonalData<SensitivityVariableSet> glsk, SystematicSensitivityResult result) {
        return DataDomain.builder()
                .id(RandomizedString.getRandomizedString())
                .name("FlowBased results")
                .description("")
                .sourceFormat("code")
                .dataPreContingency(buildDataPreContingency(crac, glsk, result))
                .dataPostContingency(buildDataPostContingencies(crac, glsk, result))
                .glskData(buildDataGlskFactors(glsk))
                .build();
    }

    private List<DataGlskFactors> buildDataGlskFactors(ZonalData<SensitivityVariableSet> glsk) {
        List<DataGlskFactors> glskFactors = new ArrayList<>();
        glsk.getDataPerZone().forEach((s, linearGlsk) -> glskFactors.add(new DataGlskFactors(s, linearGlsk.getVariables().stream().collect(Collectors.toMap(WeightedSensitivityVariable::getId, variable -> (float) variable.getWeight(), (o1, o2) -> o1)))));
        return glskFactors;
    }

    private List<DataPostContingency> buildDataPostContingencies(Crac crac, ZonalData<SensitivityVariableSet> glsk, SystematicSensitivityResult result) {
        List<DataPostContingency> postContingencyList = new ArrayList<>();
        crac.getContingencies().forEach(contingency -> postContingencyList.add(buildDataPostContingency(crac, contingency, glsk, result)));
        return postContingencyList;
    }

    private DataPostContingency buildDataPostContingency(Crac crac, Contingency contingency, ZonalData<SensitivityVariableSet> glsk, SystematicSensitivityResult result) {
        return DataPostContingency.builder()
                .contingencyId(contingency.getId())
                .dataMonitoredBranches(buildDataMonitoredBranches(crac, crac.getStates(contingency), glsk, result))
                .build();
    }

    private DataPreContingency buildDataPreContingency(Crac crac, ZonalData<SensitivityVariableSet> glsk, SystematicSensitivityResult result) {
        return DataPreContingency.builder()
                .dataMonitoredBranches(buildDataMonitoredBranches(crac, Set.of(crac.getPreventiveState()), glsk, result))
                .build();
    }

    private List<DataMonitoredBranch> buildDataMonitoredBranches(Crac crac, Set<State> states, ZonalData<SensitivityVariableSet> glsk, SystematicSensitivityResult result) {
        List<DataMonitoredBranch> branchResultList = new ArrayList<>();
        states.forEach(state -> crac.getFlowCnecs(state).forEach(cnec -> branchResultList.add(buildDataMonitoredBranch(cnec, glsk, result))));
        return branchResultList;
    }

    private DataMonitoredBranch buildDataMonitoredBranch(FlowCnec cnec, ZonalData<SensitivityVariableSet> glsk, SystematicSensitivityResult result) {
        double maxThreshold = cnec.getUpperBound(TwoSides.ONE, Unit.MEGAWATT).orElse(Double.POSITIVE_INFINITY);
        double minThreshold = cnec.getLowerBound(TwoSides.ONE, Unit.MEGAWATT).orElse(Double.NEGATIVE_INFINITY);
        return new DataMonitoredBranch(
                cnec.getId(),
                cnec.getName(),
                cnec.getState().getInstant().toString(),
                cnec.getNetworkElement().getId(),
                minThreshold,
                maxThreshold,
                zeroIfNaN(result.getReferenceFlow(cnec, TwoSides.ONE)), // TODO : handle both sides if needed
                buildDataPtdfPerCountry(cnec, glsk, result)
        );
    }

    private List<DataPtdfPerCountry> buildDataPtdfPerCountry(FlowCnec cnec, ZonalData<SensitivityVariableSet> glskProvider, SystematicSensitivityResult result) {
        Map<String, SensitivityVariableSet> glsks = glskProvider.getDataPerZone();
        return glsks.values().stream()
                .map(glsk ->
                        new DataPtdfPerCountry(
                                glsk.getId(),
                                zeroIfNaN(result.getSensitivityOnFlow(glsk.getId(), cnec, TwoSides.ONE)) // TODO : handle both sides if needed
                        )
                ).toList();
    }

    /**
     * Find all remedial actions saved in CRAC, on a given network, at a given state.
     *
     * @param crac CRAC that should contain result extension
     * @param state State for which the RAs should be applied
     * @param reportNode
     */
    public static Set<NetworkAction> findAllAvailableRemedialActionsForState(Crac crac, State state, ReportNode reportNode) {
        Set<NetworkAction> networkActionsAppl = new HashSet<>();

        crac.getNetworkActions().forEach(na -> {
            UsageMethod usageMethod = na.getUsageMethod(state);
            if (usageMethod.equals(UsageMethod.AVAILABLE) || usageMethod.equals(UsageMethod.FORCED)) {
                FlowbasedComputationReports.reportRemedialActionAppliedEvenIfConditionNotChecked(reportNode);
                networkActionsAppl.add(na);
            }
        });

        return networkActionsAppl;
    }

    /**
     * Find network actions saved in CRAC result extension on current working variant of given network, at a given state.
     *
     * @param raoResult Result of Rao computation
     * @param state State for which the RAs should be applied
     * @param networkActions All network actions
     */
    public static Set<NetworkAction> findAppliedNetworkActionsForState(RaoResult raoResult, State state, Set<NetworkAction> networkActions) {
        Set<NetworkAction> networkActionsAppl = new HashSet<>();

        networkActions.forEach(na -> {
            if (raoResult.isActivated(state, na)) {
                networkActionsAppl.add(na);
            }
        });
        return networkActionsAppl;
    }

    /**
     * Find range actions saved in CRAC result extension on current working variant of given network, at a given state.
     *
     * @param raoResult Result of Rao computation
     * @param state State for which the RAs should be applied
     */
    public static Map<RangeAction<?>, Double> findAppliedRangeActionsForState(RaoResult raoResult, State state) {
        return new HashMap<>(raoResult.getOptimizedSetPointsOnState(state));
    }

    private double zeroIfNaN(double value) {
        return Double.isNaN(value) ? 0. : value;
    }
}
