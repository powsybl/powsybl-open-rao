/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flow_decomposition_results.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import java.io.IOException;
import java.util.Map;

/**
 * Custom Deserializer for Guava Table
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class TableDeserializer extends JsonDeserializer<Table<?, ?, ?>> {
    @Override
    public Table<?, ?, ?> deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
        final ImmutableTable.Builder<Object, Object, Object> tableBuilder = ImmutableTable.builder();
        final Map<Object, Map<Object, Object>> rowMap = jp.readValueAs(Map.class);
        for (final Map.Entry<Object, Map<Object, Object>> rowEntry : rowMap.entrySet()) {
            final Object rowKey = rowEntry.getKey();
            for (final Map.Entry<Object, Object> cellEntry : rowEntry.getValue().entrySet()) {
                final Object colKey = cellEntry.getKey();
                final Object val = cellEntry.getValue();
                tableBuilder.put(rowKey, colKey, val);
            }
        }
        return tableBuilder.build();
    }
}
