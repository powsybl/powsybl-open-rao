/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputerMultiTS;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizerMultiTS;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerMultiTSInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.MultipleSensitivityResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public class ComplexScenariosInjectionMultiTsTest {
    List<Network> networks;
    List<Crac> cracs;
    RangeActionSetpointResult initialSetpoints;
    List<OptimizationPerimeter> optimizationPerimeters;
    MultipleSensitivityResult initialSensiResult;
    RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC_SCIP.json"));

    @BeforeEach
    public void setUp() {

    }

    private void importNetworksAndCracs(List<String> cracsPaths, List<String> networksPaths) {
        cracs = new ArrayList<>();
        networks = new ArrayList<>();
        for (int i = 0; i < networksPaths.size(); i++) {
            networks.add(Network.read(networksPaths.get(i), getClass().getResourceAsStream("/" + networksPaths.get(i))));
            cracs.add(CracImporters.importCrac(cracsPaths.get(i), getClass().getResourceAsStream("/" + cracsPaths.get(i)), networks.get(i)));
        }
    }

    private RangeActionSetpointResult computeInitialSetpointsResults() {
        Map<RangeAction<?>, Double> setpoints = new HashMap<>();
        for (int i = 0; i < cracs.size(); i++) {
            for (RangeAction<?> rangeAction : cracs.get(i).getRangeActions()) {
                setpoints.put(rangeAction, rangeAction.getCurrentSetpoint(networks.get(i)));
            }
        }
        return new RangeActionSetpointResultImpl(setpoints);
    }

    private List<OptimizationPerimeter> computeOptimizationPerimeters() {
        List<OptimizationPerimeter> perimeters = new ArrayList<>();
        for (Crac crac : cracs) {
            perimeters.add(new PreventiveOptimizationPerimeter(
                crac.getPreventiveState(),
                crac.getFlowCnecs(),
                new HashSet<>(),
                crac.getNetworkActions(),
                crac.getRangeActions()));
        }
        return perimeters;
    }

    private MultipleSensitivityResult runInitialSensi() {
        List<Set<FlowCnec>> cnecsList = new ArrayList<>();
        cracs.forEach(crac -> cnecsList.add(crac.getFlowCnecs()));

        Set<RangeAction<?>> rangeActionsSet = new HashSet<>();
        cracs.forEach(crac -> rangeActionsSet.addAll(crac.getRangeActions()));

        ToolProvider toolProvider = ToolProvider.create().withNetwork(networks.get(0)).withRaoParameters(raoParameters).build(); //the attributes in the class are only used for loopflow things

        SensitivityComputerMultiTS sensitivityComputerMultiTS = SensitivityComputerMultiTS.create()
            .withCnecs(cnecsList)
            .withRangeActions(rangeActionsSet)
            .withOutageInstant(cracs.get(0).getOutageInstant())
            .withToolProvider(toolProvider)
            .build();
        sensitivityComputerMultiTS.compute(networks);
        return sensitivityComputerMultiTS.getSensitivityResults();
    }

    @Test
    public void testThreeTimesteps() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-injection-ts0.json",
            "multi-ts/crac/crac-injection-case1_1.json",
            "multi-ts/crac/crac-injection-case1_2.json"
        );
        List<String> networksPaths = Collections.nCopies(3, "network/12Nodes_3gen_BE.uct");

        importNetworksAndCracs(cracsPaths, networksPaths);
        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        initialSensiResult = runInitialSensi();
        LinearOptimizationResult result = runIteratingLinearOptimization();
        System.out.println(result.getStatus());

        RangeAction<?> injectionGenRa0 = cracs.get(0).getRangeAction("injectionRangeActionGenerator - TS0");
        RangeAction<?> injectionLoadRa0 = cracs.get(0).getRangeAction("injectionRangeActionLoad - TS0");
        RangeAction<?> injectionGenRa1 = cracs.get(1).getRangeAction("injectionRangeActionGenerator - TS1");
        RangeAction<?> injectionLoadRa1 = cracs.get(1).getRangeAction("injectionRangeActionLoad - TS1");
        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        State state1 = optimizationPerimeters.get(1).getMainOptimizationState();
        double injGenOptimizedSetPoint0 = result.getRangeActionActivationResult().getOptimizedSetpoint(injectionGenRa0, state0);
        double injLoadOptimizedSetPoint0 = result.getRangeActionActivationResult().getOptimizedSetpoint(injectionLoadRa0, state0);
        double injGenOptimizedSetPoint1 = result.getRangeActionActivationResult().getOptimizedSetpoint(injectionGenRa1, state1);
        double injLoadOptimizedSetPoint1 = result.getRangeActionActivationResult().getOptimizedSetpoint(injectionLoadRa1, state1);

        System.out.println("--------TS0 BBE1AA1/DDE1AA1---------");
        System.out.println(injGenOptimizedSetPoint0);
        System.out.println(injLoadOptimizedSetPoint0);
        System.out.println("--------TS1 BBE2AA1/DDE2AA1---------");
        System.out.println(injGenOptimizedSetPoint1);
        System.out.println(injLoadOptimizedSetPoint1);

        RangeAction<?> injectionGenRa2 = cracs.get(2).getRangeAction("injectionRangeActionGenerator - TS2");
        RangeAction<?> injectionLoadRa2 = cracs.get(2).getRangeAction("injectionRangeActionLoad - TS2");
        State state2 = optimizationPerimeters.get(2).getMainOptimizationState();
        double injGenOptimizedSetPoint2 = result.getRangeActionActivationResult().getOptimizedSetpoint(injectionGenRa2, state2);
        double injLoadOptimizedSetPoint2 = result.getRangeActionActivationResult().getOptimizedSetpoint(injectionLoadRa2, state2);
        System.out.println("--------TS2 BBE1AA1/DDE1AA1---------");
        System.out.println(injGenOptimizedSetPoint2);
        System.out.println(injLoadOptimizedSetPoint2);

    }

    @Test
    public void testFourTimesteps() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-injection-ts0.json",
            "multi-ts/crac/crac-no-ra-1.json",
            "multi-ts/crac/crac-no-ra-2.json",
            "multi-ts/crac/crac-injection-3.json"
        );
        List<String> networksPaths = Collections.nCopies(4, "network/12Nodes_3gen_BE.uct");

        importNetworksAndCracs(cracsPaths, networksPaths);
        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        initialSensiResult = runInitialSensi();

        LinearOptimizationResult result = runIteratingLinearOptimization();
        System.out.println(result.getStatus());

        RangeAction<?> injGenTs0 = cracs.get(0).getRangeAction("injectionRangeActionGenerator - TS0");

        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        double injGenOptimizedSetTs0 = result.getRangeActionActivationResult().getOptimizedSetpoint(injGenTs0, state0);

        System.out.println("---- TS0 ----");
        System.out.println(injGenOptimizedSetTs0);
    }

    @Test
    public void testInjectionAndPst() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-injection-pst-ts0.json",
            "multi-ts/crac/crac-injection-pst-ts1.json"
        );
        List<String> networksPaths = List.of(
            "multi-ts/network/12Nodes3GenProdBE.uct",
            "multi-ts/network/12Nodes3GenProdDE.uct"
        );

        importNetworksAndCracs(cracsPaths, networksPaths);
        initialSetpoints = computeInitialSetpointsResults();
        optimizationPerimeters = computeOptimizationPerimeters();
        initialSensiResult = runInitialSensi();
        LinearOptimizationResult result = runIteratingLinearOptimization();
        System.out.println(result.getStatus());

        RangeAction<?> injectionGenRa0 = cracs.get(0).getRangeAction("injectionRangeActionGenerator - TS0");
        RangeAction<?> injectionLoadRa0 = cracs.get(0).getRangeAction("injectionRangeActionLoad - TS0");
        RangeAction<?> injectionGenRa1 = cracs.get(1).getRangeAction("injectionRangeActionGenerator - TS1");
        RangeAction<?> injectionLoadRa1 = cracs.get(1).getRangeAction("injectionRangeActionLoad - TS1");
        State state0 = optimizationPerimeters.get(0).getMainOptimizationState();
        State state1 = optimizationPerimeters.get(1).getMainOptimizationState();
        double injGenOptimizedSetPoint0 = result.getRangeActionActivationResult().getOptimizedSetpoint(injectionGenRa0, state0);
        double injLoadOptimizedSetPoint0 = result.getRangeActionActivationResult().getOptimizedSetpoint(injectionLoadRa0, state0);
        double injGenOptimizedSetPoint1 = result.getRangeActionActivationResult().getOptimizedSetpoint(injectionGenRa1, state1);
        double injLoadOptimizedSetPoint1 = result.getRangeActionActivationResult().getOptimizedSetpoint(injectionLoadRa1, state1);

        RangeAction<?> pstRa0 = cracs.get(0).getRangeAction("pst_be - TS0");
        RangeAction<?> pstRa1 = cracs.get(1).getRangeAction("pst_be - TS1");
        double pstOptimizedSetPoint0 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa0, state0);
        double pstOptimizedSetPoint1 = result.getRangeActionActivationResult().getOptimizedSetpoint(pstRa1, state1);

        System.out.println("--------TS0 Injection---------");
        System.out.println(injGenOptimizedSetPoint0);
        System.out.println(injLoadOptimizedSetPoint0);
        System.out.println("--------TS1 Injection---------");
        System.out.println(injGenOptimizedSetPoint1);
        System.out.println(injLoadOptimizedSetPoint1);
        System.out.println("--------TS0 Pst---------");
        System.out.println(pstOptimizedSetPoint0);
        System.out.println("--------TS1 Pst---------");
        System.out.println(pstOptimizedSetPoint1);

    }

    public LinearOptimizationResult runIteratingLinearOptimization() {

        Instant outageInstant = Mockito.mock(Instant.class);

        Set<FlowCnec> allCnecs = new HashSet<>();
        cracs.forEach(crac -> allCnecs.addAll(crac.getFlowCnecs()));

        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(
            allCnecs,
            Collections.emptySet(),
            initialSensiResult,
            initialSensiResult,
            initialSetpoints,
            null,
            Collections.emptySet(),
            raoParameters);

        ToolProvider toolProvider = ToolProvider.create().withNetwork(networks.get(0)).withRaoParameters(raoParameters).build(); //the attributes in the class are only used for loopflow things

        IteratingLinearOptimizerMultiTSInput input = IteratingLinearOptimizerMultiTSInput.create()
            .withNetworks(networks)
            .withOptimizationPerimeters(optimizationPerimeters)
            .withInitialFlowResult(initialSensiResult)
            .withPrePerimeterFlowResult(initialSensiResult)
            .withPrePerimeterSetpoints(initialSetpoints)
            .withPreOptimizationFlowResult(initialSensiResult)
            .withPreOptimizationSensitivityResult(initialSensiResult)
            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
            .withRaActivationFromParentLeaf(new RangeActionActivationResultImpl(initialSetpoints))
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(cracs.get(0).getOutageInstant())
            .build();

        IteratingLinearOptimizerParameters parameters = IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(raoParameters.getObjectiveFunctionParameters().getType())
            .withRangeActionParameters(raoParameters.getRangeActionsOptimizationParameters())
            .withMnecParameters(raoParameters.getExtension(MnecParametersExtension.class))
            .withMaxMinRelativeMarginParameters(raoParameters.getExtension(RelativeMarginsParametersExtension.class))
            .withLoopFlowParameters(raoParameters.getExtension(LoopFlowParametersExtension.class))
            .withUnoptimizedCnecParameters(null)
            .withRaLimitationParameters(new RangeActionLimitationParameters())
            .withSolverParameters(raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver())
            .withMaxNumberOfIterations(raoParameters.getRangeActionsOptimizationParameters().getMaxMipIterations())
            .withRaRangeShrinking(!raoParameters.getRangeActionsOptimizationParameters().getRaRangeShrinking().equals(RangeActionsOptimizationParameters.RaRangeShrinking.DISABLED))
            .build();

        return IteratingLinearOptimizerMultiTS.optimize(input, parameters, outageInstant);

    }
}
