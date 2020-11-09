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
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractSimpleLinearData<I> {

    private final Map<String, I> linearDataMap = new HashMap<>();

    protected AbstractSimpleLinearData(GlskDocument glskDocument, Network network, GlskPointToLinearDataConverter<I> converter, Instant instant) {
        for (String area : glskDocument.getAreas()) {
            List<AbstractGlskPoint> glskPointList = glskDocument.getGlskPoints(area).stream()
                .filter(glskPoint -> glskPoint.getPointInterval().contains(instant))
                .collect(Collectors.toList());
            addLinearDataFromList(network, converter, glskPointList, area);
        }
    }

    protected AbstractSimpleLinearData(GlskDocument glskDocument, Network network, GlskPointToLinearDataConverter<I> converter) {
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

    protected final Map<String, I> getLinearData() {
        return linearDataMap;
    }

    protected I getLinearData(String area) {
        Objects.requireNonNull(area);
        return linearDataMap.get(area);
    }
}
