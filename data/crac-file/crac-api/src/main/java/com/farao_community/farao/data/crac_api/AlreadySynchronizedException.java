package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class AlreadySynchronizedException extends FaraoException {
    public AlreadySynchronizedException(String message) {
        super(message);
    }
}
