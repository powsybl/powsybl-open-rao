/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputation;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityFactorsProvider;
import com.powsybl.sensitivity.SensitivityValue;
import com.farao_community.farao.data.crac_file.CracFile;
import org.ejml.data.DMatrix;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PsdfMatrixCalculatorTest {
    private Network testNetwork;
    private CracFile testCracFile;

    class MockSensitivityComputationFactory implements SensitivityComputationFactory {

        @Override
        public SensitivityComputation create(Network network, ComputationManager computationManager, int i) {
            return new MockSensitivityComputation();
        }
    }

    class MockSensitivityComputation implements SensitivityComputation {

        @Override
        public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider factorsProvider, String stateId, SensitivityComputationParameters parameters) {
            List<SensitivityValue> values = factorsProvider.getFactors(testNetwork).stream()
                    .map(factor -> new SensitivityValue(factor, 1, 2, 3))
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(new SensitivityComputationResults(true, new HashMap<>(), "", values));
        }

        @Override
        public String getName() {
            return "Mock";
        }

        @Override
        public String getVersion() {
            return "0.0";
        }
    }

    @Before
    public void setUp() throws IOException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", NetworkUtilTest.class.getResourceAsStream("/testCase.xiidm"));
        try (InputStream is = PsdfMatrixCalculatorTest.class.getResourceAsStream("/simpleInputs.json")) {
            testCracFile = new ObjectMapper().readValue(is, CracFile.class);
        }
    }

    @Test
    public void testMatrixCalculator() {
        PsdfMatrixCalculator matrixCalculator = new PsdfMatrixCalculator(
                testNetwork,
                testCracFile,
                NetworkIndexMapperUtil.generateBranchMapping(testNetwork),
                new SensitivityComputationService(new MockSensitivityComputationFactory(), Mockito.mock(ComputationManager.class))
        );
        DMatrix matrix = matrixCalculator.computePsdfMatrix(Mockito.mock(FullLineDecompositionParameters.class, Mockito.RETURNS_DEEP_STUBS));
        assertEquals(testNetwork.getBranchCount(), matrix.getNumRows());
        assertEquals(testNetwork.getBranchCount(), matrix.getNumCols());
    }
}
