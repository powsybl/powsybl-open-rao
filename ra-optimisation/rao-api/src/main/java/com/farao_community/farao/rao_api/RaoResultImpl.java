/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api;

import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.OptimizationState;
import com.farao_community.farao.rao_api.results.PerimeterResult;
import com.farao_community.farao.rao_api.results.PrePerimeterResult;
import com.farao_community.farao.rao_api.results.RaoResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.commons.extensions.Extension;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RAO result API. This class will contain information about the RAO computation (computation status, logs, etc).
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */

public class RaoResultImpl extends AbstractExtendable<RaoResultImpl> implements RaoResult<RaoResultImpl> {

    @Override
    public PerimeterResult getPerimeterResult(OptimizationState optimizationState, State state) {
        return null;
    }

    @Override
    public PerimeterResult getPostPreventivePerimeterResult() {
        return null;
    }

    @Override
    public PrePerimeterResult getInitialResult() {
        return null;
    }

    @Override
    public double getFunctionalCost(OptimizationState optimizationState) {
        return 0;
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(OptimizationState optimizationState, int number) {
        return null;
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState) {
        return 0;
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return null;
    }

    @Override
    public double getVirtualCost(OptimizationState optimizationState, String virtualCostName) {
        return 0;
    }

    @Override
    public List<BranchCnec> getCostlyElements(OptimizationState optimizationState, String virtualCostName, int number) {
        return null;
    }

    @Override
    public boolean wasActivatedBeforeState(State state, NetworkAction networkAction) {
        return false;
    }

    @Override
    public boolean isActivatedDuringState(State state, NetworkAction networkAction) {
        return false;
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActionsDuringState(State state) {
        return null;
    }

    @Override
    public boolean isActivatedDuringState(State state, RangeAction rangeAction) {
        return false;
    }

    @Override
    public int getPreOptimizationTapOnState(State state, PstRangeAction pstRangeAction) {
        return 0;
    }

    @Override
    public int getOptimizedTapOnState(State state, PstRangeAction pstRangeAction) {
        return 0;
    }

    @Override
    public double getPreOptimizationSetPointOnState(State state, RangeAction rangeAction) {
        return 0;
    }

    @Override
    public double getOptimizedSetPointOnState(State state, RangeAction rangeAction) {
        return 0;
    }

    @Override
    public Set<RangeAction> getActivatedRangeActionsDuringState(State state) {
        return null;
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return null;
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPointsOnState(State state) {
        return null;
    }

    @Override
    public void addExtension(Class aClass, Extension extension) {

    }

    @Override
    public Extension getExtension(Class aClass) {
        return null;
    }

    @Override
    public Extension getExtensionByName(String s) {
        return null;
    }

    @Override
    public boolean removeExtension(Class aClass) {
        return false;
    }

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
    public RaoResultImpl(@JsonProperty("status") Status status) {
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
