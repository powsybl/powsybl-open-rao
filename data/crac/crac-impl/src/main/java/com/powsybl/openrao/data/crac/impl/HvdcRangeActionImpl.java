/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.HvdcActionBuilder;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmHvdcHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Elementary HVDC range remedial action.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class HvdcRangeActionImpl extends AbstractRangeAction<HvdcRangeAction> implements HvdcRangeAction {

    private final String networkElementId;
    private final List<StandardRange> ranges;
    private Double initialSetpoint;

    HvdcRangeActionImpl(String id, String name, String operator, Set<UsageRule> usageRules, List<StandardRange> ranges,
                        Double initialSetpoint, String networkElementId, String groupId, Integer speed, Double activationCost, Map<VariationDirection, Double> variationCosts) {
        super(id, name, operator, usageRules, groupId, speed, activationCost, variationCosts);
        this.networkElementId = networkElementId;
        this.ranges = ranges;
        this.initialSetpoint = initialSetpoint;
    }

    @Override
    public String getNetworkElementId() {
        return networkElementId;
    }

    @Override
    public List<StandardRange> getRanges() {
        return ranges;
    }

    @Override
    public Set<String> getNetworkElementsIds() {
        return Collections.singleton(networkElementId);
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
        // Possible only if the network element associated is NOT in ac emulation mode (ie. fixed active power setpoint operation only)
        HvdcActionBuilder actionBuilder = null;
        if (!isAngleDroopActivePowerControlEnabled(network)) {
            actionBuilder = new HvdcActionBuilder()
                .withId("")
                .withHvdcId(networkElementId)
                .withActivePowerSetpoint(Math.abs(targetSetpoint));
            if (targetSetpoint < 0) {
                actionBuilder.withConverterMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
            } else {
                actionBuilder.withConverterMode(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
            }
            actionBuilder.build().toModification().apply(network, true, ReportNode.NO_OP);
        } else {
            throw new OpenRaoException(String.format(
                "Unable to set an active power setpoint for HVDC line %s because it is operating in AC Emulation mode.",
                networkElementId
                )
            );
        }
    }

    public boolean isAngleDroopActivePowerControlEnabled(Network network) {
        HvdcAngleDroopActivePowerControl hvdcAngleDroopActivePowerControl = IidmHvdcHelper.getHvdcLine(network, networkElementId).getExtension(HvdcAngleDroopActivePowerControl.class);
        return hvdcAngleDroopActivePowerControl != null && hvdcAngleDroopActivePowerControl.isEnabled();
    }

    @Override
    public double getCurrentSetpoint(Network network) {
        return IidmHvdcHelper.getCurrentSetpoint(network, networkElementId);
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

        return this.getNetworkElementId().equals(((HvdcRangeAction) o).getNetworkElementId())
                && this.ranges.equals(((HvdcRangeAction) o).getRanges());

    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        for (StandardRange range : ranges) {
            hashCode += 31 * range.hashCode();
        }
        hashCode += 31 * networkElementId.hashCode();
        return hashCode;
    }

    @Override
    public void setInitialSetpoint(double initialSetpoint) {
        this.initialSetpoint = initialSetpoint;
    }
}
