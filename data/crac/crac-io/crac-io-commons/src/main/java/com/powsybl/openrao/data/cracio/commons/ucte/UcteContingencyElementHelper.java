/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.ucte;

import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.data.cracio.commons.ElementHelper;
import com.powsybl.iidm.network.Identifiable;

import static java.lang.String.format;

/**
 * UcteContingencyElementHelper is a utility class which manages contingencies defined
 * with the UCTE convention
 *
 * This utility class has been designed so as to be used in CRAC creators whose format
 * is based on a UCTE network and whose CRAC identifies network elements with the following
 * information: a "from node", a "to node" and a suffix. Either identified in separate fields,
 * or in a common concatenated id such as "FROMNODE TO__NODE SUFFIX".
 *
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class UcteContingencyElementHelper extends AbstractUcteConnectableHelper implements ElementHelper {

    protected ContingencyElementType connectableContingencyTypeInNetwork;

    /**
     * Constructor, based on a separate fields.
     *
     * @param fromNode,      UCTE-id of the origin extremity of the branch
     * @param toNode,        UCTE-id of the destination extremity of the branch
     * @param suffix,        suffix of the branch, either an order code or an elementName
     * @param ucteNetworkAnalyzer, UcteNetworkHelper object built upon the network
     */
    public UcteContingencyElementHelper(String fromNode, String toNode, String suffix, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        super(fromNode, toNode, suffix);
        if (isValid) {
            interpretWithNetworkAnalyzer(ucteNetworkAnalyzer);
        }
    }

    /**
     * Constructor, based on a separate fields. Either the order code, or the element name must be
     * non-null. If the two are defined, the suffix which will be used by default is the order code.
     *
     * @param fromNode,      UCTE-id of the origin extremity of the branch
     * @param toNode,        UCTE-id of the destination extremity of the branch
     * @param orderCode,     order code of the branch
     * @param elementName,   element name of the branch
     * @param ucteNetworkAnalyzer, UcteNetworkHelper object built upon the network
     */
    public UcteContingencyElementHelper(String fromNode, String toNode, String orderCode, String elementName, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        super(fromNode, toNode, orderCode, elementName);
        if (isValid) {
            interpretWithNetworkAnalyzer(ucteNetworkAnalyzer);
        }
    }

    /**
     * Constructor, based on a concatenated id.
     *
     * @param ucteBranchId,  concatenated UCTE branch id, of the form "FROMNODE TO__NODE SUFFIX"
     * @param ucteNetworkAnalyzer, UcteNetworkHelper object built upon the network
     */
    public UcteContingencyElementHelper(String ucteBranchId, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        super(ucteBranchId);
        if (isValid) {
            interpretWithNetworkAnalyzer(ucteNetworkAnalyzer);
        }
    }

    public ContingencyElementType getContingencyTypeInNetwork() {
        return connectableContingencyTypeInNetwork;
    }

    private void interpretWithNetworkAnalyzer(UcteNetworkAnalyzer ucteNetworkAnalyzer) {

        UcteMatchingResult ucteMatchingResult = ucteNetworkAnalyzer.findContingencyElement(from, to, suffix);

        if (ucteMatchingResult.getStatus() == UcteMatchingResult.MatchStatus.NOT_FOUND) {
            invalidate(format("branch was not found in the Network (from: %s, to: %s, suffix: %s)", from, to, suffix));
            return;
        }

        if (ucteMatchingResult.getStatus() == UcteMatchingResult.MatchStatus.SEVERAL_MATCH) {
            invalidate(format("several branches were found in the Network which match the description(from: %s, to: %s, suffix: %s)", from, to, suffix));
            return;
        }

        Identifiable<?> networkElement = ucteMatchingResult.getIidmIdentifiable();

        this.connectableIdInNetwork = networkElement.getId();
        this.connectableContingencyTypeInNetwork = ContingencyElement.of(networkElement).getType();

    }
}
