/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.api.providers.simple;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.providers.GlskPointToLinearDataConverter;
import com.powsybl.iidm.network.Network;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractSimpleLinearData<I> {

    private final Map<String, I> linearDataMap = new HashMap<>();

    protected AbstractSimpleLinearData(GlskDocument glskDocument, Network network, GlskPointToLinearDataConverter<I> converter, Instant instant) {
        for (String country : glskDocument.getCountries()) {
            List<AbstractGlskPoint> glskPointList = glskDocument.getGlskPoints(country).stream()
                .filter(glskPoint -> glskPoint.getPointInterval().contains(instant)).collect(Collectors.toList());
            if (glskPointList.size() > 1) {
                throw new FaraoException("Cannot instantiate simple linear data because several glsk point match given instant");
            }
            I linearData = converter.convert(network, glskPointList.get(0));
            linearDataMap.put(country, linearData);
        }
    }

    protected AbstractSimpleLinearData(GlskDocument glskDocument, Network network, GlskPointToLinearDataConverter<I> converter) {
        for (String country : glskDocument.getCountries()) {
            List<AbstractGlskPoint> glskPointList = glskDocument.getGlskPoints(country);
            if (glskPointList.size() > 1) {
                throw new FaraoException("Cannot instantiate simple linear data because glsk document defines a chronology");
            }
            I linearData = converter.convert(network, glskPointList.get(0));
            linearDataMap.put(country, linearData);
        }
    }

    protected final Map<String, I> getLinearData() {
        return linearDataMap;
    }

    protected I getLinearData(String area) {
        Objects.requireNonNull(area);
        return linearDataMap.get(area);
    }
}
