/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Injection;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.io.network.NetworkCracCreationContext;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.apache.commons.lang3.function.TriFunction;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Adds counter-trading remedial actions with a given list of countries.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CountertradingRangeActions extends AbstractCountriesFilter {
    private TriFunction<Injection<?>, Instant, NetworkCracCreationContext, Boolean> injectionPredicate = (injection, instant, c) -> true;
    private BiFunction<Country, Instant, InjectionRangeActionCosts> raCostsProvider = (country, instant) -> new InjectionRangeActionCosts(0, 0, 0);
    private BiFunction<Country, Instant, MinAndMax<Double>> raRangeProvider = (country, instant) -> new MinAndMax<>(0., 0.);
    private ZonalData<SensitivityVariableSet> glsks = null;

    CountertradingRangeActions() {
        this.setCountryFilter(Set.of()); // no CT by default
    }

    /**
     * Set the function that indicates if a given injection should be included in the RA.
     * (Injections included in Redispatch RAs are automatically excluded)
     */
    public void setInjectionPredicate(TriFunction<Injection<?>, Instant, NetworkCracCreationContext, Boolean> injectionPredicate) {
        this.injectionPredicate = injectionPredicate;
    }

    public boolean shouldIncludeInjection(Injection<?> injection, Instant instant, NetworkCracCreationContext context) {
        return injectionPredicate.apply(injection, instant, context);
    }

    /**
     * Set the function that indicates the costs of counter-trading with a given country.
     */
    public void setRaCostsProvider(BiFunction<Country, Instant, InjectionRangeActionCosts> raCostsProvider) {
        this.raCostsProvider = raCostsProvider;
    }

    public InjectionRangeActionCosts getRaCosts(Country country, Instant instant) {
        return raCostsProvider.apply(country, instant);
    }

    public Optional<ZonalData<SensitivityVariableSet>> getZonalData() {
        return Optional.ofNullable(glsks);
    }

    /**
     * Set GLSKs for simulating countertrading.
     */
    public void setGlsks(ZonalData<SensitivityVariableSet> glsks) {
        this.glsks = glsks;
    }

    /**
     * Set the function that indicates the MW range of counter-trading with given country.
     * By default, range is reduced to zero.
     */
    public void setRaRangeProvider(BiFunction<Country, Instant, MinAndMax<Double>> raRangeProvider) {
        this.raRangeProvider = raRangeProvider;
    }

    public MinAndMax<Double> getRaRange(Country country, Instant instant) {
        return raRangeProvider.apply(country, instant);
    }
}
