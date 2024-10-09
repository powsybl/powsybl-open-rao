/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.ucte;

import com.powsybl.openrao.data.cracio.commons.ConnectableType;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.powsybl.iidm.network.*;

import java.util.*;
import java.util.stream.Collectors;

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
        One key can be associated to several values, as several Connectables
        can have the same fromNodeId.
     */
    private TreeMultimap<String, UcteConnectable> connectables;

    UcteConnectableCollection(Network network) {
        connectables = TreeMultimap.create(Ordering.<String>natural().nullsFirst(), Ordering.<UcteConnectable>natural().nullsFirst());
        addBranches(network);
        addDanglingLines(network);
        addSwitches(network);
        addHvdcs(network);
    }

    UcteMatchingResult lookForConnectable(String fromNodeId, String toNodeId, String suffix, UcteNetworkAnalyzerProperties.BusIdMatchPolicy policy, ConnectableType... connectableTypes) {

        /*
          priority is given to the search with the from/to direction given in argument
          ---

          Note that some UCTE cases have already been encountered with similar connectables. For instance, one transformer
          and one switch, with ids:
          - UCTNODE1 UCTNODE2 1
          - UCTNODE2 UCTNODE1 1

          In such cases, both connectables fit the same from/to/suffix. But instead of returning a
          UcteMatchingRule.severalPossibleMatch(), which is reserved for ambiguous situations with wildcards, this
          method returns the connectable with the id in the same order as the ones given in argument of the method.
         */

        UcteMatchingResult ucteMatchingResult = lookForMatch(fromNodeId, toNodeId, suffix, connectableTypes);

        if (!ucteMatchingResult.getStatus().equals(UcteMatchingResult.MatchStatus.NOT_FOUND)) {
            return ucteMatchingResult;
        }

        // if no result has been found in the direction in argument, look for an inverted one
        ucteMatchingResult = lookForMatch(toNodeId, fromNodeId, suffix, connectableTypes);

        if (!ucteMatchingResult.getStatus().equals(UcteMatchingResult.MatchStatus.NOT_FOUND)
            || !policy.equals(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.REPLACE_8TH_CHARACTER_WITH_WILDCARD)) {
            return ucteMatchingResult.invert();
        }

        // if no result has been found yet and busIdMatchPolicy is REPLACE_8TH_CHARACTER_WITH_WILDCARD, continue search
        // by replacing last character with wildcard
        String fromWildcard = String.format("%1$-7s", fromNodeId).substring(0, 7) + UcteUtils.WILDCARD_CHARACTER;
        String toWildcard = String.format("%1$-7s", toNodeId).substring(0, 7) + UcteUtils.WILDCARD_CHARACTER;

        // with the direction in argument ...
        ucteMatchingResult = lookForMatch(fromWildcard, toWildcard, suffix, connectableTypes);

        if (!ucteMatchingResult.getStatus().equals(UcteMatchingResult.MatchStatus.NOT_FOUND)) {
            return ucteMatchingResult;
        }

        // or, if not found, with the inverted direction
        return lookForMatch(toWildcard, fromWildcard, suffix, connectableTypes).invert();

    }

    private UcteMatchingResult lookForMatch(String fromNodeId, String toNodeId, String suffix, ConnectableType... types) {

        if (!fromNodeId.endsWith(UcteUtils.WILDCARD_CHARACTER)) {

            // if the from node contains no wildcard, directly look for the entry of the TreeMultimap with the fromNode id
            Collection<UcteConnectable> ucteElements = connectables.asMap().getOrDefault(fromNodeId, Collections.emptyList());
            return lookForMatchWithinCollection(fromNodeId, toNodeId, suffix, ucteElements, types);

        } else {

            // if the from node contains a wildcard, broad all entries of the map whose 7th first characters match the given id
            // as the map is ordered in alphabetical order, the corresponding entries can be searched efficiently

            String beforeFrom = fromNodeId.substring(0, UcteUtils.UCTE_NODE_LENGTH - 1) + Character.MIN_VALUE;
            String afterFrom = fromNodeId.substring(0, UcteUtils.UCTE_NODE_LENGTH - 1) + Character.MAX_VALUE;

            List<UcteConnectable> ucteElements = connectables.asMap().subMap(beforeFrom, afterFrom).values().stream()
                .flatMap(Collection::stream)
                .toList();

            return lookForMatchWithinCollection(fromNodeId, toNodeId, suffix, ucteElements, types);
        }
    }

    private UcteMatchingResult lookForMatchWithinCollection(String fromNodeId, String toNodeId, String suffix, Collection<UcteConnectable> ucteConnectables, ConnectableType... connectableTypes) {

        if (fromNodeId.endsWith(UcteUtils.WILDCARD_CHARACTER) || toNodeId.endsWith(UcteUtils.WILDCARD_CHARACTER)) {
            // if the nodes contains wildCards, we have to look for all possible match

            List<UcteMatchingResult> matchedConnectables = ucteConnectables.stream()
                .filter(ucteConnectable -> ucteConnectable.doesMatch(fromNodeId, toNodeId, suffix, connectableTypes))
                .map(ucteConnectable -> ucteConnectable.getUcteMatchingResult(fromNodeId, toNodeId, suffix, connectableTypes))
                .toList();

            if (matchedConnectables.size() == 1) {
                return matchedConnectables.get(0);
            } else if (matchedConnectables.size() == 2) {
                return UcteMatchingResult.severalPossibleMatch();
            } else if (matchedConnectables.size() > 2) {
                return UcteMatchingResult.severalPossibleMatch();
            } else {
                return UcteMatchingResult.notFound();
            }
        } else {

            // if the nodes contains no wildCards, speed up the search by using findAny() instead of looking for all possible matches
            return ucteConnectables.stream()
                .filter(ucteConnectable -> ucteConnectable.doesMatch(fromNodeId, toNodeId, suffix, connectableTypes))
                .map(ucteConnectable -> ucteConnectable.getUcteMatchingResult(fromNodeId, toNodeId, suffix, connectableTypes))
                .findAny().orElse(UcteMatchingResult.notFound());
        }
    }

    private void addBranches(Network network) {
        network.getBranchStream().forEach(branch -> {
            String from = getNodeName(branch.getTerminal1().getBusBreakerView().getConnectableBus().getId());
            String to = getNodeName(branch.getTerminal2().getBusBreakerView().getConnectableBus().getId());
            if (branch instanceof TieLine tieLine) {
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
                String xnode = tieLine.getPairingKey();
                connectables.put(from, new UcteConnectable(from, xnode, getOrderCode(branch, TwoSides.ONE), getElementNames(branch), branch, false, UcteConnectable.Side.ONE));
                connectables.put(xnode, new UcteConnectable(xnode, to, getOrderCode(branch, TwoSides.TWO), getElementNames(branch), branch, false, UcteConnectable.Side.TWO));
            } else if (branch instanceof TwoWindingsTransformer) {
                /*
                    The terminals of the TwoWindingTransformer are inverted in the iidm network, compared to what
                    is defined in the UCTE network.

                    The UCTE order is kept here, to avoid potential duplicates with other connectables.
                 */
                connectables.put(to, new UcteConnectable(to, from, getOrderCode(branch), getElementNames(branch), branch, true));
            } else {
                connectables.put(from, new UcteConnectable(from, to, getOrderCode(branch), getElementNames(branch), branch, false));
            }
        });
    }

    private void addDanglingLines(Network network) {
        network.getDanglingLineStream().filter(danglingLine -> !danglingLine.isPaired()).forEach(danglingLine -> {
            // A dangling line is an Injection with a generator convention.
            // After an UCTE import, the flow on the dangling line is therefore always from the X_NODE to the other node.
            String xNode = danglingLine.getPairingKey();
            String rNode = getNodeName(danglingLine.getTerminal().getBusBreakerView().getConnectableBus().getId());

            if (danglingLine.getId().startsWith("X")) {
                // UCTE definition is in the same direction as iidm definition
                connectables.put(xNode, new UcteConnectable(xNode, rNode, getOrderCode(danglingLine), getElementNames(danglingLine), danglingLine, false));
            } else {
                // UCTE definition is opposite as the iidm definition
                connectables.put(rNode, new UcteConnectable(rNode, xNode, getOrderCode(danglingLine), getElementNames(danglingLine), danglingLine, true));
            }
        });
    }

    private void addSwitches(Network network) {
        network.getSwitchStream().forEach(switchElement -> {
            String from = getNodeName(switchElement.getVoltageLevel().getBusBreakerView().getBus1(switchElement.getId()).getId());
            String to = getNodeName(switchElement.getVoltageLevel().getBusBreakerView().getBus2(switchElement.getId()).getId());
            connectables.put(from, new UcteConnectable(from, to, getOrderCode(switchElement), getElementNames(switchElement), switchElement, false));
        });
    }

    private void addHvdcs(Network network) {
        network.getHvdcLines().forEach(hvdcLine -> {
            String from = getNodeName(hvdcLine.getConverterStation1().getTerminal().getBusBreakerView().getBus().getId());
            String to = getNodeName(hvdcLine.getConverterStation2().getTerminal().getBusBreakerView().getBus().getId());
            connectables.put(from, new UcteConnectable(from, to, getOrderCode(hvdcLine), getElementNames(hvdcLine), hvdcLine, false));
        });
    }

    /**
     * Get the order code for an identifiable, on a given side (side is important for tie lines)
     */
    private static String getOrderCode(Identifiable<?> identifiable, TwoSides side) {
        String connectableId;
        if (identifiable instanceof TieLine && identifiable.getId().length() > UcteUtils.MAX_BRANCH_ID_LENGTH) {
            Objects.requireNonNull(side, "Side should be specified for tielines");
            int separator = identifiable.getId().indexOf(UcteUtils.TIELINE_SEPARATOR);
            connectableId = side.equals(TwoSides.ONE) ? identifiable.getId().substring(0, separator) : identifiable.getId().substring(separator + UcteUtils.TIELINE_SEPARATOR.length());
        } else {
            connectableId = identifiable.getId();
        }
        return connectableId.substring(UcteUtils.UCTE_NODE_LENGTH * 2 + 2);
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

    private static String getNodeName(String nodeName) {
        return nodeName.replace("YNODE_", ""); // remove 'YNODE_' prefix that is added on some Xnode by powsybl
    }
}
