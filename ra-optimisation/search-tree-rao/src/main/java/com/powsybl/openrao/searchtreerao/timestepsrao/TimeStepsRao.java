/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.timestepsrao;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.castor.algorithm.CastorFullOptimization;
import com.powsybl.openrao.searchtreerao.castor.algorithm.CastorOneStateOnly;
import com.powsybl.openrao.searchtreerao.commons.*;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizerMultiTS;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerMultiTSInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class TimeStepsRao implements RaoProvider {
    private static final String TIMESTEPS_RAO = "TimeStepsRao";

    public static LinearOptimizationResult launchMultiRao(List<RaoInput> raoInputsList, RaoParameters parameters) {
        List<Set<NetworkAction>> networkActionsToApply = new ArrayList<>();
        raoInputsList.forEach(raoInput -> {
            RaoResult result = Rao.find("SearchTreeRao").run(raoInput, parameters);
            networkActionsToApply.add(result.getActivatedNetworkActionsDuringState(raoInput.getCrac().getPreventiveState()));
        });

        raoInputsList.forEach(raoInput -> RaoUtil.initData(raoInput, parameters));
        // network actions must be applied after initData()
        for (int i = 0; i < raoInputsList.size(); i++) {
            int finalI = i;
            networkActionsToApply.get(i).forEach(networkAction ->
                // curative state not supported
                networkAction.apply(raoInputsList.get(finalI).getNetwork())
            );
        }
        return runIteratingLinearOptimization(raoInputsList, parameters);
    }

    public static LinearOptimizationResult runIteratingLinearOptimization(List<RaoInput> raoInputsList, RaoParameters raoParameters) {
        List<Crac> cracs = new ArrayList<>();
        List<Network> networks = new ArrayList<>();
        Set<FlowCnec> allCnecs = new HashSet<>();

        raoInputsList.forEach(raoInput -> {
            cracs.add(raoInput.getCrac());
            networks.add(raoInput.getNetwork());
            allCnecs.addAll(raoInput.getCrac().getFlowCnecs());
        });

        RangeActionSetpointResult initialSetpoints = computeInitialSetpointsResults(cracs, networks);
        List<OptimizationPerimeter> optimizationPerimeters = computeOptimizationPerimeters(cracs);
        MultipleSensitivityResult initialSensiResult = runInitialSensi(cracs, networks);

        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(
            allCnecs,
            Collections.emptySet(), // loopflows
            initialSensiResult,
            initialSensiResult,
            initialSetpoints,
            null, //crac(s), not useful (CNECs secured by PST)
            Collections.emptySet(), // operators not sharing CRAs
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

        return IteratingLinearOptimizerMultiTS.optimize(input, parameters, cracs.get(0).getOutageInstant());

    }

    private static RangeActionSetpointResult computeInitialSetpointsResults(List<Crac> cracs, List<Network> networks) {
        Map<RangeAction<?>, Double> setpoints = new HashMap<>();
        for (int i = 0; i < cracs.size(); i++) {
            for (RangeAction<?> rangeAction : cracs.get(i).getRangeActions()) {
                setpoints.put(rangeAction, rangeAction.getCurrentSetpoint(networks.get(i)));
            }
        }
        return new RangeActionSetpointResultImpl(setpoints);
    }

    private static List<OptimizationPerimeter> computeOptimizationPerimeters(List<Crac> cracs) {
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

    private static MultipleSensitivityResult runInitialSensi(List<Crac> cracs, List<Network> networks) {
        List<Set<FlowCnec>> cnecsList = new ArrayList<>();
        cracs.forEach(crac -> cnecsList.add(crac.getFlowCnecs()));

        Set<RangeAction<?>> rangeActionsSet = new HashSet<>();
        cracs.forEach(crac -> rangeActionsSet.addAll(crac.getRangeActions()));

        RaoParameters raoParameters = RaoParameters.load();
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


    // Do not store any big object in this class as it is a static RaoProvider
    // Objects stored in memory will not be released at the end of the RAO run
    @Override
    public String getName() {
        return TIMESTEPS_RAO;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters) {
        return run(raoInput, parameters, null);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {
        RaoUtil.initData(raoInput, parameters);

        // optimization is made on one given state only
        if (raoInput.getOptimizedState() != null) {
            try {
                return new CastorOneStateOnly(raoInput, parameters).run();
            } catch (Exception e) {
                BUSINESS_LOGS.error("Optimizing state \"{}\" failed: ", raoInput.getOptimizedState().getId(), e);
                return CompletableFuture.completedFuture(new FailedRaoResultImpl());
            }
        } else {

            // else, optimization is made on all the states
            return new CastorFullOptimization(raoInput, parameters, targetEndInstant).run();
        }
    }
}
