/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda.scenariorepository;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;

import java.time.OffsetDateTime;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface NetworkVariation {
    String getId();
    String getNetworkElementId(); // TODO I don't like this very much, but it's needed to compute shifts
    TemporalData<Double> computeShifts(TemporalData<Network> networks);
    TemporalData<Double> apply(TemporalData<Network> networks);
    void apply(Network network, OffsetDateTime timestamp);
}
