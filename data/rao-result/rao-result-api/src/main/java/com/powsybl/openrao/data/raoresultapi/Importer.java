package com.powsybl.openrao.data.raoresultapi;

import com.powsybl.openrao.data.cracapi.Crac;

import java.io.InputStream;

public interface Importer {
    /**
     * Get a unique identifier of the format.
     */
    String getFormat();

    /**
     * Create a RaoResult.
     *
     * @param inputStream RaoResult data
     * @param crac        the crac on which the RaoResult data is based
     * @return the model
     */
    RaoResult importData(InputStream inputStream, Crac crac);
}
