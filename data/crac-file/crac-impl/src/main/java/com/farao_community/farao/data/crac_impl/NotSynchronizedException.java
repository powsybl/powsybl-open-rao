package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class NotSynchronizedException extends FaraoException {
    public NotSynchronizedException(String message) {
        super(message);
    }
}
