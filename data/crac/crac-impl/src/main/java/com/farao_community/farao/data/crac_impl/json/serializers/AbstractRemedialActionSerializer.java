package com.farao_community.farao.data.crac_impl.json.serializers;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.remedial_action.AbstractRemedialAction;
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
public abstract class AbstractRemedialActionSerializer<I extends AbstractRemedialAction<I>> extends JsonSerializer<I> {
    @Override
    public void serialize(I remedialAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStringField("id", remedialAction.getId());
        jsonGenerator.writeStringField("name", remedialAction.getName());
        jsonGenerator.writeStringField("operator", remedialAction.getOperator());
        jsonGenerator.writeFieldName("usageRules");
        jsonGenerator.writeStartArray();
        for (UsageRule usageRule: remedialAction.getUsageRules()) {
            jsonGenerator.writeObject(usageRule);
        }
        jsonGenerator.writeEndArray();
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
