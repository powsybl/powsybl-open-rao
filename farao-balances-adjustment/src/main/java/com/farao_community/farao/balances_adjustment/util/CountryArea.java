/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.util;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class CountryArea extends AbstractNetworkArea {

    private final Country country;

    public CountryArea(Country country) {
        this.country = country;
    }

    public Country getCountry() {
        return country;
    }

    @Override
    public List<VoltageLevel> getAreaVoltageLevels(Network network) {
        return network.getVoltageLevelStream()
                .filter(voltageLevel -> voltageLevel.getSubstation().getCountry().isPresent())
                .filter(voltageLevel -> voltageLevel.getSubstation().getCountry().get().equals(country))
                .collect(Collectors.toList());
    }

    @Override
    public List<BorderDevice> getBorderThreeWindingTransformer(Network network, List<VoltageLevel> areaVoltageLevels) {
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return country.getName();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof CountryArea)) {
            return false;
        }
        CountryArea countryArea = (CountryArea) object;
        return country.getName().equals(countryArea.getName());
    }

    @Override
    public int hashCode() {
        return country.hashCode();
    }
}
