/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.openrao.data.crac.api.Instant;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Configures how CNECs must be created.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CriticalElements extends AbstractCountriesFilter {
    private MinAndMax<Double> minAndMaxOptimizedV;
    private MinAndMax<Double> minAndMaxMonitoredV; // non-critical branches are declared as MNECs
    private BiPredicate<Branch<?>, Contingency> cnecPredicate = (branch, contingency) -> true;
    private ThresholdDefinition thresholdDefinition;
    private Map<String, Double> limitMultiplierPerInstant; // multiplies temp or perm limit, depending on thresholdDefinition
    private Map<String, Map<Double, Double>> limitMultiplierPerInstantPerNominalV; // multiplies temp or perm limit, depending on thresholdDefinition and voltage level
    private Map<String, Double> applicableLimitDurationPerInstant; // mandatory if thresholdDefinition = FROM_OPERATIONAL_LIMITS
    private Map<String, Map<Double, Double>> applicableLimitDurationPerInstantPerNominalV; // mandatory if thresholdDefinition = FROM_OPERATIONAL_LIMITS

    public enum ThresholdDefinition {
        FROM_OPERATIONAL_LIMITS, // read perm & temp operational limits in network
        PERM_LIMIT_MULTIPLIER // multiply perm limits by a given multiplier
    }

    /**
     * Set the voltage thresholds (in kV, included) to consider branches as optimized critical elements.
     * You can use {@code null} to disable min and/or max filter.
     */
    public void setOptimizedMinAndMaxV(@Nullable Double minV, @Nullable Double maxV) {
        // TODO check that there is no overlap with minAndMaxMonitoredV
        minAndMaxOptimizedV = new MinAndMax<>(minV, maxV);
    }

    public Optional<Double> getOptimizedMinV() {
        return minAndMaxOptimizedV.getMin();
    }

    public Optional<Double> getOptimizedMaxV() {
        return minAndMaxOptimizedV.getMax();
    }

    /**
     * Set the voltage thresholds (in kV, included) to consider branches as monitored network elements.
     * You can use {@code null} to disable min and/or max filter.
     * Setting the MinAndMax object to {@code null} will disable the monitored network elements feature.
     */
    public void setMinAndMaxMonitoredV(@Nullable MinAndMax<Double> minAndMax) {
        // TODO check that there is no overlap with minAndMaxOptimizedV
        minAndMaxMonitoredV = minAndMax;
    }

    public Optional<MinAndMax<Double>> getMinAndMaxMonitoredV() {
        return Optional.of(minAndMaxMonitoredV);
    }

    public boolean shouldCreateCnec(Branch<?> branch, Contingency contingency) {
        return cnecPredicate.test(branch, contingency);
    }

    /**
     * Set the function that says if a branch and a contingency should be paired into a CNEC.
     * Not setting this will default to true.
     */
    public void setCnecPredicate(BiPredicate<Branch<?>, Contingency> cnecPredicate) {
        // TODO consider replacing Contingency with Set<Branch> (set of contingency elements)
        this.cnecPredicate = cnecPredicate;
    }

    public ThresholdDefinition getThresholdDefinition() {
        return thresholdDefinition;
    }

    /**
     * Set this to define how the CRAC creator should map CNEC thresholds to operational limits in the network:
     * - FROM_OPERATIONAL_LIMITS: will read permanent & temporary limits in the network, and multiply them as defined in the multiplier map (see setLimitMultiplierPerInstant & setLimitMultiplierPerInstantPerNominalV).
     * You must define how the acceptable duration for every threshold is mapped to a RAO instant (see setApplicableLimitDurationPerInstant)
     * - PERM_LIMIT_MULTIPLIER: will only use permanent limits in the network, and multiply them as defined in the multiplier map (see setLimitMultiplierPerInstant & setLimitMultiplierPerInstantPerNominalV).
     */
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

    /**
     * Set the operational limit multiplier for every instant.
     * This will be the default value if the multiplier is not specified for the branch's nominal V (see setLimitMultiplierPerInstantPerNominalV).
     */
    public void setLimitMultiplierPerInstant(Map<String, Double> limitMultiplierPerInstant) {
        // todo check all instants are in keys
        this.limitMultiplierPerInstant = limitMultiplierPerInstant;
    }

    /**
     * Set the operational limit multiplier for every instant and every nominal V.
     * You can use setLimitMultiplierPerInstant to set defaults for all nominal Vs.
     */
    public void setLimitMultiplierPerInstantPerNominalV(Map<String, Map<Double, Double>> limitMultiplierPerInstantPerNominalV) {
        // todo check all instants are in keys
        this.limitMultiplierPerInstantPerNominalV = limitMultiplierPerInstantPerNominalV;
    }

    public Double getApplicableLimitDuration(Instant instant, Double nominalV) {
        double defaultValue = applicableLimitDurationPerInstant.get(instant.getId());
        if (applicableLimitDurationPerInstantPerNominalV == null || !applicableLimitDurationPerInstantPerNominalV.containsKey(instant.getId())) {
            return defaultValue;
        }
        return applicableLimitDurationPerInstantPerNominalV.get(instant.getId()).getOrDefault(nominalV, defaultValue);
    }

    /**
     * For every instant, set the acceptable temporary limit duration.
     * This duration is used as a reference to select the operational limit that must be respected by the RAO at that instant.
     * This will be the default value if the duration is not specified for the branch's nominal V (see setApplicableLimitDurationPerInstantPerNominalV).
     */
    public void setApplicableLimitDurationPerInstant(Map<String, Double> applicableLimitDurationPerInstant) {
        // todo check all instants are in keys
        this.applicableLimitDurationPerInstant = applicableLimitDurationPerInstant;
    }

    /**
     * For every instant and every nominal V, set the acceptable temporary limit duration.
     * This duration is used as a reference to select the operational limit that must be respected by the RAO at that instant.
     * You can use setApplicableLimitDurationPerInstant to set defaults for all nominal Vs.
     */
    public void setApplicableLimitDurationPerInstantPerNominalV(Map<String, Map<Double, Double>> applicableLimitDurationPerInstantPerNominalV) {
        // todo check all instants are in keys
        this.applicableLimitDurationPerInstantPerNominalV = applicableLimitDurationPerInstantPerNominalV;
    }
}
