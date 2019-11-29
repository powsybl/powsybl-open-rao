/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.converter;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationResult;
import com.google.auto.service.AutoService;

import java.io.OutputStream;

/**
 * A RaoComputationResultExporter implementation which export the result in JSON
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(RaoComputationResultExporter.class)
public class JsonRaoComputationResultExporter implements RaoComputationResultExporter {
    @Override
    public String getFormat() {
        return "JSON";
    }

    @Override
    public String getComment() {
        return "Export a remedial actions computation result in JSON format";
    }

    @Override
    public void export(RaoComputationResult result, OutputStream outputStream) {
        JsonRaoComputationResult.write(result, outputStream);
    }
}
