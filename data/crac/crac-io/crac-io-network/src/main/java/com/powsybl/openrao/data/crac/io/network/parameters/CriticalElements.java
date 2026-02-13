/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Configures how CNECs must be created.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CriticalElements extends AbstractCountriesFilter {
    private final Set<String> instants;
    private MinAndMax<Double> optimizedMinMaxV = new MinAndMax<>(null, null);
    private MinAndMax<Double> monitoredMinMaxV; // non-critical branches are declared as MNECs
    private BiFunction<Branch<?>, Contingency, OptimizedMonitored> optimizedMonitoredProvider = (branch, contingency) -> new OptimizedMonitored(true, true);
    private ThresholdDefinition thresholdDefinition;
    private Map<String, Double> limitMultiplierPerInstant; // multiplies temp or perm limit, depending on thresholdDefinition
    private Map<String, Map<Double, Double>> limitMultiplierPerInstantPerNominalV; // multiplies temp or perm limit, depending on thresholdDefinition and voltage level
    // at least one of the two following attributes is mandatory if thresholdDefinition = FROM_OPERATIONAL_LIMITS
    private Map<String, Double> applicableLimitDurationPerInstant;
    private Map<String, Map<Double, Double>> applicableLimitDurationPerInstantPerNominalV;

    public record OptimizedMonitored(boolean optimized, boolean monitored) {
    }

    public enum ThresholdDefinition {
        FROM_OPERATIONAL_LIMITS, // read perm & temp operational limits in network
        PERM_LIMIT_MULTIPLIER // multiply perm limits by a given multiplier
    }

    CriticalElements(List<String> instants) {
        this.instants = new HashSet<>(instants);
    }

    /**
     * Set the voltage thresholds (in kV, included) to consider branches as optimized critical elements.
     * You can use {@code null} to disable min and/or max filter.
     * By default, this filter is disabled.
     */
    public void setOptimizedMinMaxV(@Nullable Double minV, @Nullable Double maxV) {
        optimizedMinMaxV = new MinAndMax<>(minV, maxV);
    }

    public Optional<Double> getOptimizedMinV() {
        return optimizedMinMaxV.getMin();
    }

    public Optional<Double> getOptimizedMaxV() {
        return optimizedMinMaxV.getMax();
    }

    /**
     * Set the voltage thresholds (in kV, included) to consider branches as monitored network elements.
     * You can use {@code null} to disable min and/or max filter.
     * Setting the MinAndMax object to {@code null} will disable the monitored network elements feature.
     * By default, the feature is disabled.
     */
    public void setMonitoredMinMaxV(@Nullable MinAndMax<Double> minAndMax) {
        monitoredMinMaxV = minAndMax;
    }

    public Optional<MinAndMax<Double>> getMonitoredMinMaxV() {
        return Optional.ofNullable(monitoredMinMaxV);
    }

    public OptimizedMonitored isOptimizedOrMonitored(Branch<?> branch, @Nullable Contingency contingency) {
        return optimizedMonitoredProvider.apply(branch, contingency);
    }

    /**
     * Set the function that says if a branch and a contingency should be paired into an optimized and/or monitored CNEC.
     * Basecase is represented by a null Contingency.
     * Not setting this will default to all branches being optimized and monitored.
     */
    public void setOptimizedMonitoredProvider(BiFunction<Branch<?>, Contingency, OptimizedMonitored> optimizedMonitoredProvider) {
        this.optimizedMonitoredProvider = optimizedMonitoredProvider;
    }

    public ThresholdDefinition getThresholdDefinition() {
        return thresholdDefinition;
    }

    /**
     * Set this to define how the CRAC creator should map CNEC thresholds to operational limits in the network:
     * - FROM_OPERATIONAL_LIMITS: will read permanent & temporary limits in the network, and multiply them as defined in the multiplier map (see setLimitMultiplierPerInstant & setLimitMultiplierPerInstantPerNominalV).
     * You must define how the acceptable duration for every threshold is mapped to a RAO instant (see setApplicableLimitDurationPerInstant and setApplicableLimitDurationPerInstantPerNominalV)
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

    private void checkAllInstantsListed(Map<String, ?> map) {
        if (!this.instants.equals(map.keySet())) {
            throw new OpenRaoException("You must define the value for every instant.");
        }
    }

    private void checkAllPostcontingencyInstantsListed(Map<String, ?> map) {
        if (!this.instants.stream().filter(i -> !i.equals(NetworkCracCreationParameters.PREVENTIVE_INSTANT_ID)).collect(Collectors.toSet()).equals(map.keySet())) {
            throw new OpenRaoException("You must define the value for every instant except preventive.");
        }
    }

    /**
     * Set the operational limit multiplier for every instant.
     * This will be the default value if the multiplier is not specified for the branch's nominal V (see setLimitMultiplierPerInstantPerNominalV).
     */
    public void setLimitMultiplierPerInstant(Map<String, Double> limitMultiplierPerInstant) {
        checkAllInstantsListed(limitMultiplierPerInstant);
        this.limitMultiplierPerInstant = limitMultiplierPerInstant;
    }

    /**
     * Set the operational limit multiplier for every instant and every nominal V.
     * You can use setLimitMultiplierPerInstant to set defaults for all nominal Vs.
     */
    public void setLimitMultiplierPerInstantPerNominalV(Map<String, Map<Double, Double>> limitMultiplierPerInstantPerNominalV) {
        checkAllInstantsListed(limitMultiplierPerInstantPerNominalV);
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
        checkAllPostcontingencyInstantsListed(applicableLimitDurationPerInstant);
        this.applicableLimitDurationPerInstant = applicableLimitDurationPerInstant;
    }

    /**
     * For every instant and every nominal V, set the acceptable temporary limit duration.
     * This duration is used as a reference to select the operational limit that must be respected by the RAO at that instant.
     * You can use setApplicableLimitDurationPerInstant to set defaults for all nominal Vs.
     */
    public void setApplicableLimitDurationPerInstantPerNominalV(Map<String, Map<Double, Double>> applicableLimitDurationPerInstantPerNominalV) {
        checkAllPostcontingencyInstantsListed(applicableLimitDurationPerInstantPerNominalV);
        this.applicableLimitDurationPerInstantPerNominalV = applicableLimitDurationPerInstantPerNominalV;
    }
}
