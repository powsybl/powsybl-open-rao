/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.GeneratorActionBuilder;
import com.powsybl.action.LoadActionBuilder;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionRangeActionImpl extends AbstractRangeAction<InjectionRangeAction> implements InjectionRangeAction {

    private static final double EPSILON = 1e-6;
    private final Map<NetworkElement, Double> injectionDistributionKeys;
    private final List<StandardRange> ranges;
    private final double initialSetpoint;

    InjectionRangeActionImpl(String id, String name, String operator, String groupId, Set<UsageRule> usageRules,
                             List<StandardRange> ranges, double initialSetpoint, Map<NetworkElement, Double> injectionDistributionKeys, Integer speed, Double activationCost, Map<VariationDirection, Double> variationCosts) {
        super(id, name, operator, usageRules, groupId, speed, activationCost, variationCosts);
        this.ranges = ranges;
        this.initialSetpoint = initialSetpoint;
        this.injectionDistributionKeys = injectionDistributionKeys;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return injectionDistributionKeys.keySet();
    }

    @Override
    public Map<NetworkElement, Double> getInjectionDistributionKeys() {
        return injectionDistributionKeys;
    }

    @Override
    public List<StandardRange> getRanges() {
        return ranges;
    }

    @Override
    public double getMinAdmissibleSetpoint(double previousInstantSetPoint) {
        return StandardRangeActionUtils.getMinAdmissibleSetpoint(previousInstantSetPoint, ranges, initialSetpoint);
    }

    @Override
    public double getMaxAdmissibleSetpoint(double previousInstantSetPoint) {
        return StandardRangeActionUtils.getMaxAdmissibleSetpoint(previousInstantSetPoint, ranges, initialSetpoint);
    }

    @Override
    public double getInitialSetpoint() {
        return initialSetpoint;
    }

    @Override
    public void apply(Network network, double targetSetpoint) {
        injectionDistributionKeys.forEach((ne, sk) -> applyInjection(network, ne.getId(), targetSetpoint * sk));
    }

    // Initial setpoint was taken into account in linear problem, hence targetSetpoint represents network's initial value + optimized variation
    // That's why we overwrite network's exisiting generator/load.
    private void applyInjection(Network network, String injectionId, double targetSetpoint) {
        Generator generator = network.getGenerator(injectionId);
        if (generator != null) {
            new GeneratorActionBuilder()
                .withId("id")
                .withGeneratorId(injectionId)
                .withActivePowerRelativeValue(false)
                .withActivePowerValue(targetSetpoint)
                .build()
                .toModification()
                .apply(network, true, ReportNode.NO_OP);
            return;
        }

        Load load = network.getLoad(injectionId);
        if (load != null) {
            new LoadActionBuilder()
                .withId("id")
                .withLoadId(injectionId)
                .withRelativeValue(false)
                .withActivePowerValue(-targetSetpoint)
                .build()
                .toModification()
                .apply(network, true, ReportNode.NO_OP);
            return;

        }

        if (network.getIdentifiable(injectionId) == null) {
            throw new OpenRaoException(String.format("Injection %s not found in network", injectionId));
        } else {
            throw new OpenRaoException(String.format("%s refers to an object of the network which is not an handled Injection (not a Load, not a Generator)", injectionId));
        }
    }

    // When injection range action has several generators/loads, each generator/load's value divided by its key
    // must be equal because an injection range action has a unique setpoint.
    // For instance : gen1 has a production of 10 in network with key 1, gen2 has a production of 20 with key 2
    // In this case, current setpoint = 10.
    @Override
    public double getCurrentSetpoint(Network network) {
        List<Double> currentSetpoints = injectionDistributionKeys.entrySet().stream()
            .map(entry -> getInjectionSetpoint(network, entry.getKey().getId(), entry.getValue()))
            .collect(Collectors.toList());

        if (currentSetpoints.size() == 1) {
            return currentSetpoints.get(0);
        } else {
            // Injection range action has several generators / loads
            // By sorting current setpionts, we check that all generator/load values divided by their key are equal.
            Collections.sort(currentSetpoints);
            if (Math.abs(currentSetpoints.get(0) - currentSetpoints.get(currentSetpoints.size() - 1)) < EPSILON) {
                return currentSetpoints.get(0);
            } else {
                throw new OpenRaoException(String.format("Cannot evaluate current setpoint of InjectionRangeAction %s, as several injections are not distributed according to their key", this.getId()));
            }
        }
    }

    public double getInjectionSetpoint(Network network, String injectionId, double distributionKey) {
        Generator generator = network.getGenerator(injectionId);
        if (generator != null) {
            return generator.getTargetP() / distributionKey;
        }

        Load load = network.getLoad(injectionId);
        if (load != null) {
            return -load.getP0() / distributionKey;
        }

        if (network.getIdentifiable(injectionId) == null) {
            throw new OpenRaoException(String.format("Injection %s not found in network", injectionId));
        } else {
            throw new OpenRaoException(String.format("%s refers to an object of the network which is not an handled Injection (not a Load, not a Generator)", injectionId));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return this.injectionDistributionKeys.equals(((InjectionRangeAction) o).getInjectionDistributionKeys())
            && this.ranges.equals(((InjectionRangeAction) o).getRanges());
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        for (StandardRange range : ranges) {
            hashCode += 31 * range.hashCode();
        }
        hashCode += 31 * injectionDistributionKeys.hashCode();
        return hashCode;
    }
}
