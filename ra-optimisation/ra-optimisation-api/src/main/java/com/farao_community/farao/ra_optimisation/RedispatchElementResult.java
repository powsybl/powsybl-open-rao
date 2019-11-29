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
public class RedispatchElementResult extends RemedialActionElementResult {
    private final double preOptimisationTargetP;
    private final double postOptimisationTargetP;
    private final double redispatchCost;

    @ConstructorProperties({"id", "preOptimisationTargetP", "postOptimisationTargetP", "redispatchCost"})
    public RedispatchElementResult(final String id,
                                   final double preOptimisationTargetP,
                                   final double postOptimisationTargetP,
                                   final double redispatchCost) {
        super(id);
        this.preOptimisationTargetP = preOptimisationTargetP;
        this.postOptimisationTargetP = postOptimisationTargetP;
        this.redispatchCost = redispatchCost;
    }

    public double getPreOptimisationTargetP() {
        return preOptimisationTargetP;
    }

    public double getPostOptimisationTargetP() {
        return postOptimisationTargetP;
    }

    public double getRedispatchCost() {
        return redispatchCost;
    }
}
