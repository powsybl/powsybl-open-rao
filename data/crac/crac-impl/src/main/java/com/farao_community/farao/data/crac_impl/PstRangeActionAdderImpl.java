/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PstRangeActionAdderImpl extends AbstractRemedialActionAdder<PstRangeActionAdder> implements PstRangeActionAdder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PstRangeActionAdderImpl.class);

    private String networkElementId;
    private String networkElementName;
    private List<PstRange> ranges;
    private String groupId = null;

    @Override
    protected String getTypeDescription() {
        return "PstRangeAction";
    }

    PstRangeActionAdderImpl(SimpleCrac owner) {
        super(owner);
        this.ranges = new ArrayList<>();
    }

    @Override
    public PstRangeActionAdder withNetworkElement(String networkElementId) {
        this.networkElementId = networkElementId;
        return this;
    }

    @Override
    public PstRangeActionAdder withNetworkElement(String networkElementId, String networkElementName) {
        this.networkElementName = networkElementName;
        return this;
    }

    @Override
    public PstRangeActionAdder withGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    @Override
    public PstRangeAdder newPstRange() {
        return new PstRangeAdderImpl(this);
    }

    @Override
    public PstRangeAction add() {
        checkId();
        assertAttributeNotNull(networkElementId, "PstRangeAction", "network element", "withNetworkElement()");

        if (ranges.isEmpty()) {
            LOGGER.warn("PstRangeAction {} does not contain any range, by default the range of the network will be used", id);
        }

        if (usageRules.isEmpty()) {
            LOGGER.warn("PstRangeAction {} does not contain any usage rule, by default it will never be available", id);
        }
        //todo : pour un PST préventif pur, on ne peut pas définir une range relatif to previous instant (warning + filtre)
        //todo : check that initial tap is within range

        NetworkElement networkElement;
        if (Objects.isNull(networkElementName)) {
            networkElement = this.getCrac().addNetworkElement(networkElementId);
        } else {
            networkElement = this.getCrac().addNetworkElement(networkElementId, networkElementName);
        }

        PstRangeActionImpl pstWithRange = new PstRangeActionImpl(this.id, this.name, this.operator, this.usageRules, ranges, networkElement, groupId);
        this.getCrac().addRangeAction(pstWithRange);
        return pstWithRange;
    }

    void addPstRange(PstRange pstRange) {
        ranges.add(pstRange);
    }
}
