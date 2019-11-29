/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.data.crac_file.xlsx.model;

import com.farao_community.farao.data.crac_file.xlsx.annotation.ExcelColumn;
import com.farao_community.farao.data.crac_file.xlsx.converter.*;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RaPstTap {
    @ExcelColumn(name = "Unique RA PST Name", position = 0)
    private final String uniqueRaPstTab;

    @ExcelColumn(name = "TSO", position = 1, convertorClass = TsoConverter.class)
    private final Tso tso;

    @ExcelColumn(name = "Activation", position = 2, convertorClass = ActivationConverter.class)
    private final Activation activation;

    @ExcelColumn(name = "Element Description Mode", position = 3, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode elementDescriptionMode;

    @ExcelColumn(name = "UCT Node From", position = 4)
    private final String uctNodeFrom;

    @ExcelColumn(name = "UCT Node To", position = 5)
    private final String uctNodeTo;

    @ExcelColumn(name = "Order Code / Element Name", position = 6)
    private final String ordercodeElementName;

    @ExcelColumn(name = "Angle Regulation", position = 7, convertorClass = ActivationConverter.class)
    private final Activation angleRegulation;

    @ExcelColumn(name = "Preventive", position = 8, convertorClass = ActivationConverter.class)
    private final Activation preventive;

    @ExcelColumn(name = "Curative", position = 9, convertorClass = ActivationConverter.class)
    private final Activation curative;

    @ExcelColumn(name = "Range", position = 10)
    private final String range;

    @ExcelColumn(name = "Penalty cost (EUR/tap)", position = 11, convertorClass = FloatConverter.class)
    private final float penaltyCost;

    @ExcelColumn(name = "Penalty Cost Tap Change (EUR/Delta tap h)", position = 12, convertorClass = FloatConverter.class)
    private final float penaltyCostTapChange;

    @ExcelColumn(name = "Penalty Cost Delta RA (EUR/Delta tap h)", position = 13, convertorClass = FloatConverter.class)
    private final float penaltyCostDeltaRa;

    @ExcelColumn(name = "Sharing Definition", position = 14, convertorClass = SharingDefinitionConverter.class)
    private final SharingDefinition sharingDefinition;

    @ExcelColumn(name = "Connected CBCO", position = 15)
    private final String connectedCbco;

    public RaPstTap() {
        this(null, null, null, null, null, null, null, null, null, null, null, 0, 0, 0, null, null);
    }

    @Override
    public String toString() {
        return "RaPstTap{" +
                "uniqueRaPstTab='" + uniqueRaPstTab + '\'' +
                ", tso=" + tso +
                ", activation=" + activation +
                ", elementDescriptionMode=" + elementDescriptionMode +
                ", uctNodeFrom='" + uctNodeFrom + '\'' +
                ", uctNodeTo='" + uctNodeTo + '\'' +
                ", ordercodeElementName='" + ordercodeElementName + '\'' +
                ", angleRegulation=" + angleRegulation +
                ", preventive=" + preventive +
                ", curative=" + curative +
                ", range='" + range + '\'' +
                ", penaltyCost=" + penaltyCost +
                ", penaltyCostTapChange=" + penaltyCostTapChange +
                ", penaltyCostDeltaRa=" + penaltyCostDeltaRa +
                ", sharingDefinition=" + sharingDefinition +
                ", connectedCbco='" + connectedCbco + '\'' +
                '}';
    }

    public RaPstTap(String uniqueRaPstTab, Tso tso, Activation activation, ElementDescriptionMode elementDescriptionMode, String uctNodeFrom, String uctNodeTo, String ordercodeElementName, Activation angleRegulation, Activation preventive, Activation curative, String range, float penaltyCost, float penaltyCostTapChange, float penaltyCostDeltaRa, SharingDefinition sharingDefinition, String connectedCbco) {
        this.uniqueRaPstTab = uniqueRaPstTab;
        this.tso = tso;
        this.activation = activation;
        this.elementDescriptionMode = elementDescriptionMode;
        this.uctNodeFrom = uctNodeFrom;
        this.uctNodeTo = uctNodeTo;
        this.ordercodeElementName = ordercodeElementName;
        this.angleRegulation = angleRegulation;
        this.preventive = preventive;
        this.curative = curative;
        this.range = range;
        this.penaltyCost = penaltyCost;
        this.penaltyCostTapChange = penaltyCostTapChange;
        this.penaltyCostDeltaRa = penaltyCostDeltaRa;
        this.sharingDefinition = sharingDefinition;
        this.connectedCbco = connectedCbco;
    }
}
