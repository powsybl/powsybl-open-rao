/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.iidm;

import com.powsybl.openrao.data.cracio.commons.CnecElementHelper;
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
    private TwoSides halfLineSide = null;

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
    public TwoSides getHalfLineSide() {
        return halfLineSide;
    }

    /**
     * If the branch is valid, returns the nominal voltage on a given side of the Branch
     * The side corresponds to the side of the branch in the network, which might be inverted
     * compared from the from/to nodes of the UcteBranch (see isInvertedInNetwork()).
     */
    public double getNominalVoltage(TwoSides side) {
        if (side.equals(TwoSides.ONE)) {
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
    public double getCurrentLimit(TwoSides side) {
        if (side.equals(TwoSides.ONE)) {
            return currentLimitLeft;
        } else {
            return currentLimitRight;
        }
    }

    private void interpretWithNetwork(Network network) {

        if (interpretAsNetworkIdentifiable(network)) {
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

        if (cnecElement instanceof TieLine tieLine) {
            checkBranchNominalVoltage(tieLine);
            checkTieLineCurrentLimits(tieLine);
        } else if (cnecElement instanceof Branch<?> branch) {
            checkBranchNominalVoltage(branch);
            checkBranchCurrentLimits(branch);
        } else if (cnecElement instanceof DanglingLine danglingLine) {
            if (danglingLine.isPaired()) {
                return interpretAsHalfLine(network);
            }
            checkDanglingLineNominalVoltage(danglingLine);
            checkDanglingLineCurrentLimits(danglingLine);
        } else {
            invalidate(String.format("iidm element %s of class %s is not suited to be a Cnec", branchId, cnecElement.getClass()));
        }
        return true;
    }

    private boolean interpretAsHalfLine(Network network) {
        Optional<TieLine> tieLine = ((DanglingLine) network.getIdentifiable(branchId)).getTieLine();

        if (tieLine.isEmpty()) {
            return false;
        }

        this.branchIdInNetwork = tieLine.get().getId();
        this.isHalfLine = true;
        this.halfLineSide = tieLine.get().getDanglingLine1().getId().equals(branchId) ? TwoSides.ONE : TwoSides.TWO;
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
        if (tieLine.getCurrentLimits(TwoSides.ONE).isPresent()) {
            this.currentLimitLeft = tieLine.getCurrentLimits(TwoSides.ONE).orElseThrow().getPermanentLimit();
        }
        if (tieLine.getCurrentLimits(TwoSides.TWO).isPresent()) {
            this.currentLimitRight = tieLine.getCurrentLimits(TwoSides.TWO).orElseThrow().getPermanentLimit();
        }
        if (Objects.isNull(tieLine.getCurrentLimits(TwoSides.ONE)) && Objects.isNull(tieLine.getCurrentLimits(TwoSides.TWO))) {
            invalidate(String.format("couldn't identify current limits of tie-line (%s, networkTieLineId: %s)", branchId, tieLine.getId()));
        }
    }

    private void checkBranchCurrentLimits(Branch<?> branch) {
        if (branch.getCurrentLimits1().isPresent()) {
            this.currentLimitLeft = branch.getCurrentLimits1().orElseThrow().getPermanentLimit();
        }
        if (branch.getCurrentLimits2().isPresent()) {
            this.currentLimitRight = branch.getCurrentLimits2().orElseThrow().getPermanentLimit();
        }
        if (branch.getCurrentLimits1().isEmpty() && branch.getCurrentLimits2().isPresent()) {
            this.currentLimitLeft = currentLimitRight * nominalVoltageRight / nominalVoltageLeft;
        }
        if (branch.getCurrentLimits1().isPresent() && branch.getCurrentLimits2().isEmpty()) {
            this.currentLimitRight = currentLimitLeft * nominalVoltageLeft / nominalVoltageRight;
        }
        if (branch.getCurrentLimits1().isEmpty() && branch.getCurrentLimits2().isEmpty()) {
            invalidate(String.format("couldn't identify current limits of branch (%s, networkBranchId: %s)", branchId, branch.getId()));
        }
    }

    private void checkDanglingLineCurrentLimits(DanglingLine danglingLine) {
        if (danglingLine.getCurrentLimits().isPresent()) {
            this.currentLimitLeft = danglingLine.getCurrentLimits().orElseThrow().getPermanentLimit();
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
