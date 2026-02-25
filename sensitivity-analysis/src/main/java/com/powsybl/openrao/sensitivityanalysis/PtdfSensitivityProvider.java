/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.SensitivityVariableType;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class PtdfSensitivityProvider extends AbstractSimpleSensitivityProvider {
    private final ZonalData<SensitivityVariableSet> glsk;

    PtdfSensitivityProvider(ZonalData<SensitivityVariableSet> glsk, Set<FlowCnec> cnecs, Set<Unit> units) {
        super(cnecs, units);
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
            cnecsPerContingencyId.getOrDefault(null, new ArrayList<>()).stream().filter(cnec -> cnec.isConnected(network))
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
                    cnecsPerContingencyId.getOrDefault(contingencyId, new ArrayList<>()).stream().filter(cnec -> cnec.isConnected(network))
                ));
        }
        return factors;
    }

    private List<SensitivityFactor> getFactors(ContingencyContext contingencyContext, Stream<FlowCnec> flowCnecsStream) {
        Map<String, SensitivityVariableSet> mapCountryLinearGlsk = glsk.getDataPerZone();
        List<SensitivityFactor> factors = new ArrayList<>();
        Map<NetworkElement, Set<TwoSides>> networkElementsAndSides = new HashMap<>();
        flowCnecsStream.forEach(cnec -> networkElementsAndSides.computeIfAbsent(cnec.getNetworkElement(), k -> new HashSet<>()).addAll(cnec.getMonitoredSides()));
        networkElementsAndSides
            .forEach((ne, sides) -> {
                // we allow the computation of both function types (branch_active_power and branch_current) at the same time to match loadFlowProvider behavior
                getSensitivityFunctionTypes(sides).forEach(functionType ->
                    mapCountryLinearGlsk.values().stream()
                        .map(linearGlsk -> new SensitivityFactor(
                            functionType,
                            ne.getId(),
                            SensitivityVariableType.INJECTION_ACTIVE_POWER,
                            linearGlsk.getId(),
                            true,
                            contingencyContext))
                        .forEach(factors::add)
                );
            });
        return factors;
    }

    @Override
    public Map<String, HvdcRangeAction> getHvdcs() {
        return new HashMap<>();
    }

    @Override
    public List<SensitivityVariableSet> getVariableSets() {
        return new ArrayList<>(glsk.getDataPerZone().values());
    }
}
