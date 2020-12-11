/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.cnec.adder;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.IdentifiableAdder;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElementParent;
import com.farao_community.farao.data.crac_api.cnec.Cnec;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface CnecAdder<I extends Cnec<I>, J extends CnecAdder<I, J>> extends NetworkElementParent<J>, IdentifiableAdder<J> {

    /**
     * Set the instant of the cnec's state
     * @param instant: the instant to set
     * @return the {@code CnecAdder} instance
     */
    J setInstant(Instant instant);

    /**
     * Set the contingency of the cnec's state
     * @param contingency: the contingency to set
     * @return the {@code CnecAdder} instance
     */
    J setContingency(Contingency contingency);

    /***
     * Set the reliabilityMargin of the created cnec
     * @param reliabilityMargin the value of the reliabilityMargin in Megawatts
     * @return the {@code CnecAdder} instance
     */
    J setReliabilityMargin(double reliabilityMargin);

    /***
     * Set at true if the branch is optimized
     * @return the {@code CnecAdder} instance
     */
    J optimized();

    /***
     * Set at true if the branch is monitored
     * @return the {@code CnecAdder} instance
     */
    J monitored();

    /**
     * Add the new Cnec to the Crac
     * @return the created {@code Cnec} instance
     */
    I add();
}
