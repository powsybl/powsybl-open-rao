/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.sensitivity_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.powsybl.iidm.network.Network;
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
public class SystematicSensitivityAnalysisInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicSensitivityAnalysisInterface.class);

    /**
     * LinearRao configurations, containing the default and fallback configurations
     * of the sensitivity computation
     */
    private SensitivityComputationParameters defaultParameters;

    private SensitivityComputationParameters fallbackParameters;

    /**
     * The sensitivity provider to be used in the sensitivity computation
     */
    private SensitivityProvider sensitivityProvider;

    /**
     * A boolean indicating whether or not the fallback mode of the sensitivity computation
     * engine is active.
     */
    private boolean fallbackMode = false;

    /**
     * Constructors
     */
    public SystematicSensitivityAnalysisInterface(SensitivityComputationParameters defaultParameters) {
        this.defaultParameters = defaultParameters;
    }

    public SystematicSensitivityAnalysisInterface(SensitivityComputationParameters defaultParameters, SensitivityComputationParameters fallbackParameters) {
        this.defaultParameters = defaultParameters;
        this.fallbackParameters = fallbackParameters;
    }

    public SystematicSensitivityAnalysisInterface() {
        this.defaultParameters = new SensitivityComputationParameters();
    }

    public void setSensitivityProvider(SensitivityProvider sensitivityProvider) {
        this.sensitivityProvider = sensitivityProvider;
    }

    public boolean isFallback() {
        return fallbackMode;
    }

    /**
     * Run the systematic sensitivity analysis on the given network and crac, and associates the
     * SystematicSensitivityAnalysisResult to the given network variant.
     *
     * Throw a SensitivityComputationException if the computation fails.
     */
    public SystematicSensitivityAnalysisResult run(Network network, Crac crac, Unit defaultUnit) {
        SensitivityComputationParameters sensitivityComputationParameters = fallbackMode ? fallbackParameters : defaultParameters;
        if (Objects.isNull(sensitivityProvider)) {
            throw new SensitivityComputationException("Sensitivity provider was not defined.");
        }

        try {
            return runWithConfig(network, crac, sensitivityComputationParameters, defaultUnit);
        } catch (SensitivityComputationException e) {
            if (!fallbackMode && fallbackParameters != null) { // default mode fails, retry in fallback mode
                LOGGER.warn("Error while running the sensitivity computation with default parameters, fallback sensitivity parameters are now used.");
                fallbackMode = true;
                return run(network, crac, defaultUnit);
            } else if (!fallbackMode) { // no fallback mode available, throw an exception
                throw new SensitivityComputationException("Sensitivity computation failed with default parameters. No fallback parameters available.", e);
            } else { // fallback mode fails, throw an exception
                throw new SensitivityComputationException("Sensitivity computation failed with all available sensitivity parameters.", e);
            }
        }
    }

    // Method for tests
    SystematicSensitivityAnalysisResult run(Network network, Crac crac) {
        return run(network, crac, Unit.AMPERE);
    }

    /**
     * Run the systematic sensitivity analysis with given SensitivityComputationParameters, throw a
     * SensitivityComputationException is the computation fails.
     */
    private SystematicSensitivityAnalysisResult runWithConfig(Network network, Crac crac, SensitivityComputationParameters sensitivityComputationParameters, Unit defaultUnit) {

        try {
            SystematicSensitivityAnalysisResult tempSystematicSensitivityAnalysisResult = SystematicSensitivityAnalysisService
                .runSensitivity(network, network.getVariantManager().getWorkingVariantId(), sensitivityProvider, sensitivityComputationParameters);

            if (!tempSystematicSensitivityAnalysisResult.isSuccess()) {
                throw new SensitivityComputationException("Some output data of the sensitivity computation are missing.");
            }

            checkSensiResults(crac, tempSystematicSensitivityAnalysisResult, defaultUnit);
            return tempSystematicSensitivityAnalysisResult;

        } catch (Exception e) {
            throw new SensitivityComputationException("Sensitivity computation fails.", e);
        }
    }

    private void checkSensiResults(Crac crac, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult, Unit defaultUnit) {
        if (!systematicSensitivityAnalysisResult.isSuccess()) {
            throw new SensitivityComputationException("Status of the sensitivity result indicates a failure.");
        }

        if (crac.getCnecs().stream()
            .map(systematicSensitivityAnalysisResult::getReferenceFlow)
            .anyMatch(f -> Double.isNaN(f))) {
            throw new SensitivityComputationException("Flow values are missing from the output of the sensitivity analysis.");
        }

        if (crac.getCnecs().stream()
            .map(systematicSensitivityAnalysisResult::getReferenceIntensity)
            .anyMatch(f -> Double.isNaN(f)) && !isFallback() && defaultUnit.equals(Unit.AMPERE)) {
            // in default mode, this means that there is an error in the sensitivity computation, or an
            // incompatibility with the sensitivity computation mode (i.e. the sensitivity computation is
            // made in DC mode and no intensity are computed).
            throw new FaraoException("Intensity values are missing from the output of the sensitivity analysis. Min margin cannot be calculated in AMPERE.");
        }
    }
}
