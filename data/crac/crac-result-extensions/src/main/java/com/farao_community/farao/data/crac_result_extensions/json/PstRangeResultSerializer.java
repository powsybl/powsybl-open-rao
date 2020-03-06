package com.farao_community.farao.data.crac_result_extensions.json;

import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler.RangeActionExtensionSerializer;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RangeActionExtensionSerializer.class)
public class PstRangeResultSerializer extends RangeActionResultSerializer<PstRange, PstRangeResult> {

    @Override
    public void serialize(PstRangeResult pstRangeResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(pstRangeResult, jsonGenerator, serializerProvider);
        ObjectWriter objectWriter = JsonUtil.createObjectMapper().writerWithDefaultPrettyPrinter();
        objectWriter.writeValue(jsonGenerator, pstRangeResult);
    }

    @Override
    public PstRangeResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectReader objectReader = JsonUtil.createObjectMapper().readerFor(PstRangeResult.class);
        return objectReader.readValue(jsonParser);
    }

    @Override
    public String getExtensionName() {
        return "PstRangeResult";
    }

    @Override
    public String getCategoryName() {
        return "pst-range";
    }

    @Override
    public Class<? super PstRangeResult> getExtensionClass() {
        return PstRangeResult.class;
    }
}
