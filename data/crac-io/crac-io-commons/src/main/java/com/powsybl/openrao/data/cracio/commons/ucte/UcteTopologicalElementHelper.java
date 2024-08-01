/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.ucte;

import com.powsybl.openrao.data.cracio.commons.ElementHelper;
import com.powsybl.iidm.network.Identifiable;

import static java.lang.String.format;

/**
 * UcteTopogicalElementHelper is a utility class which manages elements from topological
 * remedial actions, defined with the UCTE convention
 *
 * This utility class has been designed so as to be used in CRAC creators whose format
 * is based on a UCTE network and whose CRAC identifies network elements with the following
 * information: a "from node", a "to node" and a suffix. Either identified in separate fields,
 * or in a common concatenated id such as "FROMNODE TO__NODE SUFFIX".
 *
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class UcteTopologicalElementHelper extends AbstractUcteConnectableHelper implements ElementHelper {

    protected Identifiable<?> iidmIdentifiable;

    /**
     * Constructor, based on a separate fields.
     *
     * @param fromNode,             UCTE-id of the origin extremity of the branch
     * @param toNode,               UCTE-id of the destination extremity of the branch
     * @param suffix,               suffix of the branch, either an order code or an elementName
     * @param ucteNetworkAnalyzer,  UcteNetworkAnalyzer object built upon the network
     */
    public UcteTopologicalElementHelper(String fromNode, String toNode, String suffix, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
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
    public UcteTopologicalElementHelper(String fromNode, String toNode, String orderCode, String elementName, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
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
    public UcteTopologicalElementHelper(String ucteBranchId, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        super(ucteBranchId);
        if (isValid) {
            interpretWithNetworkAnalyzer(ucteNetworkAnalyzer);
        }
    }

    /**
     * If the connectable is valid, returns its type in the iidm network
     */
    public Identifiable<?> getIidmIdentifiable() {
        return this.iidmIdentifiable;
    }

    private void interpretWithNetworkAnalyzer(UcteNetworkAnalyzer ucteNetworkAnalyzer) {

        UcteMatchingResult ucteMatchingResult = ucteNetworkAnalyzer.findTopologicalElement(from, to, suffix);

        if (ucteMatchingResult.getStatus() == UcteMatchingResult.MatchStatus.NOT_FOUND) {
            invalidate(format("element was not found in the Network (from: %s, to: %s, suffix: %s)", from, to, suffix));
            return;
        }

        if (ucteMatchingResult.getStatus() == UcteMatchingResult.MatchStatus.SEVERAL_MATCH) {
            invalidate(format("several elements were found in the Network which match the description(from: %s, to: %s, suffix: %s)", from, to, suffix));
            return;
        }

        this.iidmIdentifiable = ucteMatchingResult.getIidmIdentifiable();
        this.connectableIdInNetwork = this.iidmIdentifiable.getId();
    }
}
