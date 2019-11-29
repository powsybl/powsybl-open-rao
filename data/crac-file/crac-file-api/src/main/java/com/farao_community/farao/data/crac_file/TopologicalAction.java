/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
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
import java.util.Map;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY)
public class TopologicalAction extends RemedialActionElement {

    public enum Status {
        OPEN,
        CLOSE
    }

    @NotNull(message = "actions")
    private final Map<String, Status> actions;

    @ConstructorProperties({"id", "actions"})
    @Builder
    public TopologicalAction(String id, Map<String, Status> actions) {
        super(id);
        this.actions = actions;
    }
}
