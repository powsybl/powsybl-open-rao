/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.api.providers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.providers.converters.GlskPointToLinearDataConverter;
import com.powsybl.iidm.network.Network;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ZonalGlskData<I> implements ZonalData<I> {

    private final Map<String, I> linearDataMap = new HashMap<>();

    public ZonalGlskData(GlskDocument glskDocument, Network network, GlskPointToLinearDataConverter<I> converter, Instant instant) {
        for (String area : glskDocument.getAreas()) {
            List<AbstractGlskPoint> glskPointList = glskDocument.getGlskPoints(area).stream()
                .filter(glskPoint -> glskPoint.getPointInterval().contains(instant))
                .collect(Collectors.toList());
            addLinearDataFromList(network, converter, glskPointList, area);
        }
    }

    public ZonalGlskData(GlskDocument glskDocument, Network network, GlskPointToLinearDataConverter<I> converter) {
        for (String area : glskDocument.getAreas()) {
            List<AbstractGlskPoint> glskPointList = glskDocument.getGlskPoints(area);
            addLinearDataFromList(network, converter, glskPointList, area);
        }
    }

    private void addLinearDataFromList(Network network, GlskPointToLinearDataConverter<I> converter, List<AbstractGlskPoint> glskPointList, String country) {
        if (glskPointList.size() > 1) {
            throw new FaraoException("Cannot instantiate simple linear data because several glsk point match given instant");
        } else if (!glskPointList.isEmpty()) {
            I linearData = converter.convert(network, glskPointList.get(0));
            linearDataMap.put(country, linearData);
        }
    }

    @Override
    public final Map<String, I> getDataPerZone() {
        return linearDataMap;
    }

    @Override
    public I getData(String zone) {
        Objects.requireNonNull(zone);
        return linearDataMap.get(zone);
    }
}
