package com.farao_community.farao.data.crac_result_extensions.json;

import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
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
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(ExtensionsHandler.CracExtensionSerializer.class)
public class JsonResultVariantManager implements ExtensionsHandler.CracExtensionSerializer<ResultVariantManager> {

    @Override
    public void serialize(ResultVariantManager variantManager, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        ObjectWriter objectWriter = JsonUtil.createObjectMapper().writerWithDefaultPrettyPrinter();
        objectWriter.writeValue(jsonGenerator, variantManager);
        jsonGenerator.writeEndObject();
    }

    @Override
    public ResultVariantManager deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectReader objectReader = JsonUtil.createObjectMapper().readerFor(ResultVariantManager.class);
        return objectReader.readValue(jsonParser);
    }

    @Override
    public String getExtensionName() {
        return "ResultVariantManager";
    }

    @Override
    public String getCategoryName() {
        return "crac";
    }

    @Override
    public Class<? super ResultVariantManager> getExtensionClass() {
        return ResultVariantManager.class;
    }


}
