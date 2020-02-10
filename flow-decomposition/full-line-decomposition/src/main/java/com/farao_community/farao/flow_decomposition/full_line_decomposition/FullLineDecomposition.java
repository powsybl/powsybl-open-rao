/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.flow_decomposition_results.FlowDecompositionResults;
import com.farao_community.farao.flow_decomposition.FlowDecomposition;
import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;
import org.ejml.data.DMatrix;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Full Line Decomposition (FLD) implementation of flow decomposition feature
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FullLineDecomposition implements FlowDecomposition {
    private static final Logger LOGGER = LoggerFactory.getLogger(FullLineDecomposition.class);

    private Network network;

    private LoadFlowService loadFlowService;

    private SensitivityComputationService sensitivityComputationService;

    FullLineDecomposition(Network network, ComputationManager computationManager, LoadFlow.Runner loadFlowRunner, SensitivityComputationFactory sensitivityComputationFactory) {
        this.network = Objects.requireNonNull(network);
        this.loadFlowService = new LoadFlowService(loadFlowRunner, computationManager);
        this.sensitivityComputationService = new SensitivityComputationService(sensitivityComputationFactory, computationManager);
    }

    @Override
    public CompletableFuture<FlowDecompositionResults> run(String workingVariantId, FlowDecompositionParameters parameters, CracFile cracFile) {
        Objects.requireNonNull(workingVariantId);
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(cracFile);

        FullLineDecompositionParameters fldParameters = parameters.getExtension(FullLineDecompositionParameters.class);
        if (fldParameters == null) {
            throw new FaraoException("Full line decomposition extension of flow decomposition parameters not available");
        }
        return CompletableFuture.supplyAsync(() -> compute(workingVariantId, fldParameters, cracFile));
    }

    private FlowDecompositionResults compute(String workingVariantId, FullLineDecompositionParameters parameters, CracFile cracFile) {
        String previousVariantId = network.getVariantManager().getWorkingVariantId();

        // Initialize computation variant
        network.getVariantManager().setWorkingVariant(workingVariantId);

        LOGGER.info("{} === Initial PST treatment", DateTime.now());
        initialPstTreatment(parameters);

        LOGGER.info("{} === Initial load flow", DateTime.now());
        loadFlowService.compute(network, workingVariantId, parameters);

        LOGGER.info("{} === Bus mapping", DateTime.now());
        Map<Bus, Integer> busMapping = NetworkIndexMapperUtil.generateBusMapping(network);

        LOGGER.info("{} === Branch mapping", DateTime.now());
        Map<Branch, Integer> branchMapping = NetworkIndexMapperUtil.generateBranchMapping(network);

        LOGGER.info("{} === PEX graph generation", DateTime.now());
        PexGraph pexGraph = new PexGraph(network, busMapping, parameters.getInjectionStrategy());

        LOGGER.info("{} === PEX matrix computation", DateTime.now());
        PexMatrixCalculator pexMatrixCalculator = new PexMatrixCalculator(pexGraph, busMapping);
        DMatrix pexMatrix = pexMatrixCalculator.computePexMatrix(parameters.getPexMatrixTolerance());

        LOGGER.info("{} === PTDF matrix computation", DateTime.now());
        PtdfMatrixCalculator ptdfMatrixCalculator = new PtdfMatrixCalculator(network, cracFile, busMapping, branchMapping, sensitivityComputationService);
        DMatrix ptdfMatrix = ptdfMatrixCalculator.computePtdfMatrix(parameters);

        LOGGER.info("{} === Final PST treatment", DateTime.now());
        PsdfMatrixCalculator psdfMatrixCalculator = new PsdfMatrixCalculator(network, cracFile, branchMapping, sensitivityComputationService);
        DMatrix psdfMatrix = psdfMatrixCalculator.computePsdfMatrix(parameters);

        LOGGER.info("{} === Flow decomposition", DateTime.now());
        FlowDecompositionCalculator flowDecompositionCalculator = new FlowDecompositionCalculator(network, cracFile, parameters, pexMatrix, ptdfMatrix, psdfMatrix, busMapping, branchMapping);
        FlowDecompositionResults results = flowDecompositionCalculator.computeDecomposition();

        LOGGER.info("{} === End of computation", DateTime.now());

        // Reset network computation variant
        network.getVariantManager().setWorkingVariant(previousVariantId);

        return results;
    }

    private void initialPstTreatment(FullLineDecompositionParameters parameters) {
        PstPreTreatmentService treatmentService = getPstTreatmentService(parameters);
        treatmentService.treatment(network, parameters);
    }

    private PstPreTreatmentService getPstTreatmentService(FullLineDecompositionParameters parameters) {
        switch (parameters.getPstStrategy()) {
            case NEUTRAL_TAP:
                return new PstPreTreatmentNeutralTap();
            case VIA_PSDF:
                // nothing to do
                return (net, param) -> {
                };
            default:
                throw new IllegalStateException("Type of PST treatment not implemented yet");
        }
    }
}
