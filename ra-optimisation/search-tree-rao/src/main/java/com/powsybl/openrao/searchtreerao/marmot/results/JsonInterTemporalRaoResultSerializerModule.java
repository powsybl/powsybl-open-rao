/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;

import java.util.List;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class JsonInterTemporalRaoResultSerializerModule extends SimpleModule {

    public JsonInterTemporalRaoResultSerializerModule(String individualRaoResultFilenameTemplate, List<Instant> instants) {
        super();
        this.addSerializer(InterTemporalRaoResult.class, new JsonInterTemporalRaoResultSerializer(individualRaoResultFilenameTemplate, instants));
    }
}
