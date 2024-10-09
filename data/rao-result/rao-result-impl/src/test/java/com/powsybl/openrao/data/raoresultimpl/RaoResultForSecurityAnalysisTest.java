/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresultimpl;

import com.powsybl.action.Action;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.*;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.openloadflow.graph.EvenShiloachGraphDecrementalConnectivityFactory;
import com.powsybl.openloadflow.sa.OpenSecurityAnalysisProvider;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.threshold.BranchThreshold;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.security.*;
import com.powsybl.security.condition.TrueCondition;
import com.powsybl.security.strategy.ConditionalActions;
import com.powsybl.security.strategy.OperatorStrategy;
import com.powsybl.security.strategy.OperationalLimitOverride;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RaoResultForSecurityAnalysisTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private RaoResultImpl raoResult;
    private Crac crac;
    private Network network;

    private void initializeCracAndItsRaoResult() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.createWithPreventiveAndCurativePstRange();
        crac.newNetworkAction().withId("na-id")
            .newSwitchAction().withNetworkElement("any").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("Contingency FR1 FR3").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .newOnContingencyStateUsageRule().withContingency("Contingency FR1 FR2").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.UNAVAILABLE).add()
            .newOnInstantUsageRule().withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        raoResult = new RaoResultImpl(crac);
    }

    @Test
    void testCreateSecurityAnalysisFromRaoResult() {
        initializeCracAndItsRaoResult();
        //TODO: only network actions here, range actions to add
        // PREVENTIVE:
        List<Action> preventiveActions = raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()).stream().flatMap(networkAction -> networkAction.getElementaryActions().stream()).toList();
        // operator strategy with ContingencyContext.none() will not be taken into account in actual load flow but limitsToOverride with ContingencyContext.none() will be
        //OperatorStrategy preventiveOp = new OperatorStrategy(crac.getPreventiveState().getId(), ContingencyContext.none(), new TrueCondition(), preventiveActionIds);
        List<OperationalLimitOverride> limitsToOverride = new ArrayList<>(getNetworkLimits(crac.getPreventiveState(), ContingencyContext.none(), null));
        //POST CONTINGENCIES:
        // do the rao have scenarios with multiple contingencies?
        List<OperatorStrategy> strategies = new ArrayList<>();
        List<Action> actions = new ArrayList<>();
        for (Contingency contingency : crac.getContingencies()) {
            ContingencyContext contingencyContext = ContingencyContext.specificContingency(contingency.getId());
            List<ConditionalActions> conditionalActionsList = new ArrayList<>();
            SortedSet<State> stateChronologicallyOrderedByInstant = crac.getStates(contingency);
            for (State state : stateChronologicallyOrderedByInstant) {
                List<Action> instantActions = raoResult.getActivatedNetworkActionsDuringState(state).stream().flatMap(networkAction -> networkAction.getElementaryActions().stream()).toList();
                ConditionalActions conditionalActions = new ConditionalActions(state.getId(), new TrueCondition(), instantActions.stream().map(Action::getId).toList());
                conditionalActionsList.add(conditionalActions);
                actions.addAll(instantActions);
                limitsToOverride.addAll(getNetworkLimits(state, contingencyContext, conditionalActions.getId()));
            }
            strategies.add(new OperatorStrategy(contingency.getId(), contingencyContext, conditionalActionsList));
        }
        // TODO: we cannot add actions on preventive state on actua open load flow,
        //  so we should play preventive actions on the newtork before the security analysis
        runSecurityAnalysis(strategies, actions, limitsToOverride);
    }

    private void runSecurityAnalysis(List<OperatorStrategy> strategies, List<Action> actions, List<OperationalLimitOverride> limitsToOverride) {
        SecurityAnalysisRunParameters runParameters = new SecurityAnalysisRunParameters()
            //.setFilter(new LimitViolationFilter())
            //.setComputationManager(computationManager)
            //.setSecurityAnalysisParameters(saParameters)
            //.setMonitors(monitors)
            //.setLimitReductions(limitReductions)
            .setOperatorStrategies(strategies)
            .setActions(actions)
            .setLimitsToOverride(limitsToOverride);
        OpenSecurityAnalysisProvider securityAnalysisProvider = new OpenSecurityAnalysisProvider(new DenseMatrixFactory(), new EvenShiloachGraphDecrementalConnectivityFactory<>());
        ContingenciesProvider provider = n -> crac.getContingencies().stream().toList();
        SecurityAnalysisReport report = securityAnalysisProvider.run(network,
                network.getVariantManager().getWorkingVariantId(),
                provider,
                runParameters)
            .join();
        SecurityAnalysisResult result = report.getResult();
        System.out.println(result);
    }

    private List<OperationalLimitOverride> getNetworkLimits(State state, ContingencyContext contingencyContext, String conditionalActionsId) {
        List<OperationalLimitOverride> limitsToOverride = new ArrayList<>();
        for (FlowCnec flowCnec : crac.getFlowCnecs(state)) {
            for (BranchThreshold threshold : flowCnec.getThresholds()) {
                if (threshold.limitsByMax()) {
                    if (threshold.getUnit() == Unit.AMPERE) {
                        limitsToOverride.add(new OperationalLimitOverride(flowCnec.getNetworkElement().getId(), threshold.getSide().toThreeSides(), LimitType.CURRENT, threshold.max().get(), contingencyContext, conditionalActionsId));
                    }
                    // TODO:  MEGAWATT  PERCENT_IMAX
                }
            }
        }
        // TODO AngleCnec angleCnec : crac.getAngleCnecs(state) Threshold threshold: angleCnec.getThresholds()
        // TODO VoltageCnec voltageCnec : crac.getVoltageCnecs(state) Threshold threshold: voltageCnec.getThresholds()
        return limitsToOverride;
    }
}
