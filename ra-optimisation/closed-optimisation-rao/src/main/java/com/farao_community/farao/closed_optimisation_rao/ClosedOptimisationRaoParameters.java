/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationParameters;
import com.powsybl.commons.extensions.AbstractExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class ClosedOptimisationRaoParameters extends AbstractExtension<RaoComputationParameters> {
    static final String DEFAULT_SOLVER_TYPE = "GLOP_LINEAR_PROGRAMMING";
    static final double DEFAULT_RELATIVE_MIP_GAP = 0.0;
    static final double DEFAULT_MAX_TIME = Double.POSITIVE_INFINITY;
    static final double DEFAULT_OVERLOAD_PENALTY_COST = 5000.0;
    static final double DEFAULT_RD_SENSITIVITY_SIGNIFICANCE_THRESHOLD = 0.0;
    static final double DEFAULT_PST_SENSITIVITY_SIGNIFICANCE_THRESHOLD = 0.0;
    static final int DEFAULT_NUMBER_OF_PARALLEL_THREADS = 1;

    private String solverType = DEFAULT_SOLVER_TYPE;
    private double relativeMipGap = DEFAULT_RELATIVE_MIP_GAP;
    private double maxTimeInSeconds = DEFAULT_MAX_TIME;
    private double overloadPenaltyCost = DEFAULT_OVERLOAD_PENALTY_COST;
    private double rdSensitivityThreshold = DEFAULT_RD_SENSITIVITY_SIGNIFICANCE_THRESHOLD;
    private double pstSensitivityThreshold = DEFAULT_PST_SENSITIVITY_SIGNIFICANCE_THRESHOLD;
    private int numberOfParallelThreads = DEFAULT_NUMBER_OF_PARALLEL_THREADS;
    private List<String> fillersList = new ArrayList<>();
    private List<String> preProcessorsList = new ArrayList<>();
    private List<String> postProcessorsList = new ArrayList<>();

    public ClosedOptimisationRaoParameters() {
    }

    public ClosedOptimisationRaoParameters(ClosedOptimisationRaoParameters other) {
        Objects.requireNonNull(other);
        this.solverType = other.solverType;
        this.relativeMipGap = other.relativeMipGap;
        this.maxTimeInSeconds = other.maxTimeInSeconds;
        this.overloadPenaltyCost = other.overloadPenaltyCost;
        this.rdSensitivityThreshold = other.rdSensitivityThreshold;
        this.pstSensitivityThreshold = other.pstSensitivityThreshold;
        this.numberOfParallelThreads = other.numberOfParallelThreads;
        this.fillersList.addAll(other.fillersList);
        this.preProcessorsList.addAll(other.preProcessorsList);
        this.postProcessorsList.addAll(other.postProcessorsList);
    }

    public String getSolverType() {
        return this.solverType;
    }

    public ClosedOptimisationRaoParameters setSolverType(String solverType) {
        this.solverType = solverType;
        return this;
    }

    public double getRelativeMipGap() {
        return this.relativeMipGap;
    }

    public ClosedOptimisationRaoParameters setRelativeMipGap(double relativeMipGap) {
        this.relativeMipGap = relativeMipGap;
        return this;
    }

    public double getMaxTimeInSeconds() {
        return this.maxTimeInSeconds;
    }

    public ClosedOptimisationRaoParameters setMaxTimeInSeconds(double maxTimeInSeconds) {
        this.maxTimeInSeconds = maxTimeInSeconds;
        return this;
    }

    public double getOverloadPenaltyCost() {
        return this.overloadPenaltyCost;
    }

    public ClosedOptimisationRaoParameters setOverloadPenaltyCost(double overloadPenaltyCost) {
        this.overloadPenaltyCost = overloadPenaltyCost;
        return this;
    }

    public double getRdSensitivityThreshold() {
        return this.rdSensitivityThreshold;
    }

    public ClosedOptimisationRaoParameters setRdSensitivityThreshold(double rdSensitivityThreshold) {
        this.rdSensitivityThreshold = rdSensitivityThreshold;
        return this;
    }

    public double getPstSensitivityThreshold() {
        return this.pstSensitivityThreshold;
    }

    public ClosedOptimisationRaoParameters setPstSensitivityThreshold(double pstSensitivityThreshold) {
        this.pstSensitivityThreshold = pstSensitivityThreshold;
        return this;
    }

    public int getNumberOfParallelThreads() {
        return this.numberOfParallelThreads;
    }

    public ClosedOptimisationRaoParameters setNumberOfParallelThreads(int numberOfParallelThreads) {
        this.numberOfParallelThreads = numberOfParallelThreads;
        return this;
    }

    public List<String> getFillersList() {
        return Collections.unmodifiableList(fillersList);
    }

    public ClosedOptimisationRaoParameters addAllFillers(List<String> fillers) {
        this.fillersList.addAll(fillers);
        return this;
    }

    public List<String> getPreProcessorsList() {
        return preProcessorsList;
    }

    public ClosedOptimisationRaoParameters addAllPreProcessors(List<String> preProcessors) {
        this.preProcessorsList.addAll(preProcessors);
        return this;
    }

    public List<String> getPostProcessorsList() {
        return postProcessorsList;
    }

    public ClosedOptimisationRaoParameters addAllPostProcessors(List<String> postProcessors) {
        this.postProcessorsList.addAll(postProcessors);
        return this;
    }

    @Override
    public String getName() {
        return "ClosedOptimisationRaoParameters";
    }
}
