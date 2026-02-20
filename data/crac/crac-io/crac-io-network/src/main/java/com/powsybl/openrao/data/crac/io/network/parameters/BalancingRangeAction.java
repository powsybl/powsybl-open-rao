/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.Injection;
import com.powsybl.openrao.data.crac.api.Instant;

import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Adds a balancing range action on a set of Injections.
 * This allows ensuring load-generation balance and should be associated to high costs.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class BalancingRangeAction {
    private BiPredicate<Injection<?>, Instant> injectionPredicate = (injection, instant) -> true;
    private Function<Instant, InjectionRangeActionCosts> raCostsProvider = instant -> new InjectionRangeActionCosts(0, 0, 0);
    private Function<Instant, MinAndMax<Double>> raRangeProvider = instant -> new MinAndMax<>(0., 0.);

    BalancingRangeAction() {
    }

    /**
     * Set the function that indicates if a given injection should be included in the RA.
     * (Injections included in Redispatch & Countertrading RAs are automatically excluded)
     */
    public void setInjectionPredicate(BiPredicate<Injection<?>, Instant> injectionPredicate) {
        this.injectionPredicate = injectionPredicate;
    }

    public boolean shouldIncludeInjection(Injection<?> injection, Instant instant) {
        return injectionPredicate.test(injection, instant);
    }

    /**
     * Set the function that indicates the costs of balancing in a given country.
     */
    public void setRaCostsProvider(Function<Instant, InjectionRangeActionCosts> raCostsProvider) {
        this.raCostsProvider = raCostsProvider;
    }

    public InjectionRangeActionCosts getRaCosts(Instant instant) {
        return raCostsProvider.apply(instant);
    }

    /**
     * Set the function that indicates the MW range of the balancing RA in a given country.
     * By default, range is reduced to zero.
     */
    public void setRaRangeProvider(Function<Instant, MinAndMax<Double>> raRangeProvider) {
        this.raRangeProvider = raRangeProvider;
    }

    public MinAndMax<Double> getRaRange(Instant instant) {
        return raRangeProvider.apply(instant);
    }
}
