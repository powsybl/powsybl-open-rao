/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.api.providers.chronology;

import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.providers.converters.GlskPointLinearGlskConverter;
import com.farao_community.farao.data.glsk.api.ChronologyGlskProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.time.Instant;
import java.util.Map;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class ChronologyGlsk extends AbstractChronologyLinearData<LinearGlsk> implements ChronologyGlskProvider {

    public ChronologyGlsk(GlskDocument glskDocument, Network network) {
        super(glskDocument, network, GlskPointLinearGlskConverter::convert);
    }

    @Override
    public Map<String, LinearGlsk> getLinearGlskPerArea() {
        return getLinearData();
    }

    @Override
    public LinearGlsk getLinearGlsk(String area) {
        return getLinearData(area);
    }

    @Override
    public ChronologyGlskProvider selectInstant(Instant instant) {
        setInstant(instant);
        return this;
    }
}
