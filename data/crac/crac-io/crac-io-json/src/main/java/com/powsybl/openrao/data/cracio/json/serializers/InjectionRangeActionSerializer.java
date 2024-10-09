/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.NETWORK_ELEMENT_IDS_AND_KEYS;

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
        gen.writeFieldName(NETWORK_ELEMENT_IDS_AND_KEYS);
        gen.writeStartObject();
        List<NetworkElement> sortedNetworkElementList = value.getInjectionDistributionKeys().keySet().stream()
                .sorted(Comparator.comparing(NetworkElement::getId))
                .toList();
        for (NetworkElement networkElement : sortedNetworkElementList) {
            gen.writeNumberField(networkElement.getId(), value.getInjectionDistributionKeys().get(networkElement));
        }
        gen.writeEndObject();
    }
}
