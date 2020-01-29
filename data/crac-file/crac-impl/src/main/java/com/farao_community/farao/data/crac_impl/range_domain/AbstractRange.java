/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.range_domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AbsoluteFixedRange.class, name = "absoluteFixedRange"),
        @JsonSubTypes.Type(value = RelativeDynamicRange.class, name = "relativeDynamicRange"),
        @JsonSubTypes.Type(value = RelativeFixedRange.class, name = "relativeFixedRange")
    })
public abstract class AbstractRange {

    public RangeType getRangeType() {
        return rangeType;
    }

    protected RangeType rangeType;
    protected double min;
    protected double max;

    @JsonCreator
    public AbstractRange(@JsonProperty("min") double min, @JsonProperty("max") double max) {
        this.min = min;
        this.max = max;
    }

    public abstract double getMinValue(Network network);

    public abstract double getMaxValue(Network network);

}
