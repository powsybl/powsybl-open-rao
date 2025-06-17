/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.HvdcActionBuilder;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

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
                        double initialSetpoint, NetworkElement networkElement, String groupId, Integer speed, Double activationCost, Map<VariationDirection, Double> variationCosts) {
        super(id, name, operator, usageRules, groupId, speed, activationCost, variationCosts);
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
        logDisableHvdcAngleDroopActivePowerControl(network);
        HvdcActionBuilder actionBuilder = new HvdcActionBuilder()
            .withId("")
            .withHvdcId(networkElement.getId())
            .withActivePowerSetpoint(Math.abs(targetSetpoint))
            .withAcEmulationEnabled(false);
        if (targetSetpoint < 0) {
            actionBuilder.withConverterMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        } else {
            actionBuilder.withConverterMode(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
        }
        actionBuilder.build().toModification().apply(network, true, ReportNode.NO_OP);
    }

    public void logDisableHvdcAngleDroopActivePowerControl(Network network) {
        if (isAngleDroopActivePowerControlEnabled(network)) {
            HvdcLine hvdcLine = getHvdcLine(network);
            TECHNICAL_LOGS.debug("Disabling HvdcAngleDroopActivePowerControl on HVDC line {}", hvdcLine.getId());
        }
    }

    public boolean isAngleDroopActivePowerControlEnabled(Network network) {
        HvdcAngleDroopActivePowerControl hvdcAngleDroopActivePowerControl = getHvdcLine(network).getExtension(HvdcAngleDroopActivePowerControl.class);
        return hvdcAngleDroopActivePowerControl != null && hvdcAngleDroopActivePowerControl.isEnabled();
    }

    private HvdcLine getHvdcLine(Network network) {
        HvdcLine hvdcLine = network.getHvdcLine(networkElement.getId());
        if (hvdcLine == null) {
            throw new OpenRaoException(String.format("HvdcLine %s does not exist in the current network.", networkElement.getId()));
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
