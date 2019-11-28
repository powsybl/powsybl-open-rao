/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import java.io.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public final class SearchTreeRaoResultImporters {

    private SearchTreeRaoResultImporters() {
    }

    public static SearchTreeRaoResult importSearchTreeRaoResult(InputStream inputStream) {
        SearchTreeRaoResultImporter importer = new JsonSearchTreeRaoResultImporter();
        return importer.importSearchTreeRaoResult(inputStream);
    }
}
