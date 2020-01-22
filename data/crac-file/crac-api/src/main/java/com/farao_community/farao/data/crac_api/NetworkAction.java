/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.Set;

/**
 * Remedial action interface specifying a direct action on the network. <br/>
 * The Network Action is completely defined by itself.<br/>
 * It involves a Set of {@link NetworkElement}: an action will be triggered on each of these elements,
 * provided they belong to the {@link Network} given in parameter of the apply method.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public interface NetworkAction extends RemedialAction {

    /**
     * Gather all the network elements present in the network action. It returns a set because network
     * elements must not be duplicated inside a network action and there is no defined order for network elements.
     *
     * @return A set of network elements.
     */
    @JsonIgnore
    Set<NetworkElement> getNetworkElements();

    void apply(Network network);
}
