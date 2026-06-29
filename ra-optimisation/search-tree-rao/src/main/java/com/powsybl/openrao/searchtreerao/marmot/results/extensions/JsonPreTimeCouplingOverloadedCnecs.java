/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results.extensions;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;
import com.powsybl.openrao.data.raoresult.io.json.extension.AbstractJsonCnecIdsExtension;

import java.io.IOException;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonPreTimeCouplingOverloadedCnecs extends AbstractJsonCnecIdsExtension<PreTimeCouplingOverloadedCnecs> {

    @Override
    public PreTimeCouplingOverloadedCnecs deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new PreTimeCouplingOverloadedCnecs());
    }

    @Override
    public String getExtensionName() {
        return "pre-time-coupling-overloaded-cnecs";
    }

    @Override
    public Class<? super PreTimeCouplingOverloadedCnecs> getExtensionClass() {
        return PreTimeCouplingOverloadedCnecs.class;
    }
}
