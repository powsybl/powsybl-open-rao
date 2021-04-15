/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.range_action.*;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_api.usage_rule.OnState;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
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
    private List<TapRange> ranges;
    private String groupId = null;

    @Override
    protected String getTypeDescription() {
        return "PstRangeAction";
    }

    PstRangeActionAdderImpl(CracImpl owner) {
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
    public TapRangeAdder newPstRange() {
        return new TapRangeAdderImpl(this);
    }

    @Override
    public PstRangeAction add() {
        checkId();
        assertAttributeNotNull(networkElementId, "PstRangeAction", "network element", "withNetworkElement()");

        if (!Objects.isNull(getCrac().getRemedialAction(id))) {
            throw new FaraoException(String.format("A remedial action with id %s already exists", id));
        }

        List<TapRange> validRanges = new ArrayList<>();

        if (usageRules.stream().allMatch(this::isPreventiveUsageRule)) {
            ranges.forEach(range -> {
                if (range.getRangeType().equals(RangeType.RELATIVE_TO_PREVIOUS_INSTANT)) {
                    LOGGER.warn("RELATIVE_TO_PREVIOUS_INSTANT range has been filtered from PstRangeAction {}, as it is a preventive RA", id);
                } else {
                    validRanges.add(range);
                }
            });
        }

        if (validRanges.isEmpty()) {
            LOGGER.warn("PstRangeAction {} does not contain any valid range, by default the range of the network will be used", id);
        }

        if (usageRules.isEmpty()) {
            LOGGER.warn("PstRangeAction {} does not contain any usage rule, by default it will never be available", id);
        }

        //todo : check that initial tap is within range (requires initial tap to be in the adder, not in the synchronization)

        NetworkElement networkElement = this.getCrac().addNetworkElement(networkElementId, networkElementName);
        PstRangeActionImpl pstWithRange = new PstRangeActionImpl(this.id, this.name, this.operator, this.usageRules, validRanges, networkElement, groupId);
        this.getCrac().addPstRangeAction(pstWithRange);
        return pstWithRange;
    }

    void addRange(TapRange pstRange) {
        ranges.add(pstRange);
    }

    private boolean isPreventiveUsageRule(UsageRule usageRule) {
        return  (usageRule instanceof FreeToUse && ((FreeToUse) usageRule).getInstant().equals(Instant.PREVENTIVE))
            || (usageRule instanceof OnState && ((OnState) usageRule).getInstant().equals(Instant.PREVENTIVE));
    }

}
