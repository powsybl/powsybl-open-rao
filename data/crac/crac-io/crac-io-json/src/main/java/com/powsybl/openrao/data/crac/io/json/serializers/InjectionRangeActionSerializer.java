/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;
import java.util.List;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionRangeActionSerializer extends AbstractJsonSerializer<InjectionRangeAction> {

    @Override
    public void serialize(InjectionRangeAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        StandardRangeActionSerializer.serializeCommon(value, gen);
        serializeInjectionDistributionKeys(value, gen);
        serializeRemedialActionSpeed(value, gen);
        gen.writeEndObject();
    }

    private void serializeInjectionDistributionKeys(InjectionRangeAction value, JsonGenerator gen) throws IOException {
        gen.writeFieldName(JsonSerializationConstants.NETWORK_ELEMENT_IDS_AND_KEYS);
        gen.writeStartObject();
        List<String> sortedNetworkElementList = value.getInjectionDistributionKeysIds().keySet().stream().toList();
        for (String networkElementId : sortedNetworkElementList) {
            gen.writeNumberField(networkElementId, value.getInjectionDistributionKeysIds().get(networkElementId));
        }
        gen.writeEndObject();
    }
}
