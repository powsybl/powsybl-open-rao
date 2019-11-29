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
public class RedispatchingRemedialActionXlsx {
    @ExcelColumn(name = "RA RD ID", position = 0)
    private final String raRdId;
    @ExcelColumn(name = "TSO", position = 1, convertorClass = TsoConverter.class)
    private final Tso tso;
    @ExcelColumn(name = "Generator Name", position = 2)
    private final String generatorName;
    @ExcelColumn(name = "UCT Node or GSK ID", position = 3)
    private final String uctNodeOrGsk;
    @ExcelColumn(name = "Fuel type", position = 4, convertorClass = FuelTypeConverter.class)
    private final FuelType fuelType;
    @ExcelColumn(name = "Minimum Power [MW]", position = 5, convertorClass = FloatConverter.class)
    private final double minimumPower;
    @ExcelColumn(name = "Maximum Power [MW]", position = 6, convertorClass = FloatConverter.class)
    private final double maximumPower;
    @ExcelColumn(name = "Minimum Redispatch [MW]", position = 7, convertorClass = FloatConverter.class)
    private final double minimumRedispatch;
    @ExcelColumn(name = "Minimum up-time [h]", position = 8, convertorClass = FloatConverter.class)
    private final double minimumUpTime;
    @ExcelColumn(name = "Minimum down-time [h]", position = 9, convertorClass = FloatConverter.class)
    private final double minimumDownTime;
    @ExcelColumn(name = "Maximum positive power gradient [MW/h]", position = 10, convertorClass = FloatConverter.class)
    private final double maximumPositivePowerGradient;
    @ExcelColumn(name = "Maximum negative power gradient [MW/h]", position = 11, convertorClass = FloatConverter.class)
    private final double maximumNegativePowerGradient;
    @ExcelColumn(name = "Lead time [h]", position = 12, convertorClass = FloatConverter.class)
    private final double leadTime;
    @ExcelColumn(name = "Lag time [h]", position = 13, convertorClass = FloatConverter.class)
    private final double lagTime;
    @ExcelColumn(name = "Startup allowed", position = 14, convertorClass = ActivationConverter.class)
    private final Activation startupAllowed;
    @ExcelColumn(name = "Shutdown allowed", position = 15, convertorClass = ActivationConverter.class)
    private final Activation shutdownAllowed;
    @ExcelColumn(name = "Marginal costs [€/MWh]", position = 16, convertorClass = FloatConverter.class)
    private final double marginalCosts;
    @ExcelColumn(name = "Startup costs [€/start]", position = 17, convertorClass = FloatConverter.class)
    private final double startupCosts;
    @ExcelColumn(name = "Penalty costs [€/MWh]", position = 18, convertorClass = FloatConverter.class)
    private final double penaltyCosts;
    @ExcelColumn(name = "Group constraint ID", position = 19)
    private final String groupeConstraintId;
    @ExcelColumn(name = "Is RES", position = 20, convertorClass = ActivationConverter.class)
    private final Activation isRes;
    @ExcelColumn(name = "Activation", position = 21, convertorClass = ActivationConverter.class)
    private final Activation activation;
    @ExcelColumn(name = "Preventive", position = 22, convertorClass = ActivationConverter.class)
    private final Activation preventive;
    @ExcelColumn(name = "Curative", position = 23, convertorClass = ActivationConverter.class)
    private final Activation curative;
    @ExcelColumn(name = "Sharing Definition", position = 24, convertorClass = SharingDefinitionConverter.class)
    private final SharingDefinition sharingDefinition;
    @ExcelColumn(name = "connected CBCO", position = 25)
    private final String connectedCbco;

    public RedispatchingRemedialActionXlsx() {
        this(null, null, null, null, null, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, null, null, 0, 0, 0, null, null, null, null, null, null, null);

    }

    public RedispatchingRemedialActionXlsx(
            String raRdId,
            Tso tso,
            String generatorName,
            String uctNodeOrGsk,
            FuelType fuelType,
            double minimumPower,
            double maximumPower,
            double minimumRedispatch,
            double minimumUpTime,
            double minimumDownTime,
            double maximumPositivePowerGradient,
            double maximumNegativePowerGradient,
            double leadTime,
            double lagTime,
            Activation startupAllowed,
            Activation shutdownAllowed,
            double marginalCosts,
            double startupCosts,
            double penaltyCosts,
            String groupeConstraintId,
            Activation isRes,
            Activation activation,
            Activation preventive,
            Activation curative,
            SharingDefinition sharingDefinition,
            String connectedCbco) {
        this.raRdId = raRdId;
        this.tso = tso;
        this.generatorName = generatorName;
        this.uctNodeOrGsk = uctNodeOrGsk;
        this.fuelType = fuelType;
        this.minimumPower = minimumPower;
        this.maximumPower = maximumPower;
        this.minimumRedispatch = minimumRedispatch;
        this.minimumUpTime = minimumUpTime;
        this.minimumDownTime = minimumDownTime;
        this.maximumPositivePowerGradient = maximumPositivePowerGradient;
        this.maximumNegativePowerGradient = maximumNegativePowerGradient;
        this.leadTime = leadTime;
        this.lagTime = lagTime;
        this.startupAllowed = startupAllowed;
        this.shutdownAllowed = shutdownAllowed;
        this.marginalCosts = marginalCosts;
        this.startupCosts = startupCosts;
        this.penaltyCosts = penaltyCosts;
        this.groupeConstraintId = groupeConstraintId;
        this.isRes = isRes;
        this.activation = activation;
        this.preventive = preventive;
        this.curative = curative;
        this.sharingDefinition = sharingDefinition;
        this.connectedCbco = connectedCbco;
    }

    @Override
    public String toString() {
        return "RedispatchingRemedialActionXlsx{" +
                "raRdId='" + raRdId + '\'' +
                ", tso=" + tso +
                ", generatorName='" + generatorName + '\'' +
                ", uctNodeOrGsk=" + uctNodeOrGsk +
                ", fuelType=" + fuelType +
                ", minimumPower=" + minimumPower +
                ", maximumPower=" + maximumPower +
                ", minimumRedispatch=" + minimumRedispatch +
                ", minimumUpTime=" + minimumUpTime +
                ", minimumDownTime=" + minimumDownTime +
                ", maximumPositivePowerGradient=" + maximumPositivePowerGradient +
                ", maximumNegativePowerGradient=" + maximumNegativePowerGradient +
                ", leadTime=" + leadTime +
                ", lagTime=" + lagTime +
                ", startupAllowed=" + startupAllowed +
                ", shutdownAllowed=" + shutdownAllowed +
                ", marginalCosts=" + marginalCosts +
                ", startupCosts=" + startupCosts +
                ", penaltyCosts=" + penaltyCosts +
                ", groupeConstraintId='" + groupeConstraintId + '\'' +
                ", isRes=" + isRes +
                ", activation=" + activation +
                ", preventive=" + preventive +
                ", curative=" + curative +
                ", sharingDefinition=" + sharingDefinition +
                ", connectedCbco='" + connectedCbco + '\'' +
                '}';
    }
}
