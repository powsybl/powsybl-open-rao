/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_io_json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionRangeActionSerializer extends AbstractJsonSerializer<InjectionRangeAction> {

    @Override
    public void serialize(InjectionRangeAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ID, value.getId());
        gen.writeStringField(NAME, value.getName());
        gen.writeStringField(OPERATOR, value.getOperator());
        UsageRulesSerializer.serializeUsageRules(value, gen);
        serializeInjectionDistributionKeys(value, gen);
        serializeGroupId(value, gen);
        serializeRanges(value, gen);
        JsonUtil.writeExtensions(value, gen, serializers, ExtensionsHandler.getExtensionsSerializers());
        gen.writeEndObject();
    }

    private void serializeInjectionDistributionKeys(InjectionRangeAction value, JsonGenerator gen) throws IOException {
        gen.writeFieldName(NETWORK_ELEMENT_IDS_AND_KEYS);
        gen.writeStartObject();
        List<NetworkElement> sortedNetworkElementList = value.getInjectionDistributionKeys().keySet().stream()
                .sorted(Comparator.comparing(NetworkElement::getId))
                .collect(Collectors.toList());
        for (NetworkElement networkElement : sortedNetworkElementList) {
            gen.writeNumberField(networkElement.getId(), value.getInjectionDistributionKeys().get(networkElement));
        }
        gen.writeEndObject();
    }

    private void serializeGroupId(InjectionRangeAction value, JsonGenerator gen) throws IOException {
        Optional<String> groupId = value.getGroupId();
        if (groupId.isPresent()) {
            gen.writeStringField(GROUP_ID, groupId.get());
        }
    }

    private void serializeRanges(InjectionRangeAction value, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(RANGES);
        for (StandardRange range : value.getRanges()) {
            gen.writeObject(range);
        }
        gen.writeEndArray();
    }
}
