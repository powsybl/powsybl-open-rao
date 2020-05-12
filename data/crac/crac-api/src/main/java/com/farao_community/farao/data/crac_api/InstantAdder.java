package com.farao_community.farao.data.crac_api;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface InstantAdder {

    /**
     * Set the id of the new instant
     * @param id: the id to set
     * @return the {@code InstantAdder} instance
     */
    InstantAdder setId(String id);

    /**
     * Set the seconds of the new instant
     * @param seconds: the number of seconds of the instant to set
     * @return the {@code InstantAdder} instance
     */
    InstantAdder setSeconds(Integer seconds);

    /**
     * Add the new instant to the Crac
     * @return the {@code Instant} created
     */
    Instant add();
}
