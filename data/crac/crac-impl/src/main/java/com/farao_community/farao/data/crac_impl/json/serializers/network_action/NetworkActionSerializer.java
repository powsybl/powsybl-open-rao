package com.farao_community.farao.data.crac_impl.json.serializers.network_action;

import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.farao_community.farao.data.crac_impl.json.serializers.RemedialActionSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class NetworkActionSerializer<I extends NetworkAction> extends RemedialActionSerializer<I> {
    @Override
    public void serialize(I networkAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStringField("id", networkAction.getId());
        jsonGenerator.writeStringField("name", networkAction.getName());
        super.serialize(networkAction, jsonGenerator, serializerProvider);

        JsonUtil.writeExtensions(networkAction, jsonGenerator, serializerProvider, ExtensionsHandler.getNetworkActionExtensionSerializers());
    }
}
