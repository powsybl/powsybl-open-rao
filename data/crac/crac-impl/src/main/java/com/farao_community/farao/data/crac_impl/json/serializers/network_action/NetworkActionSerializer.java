package com.farao_community.farao.data.crac_impl.json.serializers.network_action;

import com.farao_community.farao.data.crac_impl.json.serializers.AbstractRemedialActionSerializer;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.AbstractNetworkAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class NetworkActionSerializer<I extends AbstractNetworkAction<I>> extends AbstractRemedialActionSerializer<I> {

    @Override
    public void serialize(I networkAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(networkAction, jsonGenerator, serializerProvider);
    }

    @Override
    public void serializeWithType(I networkAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {
        WritableTypeId writableTypeId = typeSerializer.typeId(networkAction, JsonToken.START_OBJECT);
        typeSerializer.writeTypePrefix(jsonGenerator, writableTypeId);
        serialize(networkAction, jsonGenerator, serializerProvider);
        typeSerializer.writeTypeSuffix(jsonGenerator, writableTypeId);
    }
}
