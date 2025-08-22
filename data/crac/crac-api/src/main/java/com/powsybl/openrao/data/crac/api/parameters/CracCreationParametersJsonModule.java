/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.parameters;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreationParametersJsonModule extends SimpleModule {

    public CracCreationParametersJsonModule() {
        addDeserializer(CracCreationParameters.class, new CracCreationParametersDeserializer());
        addSerializer(CracCreationParameters.class, new CracCreationParametersSerializer());
    }
}
