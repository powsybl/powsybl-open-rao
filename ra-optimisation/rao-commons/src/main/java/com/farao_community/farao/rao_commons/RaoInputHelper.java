/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_util.CracCleaner;
import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoInputHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaoInputHelper.class);

    private RaoInputHelper() { }

    public static void synchronize(Crac crac, Network network) {
        if (!crac.isSynchronized()) {
            crac.synchronize(network);
            LOGGER.debug("Crac {} has been synchronized with network {}", crac.getId(), network.getId());
        } else {
            LOGGER.debug("Crac {} is already synchronized", crac.getId());
        }
    }

    // TODO: delete this! It is unused in this repo (migrated to CracAliasesUtil.java), but still used in some files of other projects
    @Deprecated
    public static List<String> cleanCrac(Crac crac, Network network) {
        CracCleaner cracCleaner = new CracCleaner();
        return cracCleaner.cleanCrac(crac, network);
    }
}
