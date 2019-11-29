/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityValue;
import com.powsybl.sensitivity.factors.BranchFlowPerInjectionIncrease;
import com.farao_community.farao.data.crac_file.CracFile;
import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;

import java.util.Map;
import java.util.Objects;

/**
 * Object dedicated to PSDF matrix calculation
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PtdfMatrixCalculator {
    private Network network;
    private CracFile cracFile;
    private Map<Bus, Integer> busMapper;
    private Map<Branch, Integer> branchMapper;
    private SensitivityComputationService sensitivityComputationService;

    public PtdfMatrixCalculator(Network network, CracFile cracFile, Map<Bus, Integer> busMapper, Map<Branch, Integer> branchMapper, SensitivityComputationService sensitivityComputationService) {
        this.network = Objects.requireNonNull(network);
        this.cracFile = Objects.requireNonNull(cracFile);
        this.busMapper = Objects.requireNonNull(busMapper);
        this.branchMapper = Objects.requireNonNull(branchMapper);
        this.sensitivityComputationService = Objects.requireNonNull(sensitivityComputationService);
    }

    // Value in generator convention
    private void addValueInTriplet(DMatrix matrix, SensitivityValue value) {
        assert matrix != null;
        assert value != null;
        // By construction in PtdfSensitivityConverter, should always be true
        assert value.getFactor() instanceof BranchFlowPerInjectionIncrease;

        BranchFlowPerInjectionIncrease castedFactor = (BranchFlowPerInjectionIncrease) value.getFactor();
        Injection associatedInjection = NetworkUtil.getInjectionFrom(network, castedFactor.getVariable().getInjectionId());
        Branch associatedBranch = network.getBranch(castedFactor.getFunction().getBranchId());
        Bus injectionConnectionBus = associatedInjection.getTerminal().getBusView().getBus();

        assert busMapper.containsKey(injectionConnectionBus);
        assert branchMapper.containsKey(associatedBranch);

        matrix.set(branchMapper.get(associatedBranch), busMapper.get(injectionConnectionBus), value.getValue());
    }

    private void fillPtdfMatrixUsingSensitivityComputation(DMatrix matrix, FullLineDecompositionParameters parameters) {
        SensitivityComputationResults sensiResults = sensitivityComputationService.compute(new PtdfSensitivityConverter(cracFile), network, network.getVariantManager().getWorkingVariantId(), parameters);
        assert sensiResults != null;
        sensiResults.getSensitivityValues().stream()
                .forEach(sensitivityValue -> addValueInTriplet(matrix, sensitivityValue));
    }

    public DMatrix computePtdfMatrix(FullLineDecompositionParameters parameters) {

        int nbBus = busMapper.size();
        int nbBranch = branchMapper.size();

        DMatrix ptdfMatrix = new DMatrixRMaj(nbBranch, nbBus);
        fillPtdfMatrixUsingSensitivityComputation(ptdfMatrix, parameters);
        return ptdfMatrix;
    }
}
