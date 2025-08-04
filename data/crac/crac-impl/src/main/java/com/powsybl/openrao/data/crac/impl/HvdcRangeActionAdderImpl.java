/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeActionAdder;

import java.util.ArrayList;
import java.util.Objects;

import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotEmpty;
import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class HvdcRangeActionAdderImpl extends AbstractStandardRangeActionAdder<HvdcRangeActionAdder> implements HvdcRangeActionAdder {

    public static final String HVDC_RANGE_ACTION = "HvdcRangeAction";
    private String networkElementId;
    private String networkElementName;

    @Override
    protected String getTypeDescription() {
        return HVDC_RANGE_ACTION;
    }

    HvdcRangeActionAdderImpl(CracImpl owner) {
        super(owner);
        this.ranges = new ArrayList<>();
    }

    @Override
    public HvdcRangeActionAdder withNetworkElement(String networkElementId) {
        return withNetworkElement(networkElementId, networkElementId);
    }

    @Override
    public HvdcRangeActionAdder withNetworkElement(String networkElementId, String networkElementName) {
        this.networkElementId = networkElementId;
        this.networkElementName = networkElementName;
        return this;
    }

    @Override
    public HvdcRangeAction add() {
        runCheckBeforeAdding();

        NetworkElement networkElement = this.getCrac().addNetworkElement(networkElementId, networkElementName);
        HvdcRangeActionImpl hvdcWithRange = new HvdcRangeActionImpl(this.id, this.name, this.operator, this.usageRules, ranges, null, networkElement, groupId, speed, activationCost, variationCosts);
        this.getCrac().addHvdcRangeAction(hvdcWithRange);
        return hvdcWithRange;
    }

    @Override
    public HvdcRangeAction addWithInitialSetpointFromNetwork(Network network) {
        runCheckBeforeAdding();

        NetworkElement networkElement = this.getCrac().addNetworkElement(networkElementId, networkElementName);
        this.initialSetpoint = getCurrentSetpoint(network, networkElement);
        HvdcRangeActionImpl hvdcWithRange = new HvdcRangeActionImpl(this.id, this.name, this.operator, this.usageRules, ranges, initialSetpoint, networkElement, groupId, speed, activationCost, variationCosts);
        this.getCrac().addHvdcRangeAction(hvdcWithRange);
        return hvdcWithRange;
    }

    private void runCheckBeforeAdding() {
        checkId();
        checkAutoUsageRules();
        assertAttributeNotNull(networkElementId, HVDC_RANGE_ACTION, "network element", "withNetworkElement()");
        assertAttributeNotEmpty(ranges, HVDC_RANGE_ACTION, "range", "newRange()");

        if (!Objects.isNull(getCrac().getRemedialAction(id))) {
            throw new OpenRaoException(String.format("A remedial action with id %s already exists", id));
        }

        if (usageRules.isEmpty()) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("HvdcRangeAction {} does not contain any usage rule, by default it will never be available", id);
        }
    }

    private HvdcLine getHvdcLine(Network network, NetworkElement networkElement) {
        HvdcLine hvdcLine = network.getHvdcLine(networkElement.getId());
        if (hvdcLine == null) {
            throw new OpenRaoException(String.format("HvdcLine %s does not exist in the current network.", networkElement.getId()));
        }
        return hvdcLine;
    }

    public double getCurrentSetpoint(Network network, NetworkElement networkElement) {
        if (getHvdcLine(network, networkElement).getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER) {
            return getHvdcLine(network, networkElement).getActivePowerSetpoint();
        } else {
            return -getHvdcLine(network, networkElement).getActivePowerSetpoint();
        }
    }
}
