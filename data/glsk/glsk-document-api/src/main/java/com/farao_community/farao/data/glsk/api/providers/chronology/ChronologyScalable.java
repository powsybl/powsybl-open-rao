/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.api.providers.chronology;

import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.providers.converters.GlskPointScalableConverter;
import com.farao_community.farao.data.glsk.api.ChronologyScalableProvider;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Network;

import java.time.Instant;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ChronologyScalable extends AbstractChronologyLinearData<Scalable> implements ChronologyScalableProvider {

    public ChronologyScalable(GlskDocument glskDocument, Network network) {
        super(glskDocument, network, GlskPointScalableConverter::convert);
    }

    @Override
    public Map<String, Scalable> getScalablePerArea() {
        return getLinearData();
    }

    @Override
    public Scalable getScalable(String area) {
        return getLinearData(area);
    }

    @Override
    public ChronologyScalableProvider selectInstant(Instant instant) {
        setInstant(instant);
        return this;
    }
}
