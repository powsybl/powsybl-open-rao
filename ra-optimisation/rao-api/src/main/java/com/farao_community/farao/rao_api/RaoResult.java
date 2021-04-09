/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.commons.extensions.AbstractExtendable;

/**
 * RAO result API. This class will contain information about the RAO computation (computation status, logs, etc).
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */

public class RaoResult extends AbstractExtendable<RaoResult> {

    public enum Status {
        DEFAULT,
        FALLBACK,
        FAILURE,
        UNDEFINED
    }

    private Status status;

    private String preOptimVariantId;

    private String postOptimVariantId;

    @JsonCreator
    public RaoResult(@JsonProperty("status") Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @JsonIgnore
    public boolean isSuccessful() {
        return status == Status.DEFAULT;
    }

    public void setPreOptimVariantId(String preOptimVariantId) {
        this.preOptimVariantId = preOptimVariantId;
    }

    public String getPreOptimVariantId() {
        return preOptimVariantId;
    }

    public String getPostOptimVariantId() {
        return postOptimVariantId;
    }

    public void setPostOptimVariantId(String postOptimVariantId) {
        this.postOptimVariantId = postOptimVariantId;
    }
}
