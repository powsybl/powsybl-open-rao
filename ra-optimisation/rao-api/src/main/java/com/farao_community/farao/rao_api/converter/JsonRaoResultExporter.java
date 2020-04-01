/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.converter;

import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_api.json.JsonRaoResult;
import com.google.auto.service.AutoService;

import java.io.OutputStream;

/**
 * A RaoComputationResultExporter implementation which export the result in JSON
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@AutoService(RaoResultExporter.class)
public class JsonRaoResultExporter implements RaoResultExporter {
    @Override
    public String getFormat() {
        return "JSON";
    }

    @Override
    public String getComment() {
        return "Export a remedial actions computation result in JSON format";
    }

    @Override
    public void export(RaoResult result, OutputStream outputStream) {
        JsonRaoResult.write(result, outputStream);
    }
}
