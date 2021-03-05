/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creator_api.CracCreationContext;
import com.farao_community.farao.data.raw_crac_api.RawCrac;
import com.powsybl.iidm.network.Network;

import java.io.OutputStream;

/**
 * Interface for CRAC object export
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

public interface CracExporter<T extends RawCrac> {

    String getFormat();

    void exportCrac(Crac crac, OutputStream outputStream);

    void exportCrac(Crac crac, Network network, OutputStream outputStream);

    void exportCrac(Crac crac, T rawCrac, Network network, CracCreationContext<T> cracCreationContext,
                    String initialVariantId, String postPraVariantId, String postCraVariantId, OutputStream outputStream);
}
