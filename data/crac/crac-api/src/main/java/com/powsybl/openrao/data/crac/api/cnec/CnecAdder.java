/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.cnec;

import com.powsybl.openrao.data.crac.api.IdentifiableAdder;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface CnecAdder<J extends CnecAdder<J>> extends IdentifiableAdder<J> {

    J withNetworkElement(String networkElementId, String networkElementName);

    J withNetworkElement(String networkElementId);

    J withInstant(String instantId);

    J withContingency(String contingencyId);

    J withReliabilityMargin(double reliabilityMargin);

    J withOperator(String operator);

    J withBorder(String border);

    J withOptimized();

    J withMonitored();

    J withOptimized(boolean optimized);

    J withMonitored(boolean monitored);
}
