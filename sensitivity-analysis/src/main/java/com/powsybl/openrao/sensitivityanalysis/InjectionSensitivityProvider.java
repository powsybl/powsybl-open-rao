/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.sensitivity.*;

import java.util.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class InjectionSensitivityProvider extends AbstractSimpleSensitivityProvider {
    private final Set<String> injectionIds;

    public InjectionSensitivityProvider(Set<FlowCnec> cnecs, Set<String> injectionIds, Set<Unit> requestedUnits) {
        super(cnecs, requestedUnits);
        this.injectionIds = injectionIds;
    }

    @Override
    public List<SensitivityFactor> getBasecaseFactors(Network network) {
        List<SensitivityFactor> sensiFactors = new ArrayList<>();
        for (String genId : injectionIds) {
            for (FlowCnec cnec : this.cnecs) {
                sensiFactors.add(new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, cnec.getNetworkElement().getId(), SensitivityVariableType.INJECTION_ACTIVE_POWER, genId, false, ContingencyContext.none()));
                sensiFactors.add(new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_2, cnec.getNetworkElement().getId(), SensitivityVariableType.INJECTION_ACTIVE_POWER, genId, false, ContingencyContext.none()));
            }
        }
        return sensiFactors;
    }

    @Override
    public List<SensitivityFactor> getContingencyFactors(Network network, List<Contingency> contingencies) {
        // TODO
        return List.of();
    }

    @Override
    public List<SensitivityVariableSet> getVariableSets() {
        return List.of();
    }

    @Override
    public Map<String, HvdcRangeAction> getHvdcs() {
        return Map.of();
    }
}
