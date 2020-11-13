/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.api.providers;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.commons.ZonalDataChronology;
import com.farao_community.farao.commons.chronology.DataChronologyManager;
import com.farao_community.farao.commons.chronology.DataChronologyManagerImpl;
import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.GlskException;
import com.farao_community.farao.data.glsk.api.providers.converters.GlskPointToLinearDataConverter;
import com.powsybl.iidm.network.Network;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ZonalGlskDataChronology<I> implements ZonalDataChronology<I> {

    private final Map<String, DataChronologyManager<I>> dataChronologyPerZone;
    private Instant instant;
    private ReplacementStrategy replacementStrategy;

    public ZonalGlskDataChronology(GlskDocument glskDocument, Network network, GlskPointToLinearDataConverter<I> converter) {
        dataChronologyPerZone = new HashMap<>();

        for (String zone : glskDocument.getZones()) {
            DataChronologyManager<I> dataChronologyManager = DataChronologyManagerImpl.create();

            //mapping with DataChronology
            List<AbstractGlskPoint> glskPointList = glskDocument.getGlskPoints(zone);
            for (AbstractGlskPoint point : glskPointList) {
                I linearData = converter.convert(network, point);
                dataChronologyManager.storeDataOnInterval(linearData, point.getPointInterval());
            }
            dataChronologyPerZone.put(zone, dataChronologyManager);
        }
    }

    private void setInstant(Instant instant) {
        this.instant = instant;
    }

    private void setReplacementStrategy(ReplacementStrategy replacementStrategy) {
        this.replacementStrategy = replacementStrategy;
    }

    @Override
    public Map<String, I> getDataPerZone() {
        if (instant == null) {
            throw new GlskException("Unable to return data if no instant are selected.");
        }
        return dataChronologyPerZone.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getDataForInstant(instant, replacementStrategy)
            ));
    }

    @Override
    public I getData(String zone) {
        Objects.requireNonNull(zone);
        if (instant == null) {
            throw new GlskException("Unable to return data if no instant are selected.");
        }
        if (!dataChronologyPerZone.containsKey(zone)) {
            return null;
        }
        DataChronologyManager<I> chronologyGlsk = dataChronologyPerZone.get(zone);
        return chronologyGlsk.getDataForInstant(instant, replacementStrategy);
    }

    @Override
    public ZonalData<I> getDataForInstant(Instant instant) {
        setInstant(instant);
        setReplacementStrategy(ReplacementStrategy.NO_REPLACEMENT);
        return this;
    }

    @Override
    public ZonalData<I> getDataForInstant(Instant instant, ReplacementStrategy replacementStrategy) {
        setInstant(instant);
        setReplacementStrategy(replacementStrategy);
        return this;
    }
}
