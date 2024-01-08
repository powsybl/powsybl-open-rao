/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.rao_api.json;

import com.powsybl.open_rao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoParametersJsonModule extends SimpleModule {

    public RaoParametersJsonModule() {
        addDeserializer(RaoParameters.class, new RaoParametersDeserializer());
        addSerializer(RaoParameters.class, new RaoParametersSerializer());
    }
}
