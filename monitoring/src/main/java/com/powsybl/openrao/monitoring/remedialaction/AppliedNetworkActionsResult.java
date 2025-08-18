/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.monitoring.remedialaction;

import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.crac.api.RemedialAction;

import java.util.*;

import static java.lang.String.format;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class AppliedNetworkActionsResult {

    public static final class AppliedNetworkActionsResultBuilder {
        private static final String REQUIRED_ARGUMENT_MESSAGE = "%s is mandatory when building AppliedNetworkActionsResult.";

        private Set<RemedialAction<?>> appliedNetworkActions;
        private Set<String> networkElementsToBeExcluded;
        private Map<Country, Double> powerToBeRedispatched;

        public AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder withAppliedNetworkActions(Set<RemedialAction<?>> appliedNetworkActions) {
            this.appliedNetworkActions = appliedNetworkActions;
            return this;
        }

        // applies only for Angle monitoring
        public AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder withNetworkElementsToBeExcluded(Set<String> networkElementsToBeExcluded) {
            this.networkElementsToBeExcluded = networkElementsToBeExcluded;
            return this;
        }

        // applies only for Angle monitoring
        public AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder withPowerToBeRedispatched(Map<Country, Double> powerToBeRedispatched) {
            this.powerToBeRedispatched = powerToBeRedispatched;
            return this;
        }

        public AppliedNetworkActionsResult build() {
            AppliedNetworkActionsResult appliedNetworkActionsResult = new AppliedNetworkActionsResult();
            appliedNetworkActionsResult.appliedNetworkActions = Objects.requireNonNull(appliedNetworkActions, format(REQUIRED_ARGUMENT_MESSAGE, "Applied network actions"));
            appliedNetworkActionsResult.networkElementsToBeExcluded = networkElementsToBeExcluded;
            appliedNetworkActionsResult.powerToBeRedispatched = powerToBeRedispatched;
            return appliedNetworkActionsResult;
        }
    }

    private Set<RemedialAction<?>> appliedNetworkActions;
    private Set<String> networkElementsToBeExcluded;
    private Map<Country, Double> powerToBeRedispatched;

    public Set<RemedialAction<?>> getAppliedNetworkActions() {
        return appliedNetworkActions;
    }

    public Set<String> getNetworkElementsToBeExcluded() {
        return networkElementsToBeExcluded;
    }

    public Map<Country, Double> getPowerToBeRedispatched() {
        return powerToBeRedispatched;
    }

}
