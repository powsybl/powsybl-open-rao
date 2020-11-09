/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.api;

import com.farao_community.farao.data.glsk.api.providers.chronology.ChronologyGlsk;
import com.farao_community.farao.data.glsk.api.providers.chronology.ChronologyScalable;
import com.farao_community.farao.data.glsk.api.providers.simple.SimpleGlsk;
import com.farao_community.farao.data.glsk.api.providers.simple.SimpleScalable;
import com.powsybl.iidm.network.Network;

import java.time.Instant;
import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface GlskDocument {

    List<String> getAreas();

    List<AbstractGlskPoint> getGlskPoints(String area);

    /**
     * This method will produce a GLSK provider which is not time-specific. It is not guarantee that all GLSK document
     * can handle this method because all time-specific data should be extracted. To implement only if GLSK document
     * is not time-specific.
     *
     * @param network: Network on which to map GLSK document information.
     * @return A {@link GlskProvider} extracted from the GLSK document.
     */
    default GlskProvider getGlskProvider(Network network) {
        return new SimpleGlsk(this, network);
    }

    /**
     * This method will produce a GLSK provider which is not time-specific. Only data that is available at the
     * specified {@param instant} will be extracted.
     *
     * @param network: Network on which to map GLSK document information.
     * @param instant: Instant at which extracted data will be available.
     * @return A {@link GlskProvider} extracted from the GLSK document.
     */
    default GlskProvider getGlskProvider(Network network, Instant instant) {
        return new SimpleGlsk(this, network, instant);
    }

    /**
     * This method will produce a time-specific GLSK provider. All time-related data will be extracted.
     *
     * @param network: Network on which to map GLSK document information.
     * @return A {@link GlskProvider} extracted from the GLSK document.
     */
    default ChronologyGlskProvider getChronologyGlskProvider(Network network) {
        return new ChronologyGlsk(this, network);
    }

    /**
     * This method will produce a scalable provider which is not time-specific. It is not guarantee that all GLSK
     * document can handle this method because all time-specific data should be extracted. To implement only if GLSK
     * document is not time-specific.
     *
     * @param network: Network on which to map GLSK document information.
     * @return A {@link ScalableProvider} extracted from the GLSK document.
     */
    default ScalableProvider getScalableProvider(Network network) {
        return new SimpleScalable(this, network);
    }

    /**
     * This method will produce a scalable provider which is not time-specific. Only data that is available at the
     * specified {@param instant} will be extracted.
     *
     * @param network: Network on which to map GLSK document information.
     * @param instant: Instant at which extracted data will be available.
     * @return A {@link ScalableProvider} extracted from the GLSK document.
     */
    default ScalableProvider getScalableProvider(Network network, Instant instant) {
        return new SimpleScalable(this, network, instant);
    }

    /**
     * This method will produce a time-specific scalable provider. All time-related data will be extracted.
     *
     * @param network: Network on which to map GLSK document information.
     * @return A {@link ScalableProvider} extracted from the GLSK document.
     */
    default ChronologyScalableProvider getChronologyScalableProvider(Network network) {
        return new ChronologyScalable(this, network);
    }
}
