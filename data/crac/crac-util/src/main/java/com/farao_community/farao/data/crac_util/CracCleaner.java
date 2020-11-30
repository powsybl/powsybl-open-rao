/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_util;

import com.farao_community.farao.data.crac_api.Crac;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public final class CracCleaner {

    private CracCleaner() {
    }

    public static void cracAliasesUtil(Crac crac, Network network) {
        // List (without duplicates) all the crac elements that need to be found in the network
        Set<String> elementIds = new HashSet<>();
        crac.getCnecs().forEach(cnec -> elementIds.add(cnec.getNetworkElement().getId()));
        crac.getContingencies().forEach(contingency -> contingency.getNetworkElements().forEach(networkElement -> elementIds.add(networkElement.getId())));

        // Try to find a corresponding element in the network, and add elementId as an alias
        elementIds.forEach(elementId -> {
            Optional<Identifiable<?>> correspondingElement = network.getIdentifiables().stream().filter(identifiable -> anyMatch(identifiable, elementId)).findAny();
            correspondingElement.ifPresent(identifiable -> identifiable.addAlias(elementId));
        });
    }

    private static boolean anyMatch(Identifiable<?> identifiable, String cnecId) {
        return nameMatches(identifiable, cnecId, false) ||
            aliasMatches(identifiable, cnecId, false) ||
            nameMatches(identifiable, cnecId, true) ||
            aliasMatches(identifiable, cnecId, true);
    }

    private static boolean nameMatches(Identifiable<?> identifiable, String cnecId, boolean reverse) {
        return identifiable.getId().trim().matches(checkWithPattern(cnecId, reverse));
    }

    private static boolean aliasMatches(Identifiable<?> identifiable, String cnecId, boolean reverse) {
        return identifiable.getAliases().stream().anyMatch(alias -> alias.trim().matches(checkWithPattern(cnecId, reverse)));
    }

    private static String checkWithPattern(String string, boolean reverse) {
        int first = reverse ? 9 : 0;
        int second = reverse ? 0 : 9;
        return Pattern.quote(string.substring(first, first + 7)) + ".*" + " " + Pattern.quote(string.substring(second, second + 7)) + ".*" + Pattern.quote(string.substring(17)).trim();
    }

}
