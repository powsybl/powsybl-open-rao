package com.farao_community.farao.data.crac_impl.json.serializers.range_action;

import com.farao_community.farao.data.crac_api.PstRange;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstRangeSerializer<I extends PstRange> extends RangeActionSerializer<PstRange> {
    @Override
    public void serialize(PstRange pstRange, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

    }
}
