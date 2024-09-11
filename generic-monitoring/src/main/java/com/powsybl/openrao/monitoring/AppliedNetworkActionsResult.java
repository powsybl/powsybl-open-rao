package com.powsybl.openrao.monitoring;

// result is Set<NetworkAction> if voltage monitoring
// result is Set<NetworkAction> , networkElementsToBeExcluded and power to redispatch if angle monitoring

import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.cracapi.RemedialAction;

import java.util.*;

import static java.lang.String.format;

public class AppliedNetworkActionsResult {

    public static final class AppliedNetworkActionsResultBuilder {
        private static final String REQUIRED_ARGUMENT_MESSAGE = "%s is mandatory when building AppliedNetworkActionsResult.";

        private Set<RemedialAction> appliedNetworkActions;
        private Set<String> networkElementsToBeExcluded;
        private Map<Country, Double> powerToBeRedispatched;

        AppliedNetworkActionsResultBuilder() {
        }

        public AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder withAppliedNetworkActions(Set<RemedialAction> appliedNetworkActions) {
            this.appliedNetworkActions = appliedNetworkActions;
            return this;
        }

        public AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder withNetworkElementsToBeExcluded(Set<String> networkElementsToBeExcluded) {
            this.networkElementsToBeExcluded = networkElementsToBeExcluded;
            return this;
        }

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

    private Set<RemedialAction> appliedNetworkActions;
    private Set<String> networkElementsToBeExcluded;
    private Map<Country, Double> powerToBeRedispatched;

    public static AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder builderForVoltageMonitoring(Set<RemedialAction> appliedNetworkActions) {
        return new AppliedNetworkActionsResultBuilder().withAppliedNetworkActions(appliedNetworkActions);
    }

    public static AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder builderForAngleMonitoring(Set<RemedialAction> appliedNetworkActions, Set<String> networkElementsToBeExcluded, Map<Country, Double> powerToBeRedispatched) {
        return new AppliedNetworkActionsResultBuilder().withAppliedNetworkActions(appliedNetworkActions).withNetworkElementsToBeExcluded(networkElementsToBeExcluded).withPowerToBeRedispatched(powerToBeRedispatched);
    }

    public Set<RemedialAction> getAppliedNetworkActions() {
        return appliedNetworkActions;
    }

    public Set<String> getNetworkElementsToBeExcluded() {
        return networkElementsToBeExcluded;
    }

    public Map<Country, Double> getPowerToBeRedispatched() {
        return powerToBeRedispatched;
    }

}
