/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkActionCombination {

    private final Set<NetworkAction> networkActionSet;
    private final boolean detectedDuringRao;

    public NetworkActionCombination(Set<NetworkAction> networkActionSet, boolean detectedDuringRao) {
        this.networkActionSet = networkActionSet;
        this.detectedDuringRao = detectedDuringRao;
    }

    public NetworkActionCombination(Set<NetworkAction> networkActionSet) {
        this(networkActionSet, false);
    }

    public NetworkActionCombination(NetworkAction networkAction) {
        this(Collections.singleton(networkAction), false);
    }

    public Set<NetworkAction> getNetworkActionSet() {
        return networkActionSet;
    }

    public Set<String> getOperators() {
        return networkActionSet.stream()
            .map(NetworkAction::getOperator)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public String getConcatenatedId() {
        return networkActionSet.stream()
            .map(Identifiable::getId)
            .collect(Collectors.joining(" + "));
    }

    public boolean isDetectedDuringRao() {
        return detectedDuringRao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NetworkActionCombination oNetworkActionCombination = (NetworkActionCombination) o;
        return this.detectedDuringRao == oNetworkActionCombination.isDetectedDuringRao()
                && this.networkActionSet.equals(oNetworkActionCombination.networkActionSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkActionSet) + 37 * Objects.hash(detectedDuringRao);
    }
}
