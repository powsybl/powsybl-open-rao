/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.network.*;

import java.util.Objects;

import static java.lang.String.format;

/**
 * Utility class to be used in Crac creators.
 * It exposes useful functions to synchronize the Crac with the Network.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class BranchHelper {
    private String branchId;
    private boolean isBranchValid = true;
    private String invalidBranchReason = "";
    protected String branchIdInNetwork;
    private Double nominalVoltageLeft = null;
    private Double nominalVoltageRight = null;
    protected Double currentLimitLeft = null;
    protected Double currentLimitRight = null;

    protected BranchHelper(String branchId) {
        this.branchId = branchId;
    }

    public BranchHelper(String branchId, Network network) {
        this.branchId = branchId;
        if (Objects.isNull(branchId)) {
            invalidate("branchId must not be null");
            return;
        }
        interpretWithNetwork(network);
    }

    /**
     * Returns a boolean indicating whether or not the branch is considered valid in the network
     */
    public boolean isBranchValid() {
        return isBranchValid;
    }

    /**
     * If the branch is not valid, returns the reason why it is considered invalid
     */
    public String getInvalidBranchReason() {
        return invalidBranchReason;
    }

    /**
     * If the branch is valid, returns its corresponding id in the PowSyBl Network
     */
    public String getBranchIdInNetwork() {
        return branchIdInNetwork;
    }

    /**
     * If the branch is valid, returns the nominal voltage on a given side of the Branch
     * The side corresponds to the side of the branch in the network, which might be inverted
     * compared from the from/to nodes of the UcteBranch (see isInvertedInNetwork()).
     */
    public double getNominalVoltage(Branch.Side side) {
        if (side.equals(Branch.Side.ONE)) {
            return nominalVoltageLeft;
        } else {
            return nominalVoltageRight;
        }
    }

    /**
     * If the branch is valid, returns the current limit on a given side of the Branch.
     * The side corresponds to the side of the branch in the network, which might be inverted
     * compared from the from/to nodes of the UcteBranch (see isInvertedInNetwork()).
     */
    public double getCurrentLimit(Branch.Side side) {
        if (side.equals(Branch.Side.ONE)) {
            return currentLimitLeft;
        } else {
            return currentLimitRight;
        }
    }

    protected void interpretWithNetwork(Network network) {
        Identifiable<?> networkElement = findEquivalentElementInNetwork(network);
        if (Objects.isNull(networkElement)) {
            return;
        }
        if (networkElement instanceof Branch) {
            checkBranchNominalVoltage((Branch) networkElement);
            checkBranchCurrentLimits((Branch) networkElement);
        } else if (networkElement instanceof DanglingLine) {
            checkDanglingLineNominalVoltage((DanglingLine) networkElement);
            checkDanglingLineCurrentLimits((DanglingLine) networkElement);
        }
    }

    protected Identifiable findEquivalentElementInNetwork(Network network) {
        Identifiable<?> branch = network.getIdentifiable(branchId);
        if (!Objects.isNull(branch)) {
            this.branchIdInNetwork = branch.getId();
            return branch;
        } else {
            invalidate(format("branch %s was not found in the Network", branchId));
            return null;
        }
    }

    protected void checkBranchNominalVoltage(Branch branch) {
        this.nominalVoltageLeft = branch.getTerminal1().getVoltageLevel().getNominalV();
        this.nominalVoltageRight = branch.getTerminal2().getVoltageLevel().getNominalV();
    }

    protected void checkDanglingLineNominalVoltage(DanglingLine danglingLine) {
        this.nominalVoltageLeft = danglingLine.getTerminal().getVoltageLevel().getNominalV();
        this.nominalVoltageRight = nominalVoltageLeft;
    }

    protected void checkBranchCurrentLimits(Branch branch) {
        if (!Objects.isNull(branch.getCurrentLimits1())) {
            this.currentLimitLeft = branch.getCurrentLimits1().getPermanentLimit();
        }
        if (!Objects.isNull(branch.getCurrentLimits2())) {
            this.currentLimitRight = branch.getCurrentLimits2().getPermanentLimit();
        }
        if (Objects.isNull(branch.getCurrentLimits1()) && !Objects.isNull(branch.getCurrentLimits2())) {
            this.currentLimitLeft = currentLimitRight * nominalVoltageRight / nominalVoltageLeft;
        }
        if (!Objects.isNull(branch.getCurrentLimits1()) && Objects.isNull(branch.getCurrentLimits2())) {
            this.currentLimitRight = currentLimitLeft * nominalVoltageLeft / nominalVoltageRight;
        }
        if (Objects.isNull(branch.getCurrentLimits1()) && Objects.isNull(branch.getCurrentLimits2())) {
            invalidate(String.format("couldn't identify current limits of branch (%s, networkBranchId: %s)", branchId, branch.getId()));
        }
    }

    protected void checkDanglingLineCurrentLimits(DanglingLine danglingLine) {
        if (!Objects.isNull(danglingLine.getCurrentLimits())) {
            this.currentLimitLeft = danglingLine.getCurrentLimits().getPermanentLimit();
            this.currentLimitRight = currentLimitLeft;
        } else {
            invalidate(String.format("couldn't identify current limits of dangling line (%s, networkDanglingLineId: %s)", branchId, danglingLine.getId()));
        }
    }

    protected void invalidate(String invalidReason) {
        this.isBranchValid = false;
        this.invalidBranchReason = invalidReason;
    }
}
