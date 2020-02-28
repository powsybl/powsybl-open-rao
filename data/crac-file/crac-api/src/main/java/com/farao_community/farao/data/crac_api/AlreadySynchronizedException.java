package com.farao_community.farao.data.crac_api;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class AlreadySynchronizedException extends SynchronizationException {
    public AlreadySynchronizedException(String message) {
        super(message);
    }
}
