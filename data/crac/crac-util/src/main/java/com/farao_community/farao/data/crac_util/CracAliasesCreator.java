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

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public class CracAliasesCreator {

    private static final int POSITION_OF_SECOND_UCTE_NODE = 9;
    private static final int BEGINNING_OF_ELEMENT_NAME = 17;

    public CracAliasesCreator() {
    }

    public void createAliases(Crac crac, Network network) {
        createAliases(crac, network, UcteNodeMatchingRule.ALL_8_CHARACTER_EQUAL);
    }

    public void createAliases(Crac crac, Network network, UcteNodeMatchingRule rule) {
        // List (without duplicates) all the crac elements that need to be found in the network
        Set<String> elementIds = new HashSet<>();
        crac.getCnecs().forEach(cnec -> elementIds.add(cnec.getNetworkElement().getId()));
        crac.getContingencies().forEach(contingency -> handleAliases(contingency.getNetworkElements(), elementIds));
        crac.getNetworkActions().forEach(networkAction -> handleAliases(networkAction.getNetworkElements(), elementIds));
        crac.getRangeActions().forEach(rangeAction -> handleAliases(rangeAction.getNetworkElements(), elementIds));

        // Try to find a corresponding element in the network, and add elementId as an alias
        elementIds.forEach(elementId -> {
            Optional<Identifiable<?>> correspondingElement = network.getIdentifiables().stream().filter(identifiable -> anyMatch(identifiable, elementId, rule)).findAny();
            correspondingElement.ifPresent(identifiable -> identifiable.addAlias(elementId));
        });
    }

    private void handleAliases(Set<NetworkElement> networkElements, Set<String> elementIds) {
        networkElements.forEach(networkElement -> elementIds.add(networkElement.getId()));
    }

    /* It only works correctly for Cnec with direction = both. This corrupts the other Cnec.
    TODO : only look for the elements with reverse ID if all the thresholds of the cnec have a BOTH direction.
     */
    private boolean anyMatch(Identifiable<?> identifiable, String cnecId, UcteNodeMatchingRule rule) {
        return nameMatches(identifiable, cnecId, rule, false) ||
            aliasMatches(identifiable, cnecId, rule, false) ||
            nameMatches(identifiable, cnecId, rule, true) ||
            aliasMatches(identifiable, cnecId, rule, true);
    }

    private boolean nameMatches(Identifiable<?> identifiable, String cnecId, UcteNodeMatchingRule rule, boolean reverse) {
        return identifiable.getId().trim().matches(checkWithPattern(cnecId, rule, reverse));
    }

    private boolean aliasMatches(Identifiable<?> identifiable, String cnecId, UcteNodeMatchingRule rule, boolean reverse) {
        return identifiable.getAliases().stream().anyMatch(alias -> alias.trim().matches(checkWithPattern(cnecId, rule, reverse)));
    }

    private String checkWithPattern(String string, UcteNodeMatchingRule rule, boolean reverse) {
        int first = reverse ? POSITION_OF_SECOND_UCTE_NODE : 0;
        int second = reverse ? 0 : POSITION_OF_SECOND_UCTE_NODE;
        String nodeFrom = Pattern.quote(string.substring(first, first + rule.getNbCharacter()));
        String nodeTo = Pattern.quote(string.substring(second, second + rule.getNbCharacter()));
        String tail = Pattern.quote(string.substring(BEGINNING_OF_ELEMENT_NAME)).trim();
        return String.format("%s.* %s.*%s", nodeFrom, nodeTo, tail);
    }
}
