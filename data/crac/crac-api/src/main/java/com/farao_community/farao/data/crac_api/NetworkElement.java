/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Element of the network in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonIdentityInfo(scope = NetworkElement.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class NetworkElement extends AbstractIdentifiable<NetworkElement> {

    @JsonCreator
    public NetworkElement(@JsonProperty("id") String id, @JsonProperty("name") String name) {
        super(id, name);
    }

    public NetworkElement(String id) {
        this(id, id);
    }

    /**
     * Check if network elements are equals. Network elements are considered equals when IDs are equals.
     *
     * @param o: If it's null or another object than NetworkElement it will return false.
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
        NetworkElement networkElement = (NetworkElement) o;
        return super.equals(networkElement);
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * Returns the location of the network element, as a set of optional countries
     * @param network: the network object used to look for the network element
     * @return a set of optional countries containing the network element
     */
    public Set<Optional<Country>> getLocation(Network network) {
        Identifiable<?> ne = network.getIdentifiable(this.getId());
        if (Objects.isNull(ne)) {
            throw new FaraoException("Network element " + this.getId() + " was not found in the network.");
        } else if (ne instanceof Branch) {
            Branch branch = (Branch) ne;
            Optional<Country> country1 = branch.getTerminal1().getVoltageLevel().getSubstation().getCountry();
            Optional<Country> country2 = branch.getTerminal2().getVoltageLevel().getSubstation().getCountry();
            if (country1.equals(country2)) {
                return Set.of(country1);
            } else {
                return Set.of(country1, country2);
            }
        } else if (ne instanceof Switch) {
            return Set.of(((Switch) ne).getVoltageLevel().getSubstation().getCountry());
        } else if (ne instanceof Injection) {
            return Set.of(((Injection) ne).getTerminal().getVoltageLevel().getSubstation().getCountry());
        } else if (ne instanceof  Bus) {
            return Set.of(((Bus) ne).getVoltageLevel().getSubstation().getCountry());
        } else if (ne instanceof VoltageLevel) {
            return Set.of(((VoltageLevel) ne).getSubstation().getCountry());
        } else if (ne instanceof Substation) {
            return Set.of(((Substation) ne).getCountry());
        }  else {
            throw new NotImplementedException("Don't know how to figure out the location of " + ne.getId() + " of type " + ne.getClass());
        }
    }
}
