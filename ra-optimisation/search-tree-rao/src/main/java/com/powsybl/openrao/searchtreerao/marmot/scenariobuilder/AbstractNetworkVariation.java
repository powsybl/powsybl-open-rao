/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.scenariobuilder;

import com.powsybl.openrao.commons.TemporalData;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public abstract class AbstractNetworkVariation implements NetworkVariation {
    protected String id;
    protected String networkElementId;
    protected TemporalData<Double> values;

    protected AbstractNetworkVariation(String id, String networkElementId, TemporalData<Double> values) {
        this.id = id;
        this.networkElementId = networkElementId;
        this.values = values;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getNetworkElementId() {
        return networkElementId;
    }
}
