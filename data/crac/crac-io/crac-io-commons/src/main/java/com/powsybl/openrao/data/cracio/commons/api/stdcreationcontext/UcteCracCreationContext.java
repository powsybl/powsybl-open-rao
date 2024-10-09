/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext;

import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.ElementaryCreationContext;

import java.util.List;

/**
 * The UcteCracCreationContext is a {@link CracCreationContext} which has been designed
 * to cover information which are pertinent for UCTE CRAC formats.
 * <p>
 * It is fitted to native CRAC formats which share some common assumptions, such as:
 * <ul>
 *     <li>the native CNECs are defined for the preventive state, or for several states sharing the same contingency</li>
 *     <li>the native CNECs have a 'from', a 'to', and 'suffix' (order or elementName) field</li>
 * </ul>
 *
 * It will however not be fitted to all native CRAC formats.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface UcteCracCreationContext extends CracCreationContext {

    /**
     * Get all branch CNEC creation contexts
     */
    List<? extends BranchCnecCreationContext> getBranchCnecCreationContexts();

    /**
     * Get a specific branch CNEC creation context
     * @param branchCnecId: the native branch CNEC ID (as it figures in the native CRAC)
     */
    BranchCnecCreationContext getBranchCnecCreationContext(String branchCnecId);

    /**
     * Get all remedial-action creation contexts
     */
    List<? extends ElementaryCreationContext> getRemedialActionCreationContexts();

    /**
     * Get a specific remedial-action creation context
     * @param remedialActionId: the native remedial-action ID (as it figures in the native CRAC)
     */
    ElementaryCreationContext getRemedialActionCreationContext(String remedialActionId);
}
