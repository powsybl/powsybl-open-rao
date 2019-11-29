/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.model;

import com.farao_community.farao.data.crac_file.xlsx.annotation.ExcelColumn;
import com.farao_community.farao.data.crac_file.xlsx.converter.ActivationConverter;
import com.farao_community.farao.data.crac_file.xlsx.converter.ElementDescriptionModeConverter;
import lombok.Builder;
import lombok.Getter;

/**
 * Excel Contingency elements
 */
@Builder
@Getter
public class ContingencyElementXlsx {

    @ExcelColumn(name = "Unique CO name", position = 0)
    private final String uniqueCOName;
    @ExcelColumn(name = "Activation", position = 1, convertorClass = ActivationConverter.class)
    private final Activation activation;
    @ExcelColumn(name = "Element Description Mode", position = 2, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode descriptionModeTimeseries1;
    @ExcelColumn(name = "UCT Node From", position = 3)
    private final String uctNodeFromTimeseries1;
    @ExcelColumn(name = "UCT Node To", position = 4)
    private final String uctNodeToTimeseries1;
    @ExcelColumn(name = "Order Code / Element Name", position = 5)
    private final String orderCodeOrElementNameTimeseries1;
    @ExcelColumn(name = "Element Description Mode", position = 6, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode descriptionModeTimeseries2;
    @ExcelColumn(name = "UCT Node From", position = 7)
    private final String uctNodeFromTimeseries2;
    @ExcelColumn(name = "UCT Node To", position = 8)
    private final String uctNodeToTimeseries2;
    @ExcelColumn(name = "Order Code / Element Name", position = 9)
    private final String orderCodeOrElementNameTimeseries2;
    @ExcelColumn(name = "Element Description Mode", position = 10, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode descriptionModeTimeseries3;
    @ExcelColumn(name = "UCT Node From", position = 11)
    private final String uctNodeFromTimeseries3;
    @ExcelColumn(name = "UCT Node To", position = 12)
    private final String uctNodeToTimeseries3;
    @ExcelColumn(name = "Order Code / Element Name", position = 13)
    private final String orderCodeOrElementNameTimeseries3;

    public ContingencyElementXlsx() {
        this(null, null, null, null,
                null, null, null,
                null, null, null,
                null, null, null, null);
    }

    public ContingencyElementXlsx(String uniqueCOName, Activation activation, ElementDescriptionMode descriptionModeTimeseries1,
                                    String uctNodeFromTimeseries1, String uctNodeToTimeseries1, String orderCodeOrElementNameTimeseries1,
                                    ElementDescriptionMode descriptionModeTimeseries2, String uctNodeFromTimeseries2,
                                    String uctNodeToTimeseries2, String orderCodeOrElementNameTimeseries2,
                                    ElementDescriptionMode descriptionModeTimeseries3, String uctNodeFromTimeseries3,
                                    String uctNodeToTimeseries3, String orderCodeOrElementNameTimeseries3) {
        this.uniqueCOName = uniqueCOName;
        this.activation = activation;
        this.descriptionModeTimeseries1 = descriptionModeTimeseries1;
        this.uctNodeFromTimeseries1 = uctNodeFromTimeseries1;
        this.uctNodeToTimeseries1 = uctNodeToTimeseries1;
        this.orderCodeOrElementNameTimeseries1 = orderCodeOrElementNameTimeseries1;
        this.descriptionModeTimeseries2 = descriptionModeTimeseries2;
        this.uctNodeFromTimeseries2 = uctNodeFromTimeseries2;
        this.uctNodeToTimeseries2 = uctNodeToTimeseries2;
        this.orderCodeOrElementNameTimeseries2 = orderCodeOrElementNameTimeseries2;
        this.descriptionModeTimeseries3 = descriptionModeTimeseries3;
        this.uctNodeFromTimeseries3 = uctNodeFromTimeseries3;
        this.uctNodeToTimeseries3 = uctNodeToTimeseries3;
        this.orderCodeOrElementNameTimeseries3 = orderCodeOrElementNameTimeseries3;
    }

    @Override
    public String toString() {
        return this.uniqueCOName;
    }

}
