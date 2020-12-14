/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * Business object for an instant in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonIdentityInfo(scope = Instant.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Instant extends AbstractIdentifiable<Instant> {
    private int seconds;

    @JsonCreator
    public Instant(@JsonProperty("id") String id, @JsonProperty("seconds") int seconds) {
        super(id, id);
        this.seconds = seconds;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    /**
     * Check if instants are equals. Instants are considered equals when IDs and seconds are equals.
     *
     * @param o: If it's null or another object than Instant it will return false.
     * @return A boolean true if objects are equals, otherwise false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Instant instant = (Instant) o;

        return super.equals(o) && seconds == instant.getSeconds();
    }

    @Override
    public int hashCode() {
        return String.format("%s%d", getId(), getSeconds()).hashCode();
    }
}
