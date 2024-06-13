package com.powsybl.openrao.data.cracapi.io;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;

import java.io.InputStream;

public interface Importer {

    /**
     * Get a unique identifier of the format.
     */
    String getFormat();

    /**
     * Create a model.
     *
     * @param inputStream data input stream
     * @param cracFactory CRAC factory
     * @param network     network upon which the CRAC is based
     * @return the model
     */
    Crac importData(InputStream inputStream, CracFactory cracFactory, Network network);
}
