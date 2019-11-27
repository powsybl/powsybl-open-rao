/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.OutputStream;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@AutoService(SearchTreeRaoResultExporter.class)
public class JsonSearchTreeRaoResultExporter implements SearchTreeRaoResultExporter {

    private static final String JSON_FORMAT = "Json";

    @Override
    public String getFormat() {
        return JSON_FORMAT;
    }

    @Override
    public void export(SearchTreeRaoResult result, OutputStream os) throws IOException {
        ObjectMapper objectMapper = createObjectMapper();
        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
        writer.writeValue(os, result);
        writer.writeValue(os, result.getRaoComputationResult());
    }
}
