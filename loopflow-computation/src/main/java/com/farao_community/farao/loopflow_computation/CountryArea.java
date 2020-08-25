/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.powsybl.iidm.network.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Mathieu Bague {@literal <mathieu.bague at rte-france.com>}
 */
public class CountryArea implements NetworkArea {

    private final List<Country> countries = new ArrayList<>();

    private final List<DanglingLine> danglingLineBordersCache;
    private final List<Line> lineBordersCache;
    private final List<HvdcLine> hvdcLineBordersCache;

    public CountryArea(Network network, List<Country> countries) {
        this.countries.addAll(countries);

        danglingLineBordersCache = network.getDanglingLineStream()
                .filter(this::isAreaBorder)
                .collect(Collectors.toList());
        lineBordersCache = network.getLineStream()
                .filter(this::isAreaBorder)
                .collect(Collectors.toList());
        hvdcLineBordersCache = network.getHvdcLineStream()
                .filter(this::isAreaBorder)
                .collect(Collectors.toList());
    }

    public List<Country> getCountries() {
        return countries;
    }

    @Override
    public double getNetPosition() {
        return danglingLineBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum()
                + lineBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum()
                + hvdcLineBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum();
    }

    private boolean isAreaBorder(DanglingLine danglingLine) {
        Country country = danglingLine.getTerminal().getVoltageLevel().getSubstation().getNullableCountry();
        return countries.contains(country);
    }

    private boolean isAreaBorder(Line line) {
        Country countrySide1 = line.getTerminal1().getVoltageLevel().getSubstation().getNullableCountry();
        Country countrySide2 = line.getTerminal2().getVoltageLevel().getSubstation().getNullableCountry();
        if (countrySide1 == null || countrySide2 == null) {
            return false;
        }
        return countries.contains(countrySide1) && !countries.contains(countrySide2) ||
                !countries.contains(countrySide1) && countries.contains(countrySide2);
    }

    private boolean isAreaBorder(HvdcLine hvdcLine) {
        Country countrySide1 = hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().getNullableCountry();
        Country countrySide2 = hvdcLine.getConverterStation2().getTerminal().getVoltageLevel().getSubstation().getNullableCountry();
        if (countrySide1 == null || countrySide2 == null) {
            return false;
        }
        return countries.contains(countrySide1) && !countries.contains(countrySide2) ||
                !countries.contains(countrySide1) && countries.contains(countrySide2);
    }

    private double getLeavingFlow(DanglingLine danglingLine) {
        return danglingLine.getTerminal().isConnected() && !Double.isNaN(danglingLine.getTerminal().getP()) ? danglingLine.getTerminal().getP() : 0;
    }

    private double getLeavingFlow(Line line) {
        double flowSide1 = line.getTerminal1().isConnected() && !Double.isNaN(line.getTerminal1().getP()) ? line.getTerminal1().getP() : 0;
        double flowSide2 = line.getTerminal2().isConnected() && !Double.isNaN(line.getTerminal2().getP()) ? line.getTerminal2().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return countries.contains(line.getTerminal1().getVoltageLevel().getSubstation().getNullableCountry()) ? directFlow : -directFlow;
    }

    private double getLeavingFlow(HvdcLine hvdcLine) {
        double flowSide1 = hvdcLine.getConverterStation1().getTerminal().isConnected() && !Double.isNaN(hvdcLine.getConverterStation1().getTerminal().getP()) ? hvdcLine.getConverterStation1().getTerminal().getP() : 0;
        double flowSide2 = hvdcLine.getConverterStation2().getTerminal().isConnected() && !Double.isNaN(hvdcLine.getConverterStation2().getTerminal().getP()) ? hvdcLine.getConverterStation2().getTerminal().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return countries.contains(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().getNullableCountry()) ? directFlow : -directFlow;
    }
}

