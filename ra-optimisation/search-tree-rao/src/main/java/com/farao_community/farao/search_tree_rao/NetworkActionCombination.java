package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class NetworkActionCombination {

    private Set<NetworkAction> networkActionSet;

    NetworkActionCombination(Set<NetworkAction> networkActionSet) {
        this.networkActionSet = networkActionSet;
    }

    NetworkActionCombination(NetworkAction networkAction) {
        this.networkActionSet = Collections.singleton(networkAction);
    }

    Set<NetworkAction> getNetworkActionSet() {
        return networkActionSet;
    }

    Set<String> getOperators() {
        return networkActionSet.stream()
            .map(NetworkAction::getOperator)
            .collect(Collectors.toSet());
    }

    String getConcatenatedId() {
        return networkActionSet.stream()
            .map(Identifiable::getId)
            .collect(Collectors.joining(" + "));
    }
}
