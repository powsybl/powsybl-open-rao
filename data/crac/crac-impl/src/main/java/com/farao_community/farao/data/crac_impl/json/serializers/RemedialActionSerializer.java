package com.farao_community.farao.data.crac_impl.json.serializers;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RemedialActionSerializer<I extends RemedialAction> extends JsonSerializer<I> {
    @Override
    public void serialize(I remedialAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStringField("operator", remedialAction.getOperator());
        jsonGenerator.writeObjectField("usageRules", remedialAction.getUsageRules());
        jsonGenerator.writeFieldName("networkElements");
        jsonGenerator.writeStartArray();
        for (NetworkElement networkElement: remedialAction.getNetworkElements()) {
            jsonGenerator.writeString(networkElement.getId());
        }
        jsonGenerator.writeEndArray();
    }

    @Override
    public void serializeWithType(I remedialAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {
        WritableTypeId writableTypeId = typeSerializer.typeId(remedialAction, JsonToken.START_OBJECT);
        typeSerializer.writeTypePrefix(jsonGenerator, writableTypeId);
        serialize(remedialAction, jsonGenerator, serializerProvider);
        typeSerializer.writeTypeSuffix(jsonGenerator, writableTypeId);
    }
}
