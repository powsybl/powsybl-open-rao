/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_util;

import com.farao_community.farao.data.crac_api.*;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.*;

import java.util.*;
import java.util.regex.Pattern;

import static com.farao_community.farao.data.crac_util.UcteNodeMatchingRule.BEGINNING_OF_ELEMENT_NAME;
import static com.farao_community.farao.data.crac_util.UcteNodeMatchingRule.FIRST_7_CHARACTER_EQUAL;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public final class CracAliasesUtil {

    static final int NB_CHARACTER_IN_UCTE_NODE = 9;

    private CracAliasesUtil() {
    }

    public static void createAliases(Crac crac, Network network) {
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

    /* It only works correctly for Cnec with direction = both. This corrupts the other Cnec.
    TODO : only look for the elements with reverse ID if all the thresholds of the cnec have a BOTH direction.
     */
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
        int first = reverse ? NB_CHARACTER_IN_UCTE_NODE : 0;
        int second = reverse ? 0 : NB_CHARACTER_IN_UCTE_NODE;
        return Pattern.quote(string.substring(first, first + FIRST_7_CHARACTER_EQUAL.getNbCharacter())) + ".*" + " " + Pattern.quote(string.substring(second, second + FIRST_7_CHARACTER_EQUAL.getNbCharacter())) + ".*" + Pattern.quote(string.substring(BEGINNING_OF_ELEMENT_NAME.getNbCharacter())).trim();
    }
}
