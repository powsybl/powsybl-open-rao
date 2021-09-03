/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.range_action.HvdcRange;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_io_json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.Optional;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class HvdcRangeActionSerializer extends AbstractJsonSerializer<HvdcRangeAction> {
    @Override
    public void serialize(HvdcRangeAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
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
        serializeRanges(value, gen);

        JsonUtil.writeExtensions(value, gen, serializers, ExtensionsHandler.getExtensionsSerializers());

        gen.writeEndObject();
    }

    private void serializeRanges(HvdcRangeAction hvdcRangeAction, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(RANGES);
        for (HvdcRange range : hvdcRangeAction.getRanges()) {
            gen.writeObject(range);
        }
        gen.writeEndArray();
    }
}
