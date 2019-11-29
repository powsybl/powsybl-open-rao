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
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY)
public class PstElement extends RemedialActionElement {

    @NotNull(message = "typeOfLimit")
    private final TypeOfLimit typeOfLimit;
    private final int minStepRange;
    private final int maxStepRange;
    private final double penaltyCost;

    @ConstructorProperties({"id", "typeOfLimit", "minStepRange", "maxStepRange", "penaltyCost"})
    @Builder
    public PstElement(String id, TypeOfLimit typeOfLimit, int minStepRange, int maxStepRange, double penaltyCost) {
        super(id);
        this.typeOfLimit = typeOfLimit;
        this.minStepRange = checkMin(minStepRange, maxStepRange);
        this.maxStepRange = checkMax(minStepRange, maxStepRange);
        this.penaltyCost = penaltyCost;
    }

    private int checkMin(int min, int max) {
        if (min >= max) {
            return max;
        }
        return min;
    }

    private int checkMax(int min, int max) {
        if (min >= max) {
            return min;
        }
        return max;
    }
}
