/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import java.io.*;

/**
 * Generates the Crac schema manually
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
final class CracSchemaGeneration {

    private CracSchemaGeneration() {

    }

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        JsonSchemaGenerator generator = new JsonSchemaGenerator(mapper);
        JsonSchema jsonSchema = generator.generateSchema(SimpleCrac.class);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        StringWriter json = new StringWriter();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.writeValue(json, jsonSchema);

        System.out.println(json.toString());
    }
}
