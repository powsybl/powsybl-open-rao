/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Injection;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Configures how redispatching remedial actions are created.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RedispatchingRangeActions extends AbstractCountriesFilter {
    private boolean includeAllInjections = true;
    private BiPredicate<Injection<?>, Instant> rdRaPredicate = (injection, instant) -> injection.getType() == IdentifiableType.GENERATOR;
    private BiFunction<Injection<?>, Instant, InjectionRangeActionCosts> raCostsProvider = (injection, instant) -> new InjectionRangeActionCosts(0, 0, 0);
    private BiFunction<Injection<?>, Instant, MinAndMax<Double>> raRangeProvider = (injection, instant) -> new MinAndMax<>(null, null);
    private Map<String, Set<String>> generatorCombinations = new HashMap<>();
    // TODO add raCostsProvider & raRangeProvider for combinations?

    RedispatchingRangeActions() {
    }

    public boolean includeAllInjections() {
        return includeAllInjections;
    }

    /**
     * This parameter allows you to configure if you want to consider all injections by default as unitary redispatching units
     */
    public void setIncludeAllInjections(boolean includeAllInjections) {
        this.includeAllInjections = includeAllInjections;
    }

    /**
     * Set the function that says if a given injection is available for redispatching at a given instant.
     * Defaults to true on generators, false for other injections.
     */
    public void setRdRaPredicate(BiPredicate<Injection<?>, Instant> rdRaPredicate) {
        this.rdRaPredicate = rdRaPredicate;
    }

    public boolean shouldCreateRedispatchingAction(Injection<?> injection, Instant instant) {
        return rdRaPredicate.test(injection, instant);
    }

    /**
     * Set the function that provides the costs of redispatching on a given injection at a given instant.
     * All costs default to 0.
     */
    public void setRaCostsProvider(BiFunction<Injection<?>, Instant, InjectionRangeActionCosts> raCostsProvider) {
        this.raCostsProvider = raCostsProvider;
    }

    public InjectionRangeActionCosts getRaCosts(Injection<?> injection, Instant instant) {
        return raCostsProvider.apply(injection, instant);
    }

    /**
     * Set the function that provides the MW range for redispatching on  given injection at a given instant.
     * Not setting this (or using null min/max) will use the physical minP - maxP in the network for Generators.
     * If you plan on including Loads, however, you must define the bounds.
     */
    public void setRaRangeProvider(BiFunction<Injection<?>, Instant, MinAndMax<Double>> raRangeProvider) {
        this.raRangeProvider = raRangeProvider;
    }

    public MinAndMax<Double> getRaRange(Injection<?> injection, Instant instant) {
        return raRangeProvider.apply(injection, instant);
    }

    public Map<String, Set<String>> getGeneratorCombinations() {
        return generatorCombinations;
    }

    /**
     * Extra generator combinations to include. Every element of this set (combination of generators) will create one injection range action,
     * with a set of keys that is proportional to the initial distribution of active power production.
     */
    public void setGeneratorCombinations(Map<String, Set<String>> generatorCombinations) {
        if (generatorCombinations.values().stream().flatMap(Set::stream).distinct().count() !=
            generatorCombinations.values().stream().map(Set::size).mapToDouble(Integer::doubleValue).sum()) {
            throw new OpenRaoException("A generator can only be used once in generator combinations.");
        }
        this.generatorCombinations = generatorCombinations;
    }
}
