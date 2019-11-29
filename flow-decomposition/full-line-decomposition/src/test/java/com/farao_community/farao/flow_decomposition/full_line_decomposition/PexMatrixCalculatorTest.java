/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.ops.ConvertDMatrixStruct;
import org.ejml.sparse.csc.CommonOps_DSCC;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PexMatrixCalculatorTest {
    private static final double EPSILON = 1e-3f;

    private PexGraph pexGraph;
    private Map<Bus, Integer> busMapper;

    @Before
    public void setUp() throws Exception {
        Network testNetwork = Importers.loadNetwork("testCase.xiidm", NetworkUtilTest.class.getResourceAsStream("/testCase.xiidm"));
        busMapper = NetworkIndexMapperUtil.generateBusMapping(testNetwork);
        pexGraph = new PexGraph(testNetwork, busMapper, FullLineDecompositionParameters.InjectionStrategy.DECOMPOSE_INJECTIONS);
    }

    private double relativeError(double a, double b) {
        return a == 0 ? a - b : (a - b) / a;
    }

    private void checkIsColumnSumEqualToLoad(DMatrixSparseCSC pexMatrix, int index, double expectedLoad) {
        DMatrixSparseCSC column = CommonOps_DSCC.identity(pexMatrix.numRows, 1);
        CommonOps_DSCC.extractColumn(pexMatrix, index, column);
        assertEquals(0, relativeError(expectedLoad, Math.abs(CommonOps_DSCC.elementSum(column))), EPSILON);
    }

    private void checkIsRowSumEqualToGen(DMatrixSparseCSC pexMatrix, int index, double expectedGeneration) {
        DMatrixSparseCSC row = CommonOps_DSCC.identity(1, pexMatrix.numCols);
        CommonOps_DSCC.extractRows(pexMatrix, index, index + 1, row);
        assertEquals(0, relativeError(expectedGeneration, Math.abs(CommonOps_DSCC.elementSum(row))), EPSILON);
    }

    private void checkMatrixOkForBus(DMatrixSparseCSC pexMatrix, PexGraphVertex vertex) {
        checkIsColumnSumEqualToLoad(pexMatrix, busMapper.get(vertex.getAssociatedBus()), vertex.getAssociatedLoad());
        checkIsRowSumEqualToGen(pexMatrix, busMapper.get(vertex.getAssociatedBus()), vertex.getAssociatedGeneration());
    }

    private void checkMatrixOk(DMatrix pexMatrix) {
        DMatrixSparseCSC matrixSparseCSC = new DMatrixSparseCSC(pexMatrix.getNumRows(), pexMatrix.getNumCols());
        ConvertDMatrixStruct.convert(pexMatrix, matrixSparseCSC);
        pexGraph.vertexSet().stream().forEach(vertex -> checkMatrixOkForBus(matrixSparseCSC, vertex));
    }

    @Test
    public void computePexMatrix() {
        PexMatrixCalculator calculator = new PexMatrixCalculator(pexGraph, busMapper);
        DMatrix pexMatrix = calculator.computePexMatrix(1e-5);
        checkMatrixOk(pexMatrix);
    }
}
