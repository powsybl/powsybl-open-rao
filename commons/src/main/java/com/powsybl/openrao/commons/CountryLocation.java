/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class CountryLocation {
    private CountryLocation() {
    }

    public static Set<Country> getLocation(Network network, String networkElementId) {
        Identifiable<?> ne = network.getIdentifiable(networkElementId);
        Set<Country> countries = new HashSet<>();
        if (Objects.isNull(ne)) {
            throw new OpenRaoException("Network element " + networkElementId + " was not found in the network.");
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
