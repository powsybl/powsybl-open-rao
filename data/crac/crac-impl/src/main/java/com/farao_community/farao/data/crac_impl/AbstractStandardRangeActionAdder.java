/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range.StandardRangeAdder;
import com.farao_community.farao.data.crac_api.range_action.StandardRangeActionAdder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public abstract class AbstractStandardRangeActionAdder<T extends StandardRangeActionAdder<T>> extends AbstractRemedialActionAdder<T> implements StandardRangeActionAdder<T> {

    protected String groupId;
    protected double initialSetpoint;
    protected List<StandardRange> ranges;

    AbstractStandardRangeActionAdder(CracImpl crac) {
        super(crac);
        this.ranges = new ArrayList<>();
    }

    @Override
    public T withGroupId(String groupId) {
        this.groupId = groupId;
        return (T) this;
    }

    @Override
    public T withInitialSetpoint(double initialSetpoint) {
        this.initialSetpoint = initialSetpoint;
        return (T) this;
    }

    @Override
    public StandardRangeAdder<T> newRange() {
        return new StandardRangeAdderImpl<>(this);
    }

    void addRange(StandardRange standardRange) {
        ranges.add(standardRange);
    }

    protected void checkAutoUsageRules() {
        usageRules.forEach(usageRule -> {
            if (usageRule.getInstant().equals(Instant.AUTO) && Objects.isNull(speed)) {
                throw new FaraoException("Cannot create an AUTO standard range action without speed defined");
            }
        });
    }
}
