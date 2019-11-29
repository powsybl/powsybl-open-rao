/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

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
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import org.ejml.data.DMatrix;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PtdfMatrixCalculatorTest {
    private Network testNetwork;
    private CracFile testCracFile;

    class MockSensitivityComputationFactory implements SensitivityComputationFactory {

        @Override
        public SensitivityComputation create(Network network, ComputationManager computationManager, int i) {
            return new PtdfMatrixCalculatorTest.MockSensitivityComputation();
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
        testCracFile = JsonCracFile.read(NetworkUtilTest.class.getResourceAsStream("/simpleInputs.json"));
    }

    @Test
    public void testMatrixCalculator() {
        PtdfMatrixCalculator matrixCalculator = new PtdfMatrixCalculator(
                testNetwork,
                testCracFile,
                NetworkIndexMapperUtil.generateBusMapping(testNetwork),
                NetworkIndexMapperUtil.generateBranchMapping(testNetwork),
                new SensitivityComputationService(new PtdfMatrixCalculatorTest.MockSensitivityComputationFactory(), Mockito.mock(ComputationManager.class))
        );
        DMatrix matrix = matrixCalculator.computePtdfMatrix(Mockito.mock(FullLineDecompositionParameters.class, Mockito.RETURNS_DEEP_STUBS));
        assertEquals(testNetwork.getBranchCount(), matrix.getNumRows());
        assertEquals(testNetwork.getBusView().getBusStream().count(), matrix.getNumCols());
    }
}
