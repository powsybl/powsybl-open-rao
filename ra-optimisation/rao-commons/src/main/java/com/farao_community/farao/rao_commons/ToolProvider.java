/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.Unit;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThreshold;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class ToolProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolProvider.class);

    private Network network;
    private RaoParameters raoParameters;
    private ReferenceProgram referenceProgram;
    private ZonalData<SensitivityVariableSet> glskProvider;
    private AbsolutePtdfSumsComputation absolutePtdfSumsComputation;
    private LoopFlowComputation loopFlowComputation;

    private ToolProvider() {
        // Should not be used
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
        if (!raoParameters.getLoopflowCountries().isEmpty()) {
            return allCnecs.stream()
                    .filter(cnec -> hasLoopFlowExtension(cnec) && cnecIsInCountryList(cnec, network, raoParameters.getLoopflowCountries()))
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
                                                                            Set<RangeAction> rangeActions,
                                                                            boolean computePtdfs,
                                                                            boolean computeLoopFlows) {
        return getSystematicSensitivityInterface(cnecs, rangeActions, computePtdfs, computeLoopFlows, null);
    }

    public SystematicSensitivityInterface getSystematicSensitivityInterface(Set<FlowCnec> cnecs,
                                                                            Set<RangeAction> rangeActions,
                                                                            boolean computePtdfs,
                                                                            boolean computeLoopFlows,
                                                                            AppliedRemedialActions appliedRemedialActions) {

        Set<Unit> flowUnits = new HashSet<>();
        flowUnits.add(Unit.MEGAWATT);
        if (!raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters().isDc()) {
            flowUnits.add(Unit.AMPERE);
        }

        SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder builder = SystematicSensitivityInterface.builder()
                .withDefaultParameters(raoParameters.getDefaultSensitivityAnalysisParameters())
                .withFallbackParameters(raoParameters.getFallbackSensitivityAnalysisParameters())
                .withRangeActionSensitivities(rangeActions, cnecs, flowUnits)
                .withAppliedRemedialActions(appliedRemedialActions);

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
        return raoParameters.getRelativeMarginPtdfBoundaries().stream().
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
                LOGGER.warn("No GLSK found for CountryEICode {}", eiCode);
            } else {
                glskBoundaries.put(eiCode, linearGlsk);
            }
        }

        return new ZonalDataImpl<>(glskBoundaries);
    }
}
