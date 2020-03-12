package com.farao_community.farao.data.crac_impl.json.serializers.range_action;

import com.farao_community.farao.data.crac_impl.json.serializers.AbstractRemedialActionSerializer;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.AbstractRangeAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionSerializer<I extends AbstractRangeAction<I>> extends AbstractRemedialActionSerializer<I> {
    @Override
    public void serialize(I abstractRangeAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(abstractRangeAction, jsonGenerator, serializerProvider);
        jsonGenerator.writeFieldName("ranges");
        jsonGenerator.writeStartArray();
        for (Range range : abstractRangeAction.getRanges()) {
            jsonGenerator.writeObject(range);
        }
        jsonGenerator.writeEndArray();
    }
}
