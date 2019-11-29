/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;

/**
 * RedispatchRemedialActionElement Remedial Action element in the CRAC file
 *
 * @author Luc Di-Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RedispatchRemedialActionElement extends RemedialActionElement {
    @NotNull(message = "minimumPower")
    private final double minimumPower;
    @NotNull(message = "maximumPower")
    private final double maximumPower;
    private final double targetPower;
    private final double startupCost;
    private final double marginalCost;
    private final String generatorName;

    @ConstructorProperties({"id", "minimumPower", "maximumPower", "targetPower", "startupCost", "marginalCost", "generatorName"})
    @Builder
    public RedispatchRemedialActionElement(final String id,
                                           final double minimumPower,
                                           final double maximumPower,
                                           final double targetPower,
                                           final double startupCost,
                                           final double marginalCost,
                                           final String generatorName) {
        super(id);
        this.minimumPower = minimumPower;
        this.maximumPower = maximumPower;
        this.targetPower = targetPower;
        this.startupCost = startupCost;
        this.marginalCost = marginalCost;
        this.generatorName = generatorName;
    }
}
