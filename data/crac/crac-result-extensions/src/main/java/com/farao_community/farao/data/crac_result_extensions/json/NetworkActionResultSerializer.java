package com.farao_community.farao.data.crac_result_extensions.json;

import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.farao_community.farao.data.crac_result_extensions.NetworkActionResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(ExtensionsHandler.NetworkActionExtensionSerializer.class)
public class NetworkActionResultSerializer implements ExtensionsHandler.NetworkActionExtensionSerializer<NetworkActionResult> {
    @Override
    public void serialize(NetworkActionResult networkActionResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

    }

    @Override
    public NetworkActionResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return null;
    }

    @Override
    public String getExtensionName() {
        return "NetworkActionResult";
    }

    @Override
    public String getCategoryName() {
        return "network-action";
    }

    @Override
    public Class<? super NetworkActionResult> getExtensionClass() {
        return NetworkActionResult.class;
    }
}
