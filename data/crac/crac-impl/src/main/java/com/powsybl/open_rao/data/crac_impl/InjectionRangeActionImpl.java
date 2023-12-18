/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.data.crac_api.NetworkElement;
import com.powsybl.open_rao.data.crac_api.range.StandardRange;
import com.powsybl.open_rao.data.crac_api.range_action.InjectionRangeAction;
import com.powsybl.open_rao.data.crac_api.usage_rule.UsageRule;
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

    private final Map<NetworkElement, Double> injectionDistributionKeys;
    private final List<StandardRange> ranges;
    private final double initialSetpoint;

    InjectionRangeActionImpl(String id, String name, String operator, String groupId, Set<UsageRule> usageRules,
                             List<StandardRange> ranges, double initialSetpoint, Map<NetworkElement, Double> injectionDistributionKeys, Integer speed) {
        super(id, name, operator, usageRules, groupId, speed);
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

    private void applyInjection(Network network, String injectionId, double targetSetpoint) {
        Generator generator = network.getGenerator(injectionId);
        if (generator != null) {
            generator.setTargetP(targetSetpoint);
            return;
        }

        Load load = network.getLoad(injectionId);
        if (load != null) {
            load.setP0(-targetSetpoint);
            return;
        }

        if (network.getIdentifiable(injectionId) == null) {
            throw new FaraoException(String.format("Injection %s not found in network", injectionId));
        } else {
            throw new FaraoException(String.format("%s refers to an object of the network which is not an handled Injection (not a Load, not a Generator)", injectionId));
        }
    }

    @Override
    public double getCurrentSetpoint(Network network) {
        List<Double> currentSetpoints = injectionDistributionKeys.entrySet().stream()
                .map(entry -> getInjectionSetpoint(network, entry.getKey().getId(), entry.getValue()))
                .collect(Collectors.toList());

        if (currentSetpoints.size() == 1) {
            return currentSetpoints.get(0);
        } else {
            Collections.sort(currentSetpoints);
            if (Math.abs(currentSetpoints.get(0) - currentSetpoints.get(currentSetpoints.size() - 1)) < 1) {
                return currentSetpoints.get(0);
            } else {
                throw new FaraoException(String.format("Cannot evaluate reference setpoint of InjectionRangeAction %s, as the injections are not distributed according to their key", this.getId()));
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
            throw new FaraoException(String.format("Injection %s not found in network", injectionId));
        } else {
            throw new FaraoException(String.format("%s refers to an object of the network which is not an handled Injection (not a Load, not a Generator)", injectionId));
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
