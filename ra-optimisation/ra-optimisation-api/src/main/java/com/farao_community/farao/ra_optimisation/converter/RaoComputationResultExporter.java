/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.converter;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;

import java.io.OutputStream;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public interface RaoComputationResultExporter {

    /**
     * Get the format of this exporter
     *
     * @return the format name of this exporter
     */
    String getFormat();

    /**
     * Get a brief description of this exporter
     *
     * @return a brief description of this exporter
     */
    String getComment();

    /**
     * Export a result of a remedial actions optimisation
     *
     * @param result The result of the remedial action optimisation
     * @param os The output stream used for the export
     */
    void export(RaoComputationResult result, OutputStream os);
}
