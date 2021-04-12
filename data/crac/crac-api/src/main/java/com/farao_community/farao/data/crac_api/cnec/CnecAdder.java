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

    /**
     * Set the instant of the cnec's state
     * @param instant: the instant to set
     * @return the {@code CnecAdder} instance
     */
    J withInstant(Instant instant);

    /**
     * Set the contingency of the cnec's state
     * @param contingencyId: the contingency id to set
     * @return the {@code CnecAdder} instance
     */
    J withContingency(String contingencyId);

    /***
     * Set the reliabilityMargin of the created cnec
     * @param reliabilityMargin the value of the reliabilityMargin in Megawatts
     * @return the {@code CnecAdder} instance
     */
    J withReliabilityMargin(double reliabilityMargin);

    /***
     * Set the operator of the created cnec
     * @param operator the name of the operator
     * @return the {@code CnecAdder} instance
     */
    J withOperator(String operator);

    J withOptimized();

    J withMonitored();

    /***
     * Set at true if the branch is optimized
     * @return the {@code CnecAdder} instance
     */
    J withOptimized(boolean optimized);

    /***
     * Set at true if the branch is monitored
     * @return the {@code CnecAdder} instance
     */
    J withMonitored(boolean monitored);

    J withNetworkElement(String networkElementId, String networkElementName);
}
