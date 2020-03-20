/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Cnec;
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
public class JsonCnecResultExtension extends AbstractJsonResultExtension<Cnec, CnecResultExtension> {

    @Override
    public CnecResultExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        CnecResultExtension resultExtension = new CnecResultExtension();
        deserializeResultExtension(jsonParser, resultExtension);
        return resultExtension;
    }

    @Override
    public String getExtensionName() {
        return "CnecResultExtension";
    }

    @Override
    public String getCategoryName() {
        return "cnec-extension";
    }

    @Override
    public Class<? super CnecResultExtension> getExtensionClass() {
        return CnecResultExtension.class;
    }
}
