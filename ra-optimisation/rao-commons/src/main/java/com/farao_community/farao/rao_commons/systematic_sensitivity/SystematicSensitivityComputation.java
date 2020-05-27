/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.systematic_sensitivity;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.util.SensitivityComputationException;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.DefaultComputationManagerConfig;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


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
    private SystematicSensitivityComputationParameters computationParameters;

    /**
     * A boolean indicating whether or not the fallback mode of the sensitivity computation
     * engine is active.
     */
    private boolean fallbackMode;

    /**
     * Computation Manager
     */
    private ComputationManager computationManager;

    /**
     * Constructors
     */
    public SystematicSensitivityComputation() {
        this(new SystematicSensitivityComputationParameters(),
            DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager());
    }

    public SystematicSensitivityComputation(RaoParameters raoParameters) {
        SystematicSensitivityComputationParameters parameters;
        if (!Objects.isNull(raoParameters.getExtension(SystematicSensitivityComputationParameters.class))) {
            parameters = raoParameters.getExtension(SystematicSensitivityComputationParameters.class);
        } else {
            parameters = new SystematicSensitivityComputationParameters();
        }
        init(parameters);
    }

    public SystematicSensitivityComputation(RaoParameters raoParameters, ComputationManager computationManager) {
        SystematicSensitivityComputationParameters parameters;
        if (!Objects.isNull(raoParameters.getExtension(SystematicSensitivityComputationParameters.class))) {
            parameters = raoParameters.getExtension(SystematicSensitivityComputationParameters.class);
        } else {
            parameters = new SystematicSensitivityComputationParameters();
        }
        init(parameters, computationManager);
    }

    public SystematicSensitivityComputation(SystematicSensitivityComputationParameters computationParameters) {
        init(computationParameters);
    }

    public SystematicSensitivityComputation(SystematicSensitivityComputationParameters computationParameters, ComputationManager computationManager) {
        init(computationParameters, computationManager);
    }

    private void init(SystematicSensitivityComputationParameters computationParameters) {
        init(computationParameters, DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager());
    }

    private void init(SystematicSensitivityComputationParameters computationParameters, ComputationManager computationManager) {
        this.computationParameters = computationParameters;
        this.computationManager = computationManager;
        this.fallbackMode = false;
    }

    public boolean isFallback() {
        return fallbackMode;
    }

    /**
     * Run the systematic sensitivity analysis on one Situation, and evaluate the value of the
     * objective function on this Situation.
     *
     * Throw a SensitivityComputationException if the computation fails.
     */
    public void run(RaoData raoData) {

        SensitivityComputationParameters sensitivityComputationParameters = fallbackMode ?
            computationParameters.getFallbackParameters()
            : computationParameters.getDefaultParameters();

        try {
            runWithConfig(raoData, sensitivityComputationParameters);
        } catch (SensitivityComputationException e) {
            if (!fallbackMode && computationParameters.getFallbackParameters() != null) { // default mode fails, retry in fallback mode
                LOGGER.warn("Error while running the sensitivity computation with default parameters, fallback sensitivity parameters are now used.");
                fallbackMode = true;
                run(raoData);
            } else if (!fallbackMode) { // no fallback mode available, throw an exception
                throw new SensitivityComputationException("Sensitivity computation failed with default parameters. No fallback parameters available.", e);
            } else { // fallback mode fails, throw an exception
                throw new SensitivityComputationException("Sensitivity computation failed with all available sensitivity parameters.", e);
            }
        }
    }

    /**
     * Run the systematic sensitivity analysis with given SensitivityComputationParameters, throw a
     * SensitivityComputationException is the computation fails.
     */
    private void runWithConfig(RaoData raoData, SensitivityComputationParameters sensitivityComputationParameters) {

        try {
            SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = SystematicSensitivityAnalysisService
                .runAnalysis(raoData.getNetwork(), raoData.getCrac(), computationManager, sensitivityComputationParameters);

            if (!systematicSensitivityAnalysisResult.isSuccess()) {
                throw new SensitivityComputationException("Some output data of the sensitivity computation are missing.");
            }

            setResults(raoData, systematicSensitivityAnalysisResult);

        } catch (Exception e) {
            throw new SensitivityComputationException("Sensitivity computation fails.", e);
        }
    }

    /**
     * add results of the systematic analysis (flows and objective function value) in the
     * Crac result variant of the situation.
     */
    private void setResults(RaoData raoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        raoData.setSystematicSensitivityAnalysisResult(systematicSensitivityAnalysisResult);
        raoData.getCracResult().setCost(-getMinMargin(raoData, systematicSensitivityAnalysisResult));
        updateCnecExtensions(raoData, systematicSensitivityAnalysisResult);
    }

    /**
     * Compute the objective function, the minimal margin.
     */
    private double getMinMargin(RaoData raoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {

        double minMargin = Double.POSITIVE_INFINITY;
        for (Cnec cnec : raoData.getCrac().getCnecs()) {
            double flow = systematicSensitivityAnalysisResult.getReferenceFlow(cnec);
            double margin = cnec.computeMargin(flow, Unit.MEGAWATT);
            if (Double.isNaN(margin)) {
                throw new SensitivityComputationException(String.format("Cnec %s is not present in the sensitivity analysis results. Bad behaviour.", cnec.getId()));
            }
            minMargin = Math.min(minMargin, margin);
        }
        return minMargin;
    }

    private void updateCnecExtensions(RaoData raoData, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        raoData.getCrac().getCnecs().forEach(cnec -> {
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
            cnecResult.setFlowInMW(systematicSensitivityAnalysisResult.getReferenceFlow(cnec));
            cnecResult.setFlowInA(systematicSensitivityAnalysisResult.getReferenceIntensity(cnec));
            cnecResult.setThresholds(cnec);
        });
    }
}
