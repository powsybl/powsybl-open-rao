/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.PstRange;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstRangeActionImpl;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUseImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.farao_community.farao.data.crac_api.usage_rule.UsageMethod.AVAILABLE;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PstRangeActionAdderImpl extends AbstractIdentifiableAdder<PstRangeActionAdder> implements PstRangeActionAdder {
    private SimpleCrac parent;
    private Unit unit;
    private Double minValue;
    private Double maxValue;
    private NetworkElement networkElement;
    private String operator;
    private String groupId = null;
    private List<UsageRule> usageRules = new ArrayList<>();

    public PstRangeActionAdderImpl(SimpleCrac parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
        this.usageRules.add(new FreeToUseImpl(AVAILABLE, parent.getPreventiveState().getInstant()));
    }

    @Override
    public PstRangeActionAdder setOperator(String operator) {
        this.operator = operator;
        return this;
    }

    @Override
    public PstRangeActionAdder setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    @Override
    public PstRangeActionAdder setUnit(Unit unit) {
        this.unit = unit;
        return this;
    }

    @Override
    public PstRangeActionAdder setMinValue(Double minValue) {
        this.minValue = minValue;
        return this;
    }

    @Override
    public PstRangeActionAdder setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    @Override
    public PstRangeActionAdder addNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
        return this;
    }

    @Override
    public NetworkElementAdder<PstRangeActionAdder> newNetworkElement() {
        if (networkElement == null) {
            return new NetworkElementAdderImpl<>(this);
        } else {
            throw new FaraoException("You can only add one network element to a PstRangeAction.");
        }
    }

    @Override
    public Crac add() {
        checkId();
        if (this.unit == null) {
            throw new FaraoException("Cannot add a PstRangeAction without a unit. Please use setUnit.");
        }
        if (this.unit != Unit.TAP) {
            throw new FaraoException("Only TAP unit is currently supported.");
        }
        if (this.minValue == null) {
            throw new FaraoException("Cannot add a PstRangeAction without a minimum value. Please use setMinValue.");
        }
        if (this.maxValue == null) {
            throw new FaraoException("Cannot add a PstRangeAction without a maximum value. Please use setMaxValue.");
        }
        if (this.networkElement == null) {
            throw new FaraoException("Cannot add a PstRangeAction without a network element. Please use newNetworkElement.");
        }
        List<PstRange> ranges = Collections.singletonList(new PstRange(this.minValue, this.maxValue, RangeType.ABSOLUTE, RangeDefinition.CENTERED_ON_ZERO));
        /*
         * First we add the network element to the crac
         * If it already exists, it will send us back the reference to the
         * existing element, thus avoiding making a copy
         * This is done here because it is too complicated to do in
         * SimpleCrac.addRangeAction, which handles abstract RangeActions
         */
        NetworkElement newNetworkElement = parent.addNetworkElement(networkElement.getId(), networkElement.getName());

        PstRangeActionImpl pstWithRange = new PstRangeActionImpl(this.id, this.name, this.operator, this.usageRules, ranges, newNetworkElement, groupId);
        this.parent.addRangeAction(pstWithRange);

        return parent;
    }
}
