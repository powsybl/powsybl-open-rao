/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class RemedialActionResult {
    private final String id;
    private final String name;
    private final boolean applied;
    private final List<RemedialActionElementResult> remedialActionElementResults;

    @ConstructorProperties({"id", "name", "applied", "remedialActionElementResults"})
    public RemedialActionResult(final String id,
                                final String name,
                                final boolean applied,
                                final List<RemedialActionElementResult> remedialActionElementResults) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.applied = applied;
        this.remedialActionElementResults = Objects.requireNonNull(remedialActionElementResults);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isApplied() {
        return applied;
    }

    public List<RemedialActionElementResult> getRemedialActionElementResults() {
        return remedialActionElementResults;
    }
}
