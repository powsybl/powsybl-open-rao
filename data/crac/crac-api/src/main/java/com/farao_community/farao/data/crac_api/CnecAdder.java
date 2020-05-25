/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface CnecAdder extends NetworkElementParent<CnecAdder> {

    /**
     * Set the ID of the cnec
     * @param id: ID to set
     * @return the {@code CnecAdder} instance
     */
    CnecAdder setId(String id);

    /**
     * Set the name of the cnec
     * @param name: name to set
     * @return the {@code CnecAdder} instance
     */
    CnecAdder setName(String name);

    /**
     * Set the instant of the cnec's state
     * @param instant: the instant to set
     * @return the {@code CnecAdder} instance
     */
    CnecAdder setInstant(Instant instant);

    /**
     * Set the contingency of the cnec's state
     * @param contingency: the contingency to set
     * @return the {@code CnecAdder} instance
     */
    CnecAdder setContingency(Contingency contingency);

    /**
     * Add a threshold to the created cnec
     * @return a {@code ThresholdAdder} instance
     */
    ThresholdAdder newThreshold();

    /**
     * Add the new Cnec to the Crac
     * @return the created {@code Cnec} instance
     */
    Cnec add();
}
