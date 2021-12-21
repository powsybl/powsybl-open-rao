/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.range_action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotEmpty;
import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class HvdcRangeActionAdderImpl extends AbstractStandardRangeActionAdder<HvdcRangeActionAdder> implements HvdcRangeActionAdder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HvdcRangeActionAdderImpl.class);

    private String networkElementId;
    private String networkElementName;

    @Override
    protected String getTypeDescription() {
        return "HvdcRangeAction";
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
        assertAttributeNotNull(networkElementId, "HvdcRangeAction", "network element", "withNetworkElement()");
        assertAttributeNotEmpty(ranges, "HvdcRangeAction", "range", "newRange()");

        if (!Objects.isNull(getCrac().getRemedialAction(id))) {
            throw new FaraoException(String.format("A remedial action with id %s already exists", id));
        }

        if (usageRules.isEmpty()) {
            LOGGER.warn("HvdcRangeAction {} does not contain any usage rule, by default it will never be available", id);
        }

        NetworkElement networkElement = this.getCrac().addNetworkElement(networkElementId, networkElementName);
        HvdcRangeActionImpl hvdcWithRange = new HvdcRangeActionImpl(this.id, this.name, this.operator, this.usageRules, ranges, networkElement, groupId);
        this.getCrac().addHvdcRangeAction(hvdcWithRange);
        return hvdcWithRange;
    }
}
