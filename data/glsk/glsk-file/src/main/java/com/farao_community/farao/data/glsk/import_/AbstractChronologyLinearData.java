/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.commons.chronology.DataChronologyImpl;
import com.farao_community.farao.data.glsk.import_.converters.GlskPointToLinearDataConverter;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.GlskDocument;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.AbstractGlskPoint;
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
public abstract class AbstractChronologyLinearData<I> {

    private final Map<String, DataChronology<I>> chronologyLinearDataMap;
    private Instant instant;

    protected AbstractChronologyLinearData(GlskDocument glskDocument, Network network, GlskPointToLinearDataConverter<I> converter, Instant instant) {
        this.instant = instant == null ? Instant.now() : instant;
        chronologyLinearDataMap = new HashMap<>();

        for (String country : glskDocument.getCountries()) {
            DataChronology<I> dataChronology = DataChronologyImpl.create();

            //mapping with DataChronology
            List<AbstractGlskPoint> glskPointList = glskDocument.getGlskPoints(country);
            for (AbstractGlskPoint point : glskPointList) {
                I linearData = converter.convert(network, point);
                dataChronology.storeDataOnInterval(linearData, point.getPointInterval());
            }
            chronologyLinearDataMap.put(country, dataChronology);
        }
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    protected Map<String, I> getLinearData() {
        return chronologyLinearDataMap.entrySet().stream()
            .filter(entry -> entry.getValue().getDataForInstant(instant).isPresent())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getDataForInstant(instant).<AssertionError>orElseThrow(() -> new AssertionError("Data should be available at that instant"))
            ));
    }

    protected I getLinearData(String area) {
        Objects.requireNonNull(area);
        if (!chronologyLinearDataMap.containsKey(area)) {
            return null;
        }
        DataChronology<I> chronologyGlsk = chronologyLinearDataMap.get(area);
        return chronologyGlsk.getDataForInstant(instant).orElse(null);
    }
}
