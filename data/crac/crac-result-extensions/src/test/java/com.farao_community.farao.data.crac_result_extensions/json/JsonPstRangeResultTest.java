package com.farao_community.farao.data.crac_result_extensions.json;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.json.deserializers.SimpleCracDeserializer;
import com.farao_community.farao.data.crac_impl.json.serializers.network_action.NetworkActionSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.range_action.PstRangeSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.range_action.PstWithRangeSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.range_action.RangeActionSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.FreeToUseSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.OnConstraintSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.OnContingencySerializer;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.usage_rule.OnConstraint;
import com.farao_community.farao.data.crac_impl.usage_rule.OnContingency;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Optional;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class JsonPstRangeResultTest {
    private static final double EPSILON = 0.1;
    private RangeAction pstRange;
    private State state;
    private PstRangeResult pstRangeResult;

    @Before
    public void setUp() {
        pstRange = new PstWithRange("id", new NetworkElement("ne"));
        state = new SimpleState(Optional.empty(), new Instant("initial", 0));
        pstRangeResult = new PstRangeResult(Collections.singleton(state.getId()));
    }

    @Test
    public void exportPstResult() {
        pstRange.addExtension(PstRangeResult.class, pstRangeResult);
        pstRangeResult.setTap(state.getId(), 15);
        roundTrip(pstRange, RangeAction.class);
    }

    static <T> T roundTrip(T object, Class<T> objectClass) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ObjectMapper objectMapper = createObjectMapper();
            objectMapper.registerModule(new Jdk8Module());
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            SimpleModule module = new SimpleModule();
            module.addSerializer(PstRange.class, new PstRangeSerializer<>());
            objectMapper.registerModule(module);
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, object);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            ObjectMapper objectMapper = createObjectMapper();
            objectMapper.registerModule(new Jdk8Module());
            SimpleModule module = new SimpleModule();
            module.addDeserializer(SimpleCrac.class, new SimpleCracDeserializer());
            objectMapper.registerModule(module);
            return objectMapper.readValue(inputStream, objectClass);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
