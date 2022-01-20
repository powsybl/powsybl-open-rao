/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.commons.RandomizedString;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.flowbased_domain.*;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.flowbased_computation.FlowbasedComputationParameters;
import com.farao_community.farao.flowbased_computation.FlowbasedComputationProvider;
import com.farao_community.farao.flowbased_computation.FlowbasedComputationResult;
import com.farao_community.farao.flowbased_computation.FlowbasedComputationResultImpl;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;
import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.TECHNICAL_LOGS;

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
    public CompletableFuture<FlowbasedComputationResult> run(Network network, Crac crac, RaoResult raoResult, ZonalData<LinearGlsk> glsk, FlowbasedComputationParameters parameters) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(crac);
        Objects.requireNonNull(glsk);
        Objects.requireNonNull(parameters);

        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();

        if (raoResult == null) {
            TECHNICAL_LOGS.debug("RAO result is null: applying all network actions from CRAC.");
            crac.getStates().forEach(state -> {
                if (state.getInstant().equals(Instant.CURATIVE)) {
                    appliedRemedialActions.addAppliedNetworkActions(state, findAllAvailableRemedialActionsForState(crac, state));
                }
            });
        } else {
            TECHNICAL_LOGS.debug("RAO result is not null: applying remedial actions selected by the RAO.");
            crac.getStates().forEach(state -> {
                if (state.getInstant().equals(Instant.CURATIVE)) {
                    appliedRemedialActions.addAppliedNetworkActions(state, findAppliedNetworkActionsForState(raoResult, state, crac.getNetworkActions()));
                    appliedRemedialActions.addAppliedRangeActions(state, findAppliedRangeActionsForState(raoResult, state));
                }
            });
        }

        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
                .withSensitivityProviderName(parameters.getSensitivityProvider())
                .withDefaultParameters(parameters.getSensitivityAnalysisParameters())
                .withPtdfSensitivities(glsk, crac.getFlowCnecs(), Collections.singleton(Unit.MEGAWATT))
                .withAppliedRemedialActions(appliedRemedialActions)
                .build();

        // Preventive perimeter
        String initialNetworkId = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().cloneVariant(initialNetworkId, INITIAL_STATE_WITH_PRA);
        network.getVariantManager().setWorkingVariant(INITIAL_STATE_WITH_PRA);
        applyPreventiveRemedialActions(raoResult, crac, network);
        SystematicSensitivityResult result = systematicSensitivityInterface.run(network);
        FlowbasedComputationResult flowBasedComputationResult = new FlowbasedComputationResultImpl(FlowbasedComputationResult.Status.SUCCESS, buildFlowbasedDomain(crac, glsk, result));

        // Restore initial variant at the end of the computation
        network.getVariantManager().setWorkingVariant(initialNetworkId);
        network.getVariantManager().removeVariant(INITIAL_STATE_WITH_PRA);

        return CompletableFuture.completedFuture(flowBasedComputationResult);
    }

    private void applyPreventiveRemedialActions(RaoResult raoResult, Crac crac, Network network) {
        if (raoResult == null) {
            TECHNICAL_LOGS.debug("RAO result is null: applying all network actions from CRAC.");
            crac.getNetworkActions().forEach(na -> {
                UsageMethod usageMethod = na.getUsageMethod(crac.getPreventiveState());
                if (usageMethod.equals(UsageMethod.AVAILABLE) || usageMethod.equals(UsageMethod.FORCED)) {
                    na.apply(network);
                } else if (usageMethod.equals(UsageMethod.TO_BE_EVALUATED)) {
                    BUSINESS_WARNS.warn("Network action {} with usage method TO_BE_EVALUATED will not be applied, as we don't have access to the flow results.", na.getId());
                    /*
                     * This method is only used in FlowbasedComputation.
                     * We do not assess the availability of such remedial actions: they're not supposed to exist.
                     * If it is needed in the future, we will have to loop around a sensitivity computation, followed by a
                     * re-assessment of additional available RAs and applying them, then re-running sensitivity, etc
                     * until the list of applied remedial actions stops changing
                     */
                }
            });
        } else {
            TECHNICAL_LOGS.debug("RAO result is not null: applying remedial actions selected by the RAO.");
            crac.getNetworkActions().forEach(na -> {
                if (raoResult.isActivated(crac.getPreventiveState(), na)) {
                    na.apply(network);
                }
            });
            raoResult.getOptimizedSetPointsOnState(crac.getPreventiveState()).forEach((ra, setpoint) -> ra.apply(network, setpoint));
        }
    }

    private DataDomain buildFlowbasedDomain(Crac crac, ZonalData<LinearGlsk> glsk, SystematicSensitivityResult result) {
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

    private List<DataGlskFactors> buildDataGlskFactors(ZonalData<LinearGlsk> glsk) {
        List<DataGlskFactors> glskFactors = new ArrayList<>();
        glsk.getDataPerZone().forEach((s, linearGlsk) -> glskFactors.add(new DataGlskFactors(s, linearGlsk.getGLSKs())));
        return glskFactors;
    }

    private List<DataPostContingency> buildDataPostContingencies(Crac crac, ZonalData<LinearGlsk> glsk, SystematicSensitivityResult result) {
        List<DataPostContingency> postContingencyList = new ArrayList<>();
        crac.getContingencies().forEach(contingency -> postContingencyList.add(buildDataPostContingency(crac, contingency, glsk, result)));
        return postContingencyList;
    }

    private DataPostContingency buildDataPostContingency(Crac crac, Contingency contingency, ZonalData<LinearGlsk> glsk, SystematicSensitivityResult result) {
        return DataPostContingency.builder()
                .contingencyId(contingency.getId())
                .dataMonitoredBranches(buildDataMonitoredBranches(crac, crac.getStates(contingency), glsk, result))
                .build();
    }

    private DataPreContingency buildDataPreContingency(Crac crac, ZonalData<LinearGlsk> glsk, SystematicSensitivityResult result) {
        return DataPreContingency.builder()
                .dataMonitoredBranches(buildDataMonitoredBranches(crac, Set.of(crac.getPreventiveState()), glsk, result))
                .build();
    }

    private List<DataMonitoredBranch> buildDataMonitoredBranches(Crac crac, Set<State> states, ZonalData<LinearGlsk> glsk, SystematicSensitivityResult result) {
        List<DataMonitoredBranch> branchResultList = new ArrayList<>();
        states.forEach(state -> crac.getFlowCnecs(state).forEach(cnec -> branchResultList.add(buildDataMonitoredBranch(cnec, glsk, result))));
        return branchResultList;
    }

    private DataMonitoredBranch buildDataMonitoredBranch(FlowCnec cnec, ZonalData<LinearGlsk> glsk, SystematicSensitivityResult result) {
        double maxThreshold = cnec.getUpperBound(Side.LEFT, Unit.MEGAWATT).orElse(Double.POSITIVE_INFINITY);
        double minThreshold = cnec.getLowerBound(Side.LEFT, Unit.MEGAWATT).orElse(Double.NEGATIVE_INFINITY);
        return new DataMonitoredBranch(
                cnec.getId(),
                cnec.getName(),
                cnec.getState().getInstant().toString(),
                cnec.getNetworkElement().getId(),
                minThreshold,
                maxThreshold,
                zeroIfNaN(result.getReferenceFlow(cnec)),
                buildDataPtdfPerCountry(cnec, glsk, result)
        );
    }

    private List<DataPtdfPerCountry> buildDataPtdfPerCountry(FlowCnec cnec, ZonalData<LinearGlsk> glskProvider, SystematicSensitivityResult result) {
        Map<String, LinearGlsk> glsks = glskProvider.getDataPerZone();
        return glsks.values().stream()
                .map(glsk ->
                        new DataPtdfPerCountry(
                                glsk.getId(),
                                zeroIfNaN(result.getSensitivityOnFlow(glsk.getId(), cnec))
                        )
                ).collect(Collectors.toList());
    }

    /**
     * Find all remedial actions saved in CRAC, on a given network, at a given state.
     *
     * @param crac CRAC that should contain result extension
     * @param state State for which the RAs should be applied
     */
    public static Set<NetworkAction> findAllAvailableRemedialActionsForState(Crac crac, State state) {
        Set<NetworkAction> networkActionsAppl = new HashSet<>();

        crac.getNetworkActions().forEach(na -> {
            UsageMethod usageMethod = na.getUsageMethod(state);
            if (usageMethod.equals(UsageMethod.AVAILABLE) || usageMethod.equals(UsageMethod.FORCED)) {
                networkActionsAppl.add(na);
            } else if (usageMethod.equals(UsageMethod.TO_BE_EVALUATED)) {
                BUSINESS_WARNS.warn("Network action {} with usage method TO_BE_EVALUATED will not be applied, as we don't have access to the flow results.", na.getId());
                /*
                 * This method is only used in FlowbasedComputation.
                 * We do not assess the availability of such remedial actions: they're not supposed to exist.
                 * If it is needed in the future, we will have to loop around a sensitivity computation, followed by a
                 * re-assessment of additional available RAs and applying them, then re-running sensitivity, etc
                 * until the list of applied remedial actions stops changing
                 */
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
