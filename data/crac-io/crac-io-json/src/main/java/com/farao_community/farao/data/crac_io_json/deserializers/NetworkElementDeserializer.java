package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.ID;
import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.NAME;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkElementDeserializer extends AbstractJsonDeserializer<DeserializedNetworkElement> {
    @Override
    public DeserializedNetworkElement deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        String id = null;
        String name = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case ID:
                    id = jsonParser.nextTextValue();
                    break;
                case NAME:
                    name = jsonParser.nextTextValue();
                    break;
                default:
                    throw new FaraoException(String.format("Unexpected field in NetworkElement: %s", jsonParser.getCurrentName()));
            }
        }
        return new DeserializedNetworkElement(id, name);
    }
}
