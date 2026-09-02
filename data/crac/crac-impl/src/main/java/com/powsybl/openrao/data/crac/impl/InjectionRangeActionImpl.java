/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.Action;
import com.powsybl.action.GeneratorActionBuilder;
import com.powsybl.action.LoadActionBuilder;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmInjectionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionRangeActionImpl extends AbstractRangeAction<InjectionRangeAction> implements InjectionRangeAction {

    private final Map<NetworkElement, Double> injectionDistributionKeys;
    private final List<StandardRange> ranges;
    private final Double initialSetpoint;

    InjectionRangeActionImpl(String id, String name, String operator, String groupId, Set<UsageRule> usageRules,
                             List<StandardRange> ranges, Double initialSetpoint, Map<NetworkElement, Double> injectionDistributionKeys,
                             Integer speed, Double activationCost, Map<VariationDirection, Double> variationCosts) {
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
    public Double getInitialSetpoint() {
        return initialSetpoint;
    }

    @Override
    public void apply(Network network, double targetSetpoint) {
        toActions(targetSetpoint, network).forEach(action -> action.toModification().apply(network, true, ReportNode.NO_OP));
    }

    @Override
    public List<Action> toActions(double setpoint, Network network) {
        List<Action> actions = new ArrayList<>();
        injectionDistributionKeys.forEach((ne, key) -> actions.add(buildInjectionAction(network, ne.getId(), setpoint * key)));
        return actions;
    }

    private static Action buildInjectionAction(Network network, String injectionId, double targetSetpoint) {
        Generator generator = network.getGenerator(injectionId);
        if (generator != null) {
            return new GeneratorActionBuilder()
                .withId(injectionId)
                .withGeneratorId(injectionId)
                .withActivePowerRelativeValue(false)
                .withActivePowerValue(targetSetpoint)
                .build();
        }
        Load load = network.getLoad(injectionId);
        if (load != null) {
            return new LoadActionBuilder()
                .withId(injectionId)
                .withLoadId(injectionId)
                .withRelativeValue(false)
                .withActivePowerValue(-targetSetpoint)
                .build();
        }
        throw new OpenRaoException(String.format("Injection %s not found in network", injectionId));
    }

    // When injection range action has several generators/loads, each generator/load's value divided by its key
    // must be equal because an injection range action has a unique setpoint.
    // For instance : gen1 has a production of 10 in network with key 1, gen2 has a production of 20 with key 2
    // In this case, current setpoint = 10.
    @Override
    public double getCurrentSetpoint(Network network) {
        return IidmInjectionHelper.getCurrentSetpoint(
            network,
            injectionDistributionKeys.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey().getId(),
                Map.Entry::getValue
            ))
        );
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
