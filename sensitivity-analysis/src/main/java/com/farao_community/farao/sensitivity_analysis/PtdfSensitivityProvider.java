/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.glsk.commons.ZonalData;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.SensitivityVariableType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class PtdfSensitivityProvider extends AbstractSimpleSensitivityProvider {
    private final ZonalData<SensitivityVariableSet> glsk;

    PtdfSensitivityProvider(ZonalData<SensitivityVariableSet> glsk, Set<FlowCnec> cnecs, Set<Unit> units) {
        super(cnecs, units);

        // todo : handle PTDFs in AMPERE
        if (factorsInAmpere || !factorsInMegawatt) {
            FaraoLoggerProvider.TECHNICAL_LOGS.warn("PtdfSensitivity provider currently only handle Megawatt unit");
            factorsInMegawatt = true;
            factorsInAmpere = false;
        }
        this.glsk = Objects.requireNonNull(glsk);
    }

    @Override
    public List<SensitivityFactor> getBasecaseFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();

        if (afterContingencyOnly) {
            return factors;
        }

        //According to ContingencyContext doc, contingencyId should be null for preContingency context
        ContingencyContext preContingencyContext = new ContingencyContext(null, ContingencyContextType.NONE);

        return getFactors(
            preContingencyContext,
            cnecs.stream().filter(cnec -> cnec.getState().getContingency().isEmpty())
        );
    }

    @Override
    public List<SensitivityFactor> getContingencyFactors(Network network, List<Contingency> contingencies) {
        List<SensitivityFactor> factors = new ArrayList<>();
        for (Contingency contingency : contingencies) {
            String contingencyId = contingency.getId();
            ContingencyContext postContingencyContext = new ContingencyContext(contingencyId, ContingencyContextType.SPECIFIC);
            factors.addAll(
                getFactors(
                    postContingencyContext,
                    cnecs.stream().filter(cnec -> cnec.getState().getContingency().isPresent() && cnec.getState().getContingency().get().getId().equals(contingency.getId()))
                ));
        }
        return factors;
    }

    private List<SensitivityFactor> getFactors(ContingencyContext contingencyContext, Stream<FlowCnec> flowCnecsStream) {
        Map<String, SensitivityVariableSet> mapCountryLinearGlsk = glsk.getDataPerZone();
        List<SensitivityFactor> factors = new ArrayList<>();
        Map<NetworkElement, Set<Side>> networkElementsAndSides = new HashMap<>();
        flowCnecsStream.forEach(cnec ->
            networkElementsAndSides.computeIfAbsent(cnec.getNetworkElement(), k -> new HashSet<>())
                .addAll(cnec.getThresholds().stream().map(BranchThreshold::getSide).collect(Collectors.toSet())));
        networkElementsAndSides
            .forEach((ne, sides) ->
                sides.forEach(side ->
                    mapCountryLinearGlsk.values().stream()
                        .map(linearGlsk -> new SensitivityFactor(
                            sideToActivePowerFunctionType(side),
                            ne.getId(),
                            SensitivityVariableType.INJECTION_ACTIVE_POWER,
                            linearGlsk.getId(),
                            true,
                            contingencyContext))
                        .forEach(factors::add)));
        return factors;
    }

    private SensitivityFunctionType sideToActivePowerFunctionType(Side side) {
        if (side.equals(Side.LEFT)) {
            return SensitivityFunctionType.BRANCH_ACTIVE_POWER_1;
        } else {
            return SensitivityFunctionType.BRANCH_ACTIVE_POWER_2;
        }
    }

    @Override
    public List<SensitivityVariableSet> getVariableSets() {
        return new ArrayList<>(glsk.getDataPerZone().values());
    }
}
