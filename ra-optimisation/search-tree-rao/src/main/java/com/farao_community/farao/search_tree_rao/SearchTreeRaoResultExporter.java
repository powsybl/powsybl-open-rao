/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import java.io.OutputStream;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public interface SearchTreeRaoResultExporter {

    /**
     * Get the format of this exporter
     *
     * @return the format name of this exporter
     */
    String getFormat();

    /**
     * Export a result of a remedial actions optimisation
     *
     * @param result The result of the remedial action optimisation
     * @param os The output stream used for the export
     */
    void export(SearchTreeRaoResult result, OutputStream os);

}
