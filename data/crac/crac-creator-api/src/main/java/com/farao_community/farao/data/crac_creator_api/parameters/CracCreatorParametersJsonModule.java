/*
 * Copyright (c) 2021, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api.parameters;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreatorParametersJsonModule extends SimpleModule {

    public CracCreatorParametersJsonModule() {
        addDeserializer(CracCreatorParameters.class, new CracCreatorParametersDeserializer());
        addSerializer(CracCreatorParameters.class, new CracCreatorParametersSerializer());
    }
}
