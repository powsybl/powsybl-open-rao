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
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY)
public class TopologicalActionElementResult extends RemedialActionElementResult {

    public enum TopologicalState {
        OPEN,
        CLOSE
    }

    private final TopologicalState topologicalState;

    @ConstructorProperties({"id", "topologicalState"})
    public TopologicalActionElementResult(final String id, final TopologicalState topologicalState) {
        super(id);
        this.topologicalState = topologicalState;
    }

    public TopologicalState getTopologicalState() {
        return topologicalState;
    }

}
