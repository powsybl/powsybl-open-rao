/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.json.serializers.network_action;

import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.InjectionSetpoint;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

import static com.farao_community.farao.data.crac_impl.json.JsonSerializationNames.SETPOINT;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class InjectionSetPointSerializer extends NetworkActionSerializer<InjectionSetpoint> {
    @Override
    public void serialize(InjectionSetpoint remedialAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(remedialAction, jsonGenerator, serializerProvider);
        jsonGenerator.writeNumberField(SETPOINT, remedialAction.getSetpoint());
        JsonUtil.writeExtensions(remedialAction, jsonGenerator, serializerProvider, ExtensionsHandler.getExtensionsSerializers());
    }
}
