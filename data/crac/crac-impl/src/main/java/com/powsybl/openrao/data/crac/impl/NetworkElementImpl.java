/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class NetworkElementImpl extends AbstractIdentifiable<NetworkElement> implements NetworkElement {

    NetworkElementImpl(String id, String name) {
        super(id, name);
    }

    NetworkElementImpl(String id) {
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
        NetworkElementImpl networkElement = (NetworkElementImpl) o;
        return super.equals(networkElement);
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * Returns the location of the network element, as a set of optional countries
     *
     * @param network: the network object used to look for the network element
     * @return a set of optional countries containing the network element
     */
    @Override
    public Set<Country> getLocation(Network network) {
        Identifiable<?> ne = network.getIdentifiable(this.getId());
        Set<Country> countries = new HashSet<>();
        if (Objects.isNull(ne)) {
            throw new OpenRaoException("Network element " + this.getId() + " was not found in the network.");
        } else if (ne instanceof Branch<?> branch) {
            getTerminalCountry(branch.getTerminal1()).ifPresent(countries::add);
            getTerminalCountry(branch.getTerminal2()).ifPresent(countries::add);
        } else if (ne instanceof Switch sw) {
            getVoltageLevelCountry(sw.getVoltageLevel()).ifPresent(countries::add);
        } else if (ne instanceof Injection<?> injection) {
            getTerminalCountry(injection.getTerminal()).ifPresent(countries::add);
        } else if (ne instanceof Bus bus) {
            getVoltageLevelCountry(bus.getVoltageLevel()).ifPresent(countries::add);
        } else if (ne instanceof VoltageLevel voltageLevel) {
            getVoltageLevelCountry(voltageLevel).ifPresent(countries::add);
        } else if (ne instanceof Substation substation) {
            substation.getCountry().ifPresent(countries::add);
        } else if (ne instanceof HvdcLine hvdcLine) {
            getTerminalCountry(hvdcLine.getConverterStation1().getTerminal()).ifPresent(countries::add);
            getTerminalCountry(hvdcLine.getConverterStation2().getTerminal()).ifPresent(countries::add);
        } else {
            throw new NotImplementedException("Don't know how to figure out the location of " + ne.getId() + " of type " + ne.getClass());
        }
        return countries;
    }

    private static Optional<Country> getTerminalCountry(Terminal terminal) {
        return getVoltageLevelCountry(terminal.getVoltageLevel());
    }

    private static Optional<Country> getVoltageLevelCountry(VoltageLevel voltageLevel) {
        Optional<Substation> substation = voltageLevel.getSubstation();
        if (substation.isEmpty()) {
            return Optional.empty();
        }
        return substation.get().getCountry();
    }
}
