package com.farao_community.farao.data.crac_api;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class NotSynchronizedException extends SynchronizationException {
    public NotSynchronizedException(String message) {
        super(message);
    }
}
