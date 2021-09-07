/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util.iidm;

import com.farao_community.farao.data.crac_creation_util.CnecElementHelper;
import com.powsybl.iidm.network.*;

import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Utility class to be used in Crac creators.
 * It exposes useful functions to synchronize the Crac with the Network.
 *
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class IidmCnecElementHelper implements CnecElementHelper {

    protected String branchId;

    protected boolean isBranchValid = true;
    protected String invalidBranchReason;

    protected String branchIdInNetwork;
    protected Double nominalVoltageLeft;
    protected Double nominalVoltageRight;
    protected Double currentLimitLeft;
    protected Double currentLimitRight;
    private boolean isHalfLine = false;
    private Branch.Side halfLineSide = null;

    public IidmCnecElementHelper(String iidmId, Network network) {
        if (Objects.isNull(iidmId)) {
            invalidate("branchId must not be null");
            return;
        }

        this.branchId = iidmId;
        interpretWithNetwork(network);
    }

    @Override
    public boolean isValid() {
        return isBranchValid;
    }

    @Override
    public String getInvalidReason() {
        return invalidBranchReason;
    }

    @Override
    public String getIdInNetwork() {
        return branchIdInNetwork;
    }

    @Override
    public boolean isInvertedInNetwork() {
        return false;
    }

    @Override
    public boolean isHalfLine() {
        return isHalfLine;
    }

    @Override
    public Branch.Side getHalfLineSide() {
        return halfLineSide;
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

    private void interpretWithNetwork(Network network) {

        if (interpretAsNetworkIdentifiable(network)) {
            return;
        }
        if (interpretAsHalfLine(network)) {
            return;
        }
        invalidate(format("branch %s was not found in the Network", branchId));

    }

    private boolean interpretAsNetworkIdentifiable(Network network) {

        Identifiable<?> cnecElement = network.getIdentifiable(branchId);

        if (Objects.isNull(cnecElement)) {
            return false;
        }
        this.branchIdInNetwork = cnecElement.getId();

        if (cnecElement instanceof TieLine) {
            checkBranchNominalVoltage((Branch<?>) cnecElement);
            checkTieLineCurrentLimits((TieLine) cnecElement);
        }
        if (cnecElement instanceof Branch) {
            checkBranchNominalVoltage((Branch<?>) cnecElement);
            checkBranchCurrentLimits((Branch<?>) cnecElement);
        } else if (cnecElement instanceof DanglingLine) {
            checkDanglingLineNominalVoltage((DanglingLine) cnecElement);
            checkDanglingLineCurrentLimits((DanglingLine) cnecElement);
        } else {
            invalidate(String.format("iidm element %s of class %s is not suited to be a Cnec", branchId, cnecElement.getClass()));
        }
        return true;
    }

    private boolean interpretAsHalfLine(Network network) {
        Optional<TieLine> tieLine = network.getBranchStream()
                .filter(TieLine.class::isInstance)
                .map(TieLine.class::cast)
                .filter(b -> b.getHalf1().getId().equals(branchId) || b.getHalf2().getId().equals(branchId))
                .findAny();

        if (tieLine.isEmpty()) {
            return false;
        }

        this.branchIdInNetwork = tieLine.get().getId();
        this.isHalfLine = true;
        this.halfLineSide = tieLine.get().getHalf1().getId().equals(branchId) ? Branch.Side.ONE : Branch.Side.TWO;
        checkBranchNominalVoltage(tieLine.get());
        checkTieLineCurrentLimits(tieLine.get());
        // todo: check if halfLine can be inverted in CGMES format
        return true;
    }

    private void checkBranchNominalVoltage(Branch<?> branch) {
        this.nominalVoltageLeft = branch.getTerminal1().getVoltageLevel().getNominalV();
        this.nominalVoltageRight = branch.getTerminal2().getVoltageLevel().getNominalV();
    }

    private void checkDanglingLineNominalVoltage(DanglingLine danglingLine) {
        this.nominalVoltageLeft = danglingLine.getTerminal().getVoltageLevel().getNominalV();
        this.nominalVoltageRight = nominalVoltageLeft;
    }

    private void checkTieLineCurrentLimits(TieLine tieLine) {
        if (!Objects.isNull(tieLine.getCurrentLimits(Branch.Side.ONE))) {
            this.currentLimitLeft = tieLine.getCurrentLimits(Branch.Side.ONE).getPermanentLimit();
        }
        if (!Objects.isNull(tieLine.getCurrentLimits(Branch.Side.TWO))) {
            this.currentLimitRight = tieLine.getCurrentLimits(Branch.Side.TWO).getPermanentLimit();
        }
        if (Objects.isNull(tieLine.getCurrentLimits(Branch.Side.ONE)) && Objects.isNull(tieLine.getCurrentLimits(Branch.Side.TWO))) {
            invalidate(String.format("couldn't identify current limits of tie-line (%s, networkTieLineId: %s)", branchId, tieLine.getId()));
        }
    }

    private void checkBranchCurrentLimits(Branch<?> branch) {
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

    private void checkDanglingLineCurrentLimits(DanglingLine danglingLine) {
        if (!Objects.isNull(danglingLine.getCurrentLimits())) {
            this.currentLimitLeft = danglingLine.getCurrentLimits().getPermanentLimit();
            this.currentLimitRight = currentLimitLeft;
        } else {
            invalidate(String.format("couldn't identify current limits of dangling line (%s, networkDanglingLineId: %s)", branchId, danglingLine.getId()));
        }
    }

    private void invalidate(String invalidReason) {
        this.isBranchValid = false;
        this.invalidBranchReason = invalidReason;
    }

}
