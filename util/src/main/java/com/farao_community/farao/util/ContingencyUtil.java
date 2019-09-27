/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.ContingencyElement;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class ContingencyUtil {

    private ContingencyUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static void applyContingency(Network network, ComputationManager computationManager, Contingency contingency) {
        contingency.getContingencyElements().forEach(contingencyElement -> applyContingencyElement(network, computationManager, contingencyElement));
    }

    private static void applyContingencyElement(Network network, ComputationManager computationManager, ContingencyElement contingencyElement) {
        Identifiable element = network.getIdentifiable(contingencyElement.getElementId());
        if (element instanceof Branch) {
            BranchContingency contingency = new BranchContingency(contingencyElement.getElementId());
            contingency.toTask().modify(network, computationManager);
        } else if (element instanceof Switch) {
            // TODO: convert into a PowSyBl ContingencyElement ?
            Switch switchElement = (Switch) element;
            switchElement.setOpen(true);
        } else {
            throw new FaraoException("Unable to apply contingency element " + contingencyElement.getElementId());
        }
    }
}
