/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.optimizationperimeters;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PreventiveOptimizationPerimeter extends AbstractOptimizationPerimeter {

    public PreventiveOptimizationPerimeter(State preventiveState,
                                           Set<FlowCnec> flowCnecs,
                                           Set<FlowCnec> loopFlowCnecs,
                                           Set<NetworkAction> availableNetworkActions,
                                           Set<RangeAction<?>> availableRangeActions) {

        super(preventiveState, flowCnecs, loopFlowCnecs, availableNetworkActions, availableRangeActions.isEmpty() ? Collections.emptyMap() : Map.of(preventiveState, availableRangeActions));

        if (!preventiveState.isPreventive()) {
            throw new OpenRaoException("a PreventiveOptimizationContext must be based on the preventive state");
        }
    }

    public static PreventiveOptimizationPerimeter buildFromBasecaseScenario(final Perimeter preventivePerimeter,
                                                                            final Crac crac,
                                                                            final Network network,
                                                                            final RaoParameters raoParameters,
                                                                            final PrePerimeterResult prePerimeterResult,
                                                                            final ReportNode reportNode) {
        return buildForStates(preventivePerimeter.getRaOptimisationState(), preventivePerimeter.getAllStates(), crac, network, raoParameters, prePerimeterResult, reportNode);
    }

    public static PreventiveOptimizationPerimeter buildWithPreventiveCnecsOnly(final Crac crac,
                                                                               final Network network,
                                                                               final RaoParameters raoParameters,
                                                                               final PrePerimeterResult prePerimeterResult,
                                                                               final ReportNode reportNode) {
        return buildForStates(crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), crac, network, raoParameters, prePerimeterResult, reportNode);
    }

    public static PreventiveOptimizationPerimeter buildForStates(final State preventiveState,
                                                                 final Set<State> allMonitoredStates,
                                                                 final Crac crac,
                                                                 final Network network,
                                                                 final RaoParameters raoParameters,
                                                                 final PrePerimeterResult prePerimeterResult,
                                                                 final ReportNode reportNode) {
        return buildForStates(preventiveState, allMonitoredStates, crac, network, crac.getRangeActions(preventiveState), raoParameters, prePerimeterResult, reportNode);
    }

    public static PreventiveOptimizationPerimeter buildForStates(final State preventiveState,
                                                                 final Set<State> allMonitoredStates,
                                                                 final Crac crac,
                                                                 final Network network,
                                                                 final Set<RangeAction<?>> rangeActions,
                                                                 final RaoParameters raoParameters,
                                                                 final PrePerimeterResult prePerimeterResult,
                                                                 final ReportNode reportNode) {
        Set<State> filteredStates = allMonitoredStates.stream()
            .filter(state -> !prePerimeterResult.getSensitivityStatus(state).equals(ComputationStatus.FAILURE))
            .collect(Collectors.toSet());

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream()
            .filter(flowCnec -> filteredStates.contains(flowCnec.getState()))
            .collect(Collectors.toSet());

        Set<FlowCnec> loopFlowCnecs = AbstractOptimizationPerimeter.getLoopFlowCnecs(flowCnecs, raoParameters, network);

        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions(preventiveState).stream()
            .filter(ra -> RaoUtil.canRemedialActionBeUsed(ra, preventiveState, prePerimeterResult, flowCnecs, network, raoParameters))
            .collect(Collectors.toSet());

        Set<RangeAction<?>> availableRangeActions = rangeActions.stream()
            .filter(ra -> RaoUtil.canRemedialActionBeUsed(ra, preventiveState, prePerimeterResult, flowCnecs, network, raoParameters))
            .filter(ra -> AbstractOptimizationPerimeter.doesPrePerimeterSetpointRespectRange(ra, prePerimeterResult, reportNode))
            .collect(Collectors.toSet());
        removeAlignedRangeActionsWithDifferentInitialSetpoints(availableRangeActions, prePerimeterResult, reportNode);

        // include auto FlowCNECs that are not protected by any ARA in the preventive perimeter
        if (crac.getInstants(InstantKind.AUTO).size() == 1) {
            crac.getStates(crac.getInstant(InstantKind.AUTO))
                .forEach(autoState -> flowCnecs.addAll(getAutoCnecsToDuplicate(autoState, crac, network, reportNode)));
        }

        return new PreventiveOptimizationPerimeter(
            preventiveState,
            flowCnecs,
            loopFlowCnecs,
            availableNetworkActions,
            availableRangeActions
        );
    }

    private static Set<FlowCnec> getAutoCnecsToDuplicate(final State autoState,
                                                         final Crac crac,
                                                         final Network network,
                                                         final ReportNode reportNode) {
        Set<RemedialAction<?>> remedialActions = new HashSet<>();
        remedialActions.addAll(crac.getRangeActions(autoState));
        remedialActions.addAll(crac.getNetworkActions(autoState));
        if (remedialActions.isEmpty()) {
            return Collections.emptySet();
        }

        Set<FlowCnec> autoCnecsToDuplicate = new HashSet<>();
        crac.getFlowCnecs(autoState)
            .stream()
            .filter(flowCnec -> shouldDuplicateAutoCnecInOutageState(remedialActions, flowCnec, network))
            .forEach(flowCnec -> {
                autoCnecsToDuplicate.add(flowCnec);
                // TODO: add report node
                OpenRaoLoggerProvider.BUSINESS_LOGS.info(
                    "FlowCnec {} is not protected by any automaton and will be included in the preventive optimization perimeter",
                    flowCnec.getId()
                );
            });
        return autoCnecsToDuplicate;
    }

    private static boolean shouldDuplicateAutoCnecInOutageState(final Set<RemedialAction<?>> remedialActions,
                                                                final FlowCnec flowCnec,
                                                                final Network network) {
        boolean raForOtherCnecs = false;
        for (RemedialAction<?> remedialAction : remedialActions) {
            for (UsageRule usageRule : remedialAction.getUsageRules()) {
                if (usageRule instanceof OnInstant onInstant && onInstant.getInstant().equals(flowCnec.getState().getInstant())) {
                    return false;
                } else if (usageRule instanceof OnContingencyState onContingencyState && onContingencyState.getState().equals(flowCnec.getState())) {
                    return false;
                } else if (usageRule instanceof OnConstraint<?> onConstraint && onConstraint.getCnec() instanceof FlowCnec && onConstraint.getCnec().getState().equals(flowCnec.getState())) {
                    if (onConstraint.getCnec().equals(flowCnec)) {
                        return false;
                    } else {
                        raForOtherCnecs = true;
                    }
                } else if (usageRule instanceof OnFlowConstraintInCountry onFlowConstraintInCountry
                    && onFlowConstraintInCountry.getInstant().equals(flowCnec.getState().getInstant())
                    && (onFlowConstraintInCountry.getContingency().isEmpty() || flowCnec.getState().getContingency().equals(onFlowConstraintInCountry.getContingency()))) {
                    if (flowCnec.getLocation(network).contains(onFlowConstraintInCountry.getCountry())) {
                        return false;
                    } else {
                        raForOtherCnecs = true;
                    }
                }
            }
        }
        return raForOtherCnecs;
    }

    @Override
    public OptimizationPerimeter copyWithFilteredAvailableHvdcRangeAction(Network network) {
        return new PreventiveOptimizationPerimeter(
            this.getMainOptimizationState(),
            this.getFlowCnecs(),
            this.getLoopFlowCnecs(),
            this.getNetworkActions(),
            this.getRangeActionsWithoutHvdcInAcEmulation(network));
    }
}
