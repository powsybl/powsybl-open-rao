/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.cnec;

import com.farao_community.farao.data.crac_api.IdentifiableAdder;
import com.farao_community.farao.data.crac_api.Instant;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface CnecAdder<J extends CnecAdder<J>> extends IdentifiableAdder<J> {

    J withNetworkElement(String networkElementId, String networkElementName);

    J withNetworkElement(String networkElementId);

    J withInstant(Instant instant);

    J withContingency(String contingencyId);

    J withReliabilityMargin(double reliabilityMargin);

    J withOperator(String operator);

    J withOptimized();

    J withMonitored();

    J withOptimized(boolean optimized);

    J withMonitored(boolean monitored);
}
