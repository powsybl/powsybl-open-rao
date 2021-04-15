/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporter;
import com.farao_community.farao.data.crac_io_json.serializers.CracSerializer;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;

/**
 * CRAC object export in json format
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@AutoService(CracExporter.class)
public class JsonExport implements CracExporter {

    private static final String JSON_FORMAT = "Json";

    @Override
    public String getFormat() {
        return JSON_FORMAT;
    }

    @Override
    public void exportCrac(Crac crac, OutputStream outputStream) {
        try {
            outputStream.write(CracSerializer.serializeCrac(crac).getBytes(Charset.forName("UTF-8")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*@Override
    public void exportCrac(Crac crac, OutputStream outputStream) {

        try {
            ObjectMapper objectMapper = createObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            SimpleModule module = new CracImplJsonModule();
            objectMapper.registerModule(module);
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, crac);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }*/

    @Override
    public void exportCrac(Crac crac, Network network, OutputStream outputStream) {
        exportCrac(crac, outputStream);
    }
}
