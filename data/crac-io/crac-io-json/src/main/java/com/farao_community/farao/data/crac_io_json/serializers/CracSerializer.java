/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationNames.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CracSerializer extends AbstractJsonSerializer<Crac> {

    @Override
    public void serialize(Crac crac, JsonGenerator gen, SerializerProvider serializers) throws IOException {

        // todo: suggestion, ajouter une petite en-tête pour dire que le Crac a été généré par farao avec un lien vers le site web ?

        gen.writeStringField(ID, crac.getId());
        gen.writeStringField(NAME, crac.getName());
        gen.writeArrayFieldStart(CONTINGENCIES);

        List<Contingency> sortedListOfContingencies = crac.getContingencies().stream()
            .sorted(Comparator.comparing(Contingency::getId))
            .collect(Collectors.toList());

        for (Contingency contingency : sortedListOfContingencies) {
            gen.writeObject(contingency);
        }
        gen.writeEndArray();

        /*
        gen.writeArrayFieldStart(CNECS);
        for (BranchCnec cnec : value.getFlowCnecs()) {
            gen.writeObject(cnec);
        }
        gen.writeEndArray();
        gen.writeArrayFieldStart(RANGE_ACTIONS);
        for (RangeAction rangeAction: value.getRangeActions()) {
            gen.writeObject(rangeAction);
        }
        gen.writeEndArray();
        gen.writeArrayFieldStart(NETWORK_ACTIONS);
        for (NetworkAction networkAction : value.getNetworkActions()) {
            gen.writeObject(networkAction);
        }
        gen.writeEndArray();

         */
        JsonUtil.writeExtensions(crac, gen, serializers, ExtensionsHandler.getExtensionsSerializers());
    }
}
