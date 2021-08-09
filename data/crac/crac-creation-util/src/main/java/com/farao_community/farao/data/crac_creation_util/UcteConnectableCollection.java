/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation_util;

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.powsybl.iidm.network.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_creation_util.UcteUtils.*;

/**
 * A utility class that reads the network and stores UCTE information in order
 * to easily map UCTE connectables (from/to/suffix) to elements in the network
 *
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class UcteConnectableCollection {

    /*
        The key of the map is the fromNodeId of the Connectable
        The TreeMultiMap is stored by alphabetical order of the fromNodeId
        One key can be associated to several values.
     */
    private TreeMultimap<String, UcteConnectable> connectables;

    UcteConnectableCollection(Network network) {
        connectables = TreeMultimap.create(Ordering.<String>natural().nullsFirst(), Ordering.<UcteConnectable>natural().nullsFirst());
        addBranches(network);
        addDanglingLines(network);
        addSwitches(network);
    }

    UcteMatchingResult lookForConnectable(String fromNodeId, String toNodeId, String suffix) {

        /*
          priority is given to the search with the from/to direction given in argument
          ---

          Note that some UCTE cases have already been encountered with similar connectables. For instance, one transformer
          and one switch, with ids:
          - UCTNODE1 UCTNODE2 1
          - UCTNODE2 UCTNODE1 1

          In such cases, both connectables fit the same from/to/suffix. But instead of returning a
          UcteMatchingRule.severalPossibleMatch(), which is reserved for ambiguous situations with wildcards, this
          method returns the connectable with the id in the same order than the the ones given in argument of the method.
         */

        UcteMatchingResult ucteMatchingResult = lookForMatch(fromNodeId, toNodeId, suffix);

        if (!ucteMatchingResult.getStatus().equals(UcteMatchingResult.MatchStatus.NOT_FOUND)) {
            return ucteMatchingResult;
        }

        // if no result has been found in the direction in argument, look for an inverted one
        ucteMatchingResult = lookForMatch(toNodeId, fromNodeId, suffix);
        return ucteMatchingResult.invert();
    }

    private UcteMatchingResult lookForMatch(String fromNodeId, String toNodeId, String suffix) {

        if (!fromNodeId.endsWith(UcteUtils.WILDCARD_CHARACTER)) {

            // if the from node contains no wildcard, directly look for the entry of the TreeMultimap with the fromNode id
            Collection<UcteConnectable> ucteElements = connectables.asMap().getOrDefault(fromNodeId, Collections.emptyList());
            return lookForMatchWithinCollection(fromNodeId, toNodeId, suffix, ucteElements);

        } else {

            // if the from node contains a wildcard, broad all entries of the map whose 7th first characters match the given id
            // as the map is ordered in alphabetical order, the corresponding entries can be searched efficiently

            String beforeFrom = fromNodeId.substring(0, UcteUtils.UCTE_NODE_LENGTH - 1) + Character.MIN_VALUE;
            String afterFrom = fromNodeId.substring(0, UcteUtils.UCTE_NODE_LENGTH - 1) + Character.MAX_VALUE;

            List<UcteConnectable> ucteElements = connectables.asMap().subMap(beforeFrom, afterFrom).values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

            return lookForMatchWithinCollection(fromNodeId, toNodeId, suffix, ucteElements);
        }
    }

    private UcteMatchingResult lookForMatchWithinCollection(String fromNodeId, String toNodeId, String suffix, Collection<UcteConnectable> ucteConnectables) {

        if (fromNodeId.endsWith(WILDCARD_CHARACTER) || toNodeId.endsWith(WILDCARD_CHARACTER)) {
            // if the nodes contains wildCards, we have to look for all possible match

            List<UcteMatchingResult> matchedConnetables = ucteConnectables.stream()
                .filter(ucteConnectable -> ucteConnectable.doesMatch(fromNodeId, toNodeId, suffix))
                .map(ucteConnectable -> ucteConnectable.getUcteMatchingResult(fromNodeId, toNodeId, suffix))
                .collect(Collectors.toList());

            if (matchedConnetables.size() == 1) {
                return matchedConnetables.get(0);
            } else if (matchedConnetables.size() > 1) {
                return UcteMatchingResult.severalPossibleMatch();
            } else {
                return UcteMatchingResult.notFound();
            }
        } else {

            // if the nodes contains no wildCards, speed up the search by using findAny() instead of looking for all possible matches
            return ucteConnectables.stream()
                .filter(ucteConnectable -> ucteConnectable.doesMatch(fromNodeId, toNodeId, suffix))
                .map(ucteConnectable -> ucteConnectable.getUcteMatchingResult(fromNodeId, toNodeId, suffix))
                .findAny().orElse(UcteMatchingResult.notFound());
        }
    }

    private void addBranches(Network network) {
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
                connectables.put(from, new UcteConnectable(from, xnode, getOrderCode(branch, Branch.Side.ONE), getElementNames(branch), branch, UcteConnectable.Side.ONE));
                connectables.put(xnode, new UcteConnectable(xnode, to, getOrderCode(branch, Branch.Side.TWO), getElementNames(branch), branch, UcteConnectable.Side.TWO));
            } else if (branch instanceof TwoWindingsTransformer) {
                /*
                    The terminals of the TwoWindingTransformer are inverted in the iidm network, compared to what
                    is defined in the UCTE network.

                    The UCTE order is kept here, to avoid potential duplicates with other connectables.
                 */
                connectables.put(to, new UcteConnectable(to, from, getOrderCode(branch), getElementNames(branch), branch));
            } else {
                connectables.put(from, new UcteConnectable(from, to, getOrderCode(branch), getElementNames(branch), branch));
            }
        });
    }

    private void addDanglingLines(Network network) {
        network.getDanglingLineStream().forEach(danglingLine -> {
            // A dangling line is an Injection with a generator convention.
            // After an UCTE import, the flow on the dangling line is therefore always from the X_NODE to the other node.
            String from = danglingLine.getUcteXnodeCode();
            String to = danglingLine.getTerminal().getBusBreakerView().getConnectableBus().getId();
            connectables.put(from, new UcteConnectable(from, to, getOrderCode(danglingLine), getElementNames(danglingLine), danglingLine));
        });
    }

    private void addSwitches(Network network) {
        network.getSwitchStream().forEach(switchElement -> {
            String from = switchElement.getVoltageLevel().getBusBreakerView().getBus1(switchElement.getId()).getId();
            String to = switchElement.getVoltageLevel().getBusBreakerView().getBus2(switchElement.getId()).getId();
            connectables.put(from, new UcteConnectable(from, to, getOrderCode(switchElement), getElementNames(switchElement), switchElement));
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
     * Get all the element name of an identifiable
     * Note that tie-line can contain several element names
     */
    private static Set<String> getElementNames(Identifiable<?> identifiable) {
        return identifiable.getPropertyNames().stream()
            .filter(propertyName -> propertyName.startsWith("elementName"))
            .map(identifiable::getProperty)
            .collect(Collectors.toSet());
    }
}
