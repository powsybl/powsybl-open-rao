/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.RangeDefinition;
import com.farao_community.farao.data.crac_api.RangeType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class PstRangeImpl extends RangeImpl implements PstRange {

    private RangeDefinition rangeDefinition;

    @JsonCreator
    @Deprecated
    // TODO : convert to private package
    public PstRangeImpl(@JsonProperty("min") double min,
                    @JsonProperty("max") double max,
                    @JsonProperty("rangeType") RangeType rangeType,
                    @JsonProperty("rangeDefinition") RangeDefinition rangeDefinition) {
        super(min, max, rangeType, Unit.TAP);
        this.rangeDefinition = rangeDefinition;
    }

    public RangeDefinition getRangeDefinition() {
        return rangeDefinition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PstRangeImpl otherRange = (PstRangeImpl) o;
        return rangeDefinition.equals(otherRange.rangeDefinition)
            && super.equals(otherRange);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 23 * result + rangeDefinition.hashCode();
        result = 31 * result + super.hashCode();
        return result;
    }

}
