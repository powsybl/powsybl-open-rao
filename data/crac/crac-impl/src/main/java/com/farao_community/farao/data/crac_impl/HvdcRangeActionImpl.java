/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Elementary HVDC range remedial action.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class HvdcRangeActionImpl extends AbstractRangeAction<HvdcRangeAction> implements HvdcRangeAction {

    private final NetworkElement networkElement;
    private final List<StandardRange> ranges;
    private final double initialSetpoint;

    HvdcRangeActionImpl(String id, String name, String operator, List<UsageRule> usageRules, List<StandardRange> ranges,
                        double initialSetpoint, NetworkElement networkElement, String groupId, Integer speed) {
        super(id, name, operator, usageRules, groupId, speed);
        this.networkElement = networkElement;
        this.ranges = ranges;
        this.initialSetpoint = initialSetpoint;
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    @Override
    public List<StandardRange> getRanges() {
        return ranges;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    @Override
    public double getMinAdmissibleSetpoint(double previousInstantSetPoint) {
        double minAdmissibleSetpoint = Double.NEGATIVE_INFINITY;
        for (StandardRange range : ranges) {
            switch (range.getRangeType()) {
                case ABSOLUTE:
                    minAdmissibleSetpoint = Math.max(minAdmissibleSetpoint, range.getMin());
                    break;
                case RELATIVE_TO_INITIAL_NETWORK:
                    minAdmissibleSetpoint = Math.max(minAdmissibleSetpoint, initialSetpoint + range.getMin());
                    break;
                case RELATIVE_TO_PREVIOUS_INSTANT:
                    minAdmissibleSetpoint = Math.max(minAdmissibleSetpoint, previousInstantSetPoint + range.getMin());
                    break;
                default:
                    throw new NotImplementedException("Range Action type is not implemented yet.");
            }
        }
        return minAdmissibleSetpoint;
    }

    @Override
    public double getMaxAdmissibleSetpoint(double previousInstantSetPoint) {
        double maxAdmissibleSetpoint = Double.POSITIVE_INFINITY;
        for (StandardRange range : ranges) {
            switch (range.getRangeType()) {
                case ABSOLUTE:
                    maxAdmissibleSetpoint = Math.min(maxAdmissibleSetpoint, range.getMax());
                    break;
                case RELATIVE_TO_INITIAL_NETWORK:
                    maxAdmissibleSetpoint = Math.min(maxAdmissibleSetpoint, initialSetpoint + range.getMax());
                    break;
                case RELATIVE_TO_PREVIOUS_INSTANT:
                    maxAdmissibleSetpoint = Math.min(maxAdmissibleSetpoint, previousInstantSetPoint + range.getMax());
                    break;
                default:
                    throw new NotImplementedException("Range Action type is not implemented yet.");
            }
        }
        return maxAdmissibleSetpoint;
    }

    @Override
    public double getInitialSetpoint() {
        return initialSetpoint;
    }

    @Override
    public void apply(Network network, double targetSetpoint) {
        if (targetSetpoint > 0) {
            getHvdcLine(network).setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
        } else {
            getHvdcLine(network).setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        }
        getHvdcLine(network).setActivePowerSetpoint(Math.abs(targetSetpoint));
    }

    private HvdcLine getHvdcLine(Network network) {
        HvdcLine hvdcLine = network.getHvdcLine(networkElement.getId());
        if (hvdcLine == null) {
            throw new FaraoException(String.format("HvdcLine %s does not exist in the current network.", networkElement.getId()));
        }
        return hvdcLine;
    }

    @Override
    public double getCurrentSetpoint(Network network) {
        if (getHvdcLine(network).getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER) {
            return getHvdcLine(network).getActivePowerSetpoint();
        } else {
            return -getHvdcLine(network).getActivePowerSetpoint();
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

        return this.networkElement.equals(((HvdcRangeAction) o).getNetworkElement())
                && this.ranges.equals(((HvdcRangeAction) o).getRanges());

    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        for (StandardRange range : ranges) {
            hashCode += 31 * range.hashCode();
        }
        hashCode += 31 * networkElement.hashCode();
        return hashCode;
    }
}
