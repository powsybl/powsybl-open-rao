/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.config;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;

import java.util.Objects;

import static java.lang.Math.max;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRaoParameters extends AbstractExtension<RaoParameters> {

    public enum ObjectiveFunction {
        MAX_MIN_MARGIN_IN_MEGAWATT,
        MAX_MIN_MARGIN_IN_AMPERE
    }

    static final int DEFAULT_MAX_NUMBER_OF_ITERATIONS = 10;
    static final boolean DEFAULT_SECURITY_ANALYSIS_WITHOUT_RAO = false;
    static final double DEFAULT_PST_SENSITIVITY_THRESHOLD = 0.0;
    static final double DEFAULT_PST_PENALTY_COST = 0.01;
    static final double DEFAULT_FALLBACK_OVERCOST = 0;
    static final ObjectiveFunction DEFAULT_OBJECTIVE_FUNCTION = ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT;

    private SensitivityComputationParameters sensitivityComputationParameters = new SensitivityComputationParameters();
    private SensitivityComputationParameters fallbackSensiParameters = null;

    private boolean securityAnalysisWithoutRao = DEFAULT_SECURITY_ANALYSIS_WITHOUT_RAO;
    private int maxIterations = DEFAULT_MAX_NUMBER_OF_ITERATIONS;
    private double pstSensitivityThreshold = DEFAULT_PST_SENSITIVITY_THRESHOLD;
    private double pstPenaltyCost = DEFAULT_PST_PENALTY_COST;
    private ObjectiveFunction objectiveFunction = DEFAULT_OBJECTIVE_FUNCTION;
    private double fallbackOvercost = DEFAULT_FALLBACK_OVERCOST;

    @Override
    public String getName() {
        return "LinearRaoParameters";
    }

    public SensitivityComputationParameters getSensitivityComputationParameters() {
        return sensitivityComputationParameters;
    }

    public LinearRaoParameters setSensitivityComputationParameters(SensitivityComputationParameters sensiParameters) {
        this.sensitivityComputationParameters = Objects.requireNonNull(sensiParameters);
        return this;
    }

    public SensitivityComputationParameters getFallbackSensiParameters() {
        return fallbackSensiParameters;
    }

    public LinearRaoParameters setFallbackSensiParameters(SensitivityComputationParameters sensiParameters) {
        this.fallbackSensiParameters = Objects.requireNonNull(sensiParameters);
        return this;
    }

    public LinearRaoParameters setSecurityAnalysisWithoutRao(boolean securityAnalysisWithoutRao) {
        this.securityAnalysisWithoutRao = securityAnalysisWithoutRao;
        return this;
    }

    public boolean isSecurityAnalysisWithoutRao() {
        return securityAnalysisWithoutRao;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public LinearRaoParameters setMaxIterations(int maxIterations) {
        this.maxIterations = max(0, maxIterations);
        return this;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public LinearRaoParameters setObjectiveFunction(ObjectiveFunction objectiveFunction) {
        this.objectiveFunction = objectiveFunction;
        return this;
    }

    public double getPstSensitivityThreshold() {
        return pstSensitivityThreshold;
    }

    public LinearRaoParameters setPstSensitivityThreshold(double threshold) {
        this.pstSensitivityThreshold = threshold;
        return this;
    }

    public double getPstPenaltyCost() {
        return pstPenaltyCost;
    }

    public LinearRaoParameters setPstPenaltyCost(double pstPenaltyCost) {
        this.pstPenaltyCost = max(0.0, pstPenaltyCost);
        return this;
    }

    public double getFallbackOvercost() {
        return fallbackOvercost;
    }

    public LinearRaoParameters setFallbackOvercost(double overcost) {
        this.fallbackOvercost = max(0.0, overcost);
        return this;
    }
}
