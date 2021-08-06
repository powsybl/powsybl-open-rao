package com.farao_community.farao.data.crac_creation_util;

import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_creation_util.UcteUtils.*;

public class UcteConnectableCollection {

    private TreeMultimap<String, UcteConnectable> connectables;

    UcteConnectableCollection(Network network) {
        connectables = TreeMultimap.create(Ordering.<String>natural().nullsFirst(), Ordering.<UcteConnectable>natural().nullsFirst());
        addBranches(network);
        addDanglingLines(network);
        addSwitches(network);
    }

    Pair<UcteConnectable, UcteConnectable.MatchResult> getConnectable(String from, String to, String suffix) {

        UcteConnectable ucteConnectable = matchFromTo(from, to, suffix);

        if (ucteConnectable != null) {
            return Pair.of(ucteConnectable, ucteConnectable.tryMatching(from, to, suffix, false));
        }

        ucteConnectable = matchFromTo(to, from, suffix);

        if (ucteConnectable != null) {
            return Pair.of(ucteConnectable, ucteConnectable.tryMatching(from, to, suffix, false));
        } else {
            return null;
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
                connectables.put(from, new UcteConnectable(from, xnode, getOrderCode(branch, Branch.Side.ONE), getElementNames(branch), branch, Branch.Side.ONE));
                connectables.put(xnode, new UcteConnectable(xnode, to, getOrderCode(branch, Branch.Side.TWO), getElementNames(branch), branch, Branch.Side.TWO));
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
     */
    private static Set<String> getElementNames(Identifiable<?> identifiable) {
        return identifiable.getPropertyNames().stream()
            .filter(propertyName -> propertyName.startsWith("elementName"))
            .map(identifiable::getProperty)
            .collect(Collectors.toSet());
    }

    private UcteConnectable matchFromTo(String from, String to, String suffix) {

        if (!from.endsWith(UcteUtils.WILDCARD_CHARACTER)) {
            Collection<UcteConnectable> ucteElements = connectables.asMap().getOrDefault(from, Collections.emptyList());

            return ucteElements.stream()
                .filter(ucteElement -> ucteElement.tryMatching(from, to, suffix, false).matched())
                .findAny().orElse(null);

        } else {

            String beforeFrom = from.substring(0, UcteUtils.UCTE_NODE_LENGTH - 2) + Character.MIN_VALUE;
            String afterFrom = from.substring(0, UcteUtils.UCTE_NODE_LENGTH - 2) + Character.MAX_VALUE;

            List<UcteConnectable> ucteElements = connectables.asMap().subMap(beforeFrom, afterFrom).values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

            return ucteElements.stream()
                .filter(ucteElement -> ucteElement.tryMatching(from, to, suffix, false).matched())
                .findAny().orElse(null);
        }
    }
}
