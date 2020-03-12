/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.json;

import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.json.deserializers.SimpleCracDeserializer;
import com.farao_community.farao.data.crac_impl.json.serializers.network_action.ComplexNetworkActionSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.range_action.AlignedRangeActionSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.range_action.PstWithRangeSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.FreeToUseSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.OnConstraintSerializer;
import com.farao_community.farao.data.crac_impl.json.serializers.usage_rule.OnContingencySerializer;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.ComplexNetworkAction;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.AlignedRangeAction;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.usage_rule.OnConstraint;
import com.farao_community.farao.data.crac_impl.usage_rule.OnContingency;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RoundTripUtil {
    private RoundTripUtil() {

    }

    /**
     * This utilitary class enable to export an object through ObjectMapper in an OutputStream
     * and then re-import this stream as the object. The purpose is to see if the whole export/import
     * process works fine.
     *
     * @param object: object to export/import
     * @param objectClass: class of the object
     * @param <T>: type of the object
     * @return the object exported and re-imported
     */
    static <T> T roundTrip(T object, Class<T> objectClass) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ObjectMapper objectMapper = createObjectMapper();
            objectMapper.registerModule(new Jdk8Module());
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            SimpleModule module = new SimpleModule();
            module.addSerializer(FreeToUse.class, new FreeToUseSerializer());
            module.addSerializer(OnConstraint.class, new OnConstraintSerializer());
            module.addSerializer(OnContingency.class, new OnContingencySerializer());
            module.addSerializer(PstWithRange.class, new PstWithRangeSerializer());
            module.addSerializer(AlignedRangeAction.class, new AlignedRangeActionSerializer());
            module.addSerializer(ComplexNetworkAction.class, new ComplexNetworkActionSerializer());
            objectMapper.registerModule(module);
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
