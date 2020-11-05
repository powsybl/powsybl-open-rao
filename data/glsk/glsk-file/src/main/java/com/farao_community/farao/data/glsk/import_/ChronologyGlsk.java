/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.commons.chronology.DataChronologyImpl;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.GlskDocument;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.GlskPoint;
import com.farao_community.farao.data.glsk.import_.converters.GlskPointLinearGlskConverter;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class ChronologyGlsk implements GlskProvider {

    private ChronologyLinearData<LinearGlsk> chronologyLinearData;

    public ChronologyGlsk(GlskDocument glskDocument, Network network) {
        Map<String, DataChronology<LinearGlsk>> chronologyLinearDataMap = new HashMap<>();

        for (String country : glskDocument.getCountries()) {
            DataChronology<LinearGlsk> dataChronology = DataChronologyImpl.create();

            //mapping with DataChronology
            List<GlskPoint> glskPointList = glskDocument.getGlskPoints(country);
            for (GlskPoint point : glskPointList) {
                LinearGlsk linearData = GlskPointLinearGlskConverter.convert(network, point, glskDocument.getType());
                dataChronology.storeDataOnInterval(linearData, point.getPointInterval());
            }
            chronologyLinearDataMap.put(country, dataChronology);
        }
        chronologyLinearData = new ChronologyLinearData<>(chronologyLinearDataMap);
    }

    @Override
    public Map<String, LinearGlsk> getLinearGlskPerCountry(Instant instant) {
        return chronologyLinearData.getLinearData(instant);
    }

    @Override
    public LinearGlsk getLinearGlsk(Instant instant, String area) {
        return chronologyLinearData.getLinearData(instant, area);
    }
}
