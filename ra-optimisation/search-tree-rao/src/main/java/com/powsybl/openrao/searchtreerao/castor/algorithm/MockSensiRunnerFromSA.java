package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.action.Action;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.security.SecurityAnalysisReport;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.condition.TrueCondition;
import com.powsybl.security.results.BranchResult;
import com.powsybl.security.results.NetworkResult;
import com.powsybl.security.strategy.ConditionalActions;
import com.powsybl.security.strategy.OperatorStrategy;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.jgrapht.alg.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MockSensiRunnerFromSA {

    public MockSensiRunnerFromSA(RaoParameters raoParameters, ToolProvider toolProvider) {
        this.raoParameters = raoParameters;
        this.toolProvider = toolProvider;
    }

    class FlowResultFromSA implements FlowResult {
        private final SecurityAnalysisResult securityAnalysisResult;
        private final Instant optimizedInstant;
        private final FlowResult initialFlowResult;

        FlowResultFromSA(SecurityAnalysisResult securityAnalysisResult, Instant optimizedInstant, FlowResult initialFlowResult) {
            this.securityAnalysisResult = securityAnalysisResult;
            this.optimizedInstant = optimizedInstant;
            this.initialFlowResult = initialFlowResult;
        }

        private double getFlow(BranchResult branchResult, TwoSides side, Unit unit) {
            switch (unit) {
                case MEGAWATT -> {
                    return (side == TwoSides.ONE) ? branchResult.getP1() : branchResult.getP2();
                }
                case AMPERE -> {
                    return (side == TwoSides.ONE) ? branchResult.getI1() : branchResult.getI2();
                }
                default -> {
                    throw new OpenRaoException("Flows are only available in MW and A");
                }
            }

        }

        @Override
        public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
            NetworkResult networkResult = null;
            if (flowCnec.getState().isPreventive()) {
                networkResult =
                    securityAnalysisResult.getPreContingencyResult().getNetworkResult();
            } else {
                networkResult = securityAnalysisResult.getPostContingencyResults().stream()
                    .filter(r -> r.getContingency().equals(flowCnec.getState().getContingency().orElseThrow())) // TODO : is it really the same contingency object ?
                    .findAny().orElseThrow()
                    .getNetworkResult();
                // TODO : how to differentiate post-contingency instants? or maybe we only need to compute flows after CRAs, others have been computed
            }
            return getFlow(networkResult.getBranchResult(flowCnec.getNetworkElement().getId()), side, unit);
        }

        @Override
        public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant optimizedInstant) {
            if (!this.optimizedInstant.equals(optimizedInstant)) {
                throw new OpenRaoException(String.format("Results are only available for %s instant", this.optimizedInstant));
            }
            return getFlow(flowCnec, side, unit);
        }

        @Override
        public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
            return initialFlowResult.getCommercialFlow(flowCnec, side, unit);
        }

        @Override
        public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
            return initialFlowResult.getPtdfZonalSum(flowCnec, side);
        }

        @Override
        public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
            return initialFlowResult.getPtdfZonalSums();
        }
    }

    class PrePerimeterResultFromSA implements PrePerimeterResult {

        private final SecurityAnalysisReport securityAnalysisReport;
        private final FlowResult flowResult;
        private final ObjectiveFunction objectiveFunction;
        private final ObjectiveFunctionResult objectiveFunctionResult;

        PrePerimeterResultFromSA(SecurityAnalysisReport securityAnalysisReport,
                                 ObjectiveFunction objectiveFunction,
                                 Instant optimizedInstant,
                                 FlowResult initialFlowResult,
                                 RangeActionActivationResult rangeActionActivationResult) {
            this.securityAnalysisReport = securityAnalysisReport;
            this.objectiveFunction = objectiveFunction;
            this.flowResult = new FlowResultFromSA(securityAnalysisReport.getResult(), optimizedInstant, initialFlowResult);
            // TODO : replace status depending on report
            this.objectiveFunctionResult = objectiveFunction.evaluate(flowResult, ComputationStatus.DEFAULT);
        }

        @Override
        public FlowResult getFlowResult() {
            return this.flowResult;
        }

        @Override
        public RangeActionSetpointResult getRangeActionSetpointResult() {
            throw new OpenRaoException("not implemented");
        }

        @Override
        public SensitivityResult getSensitivityResult() {
            throw new OpenRaoException("not implemented");
        }

        @Override
        public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
            return flowResult.getFlow(flowCnec, side, unit);
        }

        @Override
        public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant optimizedInstant) {
            return flowResult.getFlow(flowCnec, side, unit, optimizedInstant);
        }

        @Override
        public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
            return flowResult.getCommercialFlow(flowCnec, side, unit);
        }

        @Override
        public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
            return flowResult.getPtdfZonalSum(flowCnec, side);
        }

        @Override
        public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
            return flowResult.getPtdfZonalSums();
        }

        @Override
        public double getFunctionalCost() {
            return objectiveFunctionResult.getFunctionalCost();
        }

        @Override
        public List<FlowCnec> getMostLimitingElements(int number) {
            return objectiveFunctionResult.getMostLimitingElements(number);
        }

        @Override
        public double getVirtualCost() {
            return objectiveFunctionResult.getVirtualCost();
        }

        @Override
        public Set<String> getVirtualCostNames() {
            return objectiveFunctionResult.getVirtualCostNames();
        }

        @Override
        public double getVirtualCost(String virtualCostName) {
            return objectiveFunctionResult.getVirtualCost(virtualCostName);
        }

        @Override
        public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
            return objectiveFunctionResult.getCostlyElements(virtualCostName, number);
        }

        @Override
        public void excludeContingencies(Set<String> contingenciesToExclude) {
            throw new OpenRaoException("not implemented");
        }

        @Override
        public ObjectiveFunction getObjectiveFunction() {
            return objectiveFunction;
        }

        @Override
        public Set<RangeAction<?>> getRangeActions() {
            throw new OpenRaoException("not implemented");
        }

        @Override
        public double getSetpoint(RangeAction<?> rangeAction) {
            throw new OpenRaoException("not implemented");
        }

        @Override
        public int getTap(PstRangeAction pstRangeAction) {
            throw new OpenRaoException("not implemented");
        }

        @Override
        public ComputationStatus getSensitivityStatus() {
            throw new OpenRaoException("not implemented");
        }

        @Override
        public ComputationStatus getSensitivityStatus(State state) {
            throw new OpenRaoException("not implemented");
        }

        @Override
        public Set<String> getContingencies() {
            throw new OpenRaoException("not implemented");
        }

        @Override
        public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
            throw new OpenRaoException("not implemented");
        }

        @Override
        public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
            throw new OpenRaoException("not implemented");
        }
    }

    private int getInstantIndex(Instant instant) {
        return instant.getOrder() - 2; // 0 for outage, 1 for auto ...
    }

    private final RaoParameters raoParameters;
    private final ToolProvider toolProvider;

    PrePerimeterResult run(Network network, Crac crac,
                           FlowResult initialFlowResult,
                           RangeActionSetpointResult initialRangeActionSetpointResult,
                           Set<String> operatorsNotSharingCras,
                           AppliedRemedialActions appliedRemedialActions,
                           RangeActionActivationResult rangeActionActivationResult) {
        // TODO : fetch rangeActionActivationResult from appliedRemedialActions ?
        /*Network network,
        String workingVariantId,
        ContingenciesProvider contingenciesProvider,
        SecurityAnalysisRunParameters runParameters*/

        if (raoParameters.hasExtension(LoopFlowParametersExtension.class)) {
            if (raoParameters.getExtension(LoopFlowParametersExtension.class).getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                throw new OpenRaoException("Feature not implemented"); // TODO
            }
        }
        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            if (raoParameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfApproximation().shouldUpdatePtdfWithTopologicalChange()) {
                throw new OpenRaoException("Feature not implemented"); // TODO
            }
        }

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs();
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(flowCnecs, toolProvider.getLoopFlowCnecs(flowCnecs), initialFlowResult, initialFlowResult, operatorsNotSharingCras, raoParameters);

        SecurityAnalysisReport report = null;

        Pair<List<OperatorStrategy>, List<Action>> r = computeOperatorStrategies(crac, network, appliedRemedialActions);
       // SecurityAnalysis.run(network, crac.getContingencies().stream().toList(), r.getFirst(), r.getSecond());

        return new PrePerimeterResultFromSA(report, objectiveFunction, crac.getLastInstant(), initialFlowResult, rangeActionActivationResult);
    }

    private Pair<List<OperatorStrategy>, List<Action>> computeOperatorStrategies(Crac crac, Network network, AppliedRemedialActions appliedRas) {
        List<Action> allActions = new ArrayList<>();
        List<OperatorStrategy> strategies = new ArrayList<>();

        for (State state : crac.getStates()) {
            Pair<OperatorStrategy, List<Action>> r = getOperatorStrategy(state, appliedRas, network);
            strategies.add(r.getFirst());
            allActions.addAll(r.getSecond()); // TODO : remove duplicates?
        }

        return Pair.of(strategies, allActions);
    }

    private Pair<OperatorStrategy, List<Action>> getOperatorStrategy(State state, AppliedRemedialActions appliedRas, Network network) {
        List<Action> actions = appliedRas.getAppliedNetworkActions(state).stream().map(NetworkAction::toAction).toList();
        appliedRas.getAppliedRangeActions(state).entrySet().stream().map(
            e -> e.getKey().toAction(network, e.getValue())
        ).forEach(actions::add);

        ConditionalActions conditionalActions = new ConditionalActions("conditional_actions_" + state.getId(), new TrueCondition(), actions.stream().map(Action::getId).toList());
        ContingencyContext contingencyContext = state.isPreventive() ? ContingencyContext.none() : ContingencyContext.specificContingency(state.getContingency().orElseThrow().getId());
        OperatorStrategy operatorStrategy = new OperatorStrategy("operator_strategy_" + state.getId(), contingencyContext, List.of(conditionalActions));
        // TODO : should we put all instants for a given contingency in one operator strategy (then we'd have to use a list of conditional actions, one ConditionalActions per instant)

        return Pair.of(operatorStrategy, actions);
    }
}
