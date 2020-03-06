package com.farao_community.farao.data.crac_impl.json.serializers.range_action;

import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.AbstractElementaryRangeAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractElementaryRangeActionSerializer<I extends AbstractElementaryRangeAction<I>> extends RangeActionSerializer<I>  {
    @Override
    public void serialize(I abstractElementaryRangeAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(abstractElementaryRangeAction, jsonGenerator, serializerProvider);
        jsonGenerator.writeArrayFieldStart("ranges");
        jsonGenerator.writeStartArray();
        for (Range range: abstractElementaryRangeAction.getRanges()) {
            jsonGenerator.writeObject(range);
        }
        jsonGenerator.writeStringField("networkElement", abstractElementaryRangeAction.getNetworkElement().getId());
    }
}
