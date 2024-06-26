/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi.io;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;

import java.io.InputStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface Importer {

    /**
     * Get a unique identifier of the format.
     */
    String getFormat();

    boolean exists(InputStream inputStream);

    /**
     * Create a model.
     *
     * @param inputStream data input stream
     * @param cracFactory CRAC factory
     * @param network     network upon which the CRAC is based
     * @param reportNode
     * @return the model
     */
    Crac importData(InputStream inputStream, CracFactory cracFactory, Network network, ReportNode reportNode);

    /**
     * Create a model.
     *
     * @param inputStream data input stream
     * @param cracFactory CRAC factory
     * @param network     network upon which the CRAC is based
     * @return the model
     */
    default Crac importData(InputStream inputStream, CracFactory cracFactory, Network network) {
        return importData(inputStream, cracFactory, network, ReportNode.NO_OP);
    }
}
