/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.json.serializers.usage_rule;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_impl.OnStateImpl;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Optional;

import static com.farao_community.farao.data.crac_io_json.json.JsonSerializationNames.CONTINGENCY;
import static com.farao_community.farao.data.crac_io_json.json.JsonSerializationNames.INSTANT;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class OnStateSerializer extends UsageRuleSerializer<OnStateImpl> {

    @Override
    public void serialize(OnStateImpl usageRule, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(usageRule, jsonGenerator, serializerProvider);
        Optional<Contingency> optContingency = usageRule.getState().getContingency();
        if (optContingency.isPresent()) {
            jsonGenerator.writeStringField(CONTINGENCY, optContingency.get().getId());
        }
        jsonGenerator.writeStringField(INSTANT, usageRule.getState().getInstant().toString());
    }
}
