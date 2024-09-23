/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.nc;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record ShuntCompensatorModification(String mrid, String shuntCompensatorId, String propertyReference, Boolean normalEnabled, String gridStateAlterationRemedialAction, String gridStateAlterationCollection) implements GridStateAlteration {
}
