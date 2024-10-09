/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.range.TapRange;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PstRangeActionSerializer extends AbstractJsonSerializer<PstRangeAction> {
    @Override
    public void serialize(PstRangeAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeStringField(ID, value.getId());
        gen.writeStringField(NAME, value.getName());
        gen.writeStringField(OPERATOR, value.getOperator());
        UsageRulesSerializer.serializeUsageRules(value, gen);
        gen.writeStringField(NETWORK_ELEMENT_ID, value.getNetworkElement().getId());
        Optional<String> groupId = value.getGroupId();
        if (groupId.isPresent()) {
            gen.writeStringField(GROUP_ID, groupId.get());
        }
        gen.writeNumberField(INITIAL_TAP, value.getInitialTap());
        gen.writeObjectField(TAP_TO_ANGLE_CONVERSION_MAP, value.getTapToAngleConversionMap());
        serializeRemedialActionSpeed(value, gen);
        serializeRanges(value, gen);
        gen.writeEndObject();
    }

    private void serializeRanges(PstRangeAction pstRangeAction, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(RANGES);
        for (TapRange range : pstRangeAction.getRanges()) {
            gen.writeObject(range);
        }
        gen.writeEndArray();
    }
}
