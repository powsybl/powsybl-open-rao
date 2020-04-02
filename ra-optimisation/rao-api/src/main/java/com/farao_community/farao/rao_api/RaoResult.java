/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * RAO result API. This class will contain information about the RAO computation (computation status, logs, etc).
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */

public class RaoResult {

    public enum Status {
        FAILURE,
        SUCCESS
    }

    public enum LastSensiParams {
        DEFAULT,
        FALLBACK
    }

    private Status status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LastSensiParams lastSensiParams;

    private String preOptimVariantId;

    private String postOptimVariantId;

    @JsonCreator
    public RaoResult(@JsonProperty("status") Status status) {
        this.status = status;
    }

    public RaoResult(Status status, LastSensiParams lastSensiParams) {
        this.status = status;
        this.lastSensiParams = lastSensiParams;
    }

    public RaoResult(Status status, boolean lastSensiIsFallback) {
        this.status = status;
        this.lastSensiParams = lastSensiIsFallback ? LastSensiParams.FALLBACK : LastSensiParams.DEFAULT;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LastSensiParams getLastSensiParams() {
        return lastSensiParams;
    }

    public void setLastSensiParams(LastSensiParams lastSensiParams) {
        this.lastSensiParams = lastSensiParams;
    }

    @JsonIgnore
    public boolean isSuccessful() {
        return status == Status.SUCCESS;
    }

    public void setPreOptimVariantId(String preOptimVariantId) {
        this.preOptimVariantId = preOptimVariantId;
    }

    public String getPreOptimVariantId() {
        return preOptimVariantId;
    }

    public void setPostOptimVariantId(String postOptimVariantId) {
        this.postOptimVariantId = postOptimVariantId;
    }

    public String getPostOptimVariantId() {
        return postOptimVariantId;
    }
}
