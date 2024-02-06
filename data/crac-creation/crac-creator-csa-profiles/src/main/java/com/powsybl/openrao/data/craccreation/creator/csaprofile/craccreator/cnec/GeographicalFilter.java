/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.OpenRaoException;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class GeographicalFilter {

    private GeographicalFilter() {
    }

    public static Set<Country> getNetworkElementLocation(String networkElementId, Network network) {
        Identifiable<?> networkElement = network.getIdentifiable(networkElementId);
        if (Objects.isNull(networkElement)) {
            throw new OpenRaoException("Network element " + networkElementId + " was not found in the network.");
        } else if (networkElement instanceof Branch<?> branch) {
            return getBranchLocation(branch);
        } else if (networkElement instanceof Switch switchElement) {
            return getSwitchLocation(switchElement);
        } else if (networkElement instanceof Injection<?> injection) {
            return getInjectionLocation(injection);
        } else if (networkElement instanceof Bus bus) {
            return getBusLocation(bus);
        } else if (networkElement instanceof VoltageLevel voltageLevel) {
            return getVoltageLevelLocation(voltageLevel);
        } else if (networkElement instanceof Substation substation) {
            return getSubstationLocation(substation);
        } else if (networkElement instanceof HvdcLine hvdcLine) {
            return getHvdcLineLocation(hvdcLine);
        } else {
            throw new NotImplementedException("Could not figure out the location of " + networkElement.getId() + " of type " + networkElement.getClass());
        }
    }

    private static Set<Country> getBranchLocation(Branch<?> branch) {
        Optional<Country> country1 = branch.getTerminal1() == null ? Optional.empty() : getSubstationCountry(branch.getTerminal1().getVoltageLevel().getSubstation());
        Optional<Country> country2 = branch.getTerminal2() == null ? Optional.empty() : getSubstationCountry(branch.getTerminal2().getVoltageLevel().getSubstation());
        Set<Country> locations = new HashSet<>();
        country1.ifPresent(locations::add);
        country2.ifPresent(locations::add);
        return locations;
    }

    private static Set<Country> getSwitchLocation(Switch switchElement) {
        return getSubstationCountry(switchElement.getVoltageLevel().getSubstation()).map(Set::of).orElseGet(Set::of);
    }

    private static Set<Country> getInjectionLocation(Injection<?> injection) {
        Optional<Country> location = injection.getTerminal() == null ? Optional.empty() : getSubstationCountry(injection.getTerminal().getVoltageLevel().getSubstation());
        return location.map(Set::of).orElseGet(Set::of);
    }

    private static Set<Country> getBusLocation(Bus bus) {
        return getSubstationCountry(bus.getVoltageLevel().getSubstation()).map(Set::of).orElseGet(Set::of);
    }

    private static Set<Country> getVoltageLevelLocation(VoltageLevel voltageLevel) {
        return getSubstationCountry(voltageLevel.getSubstation()).map(Set::of).orElseGet(Set::of);
    }

    private static Set<Country> getSubstationLocation(Substation substation) {
        return substation.getCountry().map(Set::of).orElseGet(Set::of);
    }

    private static Set<Country> getHvdcLineLocation(HvdcLine hvdcLine) {
        Optional<Country> country1 = hvdcLine.getConverterStation1().getTerminal() == null ? Optional.empty() : getSubstationCountry(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation());
        Optional<Country> country2 = hvdcLine.getConverterStation2().getTerminal() == null ? Optional.empty() : getSubstationCountry(hvdcLine.getConverterStation2().getTerminal().getVoltageLevel().getSubstation());
        Set<Country> locations = new HashSet<>();
        country1.ifPresent(locations::add);
        country2.ifPresent(locations::add);
        return locations;
    }

    private static Optional<Country> getSubstationCountry(Optional<Substation> substation) {
        return substation.isPresent() ? substation.get().getCountry() : Optional.empty();
    }

    public static Set<Country> getNetworkElementsLocations(Set<String> networkElementIds, Network network) {
        Set<Country> locations = new HashSet<>();
        networkElementIds.forEach(networkElement -> locations.addAll(getNetworkElementLocation(networkElement, network)));
        return locations;
    }

    public static boolean networkElementsShareCommonCountry(Set<String> networkElementsSet1, Set<String> networkElementsSet2, Network network) {
        Set<Country> locations1 = getNetworkElementsLocations(networkElementsSet1, network);
        Set<Country> locations2 = getNetworkElementsLocations(networkElementsSet2, network);
        locations1.retainAll(locations2);
        return !locations1.isEmpty();
    }
}
