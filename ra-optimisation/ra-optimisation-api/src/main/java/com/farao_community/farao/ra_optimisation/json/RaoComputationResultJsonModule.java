/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.json;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class RaoComputationResultJsonModule extends SimpleModule {

    public RaoComputationResultJsonModule() {
        addDeserializer(RaoComputationResult.class, new RaoComputationResultDeserializer());
        addSerializer(RaoComputationResult.class, new RaoComputationResultSerializer());
    }
}
