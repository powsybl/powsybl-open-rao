/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_impl.FlowCnecImpl;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationNames.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class FlowCnecSerializer extends BranchCnecSerializer<FlowCnec> {

    @Override
    public void serialize(FlowCnec cnec, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(cnec, jsonGenerator, serializerProvider);
        jsonGenerator.writeObjectField(FRM, cnec.getReliabilityMargin());
        super.addExtensions(cnec, jsonGenerator, serializerProvider);
    }
}
