/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityValue;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.farao_community.farao.data.crac_file.CracFile;
import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;

import java.util.Map;
import java.util.Objects;

/**
 * Object dedicated to PTDF matrix calculation
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PsdfMatrixCalculator {
    private Network network;
    private CracFile cracFile;
    private Map<Branch, Integer> branchMapper;
    private SensitivityComputationService sensitivityComputationService;

    public PsdfMatrixCalculator(Network network, CracFile cracFile, Map<Branch, Integer> branchMapper, SensitivityComputationService sensitivityComputationService) {
        this.network = Objects.requireNonNull(network);
        this.cracFile = Objects.requireNonNull(cracFile);
        this.branchMapper = Objects.requireNonNull(branchMapper);
        this.sensitivityComputationService = Objects.requireNonNull(sensitivityComputationService);
    }

    // Value in generator convention
    private void addValueInTriplet(DMatrix matrix, SensitivityValue value) {
        assert matrix != null;
        assert value != null;
        // By construction in PtdfSensitivityConverter, should always be true
        assert value.getFactor() instanceof BranchFlowPerPSTAngle;

        BranchFlowPerPSTAngle castedFactor = (BranchFlowPerPSTAngle) value.getFactor();
        Branch associatedPhaseTapChanger = network.getBranch(castedFactor.getVariable().getPhaseTapChangerHolderId());
        Branch associatedBranch = network.getBranch(castedFactor.getFunction().getBranchId());

        assert branchMapper.containsKey(associatedBranch);
        assert branchMapper.containsKey(associatedPhaseTapChanger);

        matrix.set(branchMapper.get(associatedBranch), branchMapper.get(associatedPhaseTapChanger), value.getValue());
    }

    private void fillPsdfMatrixUsingSensitivityComputation(DMatrix matrix, FullLineDecompositionParameters parameters) {
        SensitivityComputationResults sensiResults = sensitivityComputationService.compute(new PsdfSensitivityConverter(cracFile), network, network.getVariantManager().getWorkingVariantId(), parameters);
        assert sensiResults != null;
        sensiResults.getSensitivityValues().stream()
                .forEach(sensitivityValue -> addValueInTriplet(matrix, sensitivityValue));
    }

    public DMatrix computePsdfMatrix(FullLineDecompositionParameters parameters) {
        int nbBranch = branchMapper.size();

        DMatrix psdfMatrix = new DMatrixRMaj(nbBranch, nbBranch);
        fillPsdfMatrixUsingSensitivityComputation(psdfMatrix, parameters);
        return psdfMatrix;
    }
}
