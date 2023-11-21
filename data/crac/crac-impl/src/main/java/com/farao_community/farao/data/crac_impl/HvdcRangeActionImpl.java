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
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.TECHNICAL_LOGS;

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

    HvdcRangeActionImpl(String id, String name, String operator, Set<UsageRule> usageRules, List<StandardRange> ranges,
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
        findAndDisableHvdcAngleDroopActivePowerControl(network);
        if (targetSetpoint < 0) {
            getHvdcLine(network).setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        } else {
            getHvdcLine(network).setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
        }
        getHvdcLine(network).setActivePowerSetpoint(Math.abs(targetSetpoint));
    }

    public void findAndDisableHvdcAngleDroopActivePowerControl(Network network) {
        if (isAngleDroopActivePowerControlEnabled(network)) {
            HvdcLine hvdcLine = getHvdcLine(network);
            TECHNICAL_LOGS.debug("Disabling HvdcAngleDroopActivePowerControl on HVDC line {}", hvdcLine.getId());
            hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class).setEnabled(false);
        }
    }

    public boolean isAngleDroopActivePowerControlEnabled(Network network) {
        HvdcAngleDroopActivePowerControl hvdcAngleDroopActivePowerControl = getHvdcLine(network).getExtension(HvdcAngleDroopActivePowerControl.class);
        return (hvdcAngleDroopActivePowerControl != null) && hvdcAngleDroopActivePowerControl.isEnabled();
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
