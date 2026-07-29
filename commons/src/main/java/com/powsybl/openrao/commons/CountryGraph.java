/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CountryGraph {
    private final Set<CountryBoundary> boundaries;

    /**
     * Constructor based on a set of given CountryBoundary
     */
    public CountryGraph(Set<CountryBoundary> boundaries) {
        this.boundaries = boundaries;
    }

    /**
     * Constructor based on a Network object
     * The country graph is built by the constructor, based on the topology of the Network
     */
    public CountryGraph(Network network) {

        boundaries = new HashSet<>();

        network.getBranchStream()
            .forEach(branch -> {
                Optional<Country> country1 = getTerminalCountry(branch.getTerminal1());
                Optional<Country> country2 = getTerminalCountry(branch.getTerminal2());

                if (country1.isPresent() && country2.isPresent()) {
                    if (!country1.equals(country2)) {
                        boundaries.add(new CountryBoundary(country1.get(), country2.get()));
                    }
                } else {
                    OpenRaoLoggerProvider.TECHNICAL_LOGS.debug("Countries are not defined in both sides of branch {}", branch.getId());
                }
            });
    }

    private Optional<Country> getTerminalCountry(Terminal terminal) {
        Optional<Substation> substation = terminal.getVoltageLevel().getSubstation();
        if (substation.isPresent()) {
            return substation.get().getCountry();
        } else {
            return Optional.empty();
        }
    }

    public boolean areNeighbors(Country country1, Country country2) {
        return areNeighbors(country1, country2, 1);
    }

    public boolean areNeighbors(Country country1, Country country2, int maxNumberOfBoundaries) {
        if (country1.equals(country2)) {
            return true;
        }
        if (maxNumberOfBoundaries <= 0) {
            return false;
        }

        for (CountryBoundary boundary : boundaries) {
            if (boundary.getCountryLeft().equals(country1) && areNeighbors(boundary.getCountryRight(), country2, maxNumberOfBoundaries - 1)
                || boundary.getCountryRight().equals(country1) && areNeighbors(boundary.getCountryLeft(), country2, maxNumberOfBoundaries - 1)) {
                return true;
            }
        }
        return false;
    }
}
