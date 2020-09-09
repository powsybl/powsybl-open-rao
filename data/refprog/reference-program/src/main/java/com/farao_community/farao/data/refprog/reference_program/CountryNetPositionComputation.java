/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.refprog.reference_program;

import com.powsybl.iidm.network.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CountryNetPositionComputation {

    private Network network;
    private EnumMap<Country, Double> netPositions;

    public CountryNetPositionComputation(Network network) {
        this.network = network;
    }

    public Map<Country, Double> getNetPositions() {
        if (Objects.isNull(netPositions)) {
            computeNetPositions();
        }
        return netPositions;
    }

    private void computeNetPositions() {
        netPositions = new EnumMap<>(Country.class);

        for (Country country : Country.values()) {
            netPositions.put(country, 0.0);
        }

        network.getDanglingLineStream().forEach(danglingLine -> {
            Country country = danglingLine.getTerminal().getVoltageLevel().getSubstation().getNullableCountry();
            addLeavingFlow(danglingLine, country);
        });

        network.getLineStream().forEach(line -> {
            Country countrySide1 = line.getTerminal1().getVoltageLevel().getSubstation().getNullableCountry();
            Country countrySide2 = line.getTerminal2().getVoltageLevel().getSubstation().getNullableCountry();
            if (countrySide1 == countrySide2) {
                return;
            }
            addLeavingFlow(line, countrySide1);
            addLeavingFlow(line, countrySide2);
        });

        network.getHvdcLineStream().forEach(hvdcLine -> {
            Country countrySide1 = hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().getNullableCountry();
            Country countrySide2 = hvdcLine.getConverterStation2().getTerminal().getVoltageLevel().getSubstation().getNullableCountry();
            if (countrySide1 == countrySide2) {
                return;
            }
            addLeavingFlow(hvdcLine, countrySide1);
            addLeavingFlow(hvdcLine, countrySide2);
        });
    }

    private void addLeavingFlow(DanglingLine danglingLine, Country country) {
        if (!Objects.isNull(country)) {
            netPositions.put(country, netPositions.get(country) + getLeavingFlow(danglingLine));
        }
    }

    private void addLeavingFlow(Line line, Country country) {
        if (!Objects.isNull(country)) {
            netPositions.put(country, netPositions.get(country) + getLeavingFlow(line, country));
        }
    }

    private void addLeavingFlow(HvdcLine hvdcLine, Country country) {
        if (!Objects.isNull(country)) {
            netPositions.put(country, netPositions.get(country) + getLeavingFlow(hvdcLine, country));
        }
    }

    private double getLeavingFlow(DanglingLine danglingLine) {
        return danglingLine.getTerminal().isConnected() && !Double.isNaN(danglingLine.getTerminal().getP()) ? danglingLine.getTerminal().getP() : 0;
    }

    private double getLeavingFlow(Line line, Country country) {
        double flowSide1 = line.getTerminal1().isConnected() && !Double.isNaN(line.getTerminal1().getP()) ? line.getTerminal1().getP() : 0;
        double flowSide2 = line.getTerminal2().isConnected() && !Double.isNaN(line.getTerminal2().getP()) ? line.getTerminal2().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return country.equals(line.getTerminal1().getVoltageLevel().getSubstation().getNullableCountry()) ? directFlow : -directFlow;
    }

    private double getLeavingFlow(HvdcLine hvdcLine, Country country) {
        double flowSide1 = hvdcLine.getConverterStation1().getTerminal().isConnected() && !Double.isNaN(hvdcLine.getConverterStation1().getTerminal().getP()) ? hvdcLine.getConverterStation1().getTerminal().getP() : 0;
        double flowSide2 = hvdcLine.getConverterStation2().getTerminal().isConnected() && !Double.isNaN(hvdcLine.getConverterStation2().getTerminal().getP()) ? hvdcLine.getConverterStation2().getTerminal().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return country.equals(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().getNullableCountry()) ? directFlow : -directFlow;
    }
}
