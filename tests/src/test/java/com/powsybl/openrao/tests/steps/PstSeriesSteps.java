/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.steps;

import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.CnecValue;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.util.AbstractNetworkPool;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class PstSeriesSteps {
    private static final double EPSILON = 1.;
    LoadFlowParameters loadFlowParameters;
    List<TapInfo> taps = new ArrayList<>();
    List<CnecMarginInfo> cnecMarginInfo = new ArrayList<>();
    Crac crac;
    RaoResult raoResult;
    RaoParameters raoParameters;

    String arkalePstNe = "_e071a1d4-fef5-1bd9-5278-d195c5597b6e";
    String pragnyPstNe = "_f82152ac-578e-500e-97db-84e788c471ee";
    String arkaleArgiaCnecNe = "_1d9c658e-1a01-c0ee-d127-a22e1270a242 + _2e81de07-4c22-5aa1-9683-5e51b054f7f8";
    String pragnBiescasCnecNe = "_0a3cbdb0-cd71-52b0-b93d-cb48c9fea3e2 + _6f6b15b3-9bcc-7864-7669-522e9f06e931";

    private void setUp(String cracTimestamp) throws IOException {
        CommonTestData.loadData(cracTimestamp);
        crac = CommonTestData.getCrac();
        raoResult = CommonTestData.getRaoResult();
        raoParameters = CommonTestData.getRaoParameters();
        loadFlowParameters = raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters();
    }


    // TODO : problème de reproductibilité des tests ?
    @When("I compute loadFlow with PST in regulation mode at {}")
    public void iComputeLoadFlow(String cracTimestamp) throws IOException {
        runRegulationLoadFLow(cracTimestamp);
    }
    private void runRegulationLoadFLow(String cracTimestamp) throws IOException {
        setUp(cracTimestamp);
        Network inputNetwork = CommonTestData.getNetwork();

        // Apply PRAs
        State preventiveState = crac.getPreventiveState();
        if (Objects.nonNull(preventiveState)) {
            applyOptimalRemedialActions(preventiveState, inputNetwork, raoResult);
        }

        // Select contingency states : most limiting element at an optimized instant was ARKALE or BIESCAS.
        List<CnecMarginInfo> limitingCnecInSeries = extractLimitingElementsFromRaoResult();
        Set<State> contingencyStates = limitingCnecInSeries.stream().map(CnecMarginInfo::getCnec).map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());

        // loop on contingency states
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(inputNetwork, inputNetwork.getVariantManager().getWorkingVariantId(), Math.min(2, contingencyStates.size()), true)) {
            List<ForkJoinTask<Object>> tasks = contingencyStates.stream().map(state ->
                networkPool.submit(() -> {
                    Network networkClone = networkPool.getAvailableNetwork();
                    Contingency contingency = state.getContingency().orElseThrow();

                    if (!contingency.isValid(networkClone)) {
                        System.out.println("unvalid contingency");
                        networkPool.releaseUsedNetwork(networkClone);
                        return null;
                    }
                    contingency.toModification().apply(networkClone, (ComputationManager) null);
                    applyOptimalRemedialActionsOnContingencyState(state, networkClone, crac, raoResult);

                    runLoadFlowWithoutRegulation(networkClone, state);

                    // Log limiting cnecs' margins
                    Set<FlowCnec> currentStateToBeMonitoredCnecs = limitingCnecInSeries.stream().map(CnecMarginInfo::getCnec).filter(cnec -> cnec.getState().equals(state)).collect(Collectors.toSet());
                    currentStateToBeMonitoredCnecs.forEach(cnec -> cnecMarginInfo.add(new CnecMarginInfo(cnec, cnec.getState().getInstant(), cnec.computeMargin(networkClone, Unit.AMPERE))));

                    // TODO : compute regulationValue based on margin
                    // 1) compute worse margin = > can they be different ?
//                    limitingCnecInSeries.stream().map(CnecMarginInfo::getCnec).filter(cnec -> cnec.getNetworkElement().equals(pragnBiescasCnecNe)).map(CnecMarginInfo::get)
//                        CnecMarginInfo cnec;
//                    cnec.getCnec().computeSecurityStatus(networkClone, Unit.AMPERE)
//                    // 2) define regulating value as the flow needed to make the line secure
//                        // if lf fails, it will be because the situation can't be garanteed secure
//                    setPstInSeriesToRegulating(networkClone, pragnyPstNe, pragnyRegulationValue);
//                    setPstInSeriesToRegulating(networkClone, arkalePstNe, arkaleRegulationValue);


                    runLoadFlowWithRegulation(networkClone, state);

                    currentStateToBeMonitoredCnecs.forEach(cnec -> {
                        cnecMarginInfo.stream().filter(stateCnec -> cnec.equals(stateCnec.getCnec())).findFirst().get().setMarginAfter(cnec.computeMargin(networkClone, Unit.AMPERE));
                    });
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
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }

        evaluateTapInfo();
    }

    private List<CnecMarginInfo> extractLimitingElementsFromRaoResult() {
        List<Instant> instants = crac.getSortedInstants();
        List<CnecMarginInfo> limitingCnecs = new ArrayList<>();
        instants.forEach(instant -> {
            double minMargin = -raoResult.getFunctionalCost(instant);
            crac.getStates(instant).forEach(state -> {
                crac.getFlowCnecs().stream().filter(flowCnec -> flowCnec.getNetworkElement().getId().equals(arkaleArgiaCnecNe) ||
                        flowCnec.getNetworkElement().getId().equals(pragnBiescasCnecNe)).filter(cnecInList -> cnecInList.getState().equals(state)).collect(Collectors.toSet()).
                    forEach(cnec -> {
                        if (Math.abs(raoResult.getMargin(instant, cnec, Unit.AMPERE) - minMargin) < EPSILON) {
                            limitingCnecs.add(new CnecMarginInfo(cnec, instant, minMargin));
                        }
                    });
            });
        });
        return limitingCnecs;
    }

    private boolean runLoadFlowWithoutRegulation(Network network, State state) {
        loadFlowParameters.setPhaseShifterRegulationOn(false);
        LoadFlowResult loadFlowResult = LoadFlow.find("OpenLoadFlow")
            .run(network, loadFlowParameters);
        System.out.println(String.format("LoadFlow without regulation status for state %s is %s", state.getId(), loadFlowResult.getStatus()));

        return loadFlowResult.isFailed();
    }

    private void setPstInSeriesToRegulating(Network network, String pstNe, double regulationValue) {
        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(pstNe);

        if (Objects.nonNull(twt)) {
            twt.getPhaseTapChanger().setRegulating(true);
            twt.getPhaseTapChanger().setRegulationMode(PhaseTapChanger.RegulationMode.CURRENT_LIMITER);
            twt.getPhaseTapChanger().setRegulationValue(regulationValue);
        }
    }
    private boolean runLoadFlowWithRegulation(Network network, State state) {
        // 1) look at available psts after RA application, before regulation loadflow
        // TODO : look up for what objects twoWindingTransformer.getPhaseTapChanger() is null
        Set<String> pstNetworkElements = Set.of("_e071a1d4-fef5-1bd9-5278-d195c5597b6e", "_f82152ac-578e-500e-97db-84e788c471ee");
        network.getTwoWindingsTransformers().forEach(twt -> {
//            if (pstNetworkElements.contains(twt.getId()) {
//                twt.getPhaseTapChanger().setRegulating(true);
//                twt.getPhaseTapChanger().setRegulationMode(PhaseTapChanger.RegulationMode.CURRENT_LIMITER);
//                // TODO : compute Regulation Value
//                twt.getPhaseTapChanger().setRegulationValue();
//            }

            if (Objects.nonNull(twt.getPhaseTapChanger())) {
                // Select PSTs in series with cnecs
                // TODO : investigate setRegulating
                twt.getPhaseTapChanger().setRegulating(true);
                taps.add(new TapInfo(state, twt.getNameOrId(), twt.getPhaseTapChanger().getTapPosition()));
            }
        });


        // Testing manual pst setting
//        String ne = "_f82152ac-578e-500e-97db-84e788c471ee";
//        if (state.getInstant().isCurative()) {
//            network.getTwoWindingsTransformer(ne).getPhaseTapChanger().setTapPosition(24);
//        }

        // 2) compute loadflow
        loadFlowParameters.setPhaseShifterRegulationOn(true);
        LoadFlowResult loadFlowResult = LoadFlow.find("OpenLoadFlow")
            .run(network, loadFlowParameters);

        // export network
        Properties params = new Properties();
//        if (state.getInstant().isCurative()) {
//            network.write("XIIDM", params, Path.of("divergentNetwork.iidm"));
//        }
        String newParams = loadFlowParameters.toString();

        // Log taps
        network.getTwoWindingsTransformers().forEach(twoWindingTransformer -> {
            if (Objects.nonNull(twoWindingTransformer.getPhaseTapChanger())) {
                taps.stream().filter(pstInfo -> (pstInfo.getNameOrId().equals(twoWindingTransformer.getNameOrId()) && pstInfo.getState().equals(state)))
                    .findFirst().get().setTapAfter(twoWindingTransformer.getPhaseTapChanger().getTapPosition());
            }
        });

        System.out.println(String.format("LoadFlow with regulation status for state %s is %s", state.getId(), loadFlowResult.getStatus()));
        return loadFlowResult.isFailed();
    }

    private void evaluateTapInfo() {
        taps.stream().forEach(pstInfo -> {
//            if (pstInfo.getTapAfter() != pstInfo.getTapBefore()) {
                System.out.println(String.format("Pst %s for state %s : %s -> %s", pstInfo.getNameOrId(), pstInfo.getState().getId(), pstInfo.getTapBefore(), pstInfo.getTapAfter()));
//            }
        });
        cnecMarginInfo.stream().forEach(cnecInfo -> {
            System.out.println(String.format("Cnec %s  : %.2f A -> %.2f A", cnecInfo.getCnec().getId(), cnecInfo.getMarginBefore(), cnecInfo.getMarginAfter()));
        });
    }

    private class TapInfo {
        State state;
        String nameOrId;
        Integer tapBefore;
        Integer tapAfter;

        public State getState() {
            return state;
        }

        private TapInfo(State state, String nameOrId, Integer tapBefore) {
            this.state = state;
            this.nameOrId = nameOrId;
            this.tapBefore = tapBefore;
        }

        private void setTapAfter(Integer tapAfter) {
            this.tapAfter = tapAfter;
        }

        public String getNameOrId() {
            return nameOrId;
        }

        public Integer getTapBefore() {
            return tapBefore;
        }

        public Integer getTapAfter() {
            return tapAfter;
        }
    }

    private class CnecMarginInfo {
        FlowCnec cnec;
        Instant instant;
        double marginBefore;
        double marginAfter;

        public double getMarginBefore() {
            return marginBefore;
        }

        public double getMarginAfter() {
            return marginAfter;
        }

        public FlowCnec getCnec() {
            return cnec;
        }

        private CnecMarginInfo(FlowCnec cnec, Instant instant, double marginBefore) {
            this.cnec = cnec;
            this.instant = instant;
            this.marginBefore = marginBefore;
        }
        public void setMarginAfter(double marginAfter) {
            this.marginAfter = marginAfter;
        }
    }

    private void applyOptimalRemedialActions(State state, Network network, RaoResult raoResult) {
        raoResult.getActivatedNetworkActionsDuringState(state)
            .forEach(na -> na.apply(network));
        raoResult.getActivatedRangeActionsDuringState(state)
            .forEach(ra -> ra.apply(network, raoResult.getOptimizedSetPointOnState(state, ra)));
    }

    private void applyOptimalRemedialActionsOnContingencyState(State state, Network network, Crac crac, RaoResult raoResult) {
        if (state.getInstant().isCurative()) {
            Optional<Contingency> contingency = state.getContingency();
            crac.getStates(contingency.orElseThrow()).forEach(contingencyState ->
                applyOptimalRemedialActions(state, network, raoResult));
        } else {
            applyOptimalRemedialActions(state, network, raoResult);
        }
    }
}