/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RedispatchingActionCreator {
    /**
     * TODO: implement importer
     * Steps:
     * 1. Get the RedispatchRemedialAction in the RA profile
     * 2. Find the associated PowerShiftKeyStrategy objects in the RA profile (link on nc:PowerShiftKeyStrategy.PowerRemedialAction)
     * 3. For each PowerShiftKeyStrategy:
     * 3.1. Find the associated PowerShiftKeyDistribution in the SSI profile (link on nc:PowerShiftKeyDistribution.PowerShiftKeyStrategy)
     * 3.2. Find the PowerShiftKeySchedule associated to the PowerShiftKeyDistribution in the SSI profile (link on nc:PowerShiftKeyDistribution.PowerShiftKeySchedule)
     * 3.3. Retrieve the generating unit from nc:PowerShiftKeySchedule.GeneratingUnit
     * 3.4. Find the ParticipationFactorTimePoint associated to the PowerShiftKeySchedule in the SSI profile (link on nc:ParticipationFactorTimePoint.PowerShiftKeySchedule)
     * 3.5. Retrieve the distribution key from nc:ParticipationFactorTimePoint.participationFactor
     * 3.6. Retrieve the bid validity from nc:ParticipationFactorTimePoint.atTime (must fit the import timestamp, otherwise throw exception)
     * 3.7. (optional) Ensure that nc:BaseTimeSeries.interpolationKind (from PowerShiftKeySchedule) is "next" or "none" -> to confirm, skip this at the moment but keep it as a TODO
     */
}
