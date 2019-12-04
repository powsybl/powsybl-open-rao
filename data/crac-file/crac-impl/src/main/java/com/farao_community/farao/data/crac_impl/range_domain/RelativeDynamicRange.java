/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.range_domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.iidm.network.Network;

/**
 * Definition of a range relative to the previous network.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class RelativeDynamicRange extends AbstractRange {

    @JsonCreator
    public RelativeDynamicRange(@JsonProperty("min") double min, @JsonProperty("max") double max) {
        super(min, max);
    }

    @Override
    public double getMinValue(Network network) {
        return 0;
    }

    @Override
    public double getMaxValue(Network network) {
        return 0;
    }
}
