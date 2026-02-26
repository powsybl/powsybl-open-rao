/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.refprog.referenceprogram;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.openrao.commons.EICode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CountryNetPositionComputation {

    private Network network;
    private Map<EICode, Double> netPositions;

    public CountryNetPositionComputation(Network network) {
        this.network = network;
    }

    public Map<EICode, Double> getNetPositions() {
        if (Objects.isNull(netPositions)) {
            computeNetPositions();
        }
        return netPositions;
    }

    private void computeNetPositions() {
        netPositions = new HashMap<>();

        network.getDanglingLineStream().forEach(danglingLine -> {
            EICode area = new EICode(getSubstationNullableCountry(danglingLine.getTerminal().getVoltageLevel().getSubstation()));
            addLeavingFlow(danglingLine, area);
        });

        network.getLineStream().forEach(line -> {
            EICode areaSide1 = new EICode(getSubstationNullableCountry(line.getTerminal1().getVoltageLevel().getSubstation()));
            EICode areaSide2 = new EICode(getSubstationNullableCountry(line.getTerminal2().getVoltageLevel().getSubstation()));
            if (areaSide1.equals(areaSide2)) {
                return;
            }
            addLeavingFlow(line, areaSide1);
            addLeavingFlow(line, areaSide2);
        });

        network.getHvdcLineStream().forEach(hvdcLine -> {
            EICode areaSide1 = new EICode(getSubstationNullableCountry(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation()));
            EICode areaSide2 = new EICode(getSubstationNullableCountry(hvdcLine.getConverterStation2().getTerminal().getVoltageLevel().getSubstation()));
            if (areaSide1.equals(areaSide2)) {
                return;
            }
            addLeavingFlow(hvdcLine, areaSide1);
            addLeavingFlow(hvdcLine, areaSide2);
        });
    }

    private Country getSubstationNullableCountry(Optional<Substation> substation) {
        if (substation.isPresent()) {
            return substation.get().getNullableCountry();
        } else {
            return null;
        }
    }

    private void addLeavingFlow(DanglingLine danglingLine, EICode area) {
        Double previousValue = getPreviousValue(area);
        if (!Objects.isNull(area)) {
            netPositions.put(area, previousValue + getLeavingFlow(danglingLine));
        }
    }

    private Double getPreviousValue(EICode area) {
        Double previousValue;
        if (netPositions.get(area) != null) {
            previousValue = netPositions.get(area);
        } else {
            previousValue = (double) 0;
        }
        return previousValue;
    }

    private void addLeavingFlow(Line line, EICode area) {
        Double previousValue = getPreviousValue(area);
        if (!Objects.isNull(area)) {
            netPositions.put(area, previousValue + getLeavingFlow(line, area));
        }
    }

    private void addLeavingFlow(HvdcLine hvdcLine, EICode area) {
        Double previousValue = getPreviousValue(area);
        if (!Objects.isNull(area)) {
            netPositions.put(area, previousValue + getLeavingFlow(hvdcLine, area));
        }
    }

    private double getLeavingFlow(DanglingLine danglingLine) {
        return danglingLine.getTerminal().isConnected() && !Double.isNaN(danglingLine.getTerminal().getP()) ? danglingLine.getTerminal().getP() : 0;
    }

    private double getLeavingFlow(Line line, EICode area) {
        double flowSide1 = line.getTerminal1().isConnected() && !Double.isNaN(line.getTerminal1().getP()) ? line.getTerminal1().getP() : 0;
        double flowSide2 = line.getTerminal2().isConnected() && !Double.isNaN(line.getTerminal2().getP()) ? line.getTerminal2().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return area.equals(new EICode(getSubstationNullableCountry(line.getTerminal1().getVoltageLevel().getSubstation()))) ? directFlow : -directFlow;
    }

    private double getLeavingFlow(HvdcLine hvdcLine, EICode area) {
        double flowSide1 = hvdcLine.getConverterStation1().getTerminal().isConnected()
            && !Double.isNaN(hvdcLine.getConverterStation1().getTerminal().getP()) ?
                hvdcLine.getConverterStation1().getTerminal().getP() : 0;
        double flowSide2 = hvdcLine.getConverterStation2().getTerminal().isConnected()
            && !Double.isNaN(hvdcLine.getConverterStation2().getTerminal().getP()) ?
                hvdcLine.getConverterStation2().getTerminal().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return area.equals(new EICode(getSubstationNullableCountry(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation()))) ?
            directFlow : -directFlow;
    }
}
