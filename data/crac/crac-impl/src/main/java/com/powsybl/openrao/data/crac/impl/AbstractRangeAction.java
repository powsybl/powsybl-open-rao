/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractRangeAction<T extends RangeAction<T>> extends AbstractRemedialAction<T> implements RangeAction<T> {

    protected String groupId = null;
    protected Map<VariationDirection, Double> variationCosts;

    AbstractRangeAction(String id, String name, String operator, Set<UsageRule> usageRules, String groupId, Integer speed, Double activationCost, Map<VariationDirection, Double> variationCosts) {
        super(id, name, operator, usageRules, speed, activationCost);
        this.groupId = groupId;
        this.variationCosts = variationCosts == null ? new HashMap<>() : new HashMap<>(variationCosts);
    }

    @Override
    public Optional<String> getGroupId() {
        return Optional.ofNullable(groupId);
    }

    @Override
    public Optional<Double> getVariationCost(VariationDirection variationDirection) {
        return Optional.ofNullable(variationCosts.get(variationDirection));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractRangeAction<?> otherRa = (AbstractRangeAction<?>) o;

        return super.equals(o)
                && (groupId == null && otherRa.getGroupId().isEmpty() || groupId != null && groupId.equals(otherRa.getGroupId().orElse(null)));
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        String nonNullGroupId = groupId == null ? "" : groupId;
        result = 31 * result + nonNullGroupId.hashCode();
        return result;
    }
}
