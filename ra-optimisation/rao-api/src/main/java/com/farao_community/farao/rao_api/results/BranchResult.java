/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.results;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.RaoParameters;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface BranchResult {

    /**
     * It gives the flow on a {@link BranchCnec} and in a given {@link Unit}.
     *
     * @param branchCnec: The branch to be studied.
     * @param unit: The unit in which the flow is queried. Only accepted values are MEGAWATT or AMPERE.
     * @return The flow on the branch in the given unit.
     */
    double getFlow(BranchCnec branchCnec, Unit unit);

    /**
     * It gives the margin on a {@link BranchCnec} in a given {@link Unit}. It is basically the difference between the
     * flow and the most constraining threshold in the flow direction of the given branch. If it is negative the branch
     * is under constraint.
     *
     * @param branchCnec: The branch to be studied.
     * @param unit: The unit in which the margin is queried. Only accepted values are MEGAWATT or AMPERE.
     * @return The margin on the branch in the given unit.
     */
    default double getMargin(BranchCnec branchCnec, Unit unit) {
        return branchCnec.computeMargin(getFlow(branchCnec, unit), Side.LEFT, unit);
    }

    /**
     * It gives the relative margin (according to CORE D-2 CC methodology) on a {@link BranchCnec} in a given
     * {@link Unit}. If the margin is negative it gives it directly (same value as {@code getMargin} method. If the
     * margin is positive it gives this value divided by the sum of the zonal PTDFs on this branch of the studied zone.
     * Zones to include in this computation are defined in the {@link RaoParameters}. If it is negative the branch is
     * under constraint. If the PTDFs are not defined in the computation or the sum of them is null, this method could
     * return {@code Double.NaN} values.
     *
     * @param branchCnec: The branch to be studied.
     * @param unit: The unit in which the relative margin is queried. Only accepted values are MEGAWATT or AMPERE.
     * @return The relative margin on the branch in the given unit.
     */
    double getRelativeMargin(BranchCnec branchCnec, Unit unit);

    /**
     * It gives the value of commercial flow (according to CORE D-2 CC methodology) on a {@link BranchCnec} in a given
     * {@link Unit}. If the branch is not considered as a branch on which the loop flows are monitored, this method
     * could return {@code Double.NaN} values.
     *
     * @param branchCnec: The branch to be studied.
     * @param unit: The unit in which the commercial flow is queried. Only accepted values are MEGAWATT or AMPERE.
     * @return The commercial flow on the branch in the given unit.
     */
    double getCommercialFlow(BranchCnec branchCnec, Unit unit);

    /**
     * It gives the value of loop flow (according to CORE D-2 CC methodology) on a {@link BranchCnec} in a given
     * {@link Unit}. If the branch is not considered as a branch on which the loop flows are monitored, this method
     * could return {@code Double.NaN} values.
     *
     * @param branchCnec: The branch to be studied.
     * @param unit: The unit in which the loop flow is queried. Only accepted values are MEGAWATT or AMPERE.
     * @return The loop flow on the branch in the given unit.
     */
    default double getLoopFlow(BranchCnec branchCnec, Unit unit) {
        return getFlow(branchCnec, unit) - getCommercialFlow(branchCnec, unit);
    }

    /**
     * It gives the sum of the computation areas' zonal PTDFs on a {@link BranchCnec}. If the computation does not
     * consider PTDF values or if the {@link RaoParameters} does not define any list of considered areas, this method
     * could return {@code Double.NaN} values.
     *
     * @param branchCnec: The branch to be studied.
     * @return The sum of the computation areas' zonal PTDFs on the branch.
     */
    double getPtdfZonalSum(BranchCnec branchCnec);
}
