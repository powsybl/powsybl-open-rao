/*
 * Copyright (c) 2018, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowBasedComputationParametersJsonModule extends SimpleModule {

    public FlowBasedComputationParametersJsonModule() {
        addDeserializer(FlowBasedComputationParameters.class, new FlowBasedComputationParametersDeserializer());
        addSerializer(FlowBasedComputationParameters.class, new FlowBasedComputationParametersSerializer());
    }
}
