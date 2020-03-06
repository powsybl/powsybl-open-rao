package com.farao_community.farao.data.crac_result_extensions.json;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResult;
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
@AutoService(ExtensionsHandler.RangeActionExtensionSerializer.class)
public class RangeActionResultSerializer<I extends RangeAction<I>, E extends RangeActionResult<I>> implements ExtensionsHandler.RangeActionExtensionSerializer<I, E> {

    @Override
    public void serialize(E rangeActionResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        ObjectWriter objectWriter = JsonUtil.createObjectMapper().writerWithDefaultPrettyPrinter();
        objectWriter.writeValue(jsonGenerator, rangeActionResult);
    }

    @Override
    public E deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectReader objectReader = JsonUtil.createObjectMapper().readerFor(RangeActionResult.class);
        return objectReader.readValue(jsonParser);
    }

    @Override
    public String getExtensionName() {
        return "RangeActionResult";
    }

    @Override
    public String getCategoryName() {
        return "range-action";
    }

    @Override
    public Class<? super E> getExtensionClass() {
        return RangeActionResult.class;
    }
}
