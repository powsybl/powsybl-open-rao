/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;

/**
 * An interface with the engine that computes sensitivities and flows needed in the RAO.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class SystematicSensitivityInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicSensitivityInterface.class);

    /**
     * Sensitivity configurations, containing the default and fallback configurations
     * of the sensitivity analysis
     */
    private SensitivityAnalysisParameters defaultParameters;
    private SensitivityAnalysisParameters fallbackParameters;

    /**
     * The sensitivity provider to be used in the sensitivity analysis
     */
    private CnecSensitivityProvider cnecSensitivityProvider;

    /**
     * A boolean indicating whether or not the fallback mode of the sensitivity analysis
     * engine is active.
     */
    private boolean fallbackMode = false;

    /**
     * Builder
     */
    public static final class SystematicSensitivityInterfaceBuilder {
        private SensitivityAnalysisParameters defaultParameters;
        private SensitivityAnalysisParameters fallbackParameters;
        private MultipleSensitivityProvider multipleSensitivityProvider = new MultipleSensitivityProvider();
        private boolean providerInitialised = false;

        private SystematicSensitivityInterfaceBuilder() {

        }

        public SystematicSensitivityInterfaceBuilder withFallbackParameters(SensitivityAnalysisParameters fallbackParameters) {
            this.fallbackParameters = fallbackParameters;
            return this;
        }

        public SystematicSensitivityInterfaceBuilder withDefaultParameters(SensitivityAnalysisParameters defaultParameters) {
            this.defaultParameters = defaultParameters;
            return this;
        }

        public SystematicSensitivityInterfaceBuilder withSensitivityProvider(CnecSensitivityProvider cnecSensitivityProvider) {
            this.multipleSensitivityProvider.addProvider(cnecSensitivityProvider);
            providerInitialised = true;
            return this;
        }

        public SystematicSensitivityInterfaceBuilder withPtdfSensitivities(ZonalData<LinearGlsk> glsk, Set<Cnec> cnecs) {
            PtdfSensitivityProvider ptdfSensitivityProvider = new PtdfSensitivityProvider(glsk);
            ptdfSensitivityProvider.addCnecs(cnecs);
            return this.withSensitivityProvider(ptdfSensitivityProvider);
        }

        public SystematicSensitivityInterfaceBuilder withRangeActionSensitivities(Set<RangeAction> rangeActions, Set<Cnec> cnecs) {
            RangeActionSensitivityProvider rangeActionSensitivityProvider = new RangeActionSensitivityProvider();
            rangeActionSensitivityProvider.addSensitivityFactors(rangeActions, cnecs);
            return this.withSensitivityProvider(rangeActionSensitivityProvider);
        }

        public SystematicSensitivityInterfaceBuilder withLoadflow(Set<Cnec> cnecs) {
            LoadflowProvider loadflowProvider = new LoadflowProvider();
            loadflowProvider.addCnecs(cnecs);
            return this.withSensitivityProvider(loadflowProvider);
        }

        public SystematicSensitivityInterface build() {
            if (!providerInitialised) {
                throw new SensitivityAnalysisException("Sensitivity provider is mandatory when building a SystematicSensitivityInterface.");
            }
            if (Objects.isNull(defaultParameters)) {
                defaultParameters = new SensitivityAnalysisParameters();
            }
            SystematicSensitivityInterface systematicSensitivityInterface = new SystematicSensitivityInterface();
            systematicSensitivityInterface.defaultParameters = defaultParameters;
            systematicSensitivityInterface.fallbackParameters = fallbackParameters;
            systematicSensitivityInterface.cnecSensitivityProvider = multipleSensitivityProvider;
            return systematicSensitivityInterface;
        }
    }

    public static SystematicSensitivityInterfaceBuilder builder() {
        return new SystematicSensitivityInterfaceBuilder();
    }

    SystematicSensitivityInterface() {

    }

    public boolean isFallback() {
        return fallbackMode;
    }

    /**
     * Run the systematic sensitivity analysis on the given network and crac, and associates the
     * SystematicSensitivityResult to the given network variant.
     *
     * Throw a SensitivityAnalysisException if the computation fails.
     */
    public SystematicSensitivityResult run(Network network, Unit defaultUnit) {
        SensitivityAnalysisParameters sensitivityAnalysisParameters = fallbackMode ? fallbackParameters : defaultParameters;
        if (Objects.isNull(cnecSensitivityProvider)) {
            throw new SensitivityAnalysisException("Sensitivity provider was not defined.");
        }

        try {
            SystematicSensitivityResult result = runWithConfig(network, sensitivityAnalysisParameters, defaultUnit);
            if (fallbackMode) {
                result.setStatus(SystematicSensitivityResult.SensitivityComputationStatus.FALLBACK);
            }
            return result;

        } catch (SensitivityAnalysisException e) {
            LOGGER.debug(e.getCause().getMessage());
            if (!fallbackMode && fallbackParameters != null) { // default mode fails, retry in fallback mode
                LOGGER.warn("Error while running the sensitivity analysis with default parameters, fallback sensitivity parameters are now used.");
                fallbackMode = true;
                return run(network, defaultUnit);
            } else if (!fallbackMode) { // no fallback mode available, throw an exception
                throw new SensitivityAnalysisException("Sensitivity analysis failed with default parameters. No fallback parameters available.", e);
            } else { // fallback mode fails, throw an exception
                throw new SensitivityAnalysisException("Sensitivity analysis failed with all available sensitivity parameters.", e);
            }
        }
    }

    // Method for tests
    SystematicSensitivityResult run(Network network) {
        return run(network, Unit.AMPERE);
    }

    /**
     * Run the systematic sensitivity analysis with given SensitivityComputationParameters, throw a
     * SensitivityComputationException is the computation fails.
     */
    private SystematicSensitivityResult runWithConfig(Network network, SensitivityAnalysisParameters sensitivityAnalysisParameters, Unit defaultUnit) {
        try {
            SystematicSensitivityResult tempSystematicSensitivityAnalysisResult = SystematicSensitivityAdapter
                .runSensitivity(network, cnecSensitivityProvider, sensitivityAnalysisParameters);

            if (!tempSystematicSensitivityAnalysisResult.isSuccess()) {
                throw new SensitivityAnalysisException("Some output data of the sensitivity analysis are missing.");
            }

            checkSensiResults(tempSystematicSensitivityAnalysisResult, defaultUnit);
            return tempSystematicSensitivityAnalysisResult;

        } catch (Exception e) {
            throw new SensitivityAnalysisException("Sensitivity analysis fails.", e);
        }
    }

    private void checkSensiResults(SystematicSensitivityResult systematicSensitivityAnalysisResult, Unit defaultUnit) {
        if (!systematicSensitivityAnalysisResult.isSuccess()) {
            throw new SensitivityAnalysisException("Status of the sensitivity result indicates a failure.");
        }

        if (cnecSensitivityProvider.getCnecs().stream()
            .map(systematicSensitivityAnalysisResult::getReferenceFlow)
            .anyMatch(f -> Double.isNaN(f))) {
            throw new SensitivityAnalysisException("Flow values are missing from the output of the sensitivity analysis.");
        }

        if (cnecSensitivityProvider.getCnecs().stream()
            .map(systematicSensitivityAnalysisResult::getReferenceIntensity)
            .anyMatch(f -> Double.isNaN(f)) && !isFallback() && defaultUnit.equals(Unit.AMPERE)) {
            // in default mode, this means that there is an error in the sensitivity analysis, or an
            // incompatibility with the sensitivity analysis mode (i.e. the sensitivity analysis is
            // made in DC mode and no intensity are computed).
            throw new FaraoException("Intensity values are missing from the output of the sensitivity analysis. Min margin cannot be calculated in AMPERE.");
        }
    }
}
