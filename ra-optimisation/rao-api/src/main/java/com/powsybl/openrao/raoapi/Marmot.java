///*
// * Copyright (c) 2020, RTE (http://www.rte-france.com)
// * This Source Code Form is subject to the terms of the Mozilla Public
// * License, v. 2.0. If a copy of the MPL was not distributed with this
// * file, You can obtain one at http://mozilla.org/MPL/2.0/.
// */
//
//package com.powsybl.openrao.raoapi;
//
//import com.google.auto.service.AutoService;
//import com.google.common.base.Supplier;
//import com.google.common.base.Suppliers;
//import com.powsybl.commons.Versionable;
//import com.powsybl.commons.config.PlatformConfig;
//import com.powsybl.commons.util.ServiceLoaderCache;
//import com.powsybl.iidm.network.Network;
//import com.powsybl.openrao.commons.OpenRaoException;
//import com.powsybl.openrao.data.cracapi.Crac;
//import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
//import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
//import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
//import com.powsybl.openrao.data.raoresultapi.RaoResult;
//import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
//import com.powsybl.openrao.raoapi.parameters.RaoParameters;
//import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
//import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
//import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
//import com.powsybl.openrao.searchtreerao.castor.algorithm.CastorFullOptimization;
//import com.powsybl.openrao.searchtreerao.castor.algorithm.CastorOneStateOnly;
//import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
//import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
//import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
//import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
//import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizerMultiTS;
//import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerMultiTSInput;
//import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
//import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
//import com.powsybl.tools.Version;
//
//import java.time.Instant;
//import java.util.*;
//import java.util.concurrent.CompletableFuture;
//
//import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
//import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
//
///**
// * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
// * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
// * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
// * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
// * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
// */
//@AutoService(RaoProvider.class)
//public class Marmot {
//    private static final String TIMESTEPS_RAO = "TimeStepsRao";
//
//
//    private Marmot() {
//        throw new AssertionError("Utility class should not been instantiated");
//    }
//
//    private static final Supplier<List<RaoProvider>> RAO_PROVIDERS
//        = Suppliers.memoize(() -> new ServiceLoaderCache<>(RaoProvider.class).getServices());
//
//    /**
//     * A RA optimisation runner is responsible for providing convenient methods on top of {@link RaoProvider}:
//     * several variants of synchronous and asynchronous run with default parameters.
//     */
//    public static class Runner implements Versionable {
//
//        private final RaoProvider provider;
//
//        public Runner(RaoProvider provider) {
//            this.provider = Objects.requireNonNull(provider);
//        }
//
//        public RaoResult run(List<RaoInput> raoInputsList, RaoParameters parameters, Instant targetEndInstant) {
//            Objects.requireNonNull(raoInputsList, "RAO input should not be null");
//            Objects.requireNonNull(parameters, "parameters should not be null");
//
//            Version openRaoVersion = ServiceLoader.load(Version.class).findFirst().orElseThrow();
//            BUSINESS_WARNS.warn("Running RAO using Open RAO version {} from git commit {}.", openRaoVersion.getMavenProjectVersion(), openRaoVersion.getGitVersion());
//
//            List<Set<NetworkAction>> networkActionsToApply = new ArrayList<>();
//            raoInputsList.forEach(raoInput -> {
//                // Use another rao provider?
//                RaoResult result = Rao.find("SearchTreeRao").run(raoInput, parameters);
//
//                networkActionsToApply.add(result.getActivatedNetworkActionsDuringState(raoInput.getCrac().getPreventiveState()));
//            });
//
//            raoInputsList.forEach(raoInput -> RaoUtil.initData(raoInput, parameters));
//            // network actions must be applied after initData()
//            for (int i = 0; i < raoInputsList.size(); i++) {
//                int finalI = i;
//                networkActionsToApply.get(i).forEach(networkAction ->
//                    // TODO : que faire pour le curatif ?
//                    networkAction.apply(raoInputsList.get(finalI).getNetwork())
//                );
//            }
//            return runIteratingLinearOptimization(raoInputsList, parameters);
//        }
//
//        public RaoResult run(List<RaoInput> raoInputsList, RaoParameters parameters) {
//            return run(raoInputsList, parameters, null);
//        }
//
//        public RaoResult run(List<RaoInput> raoInputsList) {
//            return run(raoInputsList, RaoParameters.load(), null);
//        }
//
//        @Override
//        public String getName() {
//            return provider.getName();
//        }
//
//        @Override
//        public String getVersion() {
//            return provider.getVersion();
//        }
//    }
//
//    /**
//     * Get a runner for a RAO named {@code name}. In the case of a null {@code name}, default
//     * implementation is used.
//     *
//     * @param name name of the RAO implementation, null if we want to use default one
//     * @return a runner for RAO implementation named {@code name}
//     */
//    public static Marmot.Runner find(String name) {
//        return find(name, RAO_PROVIDERS.get(), PlatformConfig.defaultConfig());
//    }
//
//    /**
//     * Get a runner for default RAO implementation.
//     *
//     * @throws OpenRaoException in case we cannot find a default implementation
//     * @return a runner for default RAO implementation
//     */
//    public static Marmot.Runner find() {
//        return find(null);
//    }
//
//    /**
//     * A variant of {@link Rao#find(String)} intended to be used for unit testing that allow passing
//     * an explicit provider list instead of relying on service loader and an explicit {@link PlatformConfig}
//     * instead of global one.
//     *
//     * @param name name of the RAO implementation, null if we want to use default one
//     * @param providers flowbased provider list
//     * @param platformConfig platform config to look for default flowbased implementation name
//     * @return a runner for flowbased implementation named {@code name}
//     */
//    public static Marmot.Runner find(String name, List<RaoProvider> providers, PlatformConfig platformConfig) {
//        Objects.requireNonNull(providers);
//        Objects.requireNonNull(platformConfig);
//
//        if (providers.isEmpty()) {
//            throw new OpenRaoException("No RAO providers found");
//        }
//
//        // if no RAO implementation name is provided through the API we look for information
//        // in platform configuration
//        String raOptimizerName = name != null ? name : platformConfig.getOptionalModuleConfig("rao")
//            .flatMap(mc -> mc.getOptionalStringProperty("default"))
//            .orElse(null);
//        RaoProvider provider;
//        if (providers.size() == 1 && raOptimizerName == null) {
//            // no information to select the implementation but only one provider, so we can use it by default
//            // (that is be the most common use case)
//            provider = providers.get(0);
//        } else {
//            if (providers.size() > 1 && raOptimizerName == null) {
//                // several providers and no information to select which one to choose, we can only throw
//                // an exception
//                List<String> raOptimizerNames = providers.stream().map(RaoProvider::getName).toList();
//                throw new OpenRaoException("Several RAO implementations found (" + raOptimizerNames
//                    + "), you must add configuration to select the implementation");
//            }
//            provider = providers.stream()
//                .filter(p -> p.getName().equals(raOptimizerName))
//                .findFirst()
//                .orElseThrow(() -> new OpenRaoException("RA optimizer provider '" + raOptimizerName + "' not found"));
//        }
//
//        return new Marmot.Runner(provider);
//    }
//
//
//    public static RaoResult run(List<RaoInput> raoInputsList, RaoParameters parameters, Instant targetEndInstant) {
//        return find().run(raoInputsList, parameters, targetEndInstant);
//    }
//
//    public static RaoResult run(List<RaoInput> raoInputsList, RaoParameters parameters) {
//        return find().run(raoInputsList, parameters);
//    }
//
//    public static RaoResult run(List<RaoInput> raoInputsList) {
//        return find().run(raoInputsList);
//    }
//
//    public static LinearOptimizationResult runIteratingLinearOptimization(List<RaoInput> raoInputsList, RaoParameters raoParameters) {
//
//        List<Crac> cracs = new ArrayList<>();
//        List<Network> networks = new ArrayList<>();
//        Set<FlowCnec> allCnecs = new HashSet<>();
//
//        raoInputsList.forEach(raoInput -> {
//            cracs.add(raoInput.getCrac());
//            networks.add(raoInput.getNetwork());
//            allCnecs.addAll(raoInput.getCrac().getFlowCnecs());
//        });
//
//        RangeActionSetpointResult initialSetpoints = computeInitialSetpointsResults(cracs, networks);
//        List<OptimizationPerimeter> optimizationPerimeters = computeOptimizationPerimeters(cracs);
//        MultipleSensitivityResult initialSensiResult = runInitialSensi(cracs, networks);
//
//        //        raoParameters.getRangeActionsOptimizationParameters().setPstModel(pstModel);
//
//        ObjectiveFunction objectiveFunction = ObjectiveFunction.create().build(
//            allCnecs,
//            Collections.emptySet(), // loopflows
//            initialSensiResult,
//            initialSensiResult,
//            initialSetpoints,
//            null, //crac(s), not useful (CNECs secured by PST)
//            Collections.emptySet(), // operators not sharing CRAs
//            raoParameters);
//
//        ToolProvider toolProvider = ToolProvider.create().withNetwork(networks.get(0)).withRaoParameters(raoParameters).build(); //the attributes in the class are only used for loopflow things
//
//        IteratingLinearOptimizerMultiTSInput input = IteratingLinearOptimizerMultiTSInput.create()
//            .withNetworks(networks)
//            .withOptimizationPerimeters(optimizationPerimeters)
//            .withInitialFlowResult(initialSensiResult)
//            .withPrePerimeterFlowResult(initialSensiResult)
//            .withPrePerimeterSetpoints(initialSetpoints)
//            .withPreOptimizationFlowResult(initialSensiResult)
//            .withPreOptimizationSensitivityResult(initialSensiResult)
//            .withPreOptimizationAppliedRemedialActions(new AppliedRemedialActions())
//            .withRaActivationFromParentLeaf(new RangeActionActivationResultImpl(initialSetpoints))
//            .withObjectiveFunction(objectiveFunction)
//            .withToolProvider(toolProvider)
//            .withOutageInstant(cracs.get(0).getOutageInstant())
//            .build();
//
//        IteratingLinearOptimizerParameters parameters = IteratingLinearOptimizerParameters.create()
//            .withObjectiveFunction(raoParameters.getObjectiveFunctionParameters().getType())
//            .withRangeActionParameters(raoParameters.getRangeActionsOptimizationParameters())
//            .withMnecParameters(raoParameters.getExtension(MnecParametersExtension.class))
//            .withMaxMinRelativeMarginParameters(raoParameters.getExtension(RelativeMarginsParametersExtension.class))
//            .withLoopFlowParameters(raoParameters.getExtension(LoopFlowParametersExtension.class))
//            .withUnoptimizedCnecParameters(null)
//            .withRaLimitationParameters(new RangeActionLimitationParameters())
//            .withSolverParameters(raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver())
//            .withMaxNumberOfIterations(raoParameters.getRangeActionsOptimizationParameters().getMaxMipIterations())
//            .withRaRangeShrinking(!raoParameters.getRangeActionsOptimizationParameters().getRaRangeShrinking().equals(RangeActionsOptimizationParameters.RaRangeShrinking.DISABLED))
//            .build();
//
//        return IteratingLinearOptimizerMultiTS.optimize(input, parameters, cracs.get(0).getOutageInstant());
//
//    }
//
//    private static RangeActionSetpointResult computeInitialSetpointsResults(List<Crac> cracs, List<Network> networks) {
//        Map<RangeAction<?>, Double> setpoints = new HashMap<>();
//        for (int i = 0; i < cracs.size(); i++) {
//            for (RangeAction<?> rangeAction : cracs.get(i).getRangeActions()) {
//                setpoints.put(rangeAction, rangeAction.getCurrentSetpoint(networks.get(i)));
//            }
//        }
//        return new RangeActionSetpointResultImpl(setpoints);
//    }
//
//    private static List<OptimizationPerimeter> computeOptimizationPerimeters(List<Crac> cracs) {
//        List<OptimizationPerimeter> perimeters = new ArrayList<>();
//        for (Crac crac : cracs) {
//            perimeters.add(new PreventiveOptimizationPerimeter(
//                crac.getPreventiveState(),
//                crac.getFlowCnecs(),
//                new HashSet<>(),
//                crac.getNetworkActions(),
//                crac.getRangeActions()));
//        }
//        return perimeters;
//    }
//
//    //How to manage multiple inputs????
//    //    public CompletableFuture<RaoResult> run(List<RaoInput> raoInputsList, RaoParameters parameters, Instant targetEndInstant) {
//    //        raoInputsList.forEach(raoInput -> RaoUtil.initData(raoInput, parameters));
//    //        return CompletableFuture.completedFuture(launchMultiRao(raoInputsList, parameters, targetEndInstant));
//    //    }
//
//    private static MultipleSensitivityResult runInitialSensi(List<Crac> cracs, List<Network> networks) {
//        List<Set<FlowCnec>> cnecsList = new ArrayList<>();
//        cracs.forEach(crac -> cnecsList.add(crac.getFlowCnecs()));
//
//        Set<RangeAction<?>> rangeActionsSet = new HashSet<>();
//        cracs.forEach(crac -> rangeActionsSet.addAll(crac.getRangeActions()));
//
//        RaoParameters raoParameters = RaoParameters.load();
//        ToolProvider toolProvider = ToolProvider.create().withNetwork(networks.get(0)).withRaoParameters(raoParameters).build(); //the attributes in the class are only used for loopflow things
//
//        SensitivityComputerMultiTS sensitivityComputerMultiTS = SensitivityComputerMultiTS.create()
//            .withCnecs(cnecsList)
//            .withRangeActions(rangeActionsSet)
//            .withOutageInstant(cracs.get(0).getOutageInstant())
//            .withToolProvider(toolProvider)
//            .build();
//        sensitivityComputerMultiTS.compute(networks);
//        return sensitivityComputerMultiTS.getSensitivityResults();
//    }
//
//
//    // Do not store any big object in this class as it is a static RaoProvider
//    // Objects stored in memory will not be released at the end of the RAO run
//    @Override
//    public String getName() {
//        return TIMESTEPS_RAO;
//    }
//
//    @Override
//    public String getVersion() {
//        return "1.0.0";
//    }
//
//    @Override
//    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters) {
//        return run(raoInput, parameters, null);
//    }
//
//    @Override
//    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {
//        RaoUtil.initData(raoInput, parameters);
//
//        // optimization is made on one given state only
//        if (raoInput.getOptimizedState() != null) {
//            try {
//                return new CastorOneStateOnly(raoInput, parameters).run();
//            } catch (Exception e) {
//                BUSINESS_LOGS.error("Optimizing state \"{}\" failed: ", raoInput.getOptimizedState().getId(), e);
//                return CompletableFuture.completedFuture(new FailedRaoResultImpl());
//            }
//        } else {
//
//            // else, optimization is made on all the states
//            return new CastorFullOptimization(raoInput, parameters, targetEndInstant).run();
//        }
//    }
//}
