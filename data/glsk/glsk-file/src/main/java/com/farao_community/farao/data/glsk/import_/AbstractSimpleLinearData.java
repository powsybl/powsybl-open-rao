/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.glsk.import_.converters.GlskPointToLinearDataConverter;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.GlskDocument;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractSimpleLinearData<I> {

    private final Map<String, I> linearDataMap;

    protected AbstractSimpleLinearData(GlskDocument glskDocument, Network network, GlskPointToLinearDataConverter<I> converter) {
        linearDataMap = new HashMap<>();

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
