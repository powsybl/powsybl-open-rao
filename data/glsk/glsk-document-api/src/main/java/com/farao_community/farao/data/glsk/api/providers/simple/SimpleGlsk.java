/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.api.providers.simple;

import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.providers.converters.GlskPointLinearGlskConverter;
import com.farao_community.farao.data.glsk.api.GlskProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.time.Instant;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SimpleGlsk extends AbstractSimpleLinearData<LinearGlsk> implements GlskProvider {

    public SimpleGlsk(GlskDocument glskDocument, Network network) {
        super(glskDocument, network, GlskPointLinearGlskConverter::convert);
    }

    public SimpleGlsk(GlskDocument glskDocument, Network network, Instant instant) {
        super(glskDocument, network, GlskPointLinearGlskConverter::convert, instant);
    }

    @Override
    public Map<String, LinearGlsk> getLinearGlskPerArea() {
        return getLinearData();
    }

    @Override
    public LinearGlsk getLinearGlsk(String area) {
        return getLinearData(area);
    }
}
