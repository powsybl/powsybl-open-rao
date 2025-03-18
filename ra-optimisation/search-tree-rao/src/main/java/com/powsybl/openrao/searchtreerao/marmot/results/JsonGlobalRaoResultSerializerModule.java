/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.openrao.data.raoresult.api.GlobalRaoResult;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class JsonGlobalRaoResultSerializerModule extends SimpleModule {

    public JsonGlobalRaoResultSerializerModule() {
        super();
        this.addSerializer(GlobalRaoResult.class, new JsonGlobalRaoResultSerializer());
    }
}
