/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.beans.ConstructorProperties;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY)
public class PstElementResult extends RemedialActionElementResult {
    private final double preOptimisationAngle;
    private final int preOptimisationTapPosition;
    private final double postOptimisationAngle;
    private final int postOptimisationTapPosition;

    @ConstructorProperties({"id", "preOptimisationAngle", "preOptimisationTapPosition", "postOptimisationAngle", "postOptimisationTapPosition"})
    public PstElementResult(final String id,
                            final double preOptimisationAngle,
                            final int preOptimisationTapPosition,
                            final double postOptimisationAngle,
                            final int postOptimisationTapPosition) {
        super(id);
        this.preOptimisationAngle = preOptimisationAngle;
        this.preOptimisationTapPosition = preOptimisationTapPosition;
        this.postOptimisationAngle = postOptimisationAngle;
        this.postOptimisationTapPosition = postOptimisationTapPosition;
    }

    public double getPreOptimisationAngle() {
        return preOptimisationAngle;
    }

    public int getPreOptimisationTapPosition() {
        return preOptimisationTapPosition;
    }

    public double getPostOptimisationAngle() {
        return postOptimisationAngle;
    }

    public int getPostOptimisationTapPosition() {
        return postOptimisationTapPosition;
    }
}
