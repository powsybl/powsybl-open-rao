package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.BranchCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.util.AbstractNetworkPool;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;

public final class MockSensiRunnerFromLF {

    private static class LineFlowResult {
        private final Map<TwoSides, Double> pValues;
        private final Map<TwoSides, Double> iValues;

        LineFlowResult(double p1, double p2, double i1, double i2) {
            pValues = Map.of(TwoSides.ONE, p1, TwoSides.TWO, p2);
            iValues = Map.of(TwoSides.ONE, i1, TwoSides.TWO, i2);
        }

        double getValue(TwoSides side, Unit unit) {
            return switch (unit) {
                case MEGAWATT -> pValues.get(side);
                case AMPERE -> iValues.get(side);
                default -> throw new OpenRaoException("Only MW and A flow values are supported");
            };
        }
    }

    private static class StateFlowResult {
        private final LoadFlowResult.Status computationStatus;
        private final Map<String, LineFlowResult> flowValues = new HashMap<>(); // per network element ID

        StateFlowResult(Network network, Set<FlowCnec> flowCnecs, LoadFlowResult.Status computationStatus) {
            this.computationStatus = computationStatus;
            flowCnecs.stream().map(BranchCnec::getNetworkElement).map(Identifiable::getId)
                .forEach(branchId -> {
                    Branch<?> branch = network.getBranch(branchId);
                    flowValues.put(branchId,
                        new LineFlowResult(branch.getTerminal1().getP(), branch.getTerminal2().getP(), branch.getTerminal1().getI(), branch.getTerminal2().getI())
                    );
                });
        }

        double getValue(FlowCnec flowCnec, TwoSides side, Unit unit) {
            return flowValues.get(flowCnec.getNetworkElement().getId()).getValue(side, unit);
        }
    }

    private static final class FlowResultFromLF implements FlowResult {
        private final Instant optimizedInstant;
        private final FlowResult initialFlowResult;
        private final StateFlowResult defaultFlowResult;
        private final Map<State, StateFlowResult> flowResultPerStateWithAppliedRas;

        private FlowResultFromLF(StateFlowResult defaultFlowResult, Map<State, StateFlowResult> flowResultPerStateWithAppliedRas, Instant optimizedInstant, FlowResult initialFlowResult) {
            this.defaultFlowResult = defaultFlowResult;
            this.flowResultPerStateWithAppliedRas = flowResultPerStateWithAppliedRas;
            this.optimizedInstant = optimizedInstant;
            this.initialFlowResult = initialFlowResult;
        }

        public ComputationStatus getComputationStatus() {
            if (this.defaultFlowResult.computationStatus == LoadFlowResult.Status.FAILED) {
                return ComputationStatus.FAILURE;
            }
            if (this.defaultFlowResult.computationStatus == LoadFlowResult.Status.PARTIALLY_CONVERGED ||
                this.flowResultPerStateWithAppliedRas.values().stream().anyMatch(r -> r.computationStatus == LoadFlowResult.Status.FAILED)) {
                return ComputationStatus.PARTIAL_FAILURE;
            }
            return ComputationStatus.DEFAULT;
        }

        public ComputationStatus getComputationStatus(State state) {
            return convert(flowResultPerStateWithAppliedRas.getOrDefault(state, defaultFlowResult).computationStatus);
        }

        private ComputationStatus convert(LoadFlowResult.Status status) {
            return switch (status) {
                case FAILED -> ComputationStatus.FAILURE;
                case PARTIALLY_CONVERGED -> ComputationStatus.PARTIAL_FAILURE;
                case FULLY_CONVERGED -> ComputationStatus.DEFAULT;
            };
        }

        @Override
        public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
            return flowResultPerStateWithAppliedRas.getOrDefault(flowCnec.getState(), defaultFlowResult).getValue(flowCnec, side, unit);
        }

        @Override
        public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant optimizedInstant) {
            checkOptimizedInstant(optimizedInstant);
            return getFlow(flowCnec, side, unit);
        }

        private void checkOptimizedInstant(Instant optimizedInstant) {
            if (optimizedInstant != this.optimizedInstant) {
                throw new OpenRaoException(String.format("This flow result only contains values for instant %s", this.optimizedInstant));
            }
        }

        @Override
        public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
            if (initialFlowResult != null) {
                return initialFlowResult.getCommercialFlow(flowCnec, side, unit);
            }
            throw new OpenRaoException("not implemented"); // TODO
        }

        @Override
        public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
            if (initialFlowResult != null) {
                return initialFlowResult.getPtdfZonalSum(flowCnec, side);
            }
            throw new OpenRaoException("not implemented"); // TODO
        }

        @Override
        public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
            if (initialFlowResult != null) {
                return initialFlowResult.getPtdfZonalSums();
            }
            throw new OpenRaoException("not implemented"); // TODO
        }
    }

    private static class PrePerimeterResultFromLF implements PrePerimeterResult {

        private final FlowResultFromLF flowResult;
        private final ObjectiveFunction objectiveFunction;
        private final ObjectiveFunctionResult objectiveFunctionResult;

        PrePerimeterResultFromLF(ObjectiveFunction objectiveFunction,
                                 StateFlowResult defaultFlowResult,
                                 Map<State, StateFlowResult> flowResultPerStateWithAppliedRas,
                                 Instant optimizedInstant,
                                 FlowResult initialFlowResult) {
            this.flowResult = new FlowResultFromLF(defaultFlowResult, flowResultPerStateWithAppliedRas, optimizedInstant, initialFlowResult);
            this.objectiveFunction = objectiveFunction;
            this.objectiveFunctionResult = objectiveFunction.evaluate(flowResult, flowResult.getComputationStatus());
        }

        @Override
        public FlowResult getFlowResult() {
            return flowResult;
        }

        @Override
        public RangeActionSetpointResult getRangeActionSetpointResult() {
            throw new OpenRaoException("not implemented"); // TODO
        }

        @Override
        public SensitivityResult getSensitivityResult() {
            throw new OpenRaoException("not implemented"); // TODO
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
            objectiveFunctionResult.excludeContingencies(contingenciesToExclude);
        }

        @Override
        public ObjectiveFunction getObjectiveFunction() {
            return objectiveFunction;
        }

        @Override
        public Set<RangeAction<?>> getRangeActions() {
            throw new OpenRaoException("not implemented"); // TODO
        }

        @Override
        public double getSetpoint(RangeAction<?> rangeAction) {
            throw new OpenRaoException("not implemented"); // TODO
        }

        @Override
        public int getTap(PstRangeAction pstRangeAction) {
            throw new OpenRaoException("not implemented"); // TODO
        }

        @Override
        public ComputationStatus getSensitivityStatus() {
            return flowResult.getComputationStatus();
        }

        @Override
        public ComputationStatus getSensitivityStatus(State state) {
            return flowResult.getComputationStatus(state);
        }

        @Override
        public Set<String> getContingencies() {
            throw new OpenRaoException("not implemented"); // TODO
        }

        @Override
        public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
            throw new OpenRaoException("not implemented"); // TODO
        }

        @Override
        public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
            throw new OpenRaoException("not implemented"); // TODO
        }
    }

    public static PrePerimeterResult run(Crac crac, Network network, RaoParameters raoParameters, FlowResult initialFlowResult, AppliedRemedialActions appliedRemedialActions, int numberOfLoadFlowsInParallel, Set<String> operatorsNotSharingCras, ToolProvider toolProvider) {
        // To test on  Scenario: US 23.1.3

        String lfProvider = raoParameters.getLoadFlowAndSensitivityParameters().getLoadFlowProvider();

        List<State> sortedStates = new ArrayList<>();
        sortedStates.add(crac.getPreventiveState());
        sortedStates.addAll(crac.getStates().stream().sorted(Comparator.comparing(State::getInstant)).toList());
        Map<State, StateFlowResult> flowResultMap = new HashMap<>();

        String initialVariant = network.getVariantManager().getWorkingVariantId();

        try {
            try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), numberOfLoadFlowsInParallel, true)) {
                // TODO : it is not necessary to simulate all states, only all contingencies, and some instants where RAs have changed
                // we can improve performance by computing beforehand the states to compute
                List<ForkJoinTask<Object>> tasks = sortedStates.stream().map(state ->
                    networkPool.submit(() -> {
                        Network networkClone = networkPool.getAvailableNetwork();
                        try {
                            if (!state.isPreventive()) {
                                Contingency contingency = state.getContingency().orElseThrow();
                                if (!contingency.isValid(networkClone)) {
                                    throw new OpenRaoException("Unable to apply contingency " + contingency.getId());
                                }
                                contingency.toModification().apply(networkClone, (ComputationManager) null);
                            }
                            appliedRemedialActions.applyOnNetwork(state, networkClone);
                            LoadFlowResult lfResult = LoadFlow.find(lfProvider).run(networkClone, raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters());
                            flowResultMap.put(state, new StateFlowResult(networkClone, crac.getFlowCnecs(), lfResult.getStatus()));
                        } catch (Exception e) {
                            Thread.currentThread().interrupt();
                            throw new OpenRaoException("LoadFlow error", e);
                        }
                        networkPool.releaseUsedNetwork(networkClone);
                        return null;
                    })).toList();
                for (ForkJoinTask<Object> task : tasks) {
                    try {
                        task.get();
                    } catch (ExecutionException e) {
                        throw new OpenRaoException(e);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // not sure why this is necessary, some tests like 15.11.4.1 seem to crash without it
        network.getVariantManager().setWorkingVariant(initialVariant);

        StateFlowResult defaultFlowResult = flowResultMap.get(crac.getPreventiveState());

        Map<State, StateFlowResult> flowResultMap2 = postTreatMap(crac, network, flowResultMap, appliedRemedialActions);

        Set<FlowCnec> flowCnecs = crac.getFlowCnecs();
        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(flowCnecs, toolProvider.getLoopFlowCnecs(flowCnecs), initialFlowResult, initialFlowResult, operatorsNotSharingCras, raoParameters);

        return new PrePerimeterResultFromLF(objectiveFunction, defaultFlowResult, flowResultMap2, crac.getLastInstant(), initialFlowResult);

    }

    private static Map<State, StateFlowResult> postTreatMap(Crac crac, Network network, Map<State, StateFlowResult> flowResultMap, AppliedRemedialActions appliedRemedialActions) {
        Set<State> statesWithRas = appliedRemedialActions.getStatesWithRa(network);

        Map<State, StateFlowResult> newMap = new HashMap<>(flowResultMap);
        crac.getStates().stream().filter(state -> !state.isPreventive())
            .filter(state -> !statesWithRas.contains(state))
            .forEach(state -> {
                Optional<State> lastStateBeforeWithAppliedRas =
                    crac.getStates(state.getContingency().orElseThrow())
                        .stream().filter(stateBefore -> stateBefore.getInstant().comesBefore(state.getInstant()))
                        .filter(statesWithRas::contains)
                        .max(Comparator.comparing(s -> s.getInstant().getOrder()));
                lastStateBeforeWithAppliedRas.ifPresent(value -> newMap.put(state, flowResultMap.get(value)));
            });
        return newMap;
    }

    private MockSensiRunnerFromLF() {
        // not to be used
    }
}
