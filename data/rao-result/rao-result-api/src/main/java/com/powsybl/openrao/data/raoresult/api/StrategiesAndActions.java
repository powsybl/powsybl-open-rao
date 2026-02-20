/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import com.powsybl.action.Action;
import com.powsybl.contingency.Contingency;
import com.powsybl.security.strategy.OperatorStrategy;

import java.util.Optional;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record StrategiesAndActions(Set<OperatorStrategy> operatorStrategies, Set<Action> actions) {
    public Optional<OperatorStrategy> getPreventiveStrategy() {
        return operatorStrategies.stream().filter(operatorStrategy -> operatorStrategy.getContingencyContext().getContingencyId() == null).findFirst();
    }

    public Optional<OperatorStrategy> getStrategy(Contingency contingency) {
        return operatorStrategies.stream().filter(operatorStrategy -> contingency.getId().equals(operatorStrategy.getContingencyContext().getContingencyId())).findFirst();
    }
}
