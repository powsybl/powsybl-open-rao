/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.network.parameters.InjectionRangeActionCosts;
import com.powsybl.openrao.data.crac.io.network.parameters.MinAndMax;

import java.util.Optional;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class Utils {

    private Utils() {
        // should not be used
    }

    public static boolean branchIsInVRange(Branch<?> branch, Optional<Double> minV, Optional<Double> maxV) {
        return (minV.isEmpty() || branch.getTerminal1().getVoltageLevel().getNominalV() >= minV.get() && branch.getTerminal2().getVoltageLevel().getNominalV() >= minV.get())
            && (maxV.isEmpty() || branch.getTerminal1().getVoltageLevel().getNominalV() <= maxV.get() && branch.getTerminal2().getVoltageLevel().getNominalV() <= maxV.get());
    }

    public static boolean terminalIsInCountries(Terminal terminal, Set<Country> countries) {
        Optional<Substation> optionalSubstation = terminal.getVoltageLevel().getSubstation();
        return optionalSubstation.isPresent() && optionalSubstation.get().getCountry().isPresent() &&
            countries.contains(optionalSubstation.get().getCountry().get());
    }

    public static boolean branchIsInCountries(Branch<?> branch, Set<Country> countries) {
        if (countries == null) {
            return true;
        }
        return terminalIsInCountries(branch.getTerminal1(), countries) || terminalIsInCountries(branch.getTerminal2(), countries);
    }

    public static boolean injectionIsInCountries(Injection<?> generator, Set<Country> countries) {
        if (countries == null) {
            return true;
        }
        Optional<Substation> substationOptional = generator.getTerminal().getVoltageLevel().getSubstation();
        if (substationOptional.isEmpty()) {
            return false;
        }
        Substation substation = substationOptional.get();
        if (substation.getCountry().isEmpty()) {
            return false;
        }
        return countries.contains(substation.getCountry().get());
    }

    public static boolean injectionIsNotUsedInAnyInjectionRangeAction(Crac crac, Injection<?> injection, Instant instant) {
        return crac.getInjectionRangeActions()
            .stream().noneMatch(ra -> ra.getUsageRules().stream().anyMatch(ur -> ur.getInstant().equals(instant)) &&
                ra.getNetworkElements().stream().anyMatch(ne -> ne.getId().equals(injection.getId())));
    }

    public static void addInjectionRangeAction(CracCreationContext creationContext, Set<Generator> consideredGenerators, String raIdPrefix, Instant instant, MinAndMax<Double> range, boolean relativeRange, InjectionRangeActionCosts costs) {
        double initialTotalP = Math.round(consideredGenerators.stream().mapToDouble(Generator::getTargetP).sum());
        double minP = Math.round(consideredGenerators.stream().mapToDouble(Generator::getMinP).sum());
        if (range.getMin().isPresent()) {
            minP = Math.max(minP, (relativeRange ? initialTotalP : 0) + range.getMin().orElseThrow());
        }
        minP = Math.min(minP, initialTotalP);
        double maxP = Math.round(consideredGenerators.stream().mapToDouble(Generator::getMaxP).sum());
        if (range.getMax().isPresent()) {
            maxP = Math.min(maxP, (relativeRange ? initialTotalP : 0) + range.getMax().orElseThrow());
        }
        maxP = Math.max(maxP, initialTotalP);

        if (consideredGenerators.size() >= 100) {
            creationContext.getCreationReport().warn(
                String.format("More than 100 generators included in the %s action at %s. Consider enforcing your filter, otherwise you may run into memory issues.", raIdPrefix, instant.getId())
            );
        }

        InjectionRangeActionAdder injectionRangeActionAdder = creationContext.getCrac().newInjectionRangeAction()
            .withId(raIdPrefix + "_" + instant.getId())
            .newRange()
            .withMin(minP)
            .withMax(maxP)
            .add()
            .withInitialSetpoint(initialTotalP)
            .withVariationCost(costs.downVariationCost(), VariationDirection.DOWN)
            .withVariationCost(costs.upVariationCost(), VariationDirection.UP)
            .withActivationCost(costs.activationCost())
            .newOnInstantUsageRule().withInstant(instant.getId()).add();

        if (consideredGenerators.size() > 1) {
            if (initialTotalP < 1.) {
                throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format("Cannot create injection range (with multiple generators) actions %s at instant %s because initial production is almost zero. Maybe all generators were filtered out.", raIdPrefix, instant));
            }
            consideredGenerators.forEach(generator -> injectionRangeActionAdder.withNetworkElementAndKey(generator.getTargetP() / initialTotalP, generator.getId()));
        } else {
            injectionRangeActionAdder.withNetworkElementAndKey(1., consideredGenerators.iterator().next().getId());
        }

        injectionRangeActionAdder.add();
    }
}
