/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeActionAdder;

import java.util.ArrayList;
import java.util.Objects;

import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotEmpty;
import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;

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

        NetworkElement networkElement = this.getCrac().addNetworkElement(networkElementId, networkElementName);
        HvdcRangeActionImpl hvdcWithRange = new HvdcRangeActionImpl(this.id, this.name, this.operator, this.usageRules, ranges, initialSetpoint, networkElement, groupId, speed, activationCost);
        this.getCrac().addHvdcRangeAction(hvdcWithRange);
        return hvdcWithRange;
    }
}
