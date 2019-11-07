/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.ra_optimisation.RaoComputation;
import com.farao_community.farao.ra_optimisation.RaoComputationFactory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SearchTreeRaoFactory implements RaoComputationFactory {
    @Override
    public RaoComputation create(Network network, CracFile cracFile, ComputationManager computationManager, int i) {
        return null;
    }
}
