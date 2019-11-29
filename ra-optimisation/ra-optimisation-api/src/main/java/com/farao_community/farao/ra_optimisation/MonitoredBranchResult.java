/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation;

import java.beans.ConstructorProperties;
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class MonitoredBranchResult {
    private final String id;
    private final String name;
    private final String branchId;
    private final double maximumFlow;
    private final double preOptimisationFlow;
    private final double postOptimisationFlow;

    @ConstructorProperties({"id", "name", "branchId", "maximumFlow", "preOptimisationFlow", "postOptimisationFlow"})
    public MonitoredBranchResult(final String id,
                                 final String name,
                                 final String branchId,
                                 final double maximumFlow,
                                 final double preOptimisationFlow,
                                 final double postOptimisationFlow) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.branchId = Objects.requireNonNull(branchId);
        this.maximumFlow = maximumFlow;
        this.preOptimisationFlow = preOptimisationFlow;
        this.postOptimisationFlow = postOptimisationFlow;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBranchId() {
        return branchId;
    }

    public double getMaximumFlow() {
        return maximumFlow;
    }

    public double getPreOptimisationFlow() {
        return preOptimisationFlow;
    }

    public double getPostOptimisationFlow() {
        return postOptimisationFlow;
    }
}
