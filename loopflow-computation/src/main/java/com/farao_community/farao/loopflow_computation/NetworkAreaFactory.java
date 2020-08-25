/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.powsybl.iidm.network.Network;

/**
 * A factory to create a {@link NetworkArea} instance.
 *
 *  @author Mathieu Bague {@literal <mathieu.bague at rte-france.com>}
 */
public interface NetworkAreaFactory {

    NetworkArea create(Network network);

}
