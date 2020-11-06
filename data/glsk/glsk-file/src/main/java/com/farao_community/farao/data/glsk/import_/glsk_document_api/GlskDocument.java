/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_.glsk_document_api;

import com.farao_community.farao.data.glsk.import_.*;
import com.powsybl.iidm.network.Network;

import java.time.Instant;
import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface GlskDocument {

    List<String> getCountries();

    List<AbstractGlskPoint> getGlskPoints(String country);

    default GlskProvider getGlskProvider(Network network) {
        return new SimpleGlsk(this, network);
    }

    default GlskProvider getGlskProvider(Network network, Instant instant) {
        return new SimpleGlsk(this, network, instant);
    }

    default ChronologyGlskProvider getChronologyGlskProvider(Network network, Instant instant) {
        return new ChronologyGlsk(this, network, instant);
    }

    default ScalableProvider getScalableProvider(Network network) {
        return new SimpleScalable(this, network);
    }

    default ScalableProvider getScalableProvider(Network network, Instant instant) {
        return new SimpleScalable(this, network, instant);
    }

    default ChronologyScalableProvider getChronologyScalableProvider(Network network, Instant instant) {
        return new ChronologyScalable(this, network, instant);
    }
}
