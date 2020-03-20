/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@AutoService(ExtensionsHandler.ExtensionSerializer.class)
public class JsonCracResultExtension extends AbstractJsonResultExtension<Crac, CracResultExtension> {

    @Override
    public CracResultExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        CracResultExtension resultExtension = new CracResultExtension();
        deserializeResultExtension(jsonParser, resultExtension);
        return resultExtension;
    }

    @Override
    public String getExtensionName() {
        return "CracResultExtension";
    }

    @Override
    public String getCategoryName() {
        return "identifiable";
    }

    @Override
    public Class<? super CracResultExtension> getExtensionClass() {
        return CracResultExtension.class;
    }
}
