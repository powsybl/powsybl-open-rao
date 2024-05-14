/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * This implementation automatically generates proportional GLSK all the while excluding network elements that should be
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RedispatchActionWithAutoGlsk extends AbstractRedispatchAction {
    private final Set<String> networkElementsToBeExcluded;
    private final Country country;

    RedispatchActionWithAutoGlsk(Set<String> networkElementsToBeExcluded, Country country) {
        this.networkElementsToBeExcluded = networkElementsToBeExcluded;
        this.country = Objects.requireNonNull(country);
    }

    private static Scalable getCountryGeneratorsScalable(Network network, Country country, Set<String> networkElementsToBeExcluded) {
        List<Generator> generators = network.getGeneratorStream()
            .filter(RedispatchActionWithAutoGlsk::isInjectionCorrect)
            // using getIdentifiable in case there are aliases
            .filter(generator -> country.equals(generator.getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)))
            .filter(generator -> networkElementsToBeExcluded.stream().noneMatch(ne -> network.getIdentifiable(ne) == generator))
            .toList();
        List<Scalable> scalables = new ArrayList<>();
        List<Double> percentages = new ArrayList<>();
        //calculate sum P of country's generators
        double totalCountryP = generators.stream().mapToDouble(RedispatchActionWithAutoGlsk::pseudoTargetP).sum();
        //calculate factor of each generator
        generators.forEach(generator -> {
            double generatorPercentage = 100 * pseudoTargetP(generator) / totalCountryP;
            percentages.add(generatorPercentage);
            scalables.add(Scalable.onGenerator(generator.getId()));
        });
        return Scalable.proportional(percentages, scalables);
    }

    private static double pseudoTargetP(Generator generator) {
        return Math.max(1e-5, Math.abs(generator.getTargetP()));
    }

    private static boolean isInjectionCorrect(Injection<?> injection) {
        return injection != null &&
            injection.getTerminal().isConnected() &&
            injection.getTerminal().getBusView().getBus() != null &&
            injection.getTerminal().getBusView().getBus().isInMainSynchronousComponent();
    }

    @Override
    public void apply(Network network, double powerToRedispatch) {
        super.apply(network, powerToRedispatch, getCountryGeneratorsScalable(network, country, networkElementsToBeExcluded));
    }
}
