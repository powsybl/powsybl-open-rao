/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.networkextension;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Injection;

import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class AssignedVirtualHubImpl<T extends Injection<T>> extends AbstractExtension<T> implements AssignedVirtualHub<T> {

    private final String code;
    private final String eic;
    private final boolean isMcParticipant;
    private final String nodeName;
    private final String relatedMa;

    public AssignedVirtualHubImpl(String code, String eic, boolean isMcParticipant, String nodeName, String relatedMa) {
        this.code = code;
        this.eic = Objects.requireNonNull(eic, "AssignedVirtualHubImpl creation does not allow null eic");
        this.isMcParticipant = isMcParticipant;
        this.nodeName = nodeName;
        this.relatedMa = relatedMa;
    }

    public String getCode() {
        return code;
    }

    public String getEic() {
        return eic;
    }

    public boolean isMcParticipant() {
        return isMcParticipant;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getRelatedMa() {
        return relatedMa;
    }

}
