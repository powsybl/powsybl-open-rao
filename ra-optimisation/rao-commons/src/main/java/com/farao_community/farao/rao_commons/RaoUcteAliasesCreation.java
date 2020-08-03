/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.ucte.network.UcteElementId;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.powsybl.ucte.network.UcteElementId.parseUcteElementId;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoUcteAliasesCreation {

    private static final String ALIAS_TRIPLET_TEMPLATE = "%1$-8s %2$-8s %3$s";
    private static final String ELEMENT_NAME_PROPERTY_KEY = "elementName";
    private static final String NOT_PRESENT_ELEMENT_NAME = "N/A";

    private RaoUcteAliasesCreation() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static void createAliases(Network network) {
        Set<String> duplicatedAliases = new HashSet<>();
        network.getBranchStream().forEach(branch -> {
            if (branch instanceof TieLine) {
                TieLine tieLine = (TieLine) branch;
                tieLine.addAlias(tieLine.getHalf1().getId());
                tieLine.addAlias(tieLine.getHalf2().getId());
                addHalfElementNameAliases(tieLine, duplicatedAliases);
            }
            addElementNameAliases(branch, duplicatedAliases);
        });
        network.getSwitchStream().forEach(switchEl -> addElementNameAliases(switchEl, duplicatedAliases));
        network.getDanglingLineStream().forEach(dl -> addElementNameAliases(dl, duplicatedAliases));
    }

    private static void addElementNameAliases(Identifiable<?> identifiable, UcteElementId ucteElementId, String elementNameProperty, Set<String> duplicatedAliases) {
        String node1 = ucteElementId.getNodeCode1().toString().trim();
        String node2 = ucteElementId.getNodeCode2().toString().trim();
        safeAddAlias(identifiable, duplicatedAliases, String.format(ALIAS_TRIPLET_TEMPLATE,
            node1.substring(0, node1.length() - 1),
            node2.substring(0, node2.length() - 1),
            String.valueOf(ucteElementId.getOrderCode())));
        if (elementNameProperty != null && !elementNameProperty.isEmpty() && !elementNameProperty.equals(NOT_PRESENT_ELEMENT_NAME)) {
            safeAddAlias(identifiable, duplicatedAliases, String.format(ALIAS_TRIPLET_TEMPLATE,
                node1.substring(0, node1.length() - 1),
                node2.substring(0, node2.length() - 1),
                elementNameProperty));
        }
    }

    private static void addElementNameAliases(Identifiable<?> identifiable, Set<String> duplicatedAliases) {
        Optional<UcteElementId> ucteElementIdOptional = parseUcteElementId(identifiable.getId());
        if (ucteElementIdOptional.isPresent()) {
            UcteElementId ucteElementId = ucteElementIdOptional.get();
            addElementNameAliases(identifiable, ucteElementId, identifiable.getProperty(ELEMENT_NAME_PROPERTY_KEY), duplicatedAliases);
        }
    }

    private static void addHalfElementNameAliases(TieLine tieLine, Set<String> duplicatedAliases) {
        String elementName1Property = tieLine.getProperty(ELEMENT_NAME_PROPERTY_KEY + "_1");
        Optional<UcteElementId> ucteElementIdOptional = parseUcteElementId(tieLine.getHalf1().getId());
        if (ucteElementIdOptional.isPresent()) {
            UcteElementId ucteElementId = ucteElementIdOptional.get();
            addElementNameAliases(tieLine, ucteElementId, elementName1Property, duplicatedAliases);
        }
        String elementName2Property = tieLine.getProperty(ELEMENT_NAME_PROPERTY_KEY + "_2");
        ucteElementIdOptional = parseUcteElementId(tieLine.getHalf2().getId());
        if (ucteElementIdOptional.isPresent()) {
            UcteElementId ucteElementId = ucteElementIdOptional.get();
            addElementNameAliases(tieLine, ucteElementId, elementName2Property, duplicatedAliases);
        }
    }

    private static void safeAddAlias(Identifiable<?> identifiable, Set<String> duplicatedAliases, String alias) {
        if (duplicatedAliases.contains(alias)) {
            return;
        }

        Identifiable<?> alreadyAssignedIdentifiable = identifiable.getNetwork().getIdentifiable(alias);
        if (alreadyAssignedIdentifiable != null && alreadyAssignedIdentifiable != identifiable) {
            if (!alreadyAssignedIdentifiable.getId().equals(alias)) {
                alreadyAssignedIdentifiable.removeAlias(alias);
            }
            duplicatedAliases.add(alias);
            return;
        }

        identifiable.addAlias(alias);
    }
}
