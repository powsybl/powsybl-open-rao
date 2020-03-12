/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.utils;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class NetworkImportsUtil {

    private NetworkImportsUtil() {

    }

    public static Network import12NodesNetwork() {
        return Importers.loadNetwork("utils/TestCase12Nodes.uct", NetworkImportsUtil.class.getResourceAsStream("/utils/TestCase12Nodes.uct"));
    }

}
