/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.extension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.data.raoresult.api.extension.CriticalCnecsResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonCriticalCnecsResult extends AbstractJsonCnecIdsExtension<CriticalCnecsResult> {

    @Override
    public CriticalCnecsResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new CriticalCnecsResult());
    }

    @Override
    public String getExtensionName() {
        return "critical-cnecs-result";
    }

    @Override
    public Class<? super CriticalCnecsResult> getExtensionClass() {
        return CriticalCnecsResult.class;
    }
}
