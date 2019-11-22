/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;

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
        JsonSchemaGenerator generator = new JsonSchemaGenerator(mapper);
        JsonSchema jsonSchema = generator.generateSchema(Crac.class);

        StringWriter json = new StringWriter();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.writeValue(json, jsonSchema);

        try (OutputStream os = new FileOutputStream(
                new File(CracSchemaGeneration.class.getResource("/CracSchema.json").toURI()))) {
            os.write(json.toString().getBytes());
        }
        //System.out.println(json.toString());
        //OutputStream os = new FileOutputStream(new File(CracSchemaGeneration.class.getResource("/CracSchema.json").toURI()));
        //OutputStream os = new FileOutputStream("../../../main/java/resources/CracSchema.json");
    }
}
