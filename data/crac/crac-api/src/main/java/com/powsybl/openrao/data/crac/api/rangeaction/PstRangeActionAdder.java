/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.rangeaction;

import com.powsybl.openrao.data.crac.api.RemedialActionAdder;
import com.powsybl.openrao.data.crac.api.range.TapRangeAdder;

import java.util.Map;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface PstRangeActionAdder extends RemedialActionAdder<PstRangeActionAdder> {

    PstRangeActionAdder withNetworkElement(String networkElementId);

    PstRangeActionAdder withNetworkElement(String networkElementId, String networkElementName);

    PstRangeActionAdder withGroupId(String groupId);

    PstRangeActionAdder withInitialTap(int initialTap);

    PstRangeActionAdder withTapToAngleConversionMap(Map<Integer, Double> tapToAngleConversionMap);

    PstRangeActionAdder withVariationCost(Double variationCost, VariationDirection variationDirection);

    TapRangeAdder newTapRange();

    PstRangeAction add();
}
