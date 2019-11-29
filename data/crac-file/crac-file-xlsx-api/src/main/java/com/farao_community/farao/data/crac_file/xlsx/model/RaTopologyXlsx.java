/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.model;

import com.farao_community.farao.data.crac_file.xlsx.annotation.ExcelColumn;
import com.farao_community.farao.data.crac_file.xlsx.converter.*;
import lombok.Builder;
import lombok.Getter;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@Builder
@Getter
public class RaTopologyXlsx {
    @ExcelColumn(name = "Name of the RA", position = 0)
    private final String name;

    @ExcelColumn(name = "TSO", position = 1, convertorClass = TsoConverter.class)
    private final Tso tso;

    @ExcelColumn(name = "Activation", position = 2, convertorClass = ActivationConverter.class)
    private final Activation activation;

    @ExcelColumn(name = "Penalty costs [€/RA]", position = 3, convertorClass = FloatConverter.class)
    private final float penaltyCost;

    @ExcelColumn(name = "max. Switching Actions", position = 4, convertorClass = IntConverter.class)
    private final int maxSwitchingPosition;

    @ExcelColumn(name = "Preventive", position = 5, convertorClass = ActivationConverter.class)
    private final Activation preventive;

    @ExcelColumn(name = "Curative", position = 6, convertorClass = ActivationConverter.class)
    private final Activation curative;

    @ExcelColumn(name = "SPS mode", position = 7, convertorClass = ActivationConverter.class)
    private final Activation spsMode;

    @ExcelColumn(name = "connected SPS CO", position = 8)
    private final String spsConnectedCo;

    @ExcelColumn(name = "Sharing Definition", position = 9, convertorClass = SharingDefinitionConverter.class)
    private final SharingDefinition sharingDefinition;

    @ExcelColumn(name = "Connected CBCO", position = 10)
    private final String connectedCbco;

    @ExcelColumn(name = "Element Description Mode", position = 11, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode elementDescriptionMode1;

    @ExcelColumn(name = "UCT Node From", position = 12)
    private final String uctNodeFrom1;

    @ExcelColumn(name = "UCT Node To", position = 13)
    private final String uctNodeTo1;

    @ExcelColumn(name = "Order Code / Element Name", position = 14)
    private final String orderCodeElementName1;

    @ExcelColumn(name = "Status", position = 15, convertorClass = StatusConverter.class)
    private final Status status1;

    @ExcelColumn(name = "Element Description Mode", position = 16, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode elementDescriptionMode2;

    @ExcelColumn(name = "UCT Node From", position = 17)
    private final String uctNodeFrom2;

    @ExcelColumn(name = "UCT Node To", position = 18)
    private final String uctNodeTo2;

    @ExcelColumn(name = "Order Code / Element Name", position = 19)
    private final String orderCodeElementName2;

    @ExcelColumn(name = "Status", position = 20, convertorClass = StatusConverter.class)
    private final Status status2;

    @ExcelColumn(name = "Element Description Mode", position = 21, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode elementDescriptionMode3;

    @ExcelColumn(name = "UCT Node From", position = 22)
    private final String uctNodeFrom3;

    @ExcelColumn(name = "UCT Node To", position = 23)
    private final String uctNodeTo3;

    @ExcelColumn(name = "Order Code / Element Name", position = 24)
    private final String orderCodeElementName3;

    @ExcelColumn(name = "Status", position = 25, convertorClass = StatusConverter.class)
    private final Status status3;

    @ExcelColumn(name = "Element Description Mode", position = 26, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode elementDescriptionMode4;

    @ExcelColumn(name = "UCT Node From", position = 27)
    private final String uctNodeFrom4;

    @ExcelColumn(name = "UCT Node To", position = 28)
    private final String uctNodeTo4;

    @ExcelColumn(name = "Order Code / Element Name", position = 29)
    private final String orderCodeElementName4;

    @ExcelColumn(name = "Status", position = 30, convertorClass = StatusConverter.class)
    private final Status status4;

    @ExcelColumn(name = "Element Description Mode", position = 31, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode elementDescriptionMode5;

    @ExcelColumn(name = "UCT Node From", position = 32)
    private final String uctNodeFrom5;

    @ExcelColumn(name = "UCT Node To", position = 33)
    private final String uctNodeTo5;

    @ExcelColumn(name = "Order Code / Element Name", position = 34)
    private final String orderCodeElementName5;

    @ExcelColumn(name = "Status", position = 35, convertorClass = StatusConverter.class)
    private final Status status5;

    @ExcelColumn(name = "Element Description Mode", position = 36, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode elementDescriptionMode6;

    @ExcelColumn(name = "UCT Node From", position = 37)
    private final String uctNodeFrom6;

    @ExcelColumn(name = "UCT Node To", position = 38)
    private final String uctNodeTo6;

    @ExcelColumn(name = "Order Code / Element Name", position = 39)
    private final String orderCodeElementName6;

    @ExcelColumn(name = "Status", position = 40, convertorClass = StatusConverter.class)
    private final Status status6;

    @ExcelColumn(name = "Element Description Mode", position = 41, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode elementDescriptionMode7;

    @ExcelColumn(name = "UCT Node From", position = 42)
    private final String uctNodeFrom7;

    @ExcelColumn(name = "UCT Node To", position = 43)
    private final String uctNodeTo7;

    @ExcelColumn(name = "Order Code / Element Name", position = 44)
    private final String orderCodeElementName7;

    @ExcelColumn(name = "Status", position = 45, convertorClass = StatusConverter.class)
    private final Status status7;

    @ExcelColumn(name = "Element Description Mode", position = 46, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode elementDescriptionMode8;

    @ExcelColumn(name = "UCT Node From", position = 47)
    private final String uctNodeFrom8;

    @ExcelColumn(name = "UCT Node To", position = 48)
    private final String uctNodeTo8;

    @ExcelColumn(name = "Order Code / Element Name", position = 49)
    private final String orderCodeElementName8;

    @ExcelColumn(name = "Status", position = 50, convertorClass = StatusConverter.class)
    private final Status status8;

    public RaTopologyXlsx() {
        this(null, null, null, 0, 0, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
    }

    public RaTopologyXlsx(
            String name, Tso tso, Activation activation, float penaltyCost, int maxSwitchingPosition,
            Activation preventive, Activation curative, Activation spsMode, String spsConnectedCo,
            SharingDefinition sharingDefinition, String connectedCbco,
            ElementDescriptionMode elementDescriptionMode1, String uctNodeFrom1, String uctNodeTo1, String orderCodeElementName1, Status status1,
            ElementDescriptionMode elementDescriptionMode2, String uctNodeFrom2, String uctNodeTo2, String orderCodeElementName2, Status status2,
            ElementDescriptionMode elementDescriptionMode3, String uctNodeFrom3, String uctNodeTo3, String orderCodeElementName3, Status status3,
            ElementDescriptionMode elementDescriptionMode4, String uctNodeFrom4, String uctNodeTo4, String orderCodeElementName4, Status status4,
            ElementDescriptionMode elementDescriptionMode5, String uctNodeFrom5, String uctNodeTo5, String orderCodeElementName5, Status status5,
            ElementDescriptionMode elementDescriptionMode6, String uctNodeFrom6, String uctNodeTo6, String orderCodeElementName6, Status status6,
            ElementDescriptionMode elementDescriptionMode7, String uctNodeFrom7, String uctNodeTo7, String orderCodeElementName7, Status status7,
            ElementDescriptionMode elementDescriptionMode8, String uctNodeFrom8, String uctNodeTo8, String orderCodeElementName8, Status status8) {
        this.name = name;
        this.tso = tso;
        this.activation = activation;
        this.penaltyCost = penaltyCost;
        this.maxSwitchingPosition = maxSwitchingPosition;
        this.preventive = preventive;
        this.curative = curative;
        this.spsMode = spsMode;
        this.spsConnectedCo = spsConnectedCo;
        this.sharingDefinition = sharingDefinition;
        this.connectedCbco = connectedCbco;
        this.elementDescriptionMode1 = elementDescriptionMode1;
        this.uctNodeFrom1 = uctNodeFrom1;
        this.uctNodeTo1 = uctNodeTo1;
        this.orderCodeElementName1 = orderCodeElementName1;
        this.status1 = status1;
        this.elementDescriptionMode2 = elementDescriptionMode2;
        this.uctNodeFrom2 = uctNodeFrom2;
        this.uctNodeTo2 = uctNodeTo2;
        this.orderCodeElementName2 = orderCodeElementName2;
        this.status2 = status2;
        this.elementDescriptionMode3 = elementDescriptionMode3;
        this.uctNodeFrom3 = uctNodeFrom3;
        this.uctNodeTo3 = uctNodeTo3;
        this.orderCodeElementName3 = orderCodeElementName3;
        this.status3 = status3;
        this.elementDescriptionMode4 = elementDescriptionMode4;
        this.uctNodeFrom4 = uctNodeFrom4;
        this.uctNodeTo4 = uctNodeTo4;
        this.orderCodeElementName4 = orderCodeElementName4;
        this.status4 = status4;
        this.elementDescriptionMode5 = elementDescriptionMode5;
        this.uctNodeFrom5 = uctNodeFrom5;
        this.uctNodeTo5 = uctNodeTo5;
        this.orderCodeElementName5 = orderCodeElementName5;
        this.status5 = status5;
        this.elementDescriptionMode6 = elementDescriptionMode6;
        this.uctNodeFrom6 = uctNodeFrom6;
        this.uctNodeTo6 = uctNodeTo6;
        this.orderCodeElementName6 = orderCodeElementName6;
        this.status6 = status6;
        this.elementDescriptionMode7 = elementDescriptionMode7;
        this.uctNodeFrom7 = uctNodeFrom7;
        this.uctNodeTo7 = uctNodeTo7;
        this.orderCodeElementName7 = orderCodeElementName7;
        this.status7 = status7;
        this.elementDescriptionMode8 = elementDescriptionMode8;
        this.uctNodeFrom8 = uctNodeFrom8;
        this.uctNodeTo8 = uctNodeTo8;
        this.orderCodeElementName8 = orderCodeElementName8;
        this.status8 = status8;
    }

    @Override
    public String toString() {
        return "RaTopologyXlsx{" +
                "name='" + name + "'" +
                ", tso=" + tso + "'" +
                ", activation=" + activation + "'" +
                ", penaltyCost=" + penaltyCost + "'" +
                ", maxSwitchingPosition=" + maxSwitchingPosition + "'" +
                ", preventive=" + preventive + "'" +
                ", curative=" + curative + "'" +
                ", spsMode=" + spsMode + "'" +
                ", spsConnectedCo=" + spsConnectedCo + "'" +
                ", sharingDefinition=" + sharingDefinition + "'" +
                ", connectedCbco=" + connectedCbco + "'" +
                ", elementDescriptionMode1=" + elementDescriptionMode1 + "'" +
                ", uctNodeFrom1=" + uctNodeFrom1 + "'" +
                ", uctNodeTo1=" + uctNodeTo1 + "'" +
                ", orderCodeElementName1=" + orderCodeElementName1 + "'" +
                ", status1=" + status1 + "'" +
                ", elementDescriptionMode2=" + elementDescriptionMode2 + "'" +
                ", uctNodeFrom2=" + uctNodeFrom2 + "'" +
                ", uctNodeTo2=" + uctNodeTo2 + "'" +
                ", orderCodeElementName2=" + orderCodeElementName2 + "'" +
                ", status2=" + status2 + "'" +
                ", elementDescriptionMode3=" + elementDescriptionMode3 + "'" +
                ", uctNodeFrom3=" + uctNodeFrom3 + "'" +
                ", uctNodeTo3=" + uctNodeTo3 + "'" +
                ", orderCodeElementName3=" + orderCodeElementName3 + "'" +
                ", status3=" + status3 + "'" +
                ", elementDescriptionMode4=" + elementDescriptionMode4 + "'" +
                ", uctNodeFrom4=" + uctNodeFrom4 + "'" +
                ", uctNodeTo4=" + uctNodeTo4 + "'" +
                ", orderCodeElementName4=" + orderCodeElementName4 + "'" +
                ", status4=" + status4 + "'" +
                ", elementDescriptionMode5=" + elementDescriptionMode5 + "'" +
                ", uctNodeFrom5=" + uctNodeFrom5 + "'" +
                ", uctNodeTo5=" + uctNodeTo5 + "'" +
                ", orderCodeElementName5=" + orderCodeElementName5 + "'" +
                ", status5=" + status5 + "'" +
                ", elementDescriptionMode6=" + elementDescriptionMode6 + "'" +
                ", uctNodeFrom6=" + uctNodeFrom6 + "'" +
                ", uctNodeTo6=" + uctNodeTo6 + "'" +
                ", orderCodeElementName6=" + orderCodeElementName6 + "'" +
                ", status6=" + status6 + "'" +
                ", elementDescriptionMode7=" + elementDescriptionMode7 + "'" +
                ", uctNodeFrom7=" + uctNodeFrom7 + "'" +
                ", uctNodeTo7=" + uctNodeTo7 + "'" +
                ", orderCodeElementName7=" + orderCodeElementName7 + "'" +
                ", status7=" + status7 + "'" +
                ", elementDescriptionMode8=" + elementDescriptionMode8 + "'" +
                ", uctNodeFrom8=" + uctNodeFrom8 + "'" +
                ", uctNodeTo8=" + uctNodeTo8 + "'" +
                ", orderCodeElementName8=" + orderCodeElementName8 + "'" +
                ", status8=" + status8 + "'" +
                '}';
    }
}
