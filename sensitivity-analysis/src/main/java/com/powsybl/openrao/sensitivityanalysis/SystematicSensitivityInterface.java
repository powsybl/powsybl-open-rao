/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.opentelemetry.OpenTelemetryReporter;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;
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
    /**
     * Name of sensitivity analysis provider
     */
    private String sensitivityProvider;

    /**
     * Sensitivity configurations, containing sensitivity analysis configuration
     */
    private SensitivityAnalysisParameters parameters;

    /**
     * The sensitivity provider to be used in the sensitivity analysis
     */
    private CnecSensitivityProvider cnecSensitivityProvider;

    /**
     * The remedialActions that are applied in the initial network or after some contingencies
     */
    private AppliedRemedialActions appliedRemedialActions;
    private Instant outageInstant;

    /**
     * Builder
     */
    public static final class SystematicSensitivityInterfaceBuilder {
        private String sensitivityProvider;
        private SensitivityAnalysisParameters defaultParameters;
        private final MultipleSensitivityProvider multipleSensitivityProvider = new MultipleSensitivityProvider();
        private AppliedRemedialActions appliedRemedialActions;
        private boolean providerInitialised = false;
        private Instant outageInstant;

        private SystematicSensitivityInterfaceBuilder() {

        }

        public SystematicSensitivityInterfaceBuilder withSensitivityProviderName(String sensitivityProvider) {
            this.sensitivityProvider = sensitivityProvider;
            return this;
        }

        public SystematicSensitivityInterfaceBuilder withParameters(SensitivityAnalysisParameters defaultParameters) {
            this.defaultParameters = defaultParameters;
            return this;
        }

        public SystematicSensitivityInterfaceBuilder withSensitivityProvider(CnecSensitivityProvider cnecSensitivityProvider) {
            if (Objects.isNull(cnecSensitivityProvider)) {
                throw new OpenRaoException("Null sensitivity provider.");
            }
            this.multipleSensitivityProvider.addProvider(cnecSensitivityProvider);
            providerInitialised = true;
            return this;
        }

        public SystematicSensitivityInterfaceBuilder withPtdfSensitivities(ZonalData<SensitivityVariableSet> glsk, Set<FlowCnec> cnecs, Set<Unit> units) {
            return this.withSensitivityProvider(new PtdfSensitivityProvider(glsk, cnecs, units));
        }

        public SystematicSensitivityInterfaceBuilder withRangeActionSensitivities(Set<RangeAction<?>> rangeActions, Set<FlowCnec> cnecs, Set<Unit> units) {
            return this.withSensitivityProvider(new RangeActionSensitivityProvider(rangeActions, cnecs, units));
        }

        public SystematicSensitivityInterfaceBuilder withLoadflow(Set<FlowCnec> cnecs, Set<Unit> units) {
            return this.withSensitivityProvider(new LoadflowProvider(cnecs, units));
        }

        public SystematicSensitivityInterfaceBuilder withAppliedRemedialActions(AppliedRemedialActions appliedRemedialActions) {
            this.appliedRemedialActions = appliedRemedialActions;
            return this;
        }

        public SystematicSensitivityInterfaceBuilder withOutageInstant(Instant outageInstant) {
            if (!outageInstant.isOutage()) {
                throw new OpenRaoException("Instant provided in the systematic sensitivity builder has to be an outage");
            }
            this.outageInstant = outageInstant;
            return this;
        }

        public SystematicSensitivityInterface build() {
            if (Objects.isNull(sensitivityProvider)) {
                throw new OpenRaoException("Please provide a sensitivity provider implementation name when building a SystematicSensitivityInterface");
            }
            if (!providerInitialised) {
                throw new OpenRaoException("Sensitivity provider has not been initialized");
            }
            if (Objects.isNull(defaultParameters)) {
                defaultParameters = new SensitivityAnalysisParameters();
            }
            if (Objects.isNull(outageInstant)) {
                throw new OpenRaoException("Outage instant has not been defined in the systematic sensitivity interface");
            }
            SystematicSensitivityInterface systematicSensitivityInterface = new SystematicSensitivityInterface();
            systematicSensitivityInterface.sensitivityProvider = sensitivityProvider;
            systematicSensitivityInterface.parameters = defaultParameters;
            systematicSensitivityInterface.cnecSensitivityProvider = multipleSensitivityProvider;
            systematicSensitivityInterface.appliedRemedialActions = appliedRemedialActions;
            systematicSensitivityInterface.outageInstant = outageInstant;
            return systematicSensitivityInterface;
        }
    }

    public static SystematicSensitivityInterfaceBuilder builder() {
        return new SystematicSensitivityInterfaceBuilder();
    }

    SystematicSensitivityInterface() {

    }

    /**
     * Run the systematic sensitivity analysis on the given network and crac, and associates the
     * SystematicSensitivityResult to the given network variant.
     */
    public SystematicSensitivityResult run(Network network) {
        return OpenTelemetryReporter.withSpan("rao.systematicSA.run", cx -> {
        SystematicSensitivityResult result = runWithConfig(network);
        if (!result.isSuccess()) {
            BUSINESS_WARNS.warn("Sensitivity analysis failed.");
        }
        return result;
        });
    }

    /**
     * Run the systematic sensitivity analysis with given SensitivityComputationParameters, throw a
     * SensitivityComputationException is the computation fails.
     */
    private SystematicSensitivityResult runWithConfig(Network network) {
        if (cnecSensitivityProvider.getFlowCnecs().isEmpty()) {
            TECHNICAL_LOGS.error("Sensitivity analysis skipped: no FlowCNEC provided.");
            return new SystematicSensitivityResult();
        }
        SystematicSensitivityResult tempSystematicSensitivityAnalysisResult = SystematicSensitivityAdapter
                .runSensitivity(network, cnecSensitivityProvider, appliedRemedialActions, parameters, sensitivityProvider, outageInstant);

        if (!tempSystematicSensitivityAnalysisResult.isSuccess()) {
            TECHNICAL_LOGS.error("Sensitivity analysis failed: no output data available.");
        }
        return tempSystematicSensitivityAnalysisResult;
    }
}
