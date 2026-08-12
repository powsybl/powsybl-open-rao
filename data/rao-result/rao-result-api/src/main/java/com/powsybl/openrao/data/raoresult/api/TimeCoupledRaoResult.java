/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.Crac;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipOutputStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface TimeCoupledRaoResult extends RaoResult {
    List<OffsetDateTime> getTimestamps();

    RaoResult getIndividualRaoResult(OffsetDateTime timestamp);

    void write(ZipOutputStream zipOutputStream, TemporalData<Crac> cracs, Properties properties) throws IOException;
}
