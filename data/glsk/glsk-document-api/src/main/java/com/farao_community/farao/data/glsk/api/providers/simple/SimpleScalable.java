/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.api.providers.simple;

import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.providers.converters.GlskPointScalableConverter;
import com.farao_community.farao.data.glsk.api.ScalableProvider;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Network;

import java.time.Instant;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SimpleScalable extends AbstractSimpleLinearData<Scalable> implements ScalableProvider {

    public SimpleScalable(GlskDocument glskDocument, Network network) {
        super(glskDocument, network, GlskPointScalableConverter::convert);
    }

    public SimpleScalable(GlskDocument glskDocument, Network network, Instant instant) {
        super(glskDocument, network, GlskPointScalableConverter::convert, instant);
    }

    @Override
    public Map<String, Scalable> getScalablePerArea() {
        return getLinearData();
    }

    @Override
    public Scalable getScalable(String area) {
        return getLinearData(area);
    }
}
