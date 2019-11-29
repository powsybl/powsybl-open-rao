/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.network.Bus;
import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.DMatrixSparseTriplet;
import org.ejml.ops.ConvertDMatrixStruct;
import org.ejml.sparse.csc.CommonOps_DSCC;
import org.ejml.sparse.csc.MatrixFeatures_DSCC;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Object dedicated to PEX matrix calculation
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PexMatrixCalculator {
    private static final double EPSILON = 1e-3f;
    private PexGraph pexGraph;
    private Map<PexGraphVertex, Integer> vertexMapper = new HashMap<>();

    public PexMatrixCalculator(PexGraph pexGraph, Map<Bus, Integer> busMapper) {
        this.pexGraph = Objects.requireNonNull(pexGraph);
        Objects.requireNonNull(busMapper);

        pexGraph.vertexSet().forEach(vertex -> vertexMapper.put(vertex, busMapper.get(vertex.getAssociatedBus())));
    }

    private void fillDistributionTripletsWithVertex(PexGraphVertex vertex, DMatrixSparseTriplet distributionTriplet) {
        assert distributionTriplet != null;

        double sumOfLeavingAndAbsorbedFlows = vertex.getAssociatedLoad() + Math.min(vertex.getAssociatedLoad(), vertex.getAssociatedGeneration()) +
                pexGraph.outgoingEdgesOf(vertex).stream().mapToDouble(PexGraphEdge::getAssociatedFlow).sum();
        double transferedFlow = Math.min(vertex.getAssociatedLoad(), vertex.getAssociatedGeneration());

        distributionTriplet.unsafe_set(
                vertexMapper.get(vertex),
                vertexMapper.get(vertex),
                Math.abs(sumOfLeavingAndAbsorbedFlows) < EPSILON ? 0 : transferedFlow / sumOfLeavingAndAbsorbedFlows
        );
    }

    private void fillDistributionTripletsWithEdge(PexGraphEdge edge, DMatrixSparseTriplet distributionTriplet) {
        assert distributionTriplet != null;

        //Analyse edge target
        PexGraphVertex sourceVertex = pexGraph.getEdgeSource(edge);
        PexGraphVertex targetVertex = pexGraph.getEdgeTarget(edge);

        double sumOfLeavingAndAbsorbedFlows = targetVertex.getAssociatedLoad() + Math.min(targetVertex.getAssociatedLoad(), targetVertex.getAssociatedGeneration()) +
                pexGraph.outgoingEdgesOf(targetVertex).stream().mapToDouble(PexGraphEdge::getAssociatedFlow).sum();
        double transferedFlow = edge.getAssociatedFlow();

        double oldValue = distributionTriplet.get(vertexMapper.get(sourceVertex), vertexMapper.get(targetVertex));
        double increase = Math.abs(sumOfLeavingAndAbsorbedFlows) < EPSILON ? 0 : transferedFlow / sumOfLeavingAndAbsorbedFlows;
        double newValue = oldValue + increase;

        distributionTriplet.set(
                vertexMapper.get(sourceVertex),
                vertexMapper.get(targetVertex),
                newValue
        );
    }

    private double getGenerationCoeff(PexGraphVertex vertex) {
        double sumOfLeavingAndAbsorbedFlows = vertex.getAssociatedLoad() + Math.min(vertex.getAssociatedLoad(), vertex.getAssociatedGeneration()) +
                pexGraph.outgoingEdgesOf(vertex).stream().mapToDouble(PexGraphEdge::getAssociatedFlow).sum();
        return Math.abs(sumOfLeavingAndAbsorbedFlows) < EPSILON ? 0 : vertex.getAssociatedGeneration() / sumOfLeavingAndAbsorbedFlows;
    }

    public DMatrix computePexMatrix(double matrixConstructionTolerance) {
        int matrixSize = pexGraph.vertexSet().size();
        double estimatedSparseCoeff = 0.1;

        // easy to work with sparse format, but hard to do computations with
        DMatrixSparseTriplet distributionTriplet = new DMatrixSparseTriplet(matrixSize, matrixSize, (int) (matrixSize * matrixSize * estimatedSparseCoeff));
        pexGraph.edgeSet().forEach(edge -> fillDistributionTripletsWithEdge(edge, distributionTriplet));
        pexGraph.vertexSet().forEach(vertex -> fillDistributionTripletsWithVertex(vertex, distributionTriplet));

        // convert into a format that's easier to perform math with
        DMatrixSparseCSC distributionMatrix = ConvertDMatrixStruct.convert(distributionTriplet, (DMatrixSparseCSC) null);

        // Initialize transfer matrix
        DMatrixSparseCSC transferMatrix = CommonOps_DSCC.identity(matrixSize);
        DMatrixSparseCSC stackMatrix = distributionMatrix.copy();
        for (int i = 0; i < matrixSize; i++) {
            CommonOps_DSCC.add(1., transferMatrix.copy(), 1., stackMatrix, transferMatrix, null, null);
            CommonOps_DSCC.mult(stackMatrix.copy(), distributionMatrix, stackMatrix);
            if (MatrixFeatures_DSCC.isZeros(stackMatrix, matrixConstructionTolerance)) {
                // PW: stop when matrix rank reached to speed up computation
                break;
            }
        }

        // Compute power injection matrix
        double[] generationCoeffs = new double[matrixSize];
        vertexMapper.entrySet().forEach(entry -> generationCoeffs[entry.getValue()] = getGenerationCoeff(entry.getKey()));
        DMatrixSparseCSC generationCoeffMatrix = CommonOps_DSCC.diag(generationCoeffs);

        double[] loadCoeffs = new double[matrixSize];
        vertexMapper.entrySet().forEach(entry -> loadCoeffs[entry.getValue()] = entry.getKey().getAssociatedLoad());
        DMatrixSparseCSC loadCoeffMatrix = CommonOps_DSCC.diag(loadCoeffs);

        // Compute PEX matrix
        DMatrixSparseCSC pexMatrix = transferMatrix;
        CommonOps_DSCC.mult(generationCoeffMatrix, pexMatrix.copy(), pexMatrix);
        CommonOps_DSCC.mult(pexMatrix.copy(), loadCoeffMatrix, pexMatrix);

        return ConvertDMatrixStruct.convert(pexMatrix, (DMatrixRMaj) null);
    }
}
