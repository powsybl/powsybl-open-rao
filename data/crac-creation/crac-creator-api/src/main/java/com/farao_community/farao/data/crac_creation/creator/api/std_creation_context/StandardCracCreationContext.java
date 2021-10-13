/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.api.std_creation_context;

import com.farao_community.farao.data.crac_creation.creator.api.CracCreationContext;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The StandardCracCreationContext is a {@link CracCreationContext} which has been designed so
 * as to cover information which are pertinent for several native CRAC formats.
 *
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
public interface StandardCracCreationContext extends CracCreationContext {

    List<? extends BranchCnecCreationContext> getBranchCnecCreationContexts();

    BranchCnecCreationContext getBranchCnecCreationContext(String brancCnecId);

    List<? extends RemedialActionCreationContext> getRemedialActionCreationContexts();

    RemedialActionCreationContext getRemedialActionCreationContext(String remedialActionId);

    OffsetDateTime getTimeStamp();

    String getNetworkName();
}
