/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range_action.StandardRangeAction;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Optional;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * Serializes common elements in StandardRaneAction implementations
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public final class StandardRangeActionSerializer {

    private StandardRangeActionSerializerUtils() {
    }

    public static void serializeCommon(StandardRangeAction<?> value, JsonGenerator gen) throws IOException {
        gen.writeStringField(ID, value.getId());
        gen.writeStringField(NAME, value.getName());
        gen.writeStringField(OPERATOR, value.getOperator());
        UsageRulesSerializer.serializeUsageRules(value, gen);
        serializeGroupId(value, gen);
        gen.writeNumberField(INITIAL_SETPOINT, value.getInitialSetpoint());
        serializeRanges(value, gen);
    }

    private static void serializeGroupId(StandardRangeAction<?> value, JsonGenerator gen) throws IOException {
        Optional<String> groupId = value.getGroupId();
        if (groupId.isPresent()) {
            gen.writeStringField(GROUP_ID, groupId.get());
        }
    }

    private static void serializeRanges(StandardRangeAction<?> value, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(RANGES);
        for (StandardRange range : value.getRanges()) {
            gen.writeObject(range);
        }
        gen.writeEndArray();
    }
}
