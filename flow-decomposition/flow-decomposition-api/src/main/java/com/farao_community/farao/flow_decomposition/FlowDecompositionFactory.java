/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

/**
 * Flow decomposition factory interface
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public interface FlowDecompositionFactory {
    FlowDecomposition create(Network network, ComputationManager computationManager, int priority);
}
