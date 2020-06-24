/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json.serializers.usage_rule;

import com.farao_community.farao.data.crac_impl.json.JsonSerializationNames;
import com.farao_community.farao.data.crac_impl.usage_rule.OnContingency;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class OnContingencySerializer extends UsageRuleSerializer<OnContingency> {

    @Override
    public void serialize(OnContingency usageRule, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(usageRule, jsonGenerator, serializerProvider);
        jsonGenerator.writeStringField(JsonSerializationNames.CONTINGENCY, usageRule.getContingency().getId());
    }
}
