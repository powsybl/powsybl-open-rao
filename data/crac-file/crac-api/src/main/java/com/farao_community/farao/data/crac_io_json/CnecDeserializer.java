package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Cnec;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CnecDeserializer extends StdDeserializer<Cnec> {

    CnecDeserializer() {
        super(Cnec.class);
    }

    @Override
    public Cnec deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        Cnec.Status status = Cnec.Status.FAILURE;
        PreContingencyResult preContingencyResult = null;
        List<ContingencyResult> contingencyResults = Collections.emptyList();
        List<Extension<Cnec>> extensions = Collections.emptyList();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {

                case "status":

                    status = Cnec.Status.valueOf(jsonParser.nextTextValue());
                    break;

                case "preContingencyResult":
                    jsonParser.nextValue();
                    preContingencyResult = jsonParser.readValueAs(PreContingencyResult.class);
                    break;

                case "contingencyResults":
                    jsonParser.nextToken();
                    contingencyResults = jsonParser.readValueAs(new TypeReference<ArrayList<ContingencyResult>>() {
                    });
                    break;

                case "extensions":
                    jsonParser.nextToken();
                    extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, JsonCnec.getExtensionSerializers());
                    break;

                default:
                    throw new AssertionError("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        Cnec result = new SimpleCnec(
            status,
            preContingencyResult,
            contingencyResults
        );
        JsonCnec.getExtensionSerializers().addExtensions(result, extensions);

        return result;
    }
    
}
