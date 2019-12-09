/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.pre_processors;

import com.farao_community.farao.closed_optimisation_rao.OptimisationPreProcessor;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.crac_file.PstElement;
import com.farao_community.farao.data.crac_file.RedispatchRemedialActionElement;
import com.farao_community.farao.data.crac_file.RemedialAction;
import com.farao_community.farao.util.FaraoVariantsPool;
import com.farao_community.farao.util.SensitivityComputationService;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.factors.BranchFlowPerInjectionIncrease;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.InjectionIncrease;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.*;
import static com.farao_community.farao.util.ContingencyUtil.applyContingency;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(OptimisationPreProcessor.class)
public class SensitivityPreProcessor implements OptimisationPreProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SensitivityPreProcessor.class);
    private static final String PST_SENSITIVITIES_DATA_NAME = "pst_branch_sensitivities";
    private static final String GEN_SENSITIVITIES_DATA_NAME = "generators_branch_sensitivities";
    private static final String REFERENCE_FLOWS_DATA_NAME = "reference_flows";

    private double redispatchingSensitivityThreshold = 0.0;
    private double pstSensitivityThreshold = 0.0;
    private int numberOfParallelThreads = 1;

    @Override
    public Map<String, Class> dataProvided() {
        Map<String, Class> returnMap = new HashMap<>();
        returnMap.put(PST_SENSITIVITIES_DATA_NAME, Map.class);
        returnMap.put(GEN_SENSITIVITIES_DATA_NAME, Map.class);
        returnMap.put(REFERENCE_FLOWS_DATA_NAME, Map.class);
        return returnMap;
    }

    /**
     * Check if the remedial action is a Redispatch remedial action (i.e. with only
     * one remedial action element and redispatch)
     */
    private boolean isRedispatchRemedialAction(RemedialAction remedialAction) {
        return remedialAction.getRemedialActionElements().size() == 1 &&
                remedialAction.getRemedialActionElements().get(0) instanceof RedispatchRemedialActionElement;
    }

    /**
     * Check if the remedial action is a PST remedial action (i.e. with only
     * one remedial action element and PST)
     */
    private boolean isPstRemedialAction(RemedialAction remedialAction) {
        return remedialAction.getRemedialActionElements().size() == 1 &&
                remedialAction.getRemedialActionElements().get(0) instanceof PstElement;
    }

    @Override
    public void fillData(Network network, CracFile cracFile, ComputationManager computationManager, Map<String, Object> data) {
        Map<Pair<String, String>, Double> genSensitivities = new ConcurrentHashMap<>();
        Map<Pair<String, String>, Double> pstSensitivities = new ConcurrentHashMap<>();
        Map<String, Double> referenceFlows = new ConcurrentHashMap<>();

        initThreshold(data);

        List<Generator> generators = cracFile.getRemedialActions().stream()
                .filter(this::isRedispatchRemedialAction)
                .flatMap(remedialAction -> remedialAction.getRemedialActionElements().stream())
                .map(remedialActionElement -> (RedispatchRemedialActionElement) remedialActionElement)
                .map(genRedispatch -> network.getGenerator(genRedispatch.getId()))
                .collect(Collectors.toList());

        List<TwoWindingsTransformer> twoWindingsTransformers = cracFile.getRemedialActions().stream()
                .filter(this::isPstRemedialAction)
                .flatMap(remedialAction -> remedialAction.getRemedialActionElements().stream())
                .map(remedialActionElement -> (PstElement) remedialActionElement)
                .map(pstElement -> network.getTwoWindingsTransformer(pstElement.getId()))
                .collect(Collectors.toList());

        // Pre-contingency sensitivity computation analysis
        LOGGER.info("Running pre contingency sensitivity computation");
        runSensitivityComputation(
                network,
                cracFile.getPreContingency().getMonitoredBranches(),
                generators,
                twoWindingsTransformers,
                genSensitivities,
                pstSensitivities,
                referenceFlows
        );

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        try (FaraoVariantsPool variantsPool = new FaraoVariantsPool(network, initialVariantId, numberOfParallelThreads)) {
            variantsPool.submit(() -> cracFile.getContingencies().parallelStream().forEach(contingency -> {
                try {
                    LOGGER.info("Running post contingency sensitivity computation for contingency '{}'", contingency.getId());
                    String workingVariant = variantsPool.getAvailableVariant();
                    network.getVariantManager().setWorkingVariant(workingVariant);
                    applyContingency(network, computationManager, contingency);

                    runSensitivityComputation(
                            network,
                            contingency.getMonitoredBranches(),
                            generators,
                            twoWindingsTransformers,
                            genSensitivities,
                            pstSensitivities,
                            referenceFlows
                    );
                    variantsPool.releaseUsedVariant(workingVariant);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            })).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new FaraoException(e);
        }
        network.getVariantManager().setWorkingVariant(initialVariantId);

        data.put(GEN_SENSITIVITIES_DATA_NAME, genSensitivities);
        data.put(PST_SENSITIVITIES_DATA_NAME, pstSensitivities);
        data.put(REFERENCE_FLOWS_DATA_NAME, referenceFlows);
    }

    private void initThreshold(Map<String, Object> data) {
        if (data.containsKey(OPTIMISATION_CONSTANTS_DATA_NAME)) {
            Map<String, Object> optimisationConstants = (Map<String, Object>) data.get(OPTIMISATION_CONSTANTS_DATA_NAME);
            if (optimisationConstants.containsKey(RD_SENSITIVITY_SIGNIFICANCE_THRESHOLD_NAME)) {
                redispatchingSensitivityThreshold = (Double) optimisationConstants.get(RD_SENSITIVITY_SIGNIFICANCE_THRESHOLD_NAME);
            }
            if (optimisationConstants.containsKey(PST_SENSITIVITY_SIGNIFICANCE_THRESHOLD_NAME)) {
                pstSensitivityThreshold = (Double) optimisationConstants.get(PST_SENSITIVITY_SIGNIFICANCE_THRESHOLD_NAME);
            }
            if (optimisationConstants.containsKey(NUMBER_OF_PARALLEL_THREADS_NAME)) {
                numberOfParallelThreads = (Integer) optimisationConstants.get(NUMBER_OF_PARALLEL_THREADS_NAME);
            }
        }
    }

    private void runSensitivityComputation(
            Network network,
            List<MonitoredBranch> monitoredBranches,
            List<Generator> generators,
            List<TwoWindingsTransformer> twoWindingsTransformers,
            Map<Pair<String, String>, Double> genSensitivities,
            Map<Pair<String, String>, Double> pstSensitivities,
            Map<String, Double> referenceFlows) {

        SensitivityFactorsProvider factorsProvider = net -> {
            List<SensitivityFactor> factors = new ArrayList<>();
            monitoredBranches.forEach(monitoredBranch -> {
                String monitoredBranchId = monitoredBranch.getId();
                String monitoredBranchName = monitoredBranch.getName();
                String branchId = monitoredBranch.getBranchId();
                BranchFlow branchFlow = new BranchFlow(monitoredBranchId, monitoredBranchName, branchId);
                generators.forEach(generator -> {
                    String genId = generator.getId();
                    factors.add(new BranchFlowPerInjectionIncrease(branchFlow,
                            new InjectionIncrease(genId, genId, genId)));
                });
                twoWindingsTransformers.forEach(twt -> {
                    String twtId = twt.getId();
                    factors.add(new BranchFlowPerPSTAngle(branchFlow,
                            new PhaseTapChangerAngle(twtId, twtId, twtId)));
                });
            });
            return factors;
        };

        SensitivityComputationResults results = SensitivityComputationService.runSensitivity(network, network.getVariantManager().getWorkingVariantId(), factorsProvider);

        results.getSensitivityValues().forEach(sensitivityValue -> {
            if (sensitivityValue.getFactor() instanceof BranchFlowPerInjectionIncrease) {
                fillSensitivitiesAndReferenceFlows(sensitivityValue, genSensitivities, referenceFlows);
            } else if (sensitivityValue.getFactor() instanceof BranchFlowPerPSTAngle) {
                fillSensitivitiesAndReferenceFlows(sensitivityValue, pstSensitivities, referenceFlows);
            }
        });
    }

    private void fillSensitivitiesAndReferenceFlows(SensitivityValue sensitivityValue, Map<Pair<String, String>, Double> sensitivities, Map<String, Double> referenceFlows) {
        String networkElementId = sensitivityValue.getFactor().getVariable().getId();
        String monitoredBranchId = sensitivityValue.getFactor().getFunction().getId();
        double sensitivity = sensitivityValue.getValue();
        double referenceFlow = sensitivityValue.getFunctionReference();

        /*
         * /!\ Memory optimisation
         *
         * Only keep sensitivity in the map if it is high enough
         */
        double threshold = getThresholdForSensitivityValue(sensitivityValue);
        if (!Double.isNaN(sensitivity) && Math.abs(sensitivity) > threshold) {
            sensitivities.put(Pair.of(monitoredBranchId, networkElementId), sensitivity);
        }
        referenceFlows.put(monitoredBranchId, Double.isNaN(referenceFlow) ? 0. : referenceFlow);
    }

    private double getThresholdForSensitivityValue(SensitivityValue sensitivityValue) {
        SensitivityVariable variable = sensitivityValue.getFactor().getVariable();
        if (variable instanceof InjectionIncrease) {
            return redispatchingSensitivityThreshold;
        } else if (variable instanceof PhaseTapChangerAngle) {
            return pstSensitivityThreshold;
        } else {
            return 0;
        }
    }
}
