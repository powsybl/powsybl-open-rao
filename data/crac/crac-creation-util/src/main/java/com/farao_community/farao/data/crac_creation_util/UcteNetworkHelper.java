/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A utility class that reads the network and stores UCTE information in order
 * to easily map UCTE identifiables (from/to/suffix) to elements in the network
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteNetworkHelper {
    private static final int UCTE_NODE_LENGTH = 8;
    private static final int ELEMENT_NAME_LENGTH = 12;
    private static final int MAX_BRANCH_ID_LENGTH = UCTE_NODE_LENGTH * 2 + ELEMENT_NAME_LENGTH + 3;
    private static final String TIELINE_SEPARATOR = " + ";

    private Network network;
    private Map<UcteElement, String> referenceMap;
    private boolean completeSmallBusIdsWithWildcards;
    private Map<String, Pair<Identifiable<?>, UcteElement.MatchResult>> resultCache = new ConcurrentHashMap<>();
    private Set<String> notFoundCache = new HashSet<>();

    public UcteNetworkHelper(Network network, UcteNetworkHelperProperties properties) {
        if (!network.getSourceFormat().equals("UCTE")) {
            throw new IllegalArgumentException("UcteNetworkHelper can only be used for an UCTE network");
        }
        this.network = network;
        completeSmallBusIdsWithWildcards = properties.getBusIdMatchPolicy().equals(UcteNetworkHelperProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS);
        buildReferenceMap();
    }

    public Network getNetwork() {
        return network;
    }

    private void buildReferenceMap() {
        referenceMap = new ConcurrentHashMap<>();
        addBranchesToReferenceMap();
        addDanglingLinesToReferenceMap();
        addSwitchesToReferenceMap();
    }

    private void addBranchesToReferenceMap() {
        network.getBranchStream().forEach(branch -> {
            String from = branch.getTerminal1().getBusBreakerView().getConnectableBus().getId();
            String to = branch.getTerminal2().getBusBreakerView().getConnectableBus().getId();
            if (branch instanceof TieLine) {
                /*
                 in UCTE import, the two Half Lines of an interconnection are merged into a TieLine
                 For instance, the TieLine "UCTNODE1 X___NODE 1 + UCTNODE2 X___NODE 1" is imported by PowSybl,
                 with :
                  - half1 = "UCTNODE1 X___NODE 1"
                  - half2 = "UCTNODE2 X___NODE 1"
                 In that case :
                  - if a criticial branch is defined with from = "UCTNODE1" and to = "X___NODE", the threshold
                    is ok as "UCTNODE1" is in the first half of the TieLine
                  - if a criticial branch is defined with from = "UCTNODE2" and to = "X___NODE", the threshold
                    should be inverted as "UCTNODE2" is in the second half of the TieLine
                */
                String xnode = ((TieLine) branch).getUcteXnodeCode();
                referenceMap.put(new UcteElement(from, xnode, getOrderCode(branch, Branch.Side.ONE), getElementNames(branch), branch.getClass(), Branch.Side.ONE), branch.getId());
                referenceMap.put(new UcteElement(xnode, to, getOrderCode(branch, Branch.Side.TWO), getElementNames(branch), branch.getClass(), Branch.Side.TWO), branch.getId());
            } else {
                referenceMap.put(new UcteElement(from, to, getOrderCode(branch), getElementNames(branch), branch.getClass()), branch.getId());
            }
        });
    }

    private void addDanglingLinesToReferenceMap() {
        network.getDanglingLineStream().forEach(danglingLine -> {
            // A dangling line is an Injection with a generator convention.
            // After an UCTE import, the flow on the dangling line is therefore always from the X_NODE to the other node.
            String from = danglingLine.getUcteXnodeCode();
            String to = danglingLine.getTerminal().getBusBreakerView().getConnectableBus().getId();
            referenceMap.put(new UcteElement(from, to, getOrderCode(danglingLine), getElementNames(danglingLine), danglingLine.getClass()), danglingLine.getId());
        });
    }

    private void addSwitchesToReferenceMap() {
        network.getSwitchStream().forEach(switchElement -> {
            String from = switchElement.getVoltageLevel().getBusBreakerView().getBus1(switchElement.getId()).getId();
            String to = switchElement.getVoltageLevel().getBusBreakerView().getBus2(switchElement.getId()).getId();
            referenceMap.put(new UcteElement(from, to, getOrderCode(switchElement), getElementNames(switchElement), switchElement.getClass()), switchElement.getId());
        });
    }

    /**
     * Get the order code for an identifiable, on a given side (side is important for tie lines)
     */
    private static String getOrderCode(Identifiable<?> identifiable, Branch.Side side) {
        String connectableId;
        if (identifiable instanceof TieLine && identifiable.getId().length() > MAX_BRANCH_ID_LENGTH) {
            Objects.requireNonNull(side, "Side should be specified for tielines");
            int separator = identifiable.getId().indexOf(TIELINE_SEPARATOR);
            connectableId = side.equals(Branch.Side.ONE) ? identifiable.getId().substring(0, separator) : identifiable.getId().substring(separator + TIELINE_SEPARATOR.length());
        } else {
            connectableId = identifiable.getId();
        }
        return connectableId.substring(UCTE_NODE_LENGTH * 2 + 2);
    }

    private static String getOrderCode(Identifiable<?> identifiable) {
        return getOrderCode(identifiable, null);
    }

    /**
     * Get all the element names of an identifiable
     */
    private static Set<String> getElementNames(Identifiable<?> identifiable) {
        return identifiable.getPropertyNames().stream()
            .filter(propertyName -> propertyName.startsWith("elementName"))
            .map(identifiable::getProperty)
            .collect(Collectors.toSet());
    }

    // TODO : refactor to reduce complexity
    Pair<Identifiable<?>, UcteElement.MatchResult> findNetworkElement(String from, String to, String suffix) {
        String uniqueId = getUniqueId(from, to, suffix);
        if (resultCache.containsKey(uniqueId)) {
            return resultCache.get(uniqueId);
        } else if (notFoundCache.contains(uniqueId)) {
            return null;
        }

        // TODO : improve performance (cbcora import 11sec -> 1m13sec)
        UcteElement matchedElement = null;
        Pair<Identifiable<?>, UcteElement.MatchResult> matchedResult = null;
        for (Map.Entry<UcteElement, String> entry : referenceMap.entrySet()) {
            UcteElement.MatchResult newMatchResult = entry.getKey().tryMatching(from, to, suffix, completeSmallBusIdsWithWildcards);
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

        if (matchedResult != null) {
            resultCache.put(uniqueId, matchedResult);
        } else {
            notFoundCache.add(uniqueId);
        }
        return matchedResult;
    }

    private String getUniqueId(String from, String to, String suffix) {
        return String.format("%s %s %s", from, to, suffix);
    }

}
