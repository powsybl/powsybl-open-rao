/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_.glsk_document_api.providers;

import com.farao_community.farao.data.glsk.import_.glsk_document_api.GlskDocument;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.GlskPoint;
import com.farao_community.farao.data.glsk.import_.converters.GlskPointLinearGlskConverter;
import com.farao_community.farao.data.glsk.import_.glsk_document_api.TypeGlskFile;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.time.Instant;
import java.util.Map;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class ChronologyGlsk extends ChronologyLinearData<LinearGlsk> implements GlskProvider {

    public ChronologyGlsk(GlskDocument glskDocument, Network network) {
        super(glskDocument, network);
    }

    @Override
    LinearGlsk getLinearData(Network network, GlskPoint glskPoint, TypeGlskFile type) {
        return GlskPointLinearGlskConverter.convert(network, glskPoint, type);
    }

    @Override
    public Map<String, LinearGlsk> getLinearGlskPerCountry(Instant instant) {
        return getLinearData(instant);
    }

    @Override
    public LinearGlsk getLinearGlsk(Instant instant, String area) {
        return getLinearData(instant, area);
    }
}
