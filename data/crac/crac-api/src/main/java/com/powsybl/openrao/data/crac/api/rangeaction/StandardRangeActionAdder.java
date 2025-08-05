/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.api.rangeaction;

import com.powsybl.openrao.data.crac.api.RemedialActionAdder;
import com.powsybl.openrao.data.crac.api.range.StandardRangeAdder;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface StandardRangeActionAdder<T extends StandardRangeActionAdder<T>> extends RemedialActionAdder<T> {

    StandardRangeAdder<T> newRange();

    T withGroupId(String groupId);

    T withInitialSetpoint(double initialSetpoint);

    T withVariationCost(Double variationCost, VariationDirection variationDirection);

}
