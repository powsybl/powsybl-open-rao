/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.ucte;

import com.powsybl.openrao.data.cracio.commons.CnecElementHelper;
import com.powsybl.iidm.network.*;

import java.util.Objects;
import java.util.Optional;

import static com.powsybl.openrao.data.cracio.commons.ucte.UcteConnectable.Side.TWO;
import static java.lang.String.format;

/**
 * UcteFlowElementHelper is a utility class which manages CNECs defined with the UCTE convention.
 * This utility class has been designed so as to be used in CRAC creators whose format
 * is based on a UCTE network and whose CRAC identifies network elements with the following
 * information: a "from node", a "to node" and a suffix. Either identified in separate fields,
 * or in a common concatenated id such as "FROMNODE TO__NODE SUFFIX".
 *
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class UcteFlowElementHelper extends AbstractUcteConnectableHelper implements CnecElementHelper {

    private boolean isInvertedInNetwork = false;
    private TwoSides halfLineSide = null;
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
    public UcteFlowElementHelper(String fromNode, String toNode, String suffix, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
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
    public UcteFlowElementHelper(String fromNode, String toNode, String orderCode, String elementName, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
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
    public UcteFlowElementHelper(String ucteBranchId, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
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
    public double getNominalVoltage(TwoSides side) {
        if (side.equals(TwoSides.ONE)) {
            return nominalVoltageLeft;
        } else {
            return nominalVoltageRight;
        }
    }

    @Override
    public double getCurrentLimit(TwoSides side) {
        if (side.equals(TwoSides.ONE)) {
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
    public TwoSides getHalfLineSide() {
        return halfLineSide;
    }

    protected void interpretWithNetworkAnalyzer(UcteNetworkAnalyzer networkAnalyzer) {

        UcteMatchingResult ucteMatchingResult = networkAnalyzer.findFlowElement(from, to, suffix);

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

        if (networkElement instanceof TieLine tieLine) {
            interpretTieLine(tieLine, ucteMatchingResult.getSide() == TWO ? TwoSides.TWO : TwoSides.ONE);
        } else if (networkElement instanceof Branch<?> branch) {
            checkBranchNominalVoltage(branch);
            checkBranchCurrentLimits(branch);
        } else if (networkElement instanceof DanglingLine danglingLine) {
            interpretDanglingLine(danglingLine);
        }
    }

    private void interpretTieLine(TieLine tieLine, TwoSides side) {
        this.isHalfLine = true;
        this.halfLineSide = side;
        checkBranchNominalVoltage(tieLine);
        checkTieLineCurrentLimits(tieLine);
    }

    private void checkTieLineCurrentLimits(TieLine tieLine) {
        if (tieLine.getCurrentLimits(TwoSides.ONE).isPresent()) {
            this.currentLimitLeft = tieLine.getCurrentLimits(TwoSides.ONE).orElseThrow().getPermanentLimit();
        }
        if (tieLine.getCurrentLimits(TwoSides.TWO).isPresent()) {
            this.currentLimitRight = tieLine.getCurrentLimits(TwoSides.TWO).orElseThrow().getPermanentLimit();
        }
        if (Objects.isNull(tieLine.getCurrentLimits(TwoSides.ONE)) && Objects.isNull(tieLine.getCurrentLimits(TwoSides.TWO))) {
            invalidate(format("couldn't identify current limits of tie-line (from: %s, to: %s, suffix: %s, networkTieLineId: %s)", from, to, suffix, tieLine.getId()));
        }
    }

    protected void checkBranchNominalVoltage(Branch<?> branch) {
        this.nominalVoltageLeft = branch.getTerminal1().getVoltageLevel().getNominalV();
        this.nominalVoltageRight = branch.getTerminal2().getVoltageLevel().getNominalV();
    }

    private void interpretDanglingLine(DanglingLine danglingLine) {
        Optional<TieLine> optionalTieLine = danglingLine.getTieLine();
        if (optionalTieLine.isPresent()) {
            TieLine tieLine = optionalTieLine.get();
            this.connectableIdInNetwork = tieLine.getId();
            TwoSides side = tieLine.getDanglingLine1() == danglingLine ? TwoSides.ONE : TwoSides.TWO;
            // dangling line convention is x node to terminal, so dl 1 is towards terminal 1 (opposite) and dl 2 is towards terminal 2 (direct)
            this.isInvertedInNetwork = tieLine.getDanglingLine1() == danglingLine ? !isInvertedInNetwork : isInvertedInNetwork;
            interpretTieLine(tieLine, side);
        } else {
            checkDanglingLineNominalVoltage(danglingLine);
            checkDanglingLineCurrentLimits(danglingLine);
        }
    }

    protected void checkDanglingLineNominalVoltage(DanglingLine danglingLine) {
        this.nominalVoltageLeft = danglingLine.getTerminal().getVoltageLevel().getNominalV();
        this.nominalVoltageRight = nominalVoltageLeft;
    }

    protected void checkBranchCurrentLimits(Branch<?> branch) {
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
            invalidate(format("couldn't identify current limits of branch (%s, networkBranchId: %s)", connectableId, branch.getId()));
        }
    }

    protected void checkDanglingLineCurrentLimits(DanglingLine danglingLine) {
        if (danglingLine.getCurrentLimits().isPresent()) {
            this.currentLimitLeft = danglingLine.getCurrentLimits().orElseThrow().getPermanentLimit();
            this.currentLimitRight = currentLimitLeft;
        } else {
            invalidate(format("couldn't identify current limits of dangling line (%s, networkDanglingLineId: %s)", connectableId, danglingLine.getId()));
        }
    }

}
