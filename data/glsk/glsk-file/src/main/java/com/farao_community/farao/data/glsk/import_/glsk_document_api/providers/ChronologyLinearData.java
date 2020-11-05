/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_.glsk_document_api.providers;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.commons.chronology.DataChronologyImpl;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.TypeGlskFile;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.GlskDocument;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.GlskPoint;
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
public abstract class ChronologyLinearData<I> {

    private final Map<String, DataChronology<I>> chronologyLinearDataMap = new HashMap<>();

    public ChronologyLinearData(GlskDocument glskDocument, Network network) {
        List<String> countries = glskDocument.getCountries();

        for (String country : countries) {
            DataChronology<I> dataChronology = DataChronologyImpl.create();

            //mapping with DataChronology
            List<GlskPoint> glskPointList = glskDocument.getGlskPoints(country);
            for (GlskPoint point : glskPointList) {
                I linearData = getLinearData(network, point, glskDocument.getType());
                dataChronology.storeDataOnInterval(linearData, point.getPointInterval());
            }
            chronologyLinearDataMap.put(country, dataChronology);
        }
    }

    public Map<String, I> getLinearData(Instant instant) {
        return chronologyLinearDataMap.entrySet().stream()
            .filter(entry -> entry.getValue().getDataForInstant(instant).isPresent())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getDataForInstant(instant).<AssertionError>orElseThrow(() -> new AssertionError("Data should be available at that instant"))
            ));
    }

    public I getLinearData(Instant instant, String area) {
        Objects.requireNonNull(area);
        if (!chronologyLinearDataMap.containsKey(area)) {
            return null;
        }
        DataChronology<I> chronologyGlsk = chronologyLinearDataMap.get(area);
        return chronologyGlsk.getDataForInstant(instant).orElse(null);
    }

    abstract I getLinearData(Network network, GlskPoint glskPoint, TypeGlskFile typeGlskFile);
}
