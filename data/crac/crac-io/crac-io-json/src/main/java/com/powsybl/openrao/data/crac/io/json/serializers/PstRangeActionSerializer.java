/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.range.TapRange;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.serializeActivationCost;
import static com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants.serializeVariationCosts;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PstRangeActionSerializer extends AbstractJsonSerializer<PstRangeAction> {
    @Override
    public void serialize(PstRangeAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeStringField(JsonSerializationConstants.ID, value.getId());
        gen.writeStringField(JsonSerializationConstants.NAME, value.getName());
        gen.writeStringField(JsonSerializationConstants.OPERATOR, value.getOperator());
        serializeActivationCost(value, gen);
        serializeVariationCosts(value, gen);
        UsageRulesSerializer.serializeUsageRules(value, gen);
        gen.writeStringField(JsonSerializationConstants.NETWORK_ELEMENT_ID, value.getNetworkElement().getId());
        Optional<String> groupId = value.getGroupId();
        if (groupId.isPresent()) {
            gen.writeStringField(JsonSerializationConstants.GROUP_ID, groupId.get());
        }
        serializeRemedialActionSpeed(value, gen);
        serializeRanges(value, gen);
        gen.writeEndObject();
    }

    private void serializeRanges(PstRangeAction pstRangeAction, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.RANGES);
        for (TapRange range : pstRangeAction.getRanges()) {
            gen.writeObject(range);
        }
        gen.writeEndArray();
    }
}
