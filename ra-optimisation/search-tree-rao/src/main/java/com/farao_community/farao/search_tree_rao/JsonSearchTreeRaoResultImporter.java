/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationResult;
import com.google.auto.service.AutoService;

import java.io.InputStream;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@AutoService(SearchTreeRaoResultImporter.class)
public class JsonSearchTreeRaoResultImporter implements SearchTreeRaoResultImporter {

    @Override
    public SearchTreeRaoResult importSearchTreeRaoResult(InputStream inputStream) {
        RaoComputationResult result = JsonRaoComputationResult.read(inputStream);
        return result.getExtension(SearchTreeRaoResult.class);
    }

}
