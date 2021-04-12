/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@AutoService(ExtensionsHandler.ExtensionSerializer.class)
public class JsonNetworkActionResultExtension extends AbstractJsonResultExtension<NetworkAction, NetworkActionResultExtension> {
    @Override
    public NetworkActionResultExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        NetworkActionResultExtension networkActionResultExtension = new NetworkActionResultExtension();
        deserializeResultExtension(jsonParser, networkActionResultExtension);
        return networkActionResultExtension;
    }

    @Override
    public String getExtensionName() {
        return "NetworkActionResultExtension";
    }

    @Override
    public String getCategoryName() {
        return "network-action-result-extension";
    }

    @Override
    public Class<? super NetworkActionResultExtension> getExtensionClass() {
        return NetworkActionResultExtension.class;
    }
}
