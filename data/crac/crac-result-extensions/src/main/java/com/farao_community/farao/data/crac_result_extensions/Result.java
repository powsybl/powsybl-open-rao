/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The Result interface is use to stamp some classes as results which can be
 * embedded in a {@link AbstractResultExtension} object.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CnecResult.class, name = "cnec-result"),
        @JsonSubTypes.Type(value = CracResult.class, name = "crac-result"),
        @JsonSubTypes.Type(value = PstRangeResult.class, name = "pst-range-result"),
                @JsonSubTypes.Type(value = NetworkActionResult.class, name = "network-action-result")
})
public interface Result { }
