/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.openrao.commons.*;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracloopflowextension.LoopFlowThreshold;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputation;
import com.powsybl.openrao.loopflowcomputation.LoopFlowComputationImpl;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityInterface;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityProvider;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class ToolProvider {
    private Network network;
    private RaoParameters raoParameters;
    private ReferenceProgram referenceProgram;
    private ZonalData<SensitivityVariableSet> glskProvider;
    private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
    private LoopFlowComputation loopFlowComputation;

    private ToolProvider() {
        // Should not be used
    }

    public AbsolutePtdfSumsComputation getAbsolutePtdfSumsComputation() {
        return absolutePtdfSumsComputation;
    }

    public LoopFlowComputation getLoopFlowComputation() {
        return loopFlowComputation;
    }

    private boolean hasLoopFlowExtension(FlowCnec cnec) {
        return !Objects.isNull(cnec.getExtension(LoopFlowThreshold.class));
    }

    public Set<FlowCnec> getLoopFlowCnecs(Set<FlowCnec> allCnecs) {
        LoopFlowParametersExtension loopFlowParameters = raoParameters.getExtension(LoopFlowParametersExtension.class);
        if (raoParameters.hasExtension(LoopFlowParametersExtension.class) && !loopFlowParameters.getCountries().isEmpty()) {
            return allCnecs.stream()
                .filter(cnec -> hasLoopFlowExtension(cnec) && cnecIsInCountryList(cnec, network, loopFlowParameters.getCountries()))
                .collect(Collectors.toSet());
        } else {
            return allCnecs.stream()
                .filter(this::hasLoopFlowExtension)
                .collect(Collectors.toSet());
        }
    }

    static boolean cnecIsInCountryList(Cnec<?> cnec, Network network, Set<Country> loopflowCountries) {
        return cnec.getLocation(network).stream().anyMatch(country -> country.isPresent() && loopflowCountries.contains(country.get()));
    }

    public SystematicSensitivityInterface getSystematicSensitivityInterface(Set<FlowCnec> cnecs,
                                                                            Set<RangeAction<?>> rangeActions,
                                                                            boolean computePtdfs,
                                                                            boolean computeLoopFlows, Instant outageInstant) {
        return getSystematicSensitivityInterface(cnecs, rangeActions, computePtdfs, computeLoopFlows, null, outageInstant);
    }

    public SystematicSensitivityInterface getSystematicSensitivityInterface(Set<FlowCnec> cnecs,
                                                                            Set<RangeAction<?>> rangeActions,
                                                                            boolean computePtdfs,
                                                                            boolean computeLoopFlows,
                                                                            AppliedRemedialActions appliedRemedialActions,
                                                                            Instant outageInstant) {

        SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder builder = SystematicSensitivityInterface.builder()
            .withSensitivityProviderName(getSensitivityProvider(raoParameters))
            .withParameters(getSensitivityWithLoadFlowParameters(raoParameters))
            .withRangeActionSensitivities(rangeActions, cnecs, Collections.singleton(Unit.MEGAWATT))
            .withAppliedRemedialActions(appliedRemedialActions)
            .withOutageInstant(outageInstant);

        if (!getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters().isDc()) {
            builder.withLoadflow(cnecs, Collections.singleton(Unit.AMPERE));
        }

        if (computePtdfs && computeLoopFlows) {
            Set<String> eic = getEicForObjectiveFunction();
            eic.addAll(getEicForLoopFlows());
            builder.withPtdfSensitivities(getGlskForEic(eic), cnecs, Collections.singleton(Unit.MEGAWATT));
        } else if (computeLoopFlows) {
            Set<FlowCnec> loopflowCnecs = getLoopFlowCnecs(cnecs);
            builder.withPtdfSensitivities(getGlskForEic(getEicForLoopFlows()), loopflowCnecs, Collections.singleton(Unit.MEGAWATT));
        } else if (computePtdfs) {
            builder.withPtdfSensitivities(getGlskForEic(getEicForObjectiveFunction()), cnecs, Collections.singleton(Unit.MEGAWATT));
        }

        return builder.build();
    }

    Set<String> getEicForObjectiveFunction() {
        if (!raoParameters.hasExtension(RelativeMarginsParametersExtension.class)) {
            throw new OpenRaoException("No relative margins parameters were defined");
        }
        return raoParameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().stream().
            flatMap(boundary -> boundary.getEiCodes().stream()).
            map(EICode::getAreaCode).
            collect(Collectors.toSet());
    }

    Set<String> getEicForLoopFlows() {
        return referenceProgram.getListOfAreas().stream().
            map(EICode::getAreaCode).
            collect(Collectors.toSet());
    }

    ZonalData<SensitivityVariableSet> getGlskForEic(Set<String> listEicCode) {
        Map<String, SensitivityVariableSet> glskBoundaries = new HashMap<>();

        for (String eiCode : listEicCode) {
            SensitivityVariableSet linearGlsk = glskProvider.getData(eiCode);
            if (Objects.isNull(linearGlsk)) {
                OpenRaoLoggerProvider.TECHNICAL_LOGS.warn("No GLSK found for CountryEICode {}", eiCode);
            } else {
                glskBoundaries.put(eiCode, linearGlsk);
            }
        }

        return new ZonalDataImpl<>(glskBoundaries);
    }

    public static ToolProviderBuilder create() {
        return new ToolProviderBuilder();
    }

    public static final class ToolProviderBuilder {
        private Network network;
        private RaoParameters raoParameters;
        private ReferenceProgram referenceProgram;
        private ZonalData<SensitivityVariableSet> glskProvider;
        private LoopFlowComputation loopFlowComputation;
        private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;

        public ToolProviderBuilder withNetwork(Network network) {
            this.network = network;
            return this;
        }

        public ToolProviderBuilder withRaoParameters(RaoParameters raoParameters) {
            this.raoParameters = raoParameters;
            return this;
        }

        public ToolProviderBuilder withLoopFlowComputation(ReferenceProgram referenceProgram, ZonalData<SensitivityVariableSet> glskProvider, LoopFlowComputation loopFlowComputation) {
            this.referenceProgram = referenceProgram;
            this.glskProvider = glskProvider;
            this.loopFlowComputation = loopFlowComputation;
            return this;
        }

        public ToolProviderBuilder withAbsolutePtdfSumsComputation(ZonalData<SensitivityVariableSet> glskProvider, AbsolutePtdfSumsComputation absolutePtdfSumsComputation) {
            this.glskProvider = glskProvider;
            this.absolutePtdfSumsComputation = absolutePtdfSumsComputation;
            return this;
        }

        public ToolProvider build() {
            Objects.requireNonNull(network);
            Objects.requireNonNull(raoParameters);
            ToolProvider toolProvider = new ToolProvider();
            toolProvider.network = network;
            toolProvider.raoParameters = raoParameters;
            toolProvider.referenceProgram = referenceProgram;
            toolProvider.glskProvider = glskProvider;
            toolProvider.loopFlowComputation = loopFlowComputation;
            toolProvider.absolutePtdfSumsComputation = absolutePtdfSumsComputation;
            return toolProvider;
        }
    }

    public static ToolProvider buildFromRaoInputAndParameters(RaoInput raoInput, RaoParameters raoParameters) {

        ToolProvider.ToolProviderBuilder toolProviderBuilder = ToolProvider.create()
            .withNetwork(raoInput.getNetwork())
            .withRaoParameters(raoParameters);
        if (raoInput.getReferenceProgram() != null) {
            toolProviderBuilder.withLoopFlowComputation(
                raoInput.getReferenceProgram(),
                raoInput.getGlskProvider(),
                new LoopFlowComputationImpl(
                    raoInput.getGlskProvider(),
                    raoInput.getReferenceProgram()
                )
            );
        }
        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            if (!raoParameters.hasExtension(RelativeMarginsParametersExtension.class)) {
                throw new OpenRaoException("No relative margins parameters were defined with objective function " + raoParameters.getObjectiveFunctionParameters().getType());
            }
            toolProviderBuilder.withAbsolutePtdfSumsComputation(
                raoInput.getGlskProvider(),
                new AbsolutePtdfSumsComputation(
                    raoInput.getGlskProvider(),
                        raoParameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries()
                )
            );
        }
        return toolProviderBuilder.build();
    }
}
