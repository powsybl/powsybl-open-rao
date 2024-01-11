/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

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
import com.powsybl.open_rao.commons.OpenRaoException;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class GeographicalFilter {

    private GeographicalFilter() { }

    public static Set<Country> getNetworkElementLocation(String networkElementId, Network network) {
        Identifiable<?> networkElement = network.getIdentifiable(networkElementId);
        if (Objects.isNull(networkElement)) {
            throw new OpenRaoException("Network element " + networkElementId + " was not found in the network.");
        } else if (networkElement instanceof Branch<?> branch) {
            return filterEmptyCountries(getBranchLocation(branch));
        } else if (networkElement instanceof Switch switchElement) {
            return filterEmptyCountries(getSwitchLocation(switchElement));
        } else if (networkElement instanceof Injection<?> injection) {
            return filterEmptyCountries(getInjectionLocation(injection));
        } else if (networkElement instanceof Bus bus) {
            return filterEmptyCountries(getBusLocation(bus));
        } else if (networkElement instanceof VoltageLevel voltageLevel) {
            return filterEmptyCountries(getVoltageLevelLocation(voltageLevel));
        } else if (networkElement instanceof Substation substation) {
            return filterEmptyCountries(getSubstationLocation(substation));
        } else if (networkElement instanceof HvdcLine hvdcLine) {
            return filterEmptyCountries(getHvdcLineLocation(hvdcLine));
        } else {
            throw new NotImplementedException("Don't know how to figure out the location of " + networkElement.getId() + " of type " + networkElement.getClass());
        }
    }

    private static Set<Optional<Country>> getBranchLocation(Branch<?> branch) {
        Optional<Country> country1 = branch.getTerminal1() == null ? Optional.empty() : getSubstationCountry(branch.getTerminal1().getVoltageLevel().getSubstation());
        Optional<Country> country2 = branch.getTerminal2() == null ? Optional.empty() : getSubstationCountry(branch.getTerminal2().getVoltageLevel().getSubstation());
        return country1.equals(country2) ? Set.of(country1) : Set.of(country1, country2);
    }

    private static Set<Optional<Country>> getSwitchLocation(Switch switchElement) {
        return Set.of(getSubstationCountry(switchElement.getVoltageLevel().getSubstation()));
    }

    private static Set<Optional<Country>> getInjectionLocation(Injection<?> injection) {
        return Set.of(injection.getTerminal() == null ? Optional.empty() : getSubstationCountry(injection.getTerminal().getVoltageLevel().getSubstation()));
    }

    private static Set<Optional<Country>> getBusLocation(Bus bus) {
        return Set.of(getSubstationCountry(bus.getVoltageLevel().getSubstation()));
    }

    private static Set<Optional<Country>> getVoltageLevelLocation(VoltageLevel voltageLevel) {
        return Set.of(getSubstationCountry(voltageLevel.getSubstation()));
    }

    private static Set<Optional<Country>> getSubstationLocation(Substation substation) {
        return Set.of(substation.getCountry());
    }

    private static Set<Optional<Country>> getHvdcLineLocation(HvdcLine hvdcLine) {
        return Set.of(
                hvdcLine.getConverterStation1().getTerminal() == null ? Optional.empty() : getSubstationCountry(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation()),
                hvdcLine.getConverterStation2().getTerminal() == null ? Optional.empty() : getSubstationCountry(hvdcLine.getConverterStation2().getTerminal().getVoltageLevel().getSubstation())
        );
    }

    private static Optional<Country> getSubstationCountry(Optional<Substation> substation) {
        return substation.isPresent() ? substation.get().getCountry() : Optional.empty();
    }

    private static Set<Country> filterEmptyCountries(Set<Optional<Country>> countries) {
        return countries.stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
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
