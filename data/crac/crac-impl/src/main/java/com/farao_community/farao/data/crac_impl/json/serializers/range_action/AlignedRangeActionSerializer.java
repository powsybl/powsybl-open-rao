/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json.serializers.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.AlignedRangeAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class AlignedRangeActionSerializer extends RangeActionSerializer<AlignedRangeAction> {

    @Override
    public void serialize(AlignedRangeAction alignedRangeAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(alignedRangeAction, jsonGenerator, serializerProvider);
        jsonGenerator.writeArrayFieldStart("ranges");
        jsonGenerator.writeStartArray();
        for (Range range: alignedRangeAction.getRanges()) {
            jsonGenerator.writeObject(range);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeArrayFieldStart("networkElements");
        jsonGenerator.writeStartArray();
        for (NetworkElement networkElement: alignedRangeAction.getNetworkElements()) {
            jsonGenerator.writeString(networkElement.getId());
        }
        jsonGenerator.writeEndArray();
    }
}
