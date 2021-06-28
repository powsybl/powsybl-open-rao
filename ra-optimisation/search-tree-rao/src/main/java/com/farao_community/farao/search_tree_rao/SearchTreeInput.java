/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.results.PrePerimeterResult;
import com.farao_community.farao.rao_commons.linear_optimisation.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.powsybl.iidm.network.Network;

import java.util.Set;

public class SearchTreeInput {
    private Network network;
    private Set<FlowCnec> flowCnecs;
    private Set<NetworkAction> networkActions;
    private Set<RangeAction> rangeActions;
    private State optimizedState;

    private ObjectiveFunction objectiveFunction;
    private IteratingLinearOptimizer iteratingLinearOptimizer;
    private SearchTreeBloomer searchTreeBloomer;
    private SearchTreeProblem searchTreeProblem;
    private SearchTreeComputer searchTreeComputer;

    private PrePerimeterResult prePerimeterOutput;

    public State getOptimizedState() {
        return optimizedState;
    }

    public void setOptimizedState(State optimizedState) {
        this.optimizedState = optimizedState;
    }

    public SearchTreeBloomer getSearchTreeBloomer() {
        return searchTreeBloomer;
    }

    public void setSearchTreeBloomer(SearchTreeBloomer searchTreeBloomer) {
        this.searchTreeBloomer = searchTreeBloomer;
    }

    public SearchTreeComputer getSearchTreeComputer() {
        return searchTreeComputer;
    }

    public void setSearchTreeComputer(SearchTreeComputer searchTreeComputer) {
        this.searchTreeComputer = searchTreeComputer;
    }

    public Set<NetworkAction> getNetworkActions() {
        return networkActions;
    }

    public Network getNetwork() {
        return network;
    }

    public Set<RangeAction> getRangeActions() {
        return rangeActions;
    }

    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }

    public void setNetworkActions(Set<NetworkAction> networkActions) {
        this.networkActions = networkActions;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public void setRangeActions(Set<RangeAction> rangeActions) {
        this.rangeActions = rangeActions;
    }

    public void setFlowCnecs(Set<FlowCnec> flowCnecs) {
        this.flowCnecs = flowCnecs;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public void setObjectiveFunction(ObjectiveFunction objectiveFunction) {
        this.objectiveFunction = objectiveFunction;
    }

    public IteratingLinearOptimizer getIteratingLinearOptimizer() {
        return iteratingLinearOptimizer;
    }

    public void setIteratingLinearOptimizer(IteratingLinearOptimizer iteratingLinearOptimizer) {
        this.iteratingLinearOptimizer = iteratingLinearOptimizer;
    }

    public SearchTreeProblem getSearchTreeProblem() {
        return searchTreeProblem;
    }

    public void setSearchTreeProblem(SearchTreeProblem searchTreeProblem) {
        this.searchTreeProblem = searchTreeProblem;
    }

    public PrePerimeterResult getPrePerimeterOutput() {
        return prePerimeterOutput;
    }

    public void setPrePerimeterOutput(PrePerimeterResult prePerimeterOutput) {
        this.prePerimeterOutput = prePerimeterOutput;
    }
}
