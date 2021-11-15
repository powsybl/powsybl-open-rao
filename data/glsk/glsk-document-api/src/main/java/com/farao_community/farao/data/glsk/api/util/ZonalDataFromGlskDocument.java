/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.api.util;

import com.farao_community.farao.commons.ZonalDataImpl;
import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.GlskException;
import com.farao_community.farao.data.glsk.api.util.converters.GlskPointToLinearDataConverter;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ZonalDataFromGlskDocument<I> extends ZonalDataImpl<I> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZonalDataFromGlskDocument.class);

    public ZonalDataFromGlskDocument(GlskDocument glskDocument, Network network, GlskPointToLinearDataConverter<I> converter, Instant instant) {
        super(new HashMap<>());
        for (String zone : glskDocument.getZones()) {
            List<AbstractGlskPoint> glskPointList = glskDocument.getGlskPoints(zone).stream()
                .filter(glskPoint -> glskPoint.getPointInterval().contains(instant))
                .collect(Collectors.toList());
            try {
                addLinearDataFromList(network, converter, glskPointList, zone);
            } catch (GlskException e) {
                LOGGER.warn(String.format("Could not create linear data for zone %s: %s", zone, e.getMessage()));
            }
        }
    }

    public ZonalDataFromGlskDocument(GlskDocument glskDocument, Network network, GlskPointToLinearDataConverter<I> converter) {
        super(new HashMap<>());
        for (String zone : glskDocument.getZones()) {
            List<AbstractGlskPoint> glskPointList = glskDocument.getGlskPoints(zone);
            addLinearDataFromList(network, converter, glskPointList, zone);
        }
    }

    private void addLinearDataFromList(Network network, GlskPointToLinearDataConverter<I> converter, List<AbstractGlskPoint> glskPointList, String country) {
        if (glskPointList.size() > 1) {
            throw new GlskException("Cannot instantiate simple linear data because several glsk point match given instant");
        } else if (!glskPointList.isEmpty()) {
            I linearData = converter.convert(network, glskPointList.get(0));
            dataPerZone.put(country, linearData);
        }
    }
}
