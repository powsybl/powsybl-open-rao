/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class that reads the network and stores UCTE information in order
 * to easily map UCTE identifiables (from/to/suffix) to elements in the network
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteNetworkHelper {

    private Network network;
    private UcteConnectableCollection connectablesInNetwork;
    private boolean completeSmallBusIdsWithWildcards;
    private Map<String, Pair<Identifiable<?>, UcteConnectable.MatchResult>> resultCache = new ConcurrentHashMap<>();
    private Set<String> notFoundCache = new HashSet<>();

    public UcteNetworkHelper(Network network, UcteNetworkHelperProperties properties) {
        if (!network.getSourceFormat().equals("UCTE")) {
            throw new IllegalArgumentException("UcteNetworkHelper can only be used for an UCTE network");
        }
        this.network = network;
        completeSmallBusIdsWithWildcards = properties.getBusIdMatchPolicy().equals(UcteNetworkHelperProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS);
        connectablesInNetwork = new UcteConnectableCollection(network);
    }

    public Network getNetwork() {
        return network;
    }

    // TODO : refactor to reduce complexity
    Pair<Identifiable<?>, UcteConnectable.MatchResult> findNetworkElement(String from, String to, String suffix) {

        String uniqueId = getUniqueId(from, to, suffix);
        if (resultCache.containsKey(uniqueId)) {
            return resultCache.get(uniqueId);
        } else if (notFoundCache.contains(uniqueId)) {
            return null;
        }

        // TODO : improve performance (cbcora import 11sec -> 1m13sec)
        UcteConnectable matchedElement = null;
        Pair<Identifiable<?>, UcteConnectable.MatchResult> matchedResult = null;

        Pair<UcteConnectable, UcteConnectable.MatchResult> resultPair = connectablesInNetwork.getConnectable(completeNodeName(from), completeNodeName(to), suffix);

        if (resultPair != null) {
            matchedResult = Pair.of(network.getIdentifiable(resultPair.getLeft().getIidmId()), resultPair.getRight());
        }

        /*
        for (Map.Entry<UcteConnectable, String> entry : referenceMap.entrySet()) {

            UcteConnectable.MatchResult newMatchResult = entry.getKey().tryMatching(from, to, suffix, completeSmallBusIdsWithWildcards);

            if (newMatchResult.matched()) {
                if (matchedElement != null
                    && newMatchResult.isInverted() == matchedResult.getRight().isInverted()
                    && entry.getKey().isConventionInverted() == matchedElement.isConventionInverted()) {
                    throw new IllegalArgumentException(
                        String.format("too many branches match the branch in the network (from: %s, to: %s, suffix: %s), for example %s and %s", from, to, suffix, matchedResult.getLeft().getId(), entry.getValue())
                    );
                } else if (matchedElement == null
                    || (!matchedResult.getRight().isInverted() && matchedElement.isConventionInverted())
                    || (matchedResult.getRight().isInverted() && !matchedElement.isConventionInverted())
                ) {
                    // Two UCTE elements from/to and to/from can be defined in one UCTE network, and both can have different types
                    // TODO : handle this case in a more elegant way, maybe inside enum
                    // TODO : add unit tests for this behavior
                    matchedElement = entry.getKey();
                    matchedResult = Pair.of(network.getIdentifiable(entry.getValue()), newMatchResult);
                }
            }
        }
         */

        if (matchedResult != null) {
            resultCache.put(uniqueId, matchedResult);
        } else {
            notFoundCache.add(uniqueId);
        }
        return matchedResult;
    }

    private String completeNodeName(String nodeName) {

        if (nodeName.length() == UcteUtils.UCTE_NODE_LENGTH) {
            return nodeName;
        } else if (completeSmallBusIdsWithWildcards) {
            return String.format("%1$-7s", nodeName) + UcteUtils.WILDCARD_CHARACTER;
        } else {
            return String.format("%1$-8s", nodeName);
        }
    }

    private String getUniqueId(String from, String to, String suffix) {
        return String.format("%s %s %s", from, to, suffix);
    }

}
