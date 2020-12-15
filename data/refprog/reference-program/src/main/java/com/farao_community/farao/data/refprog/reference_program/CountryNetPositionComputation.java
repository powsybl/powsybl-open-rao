/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.refprog.reference_program;

import com.powsybl.iidm.network.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CountryNetPositionComputation {

    private Network network;
    private Map<ReferenceProgramArea, Double> netPositions;

    public CountryNetPositionComputation(Network network) {
        this.network = network;
    }

    public Map<ReferenceProgramArea, Double> getNetPositions() {
        if (Objects.isNull(netPositions)) {
            computeNetPositions();
        }
        return netPositions;
    }

    private void computeNetPositions() {
        netPositions = new HashMap<>();

        network.getDanglingLineStream().forEach(danglingLine -> {
            ReferenceProgramArea referenceProgramArea = new ReferenceProgramArea(danglingLine.getTerminal().getVoltageLevel().getSubstation().getNullableCountry());
            addLeavingFlow(danglingLine, referenceProgramArea);
        });

        network.getLineStream().forEach(line -> {
            ReferenceProgramArea referenceProgramAreaSide1 = new ReferenceProgramArea(line.getTerminal1().getVoltageLevel().getSubstation().getNullableCountry());
            ReferenceProgramArea referenceProgramAreaSide2 = new ReferenceProgramArea(line.getTerminal2().getVoltageLevel().getSubstation().getNullableCountry());
            if (referenceProgramAreaSide1.equals(referenceProgramAreaSide2)) {
                return;
            }
            addLeavingFlow(line, referenceProgramAreaSide1);
            addLeavingFlow(line, referenceProgramAreaSide2);
        });

        network.getHvdcLineStream().forEach(hvdcLine -> {
            ReferenceProgramArea referenceProgramAreaSide1 = new ReferenceProgramArea(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().getNullableCountry());
            ReferenceProgramArea referenceProgramAreaSide2 = new ReferenceProgramArea(hvdcLine.getConverterStation2().getTerminal().getVoltageLevel().getSubstation().getNullableCountry());
            if (referenceProgramAreaSide1.equals(referenceProgramAreaSide2)) {
                return;
            }
            addLeavingFlow(hvdcLine, referenceProgramAreaSide1);
            addLeavingFlow(hvdcLine, referenceProgramAreaSide2);
        });
    }

    private void addLeavingFlow(DanglingLine danglingLine, ReferenceProgramArea referenceProgramArea) {
        Double previousValue = getPreviousValue(referenceProgramArea);
        if (!Objects.isNull(referenceProgramArea)) {
            netPositions.put(referenceProgramArea, previousValue + getLeavingFlow(danglingLine));
        }
    }

    private Double getPreviousValue(ReferenceProgramArea referenceProgramArea) {
        Double previousValue;
        if (netPositions.get(referenceProgramArea) != null) {
            previousValue = netPositions.get(referenceProgramArea);
        } else {
            previousValue = (double) 0;
        }
        return previousValue;
    }

    private void addLeavingFlow(Line line, ReferenceProgramArea referenceProgramArea) {
        Double previousValue = getPreviousValue(referenceProgramArea);
        if (!Objects.isNull(referenceProgramArea)) {
            netPositions.put(referenceProgramArea, previousValue + getLeavingFlow(line, referenceProgramArea));
        }
    }

    private void addLeavingFlow(HvdcLine hvdcLine, ReferenceProgramArea referenceProgramArea) {
        Double previousValue = getPreviousValue(referenceProgramArea);
        if (!Objects.isNull(referenceProgramArea)) {
            netPositions.put(referenceProgramArea, previousValue + getLeavingFlow(hvdcLine, referenceProgramArea));
        }
    }

    private double getLeavingFlow(DanglingLine danglingLine) {
        return danglingLine.getTerminal().isConnected() && !Double.isNaN(danglingLine.getTerminal().getP()) ? danglingLine.getTerminal().getP() : 0;
    }

    private double getLeavingFlow(Line line, ReferenceProgramArea referenceProgramArea) {
        double flowSide1 = line.getTerminal1().isConnected() && !Double.isNaN(line.getTerminal1().getP()) ? line.getTerminal1().getP() : 0;
        double flowSide2 = line.getTerminal2().isConnected() && !Double.isNaN(line.getTerminal2().getP()) ? line.getTerminal2().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return referenceProgramArea.equals(new ReferenceProgramArea(line.getTerminal1().getVoltageLevel().getSubstation().getNullableCountry())) ? directFlow : -directFlow;
    }

    private double getLeavingFlow(HvdcLine hvdcLine, ReferenceProgramArea referenceProgramArea) {
        double flowSide1 = hvdcLine.getConverterStation1().getTerminal().isConnected() && !Double.isNaN(hvdcLine.getConverterStation1().getTerminal().getP()) ? hvdcLine.getConverterStation1().getTerminal().getP() : 0;
        double flowSide2 = hvdcLine.getConverterStation2().getTerminal().isConnected() && !Double.isNaN(hvdcLine.getConverterStation2().getTerminal().getP()) ? hvdcLine.getConverterStation2().getTerminal().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return referenceProgramArea.equals(new ReferenceProgramArea(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().getNullableCountry())) ? directFlow : -directFlow;
    }
}
