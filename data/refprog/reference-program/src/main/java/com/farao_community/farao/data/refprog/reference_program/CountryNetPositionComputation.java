/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.refprog.reference_program;

import com.farao_community.farao.commons.EICode;
import com.powsybl.iidm.network.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
            EICode area = new EICode(danglingLine.getTerminal().getVoltageLevel().getSubstation().getNullableCountry());
            addLeavingFlow(danglingLine, area);
        });

        network.getLineStream().forEach(line -> {
            EICode areaSide1 = new EICode(line.getTerminal1().getVoltageLevel().getSubstation().getNullableCountry());
            EICode areaSide2 = new EICode(line.getTerminal2().getVoltageLevel().getSubstation().getNullableCountry());
            if (areaSide1.equals(areaSide2)) {
                return;
            }
            addLeavingFlow(line, areaSide1);
            addLeavingFlow(line, areaSide2);
        });

        network.getHvdcLineStream().forEach(hvdcLine -> {
            EICode areaSide1 = new EICode(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().getNullableCountry());
            EICode areaSide2 = new EICode(hvdcLine.getConverterStation2().getTerminal().getVoltageLevel().getSubstation().getNullableCountry());
            if (areaSide1.equals(areaSide2)) {
                return;
            }
            addLeavingFlow(hvdcLine, areaSide1);
            addLeavingFlow(hvdcLine, areaSide2);
        });
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
        return area.equals(new EICode(line.getTerminal1().getVoltageLevel().getSubstation().getNullableCountry())) ? directFlow : -directFlow;
    }

    private double getLeavingFlow(HvdcLine hvdcLine, EICode area) {
        double flowSide1 = hvdcLine.getConverterStation1().getTerminal().isConnected() && !Double.isNaN(hvdcLine.getConverterStation1().getTerminal().getP()) ? hvdcLine.getConverterStation1().getTerminal().getP() : 0;
        double flowSide2 = hvdcLine.getConverterStation2().getTerminal().isConnected() && !Double.isNaN(hvdcLine.getConverterStation2().getTerminal().getP()) ? hvdcLine.getConverterStation2().getTerminal().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return area.equals(new EICode(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().getNullableCountry())) ? directFlow : -directFlow;
    }
}
