/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util.ucte;

import com.farao_community.farao.data.crac_creation_util.CnecElementHelper;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.TieLine;

import java.util.Objects;

import static com.farao_community.farao.data.crac_creation_util.ucte.UcteConnectable.Side.TWO;
import static java.lang.String.format;

/**
 * UcteCnecElementHelper is a utility class which manages CNECs defined with the UCTE convention
 *
 * This utility class has been designed so as to be used in CRAC creators whose format
 * is based on a UCTE network and whose CRAC identifies network elements with the following
 * information: a "from node", a "to node" and a suffix. Either identified in separate fields,
 * or in a common concatenated id such as "FROMNODE TO__NODE SUFFIX".
 *
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class UcteCnecElementHelper extends AbstractUcteConnectableHelper implements CnecElementHelper {

    private boolean isInvertedInNetwork = false;
    private Branch.Side halfLineSide = null;
    private boolean isHalfLine = false;

    protected Double nominalVoltageLeft;
    protected Double nominalVoltageRight;
    protected Double currentLimitLeft;
    protected Double currentLimitRight;

    /**
     * Constructor, based on a separate fields.
     *
     * @param fromNode,             UCTE-id of the origin extremity of the branch
     * @param toNode,               UCTE-id of the destination extremity of the branch
     * @param suffix,               suffix of the branch, either an order code or an elementName
     * @param ucteNetworkAnalyzer,  UcteNetworkAnalyzer object built upon the network
     */
    public UcteCnecElementHelper(String fromNode, String toNode, String suffix, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        super(fromNode, toNode, suffix);
        if (isValid) {
            interpretWithNetworkAnalyzer(ucteNetworkAnalyzer);
        }
    }

    /**
     * Constructor, based on a separate fields. Either the order code, or the element name must be
     * non-null. If the two are defined, the suffix which will be used by default is the order code.
     *
     * @param fromNode,             UCTE-id of the origin extremity of the branch
     * @param toNode,               UCTE-id of the destination extremity of the branch
     * @param orderCode,            order code of the branch
     * @param elementName,          element name of the branch
     * @param ucteNetworkAnalyzer,  UcteNetworkAnalyzer object built upon the network
     */
    public UcteCnecElementHelper(String fromNode, String toNode, String orderCode, String elementName, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        super(fromNode, toNode, orderCode, elementName);
        if (isValid) {
            interpretWithNetworkAnalyzer(ucteNetworkAnalyzer);
        }
    }

    /**
     * Constructor, based on a concatenated id.
     *
     * @param ucteBranchId,         concatenated UCTE branch id, of the form "FROMNODE TO__NODE SUFFIX"
     * @param ucteNetworkAnalyzer,  UcteNetworkAnalyzer object built upon the network
     */
    public UcteCnecElementHelper(String ucteBranchId, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        super(ucteBranchId);
        if (isValid) {
            interpretWithNetworkAnalyzer(ucteNetworkAnalyzer);
        }
    }

    /**
     * If the branch is valid, returns a boolean indicating whether or not the from/to are
     * inverted in the network, compared to the values originally used in the constructor
     * of the UcteBranchHelper
     */
    @Override
    public boolean isInvertedInNetwork() {
        return isInvertedInNetwork;
    }

    @Override
    public double getNominalVoltage(Branch.Side side) {
        if (side.equals(Branch.Side.ONE)) {
            return nominalVoltageLeft;
        } else {
            return nominalVoltageRight;
        }
    }

    @Override
    public double getCurrentLimit(Branch.Side side) {
        if (side.equals(Branch.Side.ONE)) {
            return currentLimitLeft;
        } else {
            return currentLimitRight;
        }
    }

    @Override
    public boolean isHalfLine() {
        return isHalfLine;
    }

    @Override
    public Branch.Side getHalfLineSide() {
        return halfLineSide;
    }

    protected void interpretWithNetworkAnalyzer(UcteNetworkAnalyzer networkAnalyzer) {

        UcteMatchingResult ucteMatchingResult = networkAnalyzer.findCnecElement(from, to, suffix);

        if (ucteMatchingResult.getStatus() == UcteMatchingResult.MatchStatus.NOT_FOUND) {
            invalidate(format("branch was not found in the Network (from: %s, to: %s, suffix: %s)", from, to, suffix));
            return;
        }

        if (ucteMatchingResult.getStatus() == UcteMatchingResult.MatchStatus.SEVERAL_MATCH) {
            invalidate(format("several branches were found in the Network which match the description(from: %s, to: %s, suffix: %s)", from, to, suffix));
            return;
        }

        this.isInvertedInNetwork = ucteMatchingResult.isInverted();
        Identifiable<?> networkElement = ucteMatchingResult.getIidmIdentifiable();
        this.connectableIdInNetwork = networkElement.getId();

        if (networkElement instanceof TieLine) {
            this.isHalfLine = true;
            this.halfLineSide = ucteMatchingResult.getSide() == TWO ? Branch.Side.TWO : Branch.Side.ONE;
            checkBranchNominalVoltage((TieLine) networkElement);
            checkTieLineCurrentLimits((TieLine) networkElement);
        } else if (networkElement instanceof Branch) {
            checkBranchNominalVoltage((Branch) networkElement);
            checkBranchCurrentLimits((Branch) networkElement);
        } else if (networkElement instanceof DanglingLine) {
            checkDanglingLineNominalVoltage((DanglingLine) networkElement);
            checkDanglingLineCurrentLimits((DanglingLine) networkElement);
        }
    }

    private void checkTieLineCurrentLimits(TieLine tieLine) {
        if (!Objects.isNull(tieLine.getCurrentLimits(Branch.Side.ONE))) {
            this.currentLimitLeft = tieLine.getCurrentLimits(Branch.Side.ONE).getPermanentLimit();
        }
        if (!Objects.isNull(tieLine.getCurrentLimits(Branch.Side.TWO))) {
            this.currentLimitRight = tieLine.getCurrentLimits(Branch.Side.TWO).getPermanentLimit();
        }
        if (Objects.isNull(tieLine.getCurrentLimits(Branch.Side.ONE)) && Objects.isNull(tieLine.getCurrentLimits(Branch.Side.TWO))) {
            invalidate(String.format("couldn't identify current limits of tie-line (from: %s, to: %s, suffix: %s, networkTieLineId: %s)", from, to, suffix, tieLine.getId()));
        }
    }

    protected void checkBranchNominalVoltage(Branch<?> branch) {
        this.nominalVoltageLeft = branch.getTerminal1().getVoltageLevel().getNominalV();
        this.nominalVoltageRight = branch.getTerminal2().getVoltageLevel().getNominalV();
    }

    protected void checkDanglingLineNominalVoltage(DanglingLine danglingLine) {
        this.nominalVoltageLeft = danglingLine.getTerminal().getVoltageLevel().getNominalV();
        this.nominalVoltageRight = nominalVoltageLeft;
    }

    protected void checkBranchCurrentLimits(Branch<?> branch) {
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
            invalidate(String.format("couldn't identify current limits of branch (%s, networkBranchId: %s)", connectableId, branch.getId()));
        }
    }

    protected void checkDanglingLineCurrentLimits(DanglingLine danglingLine) {
        if (!Objects.isNull(danglingLine.getCurrentLimits())) {
            this.currentLimitLeft = danglingLine.getCurrentLimits().getPermanentLimit();
            this.currentLimitRight = currentLimitLeft;
        } else {
            invalidate(String.format("couldn't identify current limits of dangling line (%s, networkDanglingLineId: %s)", connectableId, danglingLine.getId()));
        }
    }

}
