/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

/**
 * Remedial action interface specifying a direct action on the network. <br/>
 * The Network Action is completely defined by itself.<br/>
 * It involves a Set of {@link NetworkElement}s.<br/>
 * When the apply method is called, an action is triggered on these NetworkElement.<br/>
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface NetworkAction<I extends NetworkAction<I>> extends RemedialAction<I> {

    /**
     * Trigger the actions on the NetworkElements, in a given network.
     * @param network The network in which the actions are triggered
     */
    void apply(Network network);
}
