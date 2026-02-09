/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.crac.api.Instant;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CriticalElements {

    public enum ThresholdDefinition {
        FROM_OPERATIONAL_LIMITS, // read perm & temp operational limits in network
        PERM_LIMIT_MULTIPLIER // multiply perm limits by a given multiplier
    }

    Optional<Set<Country>> countries = Optional.empty();
    Optional<Double> optimizedMinV = Optional.empty();
    Optional<Double> optimizedMaxV = Optional.empty();
    boolean monitorOtherBranches = false; // non-critical branches are declared as MNECs
    BiPredicate<Branch<?>, Contingency> cnecPredicate = (branch, contingency) -> true;
    ThresholdDefinition thresholdDefinition;
    Map<String, Double> limitMultiplierPerInstant; // multiplies temp or perm limit, depending on thresholdDefinition
    Map<String, Map<Double, Double>> limitMultiplierPerInstantPerNominalV; // multiplies temp or perm limit, depending on thresholdDefinition and voltage level
    Map<String, Double> applicableLimitDurationPerInstant; // mandatory if thresholdDefinition = FROM_OPERATIONAL_LIMITS
    Map<String, Map<Double, Double>> applicableLimitDurationPerInstantPerNominalV; // mandatory if thresholdDefinition = FROM_OPERATIONAL_LIMITS

    public Optional<Set<Country>> getCountries() {
        return countries;
    }

    public void setCountries(Set<Country> countries) {
        this.countries = Optional.ofNullable(countries);
    }

    public Optional<Double> getOptimizedMinV() {
        return optimizedMinV;
    }

    public void setOptimizedMinV(Double optimizedMinV) {
        this.optimizedMinV = Optional.ofNullable(optimizedMinV);
    }

    public Optional<Double> getOptimizedMaxV() {
        return optimizedMaxV;
    }

    public void setOptimizedMaxV(Double optimizedMaxV) {
        this.optimizedMaxV = Optional.ofNullable(optimizedMaxV);
    }

    public boolean isMonitorOtherBranches() {
        return monitorOtherBranches;
    }

    public void setMonitorOtherBranches(boolean monitorOtherBranches) {
        this.monitorOtherBranches = monitorOtherBranches;
    }

    public boolean shouldCreateCnec(Branch<?> branch, Contingency contingency) {
        return cnecPredicate.test(branch, contingency);
    }

    public void setCnecPredicate(BiPredicate<Branch<?>, Contingency> cnecPredicate) {
        this.cnecPredicate = cnecPredicate;
    }

    public ThresholdDefinition getThresholdDefinition() {
        return thresholdDefinition;
    }

    public void setThresholdDefinition(ThresholdDefinition thresholdDefinition) {
        this.thresholdDefinition = thresholdDefinition;
    }

    public Double getLimitMultiplierPerInstant(Instant instant, Double nominalV) {
        double defaultValue = limitMultiplierPerInstant.get(instant.getId());
        if (limitMultiplierPerInstantPerNominalV == null || !limitMultiplierPerInstantPerNominalV.containsKey(instant.getId())) {
            return defaultValue;
        }
        return limitMultiplierPerInstantPerNominalV.get(instant.getId()).getOrDefault(nominalV, defaultValue);
    }

    public void setLimitMultiplierPerInstantPerNominalV(Map<String, Map<Double, Double>> limitMultiplierPerInstantPerNominalV) {
        // todo check all instants are in keys
        this.limitMultiplierPerInstantPerNominalV = limitMultiplierPerInstantPerNominalV;
    }

    public void setLimitMultiplierPerInstant(Map<String, Double> limitMultiplierPerInstant) {
        // todo check all instants are in keys
        this.limitMultiplierPerInstant = limitMultiplierPerInstant;
    }

    public Double getApplicableLimitDuration(Instant instant, Double nominalV) {
        double defaultValue = applicableLimitDurationPerInstant.get(instant.getId());
        if (applicableLimitDurationPerInstantPerNominalV == null || !applicableLimitDurationPerInstantPerNominalV.containsKey(instant.getId())) {
            return defaultValue;
        }
        return applicableLimitDurationPerInstantPerNominalV.get(instant.getId()).getOrDefault(nominalV, defaultValue);
    }

    public void setApplicableLimitDurationPerInstant(Map<String, Double> applicableLimitDurationPerInstant) {
        // todo check all instants are in keys
        this.applicableLimitDurationPerInstant = applicableLimitDurationPerInstant;
    }

    public void setApplicableLimitDurationPerInstantPerNominalV(Map<String, Map<Double, Double>> applicableLimitDurationPerInstantPerNominalV) {
        // todo check all instants are in keys
        this.applicableLimitDurationPerInstantPerNominalV = applicableLimitDurationPerInstantPerNominalV;
    }
}
