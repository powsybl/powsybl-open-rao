/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.results;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.monitoring.SecurityStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface MonitoringResult<I extends Cnec<?>> {
    Set<CnecResult<I>> getCnecResults();

    Map<State, Set<NetworkAction>> getAppliedNetworkActions();

    Set<NetworkAction> getAppliedNetworkActions(State state);

    Set<String> getAppliedNetworkActions(String stateId);

    SecurityStatus getStatus();

    List<String> printConstraints();

    void combine(MonitoringResult<I> monitoringResult);

    static SecurityStatus combineStatuses(SecurityStatus... statuses) {
        boolean atLeastOneFailed = Arrays.asList(statuses).contains(SecurityStatus.FAILURE);
        if (atLeastOneFailed) {
            return SecurityStatus.FAILURE;
        }

        boolean atLeastOneHigh = Arrays.asList(statuses).contains(SecurityStatus.HIGH_CONSTRAINT);
        boolean atLeastOneLow = Arrays.asList(statuses).contains(SecurityStatus.LOW_CONSTRAINT);
        boolean atLeastOneHighAndLow = Arrays.asList(statuses).contains(SecurityStatus.HIGH_AND_LOW_CONSTRAINTS) || atLeastOneHigh && atLeastOneLow;

        if (atLeastOneHighAndLow) {
            return SecurityStatus.HIGH_AND_LOW_CONSTRAINTS;
        }
        if (atLeastOneHigh) {
            return SecurityStatus.HIGH_CONSTRAINT;
        }
        if (atLeastOneLow) {
            return SecurityStatus.LOW_CONSTRAINT;
        }
        return SecurityStatus.SECURE;
    }

    void setStatusToFailure();
}
