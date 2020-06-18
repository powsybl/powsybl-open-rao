/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.systematic_sensitivity;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.rao_api.Rao;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.util.SensitivityComputationException;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * A computation engine dedicated to the systematic sensitivity analyses performed
 * in the scope of the LinearRao.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SystematicSensitivityComputation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicSensitivityComputation.class);

    /**
     * LinearRao configurations, containing the default and fallback configurations
     * of the sensitivity computation
     */
    private SystematicSensitivityComputationParameters parameters;

    /**
     * A boolean indicating whether or not the fallback mode of the sensitivity computation
     * engine is active.
     */
    private boolean fallbackMode;

    /**
     * Constructors
     */
    public SystematicSensitivityComputation(RaoParameters raoParameters) {
        SystematicSensitivityComputationParameters computationParameters;
        if (!Objects.isNull(raoParameters.getExtension(SystematicSensitivityComputationParameters.class))) {
            computationParameters = raoParameters.getExtension(SystematicSensitivityComputationParameters.class);
        } else {
            computationParameters = new SystematicSensitivityComputationParameters();
        }
        init(computationParameters);
    }

    public SystematicSensitivityComputation(SystematicSensitivityComputationParameters parameters) {
        init(parameters);
    }

    private void init(SystematicSensitivityComputationParameters computationParameters) {
        this.parameters = computationParameters;
        this.fallbackMode = false;
    }

    public boolean isFallback() {
        return fallbackMode;
    }

    public SystematicSensitivityComputationParameters getParameters() {
        return parameters;
    }

    /**
     * Run the systematic sensitivity analysis on one Situation, and evaluate the value of the
     * objective function on this Situation.
     *
     * Throw a SensitivityComputationException if the computation fails.
     */
    public void run(RaoData raoData, Unit defaultUnit) {
        SensitivityComputationParameters sensitivityComputationParameters = fallbackMode ?
            parameters.getFallbackParameters()
            : parameters.getDefaultParameters();

        try {
            runWithConfig(raoData, sensitivityComputationParameters, defaultUnit);
        } catch (SensitivityComputationException e) {
            if (!fallbackMode && parameters.getFallbackParameters() != null) { // default mode fails, retry in fallback mode
                LOGGER.warn("Error while running the sensitivity computation with default parameters, fallback sensitivity parameters are now used.");
                fallbackMode = true;
                run(raoData, defaultUnit);
            } else if (!fallbackMode) { // no fallback mode available, throw an exception
                throw new SensitivityComputationException("Sensitivity computation failed with default parameters. No fallback parameters available.", e);
            } else { // fallback mode fails, throw an exception
                throw new SensitivityComputationException("Sensitivity computation failed with all available sensitivity parameters.", e);
            }
        }
    }

    public void run(RaoData raoData) {
        run(raoData, Unit.AMPERE);
    }

    /**
     * Run the systematic sensitivity analysis with given SensitivityComputationParameters, throw a
     * SensitivityComputationException is the computation fails.
     */
    private void runWithConfig(RaoData raoData, SensitivityComputationParameters sensitivityComputationParameters, Unit defaultUnit) {

        try {
            SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = SystematicSensitivityAnalysisService
                .runAnalysis(raoData.getNetwork(), raoData.getCrac(), sensitivityComputationParameters);

            if (!systematicSensitivityAnalysisResult.isSuccess()) {
                throw new SensitivityComputationException("Some output data of the sensitivity computation are missing.");
            }

            checkSensiResults(raoData, systematicSensitivityAnalysisResult, defaultUnit);
            setResults(raoData, systematicSensitivityAnalysisResult);

        } catch (Exception e) {
            throw new SensitivityComputationException("Sensitivity computation fails.", e);
        }
    }

    private void checkSensiResults(RaoData raoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult, Unit defaultUnit) {
        if (!systematicSensitivityAnalysisResult.isSuccess()) {
            throw new SensitivityComputationException("Status of the sensitivity result indicates a failure.");
        }

        if (raoData.getCrac().getCnecs().stream()
            .map(systematicSensitivityAnalysisResult::getReferenceFlow)
            .anyMatch(f -> Double.isNaN(f))) {
            throw new SensitivityComputationException("Flow values are missing from the output of the sensitivity analysis.");
        }

        if (raoData.getCrac().getCnecs().stream()
            .map(cnec -> raoData.getSystematicSensitivityAnalysisResult().getReferenceIntensity(cnec))
            .anyMatch(f -> Double.isNaN(f)) && !isFallback() && defaultUnit.equals(Unit.AMPERE)) {
            // in default mode, this means that there is an error in the sensitivity computation, or an
            // incompatibility with the sensitivity computation mode (i.e. the sensitivity computation is
            // made in DC mode and no intensity are computed).
            throw new FaraoException("Intensity values are missing from the output of the sensitivity analysis. Min margin cannot be calculated in AMPERE.");
        }
    }

    /**
     * add results of the systematic analysis (flows and objective function value) in the
     * Crac result variant of the situation.
     */
    private void setResults(RaoData raoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        raoData.setSystematicSensitivityAnalysisResult(systematicSensitivityAnalysisResult);
    }
}
