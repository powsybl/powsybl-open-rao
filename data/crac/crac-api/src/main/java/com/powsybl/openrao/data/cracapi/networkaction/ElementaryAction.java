/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi.networkaction;
import com.powsybl.iidm.network.Network;

/**
 * Generic interface for the definition of elementary actions
 *
 * An elementary action is an action on the network which can be
 * activated by a {@link NetworkAction}
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface ElementaryAction {

    /**
     * Apply the elementary action on a given network.
     */
    void apply(Network network);
}
