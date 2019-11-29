/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.farao_community.farao.data.flow_decomposition_results.FlowDecompositionResults;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowDecompositionCalculatorTest {
    private Network testNetwork;
    private CracFile testCracFile;

    @Before
    public void setUp() throws IOException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", NetworkUtilTest.class.getResourceAsStream("/testCase.xiidm"));
        testCracFile = JsonCracFile.read(NetworkUtilTest.class.getResourceAsStream("/simpleInputs.json"));
    }

    @Test
    public void testComputeDecomposition() {
        int busCount = (int) testNetwork.getBusView().getBusStream().count();
        int branchCount = testNetwork.getBranchCount();
        DMatrixRMaj mockPexMatrix = new DMatrixRMaj(busCount, busCount);
        RandomMatrices_DDRM.fillUniform(mockPexMatrix, new Random(414));
        DMatrixRMaj mockPtdfMatrix = new DMatrixRMaj(branchCount, busCount);
        RandomMatrices_DDRM.fillUniform(mockPtdfMatrix, new Random(415));
        DMatrixRMaj mockPsdfMatrix = new DMatrixRMaj(branchCount, branchCount);
        RandomMatrices_DDRM.fillUniform(mockPsdfMatrix, new Random(416));
        FlowDecompositionCalculator calculator = new FlowDecompositionCalculator(
                testNetwork,
                testCracFile,
                new FullLineDecompositionParameters(),
                mockPexMatrix,
                mockPtdfMatrix,
                mockPsdfMatrix,
                NetworkIndexMapperUtil.generateBusMapping(testNetwork),
                NetworkIndexMapperUtil.generateBranchMapping(testNetwork)
        );

        FlowDecompositionResults results = calculator.computeDecomposition();
        assertEquals(testCracFile.getPreContingency().getMonitoredBranches().size(), results.getPerBranchResults().size());
        assertEquals(testCracFile.getPreContingency().getMonitoredBranches().stream()
                .map(branch -> branch.getBranchId())
                .collect(Collectors.toSet()),
                results.getPerBranchResults().keySet()
        );
    }
}
