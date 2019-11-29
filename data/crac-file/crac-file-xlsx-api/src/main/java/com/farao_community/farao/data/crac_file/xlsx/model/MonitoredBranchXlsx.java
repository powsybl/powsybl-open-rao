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
import com.farao_community.farao.data.crac_file.xlsx.validator.UCTNodeValidater;
import lombok.Builder;
import lombok.Getter;

/**
 * 3.4.4. Branch ID
 * <p>
 * Just like contingency elements, monitored branches are referenced by the content of for columns:
 * <p>
 * Element Description Mode
 * UCT Node From
 * UCT Node To
 * Order Code / Element Name
 * <p>
 * There are two possibilities:
 * <p>
 * If "Element Description Mode" column content is "Element Name" then the monitored branch "branchId" will be equal to the content of "Order Code / Element Name" column.
 * If "Element Description Mode" column content is "Order Code" then themonitored branch "branchId" will be equal to "{@literal <XXXXXXXX> <YYYYYYYY> <Z>}" where {@literal <XXXXXXXX>} is the exactly 8 character long content of "UCT Node From" column, {@literal <YYYYYYYY>} is the exactly 8 character long content of "UCT Node To" column and {@literal <Z>} is the content of "Order Code / Element Name" column.
 * <p>
 * 3.4.5. Fmax
 * <p>
 * The monitored branch' Fmax value is equal to the content of the CRAC file's timestamp column and monitored branch' ID row in "Branch_Timeseries".
 */
@Builder
@Getter
public class MonitoredBranchXlsx {
    private final String branchId;
    @ExcelColumn(name = "Unique CBCO Name", position = 0)
    private final String uniqueCbcoName;
    @ExcelColumn(name = "TSO", position = 1, convertorClass = TsoConverter.class)
    private final Tso tso;
    @ExcelColumn(name = "Activation", position = 2, convertorClass = ActivationConverter.class)
    private final Activation activation;
    @ExcelColumn(name = "Element Description Mode", position = 3, convertorClass = ElementDescriptionModeConverter.class)
    private final ElementDescriptionMode descriptionMode;
    @ExcelColumn(name = "UCT Node From", position = 4, validatorClass = UCTNodeValidater.class)
    private final String uctNodeFrom;
    @ExcelColumn(name = "UCT Node To", position = 5, validatorClass = UCTNodeValidater.class)
    private final String uctNodeTo;
    @ExcelColumn(name = "Order Code / Element Name", position = 6)
    private final String orderCodeElementName;
    @ExcelColumn(position = 7)
    private final String uniqueCOName;
    @ExcelColumn(name = "Absolute / Relative constraint", position = 8, convertorClass = AbsoluteRelativeConstraintConverter.class)
    private final AbsoluteRelativeConstraint absoluteRelativeConstraint;
    @ExcelColumn(position = 9, convertorClass = FloatConverter.class)
    private final float penaltyCostsForviolations;

    public MonitoredBranchXlsx() {
        this(null, null, null, null, null,
                null, null, null, null,
                null, 0);
    }

    public MonitoredBranchXlsx(String branchId, String uniqueCbcoName, Tso tso, Activation activation,
                               ElementDescriptionMode descriptionMode, String uctNodeFrom, String uctNodeTo,
                               String orderCodeElementName, String uniqueCOName,
                               AbsoluteRelativeConstraint absoluteRelativeConstraint, float penaltyCostsForviolations) {
        this.branchId = branchId;
        this.uniqueCbcoName = uniqueCbcoName;
        this.tso = tso;
        this.activation = activation;
        this.descriptionMode = descriptionMode;
        this.uctNodeFrom = uctNodeFrom;
        this.uctNodeTo = uctNodeTo;
        this.orderCodeElementName = orderCodeElementName;
        this.uniqueCOName = uniqueCOName;
        this.absoluteRelativeConstraint = absoluteRelativeConstraint;
        this.penaltyCostsForviolations = penaltyCostsForviolations;
    }

    @Override
    public String toString() {
        return this.uniqueCbcoName;
    }
}
