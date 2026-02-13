/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.util.SwitchPredicates;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.network.parameters.InjectionRangeActionCosts;
import com.powsybl.openrao.data.crac.io.network.parameters.MinAndMax;
import com.powsybl.openrao.data.crac.io.network.parameters.RedispatchingRangeActions;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RedispatchingCreator {
    private final Crac crac;
    private final Network network;
    private final RedispatchingRangeActions parameters;
    private final NetworkCracCreationContext creationContext;
    private int nActions = 0;

    RedispatchingCreator(NetworkCracCreationContext creationContext, Network network, RedispatchingRangeActions parameters) {
        this.creationContext = creationContext;
        this.crac = creationContext.getCrac();
        this.network = network;
        this.parameters = parameters;
    }

    void addRedispatchRangeActions() {
        Set<Instant> instants = crac.getSortedInstants().stream().filter(instant -> !instant.isOutage()).collect(Collectors.toSet());
        instants.forEach(instant -> parameters.getGeneratorCombinations().forEach((id, generators) -> addGeneratorCombinationActionForInstant(id, generators, instant)));
        if (!parameters.includeAllInjections()) {
            return;
        }
        Set<String> generatorsInCombinations = // should be excluded from individual redispatching
            parameters.getGeneratorCombinations().values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        network.getGeneratorStream()
            .filter(generator -> Utils.injectionIsInCountries(generator, parameters.getCountries().orElse(null)))
            .filter(generator -> !generatorsInCombinations.contains(generator.getId()))
            .forEach(generator ->
                instants.stream().filter(instant -> parameters.shouldCreateRedispatchingAction(generator, instant))
                    .forEach(instant -> addGeneratorActionForInstant(generator, instant)));
        network.getLoadStream()
            .filter(load -> Utils.injectionIsInCountries(load, parameters.getCountries().orElse(null)))
            .forEach(load ->
                instants.stream().filter(instant -> parameters.shouldCreateRedispatchingAction(load, instant))
                    .forEach(instant -> addLoadActionForInstant(load, instant)));
        // TODO add other injections (batteries...)
    }

    private void checkNumberOfActions() {
        nActions++;
        if (nActions == 500) {
            creationContext.getCreationReport().warn("More than 500 redispatching actions have been created. Consider enforcing your filter, otherwise you may run into memory issues.");
        }
    }

    private void addGeneratorCombinationActionForInstant(String combinationId, Set<String> generatorIds, Instant instant) {
        Set<Generator> consideredGenerators;
        try {
            consideredGenerators = generatorIds.stream().map(network::getGenerator).filter(g -> parameters.shouldCreateRedispatchingAction(g, instant)).collect(Collectors.toSet());
        } catch (PowsyblException e) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, e.getMessage());
        }

        if (consideredGenerators.isEmpty()) {
            return;
        }

        if (consideredGenerators.stream().anyMatch(g -> parameters.getRaRange(g, instant).getMin().isPresent()) && consideredGenerators.stream().anyMatch(g -> parameters.getRaRange(g, instant).getMin().isEmpty())
            || consideredGenerators.stream().anyMatch(g -> parameters.getRaRange(g, instant).getMax().isPresent()) && consideredGenerators.stream().anyMatch(g -> parameters.getRaRange(g, instant).getMax().isEmpty())) {
            throw new OpenRaoImportException(ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, String.format(
                "Cannot create generator combination %s: if you define range min (resp. max) for one generator, you have to define it for all the others too.", combinationId));
        }

        Double min = null;
        if (consideredGenerators.stream().anyMatch(g -> parameters.getRaRange(g, instant).getMin().isPresent())) {
            // Then all min values are present because of previous check
            min = consideredGenerators.stream().map(g -> parameters.getRaRange(g, instant).getMin().orElseThrow()).mapToDouble(Double::doubleValue).sum();
        }
        Double max = null;
        if (consideredGenerators.stream().anyMatch(g -> parameters.getRaRange(g, instant).getMax().isPresent())) {
            // Then all max values are present because of previous check
            max = consideredGenerators.stream().map(g -> parameters.getRaRange(g, instant).getMax().orElseThrow()).mapToDouble(Double::doubleValue).sum();
        }

        InjectionRangeActionCosts averageCosts;
        try {
            averageCosts = new InjectionRangeActionCosts(
                consideredGenerators.stream().map(g -> parameters.getRaCosts(g, instant).activationCost()).mapToDouble(Double::doubleValue).average().orElseThrow(),
                consideredGenerators.stream().map(g -> parameters.getRaCosts(g, instant).upVariationCost()).mapToDouble(Double::doubleValue).average().orElseThrow(),
                consideredGenerators.stream().map(g -> parameters.getRaCosts(g, instant).downVariationCost()).mapToDouble(Double::doubleValue).average().orElseThrow()
            );
        } catch (NoSuchElementException e) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Cannot create generator combination %s: you have to define the costs for at least one generator.", combinationId));
        }

        Utils.addInjectionRangeAction(
            creationContext,
            consideredGenerators,
            "RD_COMBI_" + combinationId,
            instant,
            new MinAndMax<>(min, max),
            false,
            averageCosts);

        consideredGenerators.forEach(generator -> generator.connect(SwitchPredicates.IS_OPEN));

        checkNumberOfActions();
    }

    private void addGeneratorActionForInstant(Generator generator, Instant instant) {
        // TODO merge preventive & curative RA if ranges are the same?
        // advantage : simpler crac
        // disadvantage : more complex code + we might not see bugs in "detailed" version
        Utils.addInjectionRangeAction(
            creationContext,
            Set.of(generator),
            "RD_GEN_" + generator.getId(),
            instant,
            parameters.getRaRange(generator, instant),
            false,
            parameters.getRaCosts(generator, instant));

        // connect the generator
        generator.connect(SwitchPredicates.IS_OPEN);

        checkNumberOfActions();
    }

    private void addLoadActionForInstant(Load load, Instant instant) {
        double initialP = Math.round(load.getP0());
        // TODO round it in network too ?
        if (parameters.getRaRange(load, instant).getMin().isEmpty() || parameters.getRaRange(load, instant).getMax().isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, String.format("Could not create range action for load %s at instant %s, because you did not define its min or max value in the parameters.", load.getId(), instant.getId()));
        }
        double minP = Math.min(initialP, parameters.getRaRange(load, instant).getMin().get());
        double maxP = Math.max(initialP, parameters.getRaRange(load, instant).getMax().get());
        InjectionRangeActionCosts costs = parameters.getRaCosts(load, instant);
        crac.newInjectionRangeAction()
            .withId("RD_LOAD_" + load.getId() + "_" + instant.getId())
            .withNetworkElementAndKey(1.0, load.getId())
            .newRange()
            .withMin(minP)
            .withMax(maxP).add()
            .newOnInstantUsageRule().withInstant(instant.getId()).add()
            .withInitialSetpoint(initialP)
            .withVariationCost(costs.downVariationCost(), VariationDirection.DOWN)
            .withVariationCost(costs.upVariationCost(), VariationDirection.UP)
            .withActivationCost(costs.activationCost())
            .add();

        // connect the generator
        load.connect(SwitchPredicates.IS_OPEN);

        checkNumberOfActions();
    }
}
