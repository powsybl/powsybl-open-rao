package com.farao_community.farao.data.crac_impl.json.serializers.range_action;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.farao_community.farao.data.crac_impl.json.serializers.RemedialActionSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionSerializer<I extends RangeAction<I>> extends RemedialActionSerializer<I> {
    @Override
    public void serialize(I rangeAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStringField("id", rangeAction.getId());
        jsonGenerator.writeStringField("name", rangeAction.getName());
        super.serialize(rangeAction, jsonGenerator, serializerProvider);

        JsonUtil.writeExtensions(rangeAction, jsonGenerator, serializerProvider, ExtensionsHandler.getRangeActionExtensionSerializers());
    }
}
