/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.network.Network;

/**
 * Initial PST treatment functional interface
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@FunctionalInterface
public interface PstPreTreatmentService {
    void treatment(Network network, FullLineDecompositionParameters parameters);
}
