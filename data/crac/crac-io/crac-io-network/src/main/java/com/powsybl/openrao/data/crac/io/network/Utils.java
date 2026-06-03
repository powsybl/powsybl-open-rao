/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.network.parameters.RangeActionCosts;
import com.powsybl.openrao.data.crac.io.network.parameters.MinAndMax;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
            countries.contains(optionalSubstation.get().getCountry().orElseThrow());
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
        return countries.contains(substation.getCountry().orElseThrow());
    }

    public static void addInjectionRangeAction(NetworkCracCreationContext creationContext,
                                               Set<Injection<?>> consideredInjections,
                                               String raIdPrefix,
                                               Instant instant,
                                               MinAndMax<Double> range,
                                               boolean relativeRange,
                                               RangeActionCosts costs) {
        if (consideredInjections.isEmpty()) {
            return;
        }

        Set<Generator> generators = consideredInjections.stream().filter(g -> g.getType().equals(IdentifiableType.GENERATOR)).map(Generator.class::cast).collect(Collectors.toSet());
        Set<Load> loads = consideredInjections.stream().filter(g -> g.getType().equals(IdentifiableType.LOAD)).map(Load.class::cast).collect(Collectors.toSet());
        if (generators.size() + loads.size() != consideredInjections.size()) {
            throw new OpenRaoImportException(ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO,
                "Injection combinations only allows generators and/or loads"
            );
        }

        consideredInjections.stream().filter(g -> g.getType().equals(IdentifiableType.GENERATOR))
            .map(Generator.class::cast)
            .forEach(g -> g.setTargetP(Math.round(g.getTargetP())));
        double initialTotalP = generators.stream().mapToDouble(Generator::getTargetP).sum() - loads.stream().mapToDouble(Load::getP0).sum();

        // We have to filter out injections with participation smaller than 1e-3
        // If we don't, InjectionRangeActionAdderImpl will, and we will end up with a sum of keys different from 1
        // TODO : add tests
        Set<Generator> generatorsToIgnore = generators.stream().filter(generator ->
            Math.abs(generator.getTargetP() / initialTotalP) < 1e-3
        ).collect(Collectors.toSet());
        generators.removeAll(generatorsToIgnore);
        Set<Load> loadsToIgnore = loads.stream().filter(load ->
            Math.abs(-load.getP0() / initialTotalP) < 1e-3
        ).collect(Collectors.toSet());
        loads.removeAll(loadsToIgnore);

        double totalP = generators.stream().mapToDouble(Generator::getTargetP).sum() - loads.stream().mapToDouble(Load::getP0).sum();

        double minP = Math.round(generators.stream().mapToDouble(Generator::getMinP).sum()
            - loads.stream().mapToDouble(l -> Math.max(l.getP0(), 0)).sum());
        if (range.getMin().isPresent()) {
            minP = Math.max(minP, (relativeRange ? totalP : 0) + range.getMin().orElseThrow());
        }
        minP = Math.round(Math.min(minP, totalP));
        double maxP = Math.round(generators.stream().mapToDouble(Generator::getMaxP).sum()
            - loads.stream().mapToDouble(l -> Math.min(l.getP0(), 0)).sum());
        if (range.getMax().isPresent()) {
            maxP = Math.min(maxP, (relativeRange ? totalP : 0) + range.getMax().orElseThrow());
        }
        maxP = Math.round(Math.max(maxP, totalP));

        if (generators.size() + loads.size() >= 100) {
            creationContext.getCreationReport().warn(
                String.format("More than 100 injections included in the %s action at %s. Consider enforcing your filter, otherwise you may run into memory issues.", raIdPrefix, instant.getId())
            );
        }

        InjectionRangeActionAdder injectionRangeActionAdder = creationContext.getCrac().newInjectionRangeAction()
            .withId(raIdPrefix + "_" + instant.getId())
            .newRange()
            .withMin(minP)
            .withMax(maxP)
            .add()
            .withInitialSetpoint(totalP)
            .withVariationCost(costs.downVariationCost(), VariationDirection.DOWN)
            .withVariationCost(costs.upVariationCost(), VariationDirection.UP)
            .withActivationCost(costs.activationCost())
            .newOnInstantUsageRule().withInstant(instant.getId()).add();

        if (consideredInjections.size() > 1) {
            if (Math.abs(totalP) < 1.) {
                throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA,
                    String.format(
                        "Cannot create injection range (with multiple generators) actions %s at instant %s because initial production is almost zero. Maybe all generators were filtered out.",
                        raIdPrefix,
                        instant
                    )
                );
            }
            generators.forEach(generator -> {
                injectionRangeActionAdder.withNetworkElementAndKey(generator.getTargetP() / totalP, generator.getId());
                creationContext.addInjectionUsedInAction(instant, generator.getId());
            });
            loads.forEach(load -> {
                injectionRangeActionAdder.withNetworkElementAndKey(-load.getP0() / totalP, load.getId());
                creationContext.addInjectionUsedInAction(instant, load.getId());
            });
        } else {
            injectionRangeActionAdder.withNetworkElementAndKey(1., consideredInjections.iterator().next().getId());
        }

        injectionRangeActionAdder.add();
    }
}
