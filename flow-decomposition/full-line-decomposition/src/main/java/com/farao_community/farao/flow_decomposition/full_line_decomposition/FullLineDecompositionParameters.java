/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.google.common.collect.ImmutableMap;
import com.powsybl.commons.extensions.AbstractExtension;
import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;

import java.util.Objects;

/**
 * Full line decomposition parameters extension business object
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FullLineDecompositionParameters extends AbstractExtension<FlowDecompositionParameters> {
    static final InjectionStrategy DEFAULT_INJECTION_STRATEGY = InjectionStrategy.SUM_INJECTIONS;
    static final double DEFAULT_PEX_MATRIX_TOLERANCE = 1e-5;
    static final int DEFAULT_THREADS_NUMBER = 5;
    static final PstStrategy DEFAULT_PST_STRATEGY = PstStrategy.VIA_PSDF;

    private InjectionStrategy injectionStrategy = DEFAULT_INJECTION_STRATEGY;
    private double pexMatrixTolerance = DEFAULT_PEX_MATRIX_TOLERANCE;
    private int threadsNumber = DEFAULT_THREADS_NUMBER;
    private PstStrategy pstStrategy = DEFAULT_PST_STRATEGY;

    public enum PstStrategy {
        NEUTRAL_TAP,
        VIA_PSDF
    }

    public enum InjectionStrategy {
        SUM_INJECTIONS,
        DECOMPOSE_INJECTIONS
    }

    public FullLineDecompositionParameters() {
    }

    public FullLineDecompositionParameters(FullLineDecompositionParameters other) {
        Objects.requireNonNull(other);

        this.injectionStrategy = other.injectionStrategy;
        this.pexMatrixTolerance = other.pexMatrixTolerance;
        this.threadsNumber = other.threadsNumber;
        this.pstStrategy = other.pstStrategy;
    }

    @Override
    public String getName() {
        return "FullLineDecompositionParameters";
    }

    public InjectionStrategy getInjectionStrategy() {
        return injectionStrategy;
    }

    public FullLineDecompositionParameters setInjectionStrategy(InjectionStrategy injectionStrategy) {
        this.injectionStrategy = injectionStrategy;
        return this;
    }

    public double getPexMatrixTolerance() {
        return pexMatrixTolerance;
    }

    public FullLineDecompositionParameters setPexMatrixTolerance(double pexMatrixTolerance) {
        this.pexMatrixTolerance = pexMatrixTolerance;
        return this;
    }

    public int getThreadsNumber() {
        return threadsNumber;
    }

    public FullLineDecompositionParameters setThreadsNumber(int threadsNumber) {
        this.threadsNumber = threadsNumber;
        return this;
    }

    public PstStrategy getPstStrategy() {
        return pstStrategy;
    }

    public FullLineDecompositionParameters setPstStrategy(PstStrategy pstStrategy) {
        this.pstStrategy = pstStrategy;
        return this;
    }

    @Override
    public String toString() {
        ImmutableMap.Builder<String, Object> immutableMapBuilder = ImmutableMap.builder();
        immutableMapBuilder.put("injectionStrategy", injectionStrategy)
                .put("pexMatrixTolerance", pexMatrixTolerance)
                .put("threadsNumber", threadsNumber)
                .put("pstStrategy", pstStrategy);

        return immutableMapBuilder.build().toString();
    }
}
