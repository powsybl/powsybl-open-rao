/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.range.StandardRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.StandardRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public abstract class AbstractStandardRangeActionAdder<T extends StandardRangeActionAdder<T>> extends AbstractRemedialActionAdder<T> implements StandardRangeActionAdder<T> {

    protected String groupId;
    protected Double initialSetpoint;
    protected List<StandardRange> ranges;
    protected Map<VariationDirection, Double> variationCosts;

    AbstractStandardRangeActionAdder(CracImpl crac) {
        super(crac);
        this.ranges = new ArrayList<>();
        this.variationCosts = new HashMap<>();
    }

    @Override
    public T withGroupId(String groupId) {
        this.groupId = groupId;
        return (T) this;
    }

    @Override
    public T withInitialSetpoint(Double initialSetpoint) {
        this.initialSetpoint = initialSetpoint;
        return (T) this;
    }

    @Override
    public T withVariationCost(Double variationCost, VariationDirection variationDirection) {
        this.variationCosts.put(variationDirection, variationCost);
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
            if (usageRule.getInstant().isAuto() && Objects.isNull(speed)) {
                throw new OpenRaoException("Cannot create an AUTO standard range action without speed defined");
            }
        });
    }
}
