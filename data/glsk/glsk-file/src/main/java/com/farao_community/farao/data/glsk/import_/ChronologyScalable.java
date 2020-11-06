/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.data.glsk.import_.glsk_document_api.GlskDocument;
import com.farao_community.farao.data.glsk.import_.converters.GlskPointScalableConverter;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Network;

import java.time.Instant;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ChronologyScalable implements ScalableProvider {

    private final ChronologyLinearData<Scalable> chronologyLinearData;

    public ChronologyScalable(GlskDocument glskDocument, Network network) {
        chronologyLinearData = new ChronologyLinearData<>(glskDocument, network, GlskPointScalableConverter::convert);
    }

    @Override
    public Map<String, Scalable> getScalablePerCountry(Instant instant) {
        return chronologyLinearData.getLinearData(instant);
    }

    @Override
    public Scalable getScalable(Instant instant, String area) {
        return chronologyLinearData.getLinearData(instant, area);
    }
}
