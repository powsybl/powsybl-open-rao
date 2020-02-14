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

import java.util.Objects;

import static java.lang.Math.max;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRaoParameters extends AbstractExtension<RaoParameters> {

    static final int DEFAULT_MAX_NUMBER_OF_ITERATIONS = 10;
    static final boolean DEFAULT_SECURITY_ANALYSIS_WITHOUT_RAO = false;

    private SensitivityComputationParameters sensitivityComputationParameters = new SensitivityComputationParameters();
    private int maxIterations = DEFAULT_MAX_NUMBER_OF_ITERATIONS;
    private boolean securityAnalysisWithoutRao = DEFAULT_SECURITY_ANALYSIS_WITHOUT_RAO;

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

    public LinearRaoParameters setSecurityAnalysisWithoutRao(boolean securityAnalysisWithoutRao) {
        this.securityAnalysisWithoutRao = securityAnalysisWithoutRao;
        return this;
    }

    public boolean getSecurityAnalysisWithoutRao() {
        return securityAnalysisWithoutRao;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public LinearRaoParameters setMaxIterations(int maxIterations) {
        this.maxIterations = max(0, maxIterations);
        return this;
    }
}
