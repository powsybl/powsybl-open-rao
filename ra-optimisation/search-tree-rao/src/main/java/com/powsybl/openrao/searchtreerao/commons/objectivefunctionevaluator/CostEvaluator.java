/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface CostEvaluator {
    String getName();

    CostEvaluatorResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, ReportNode reportNode);
}
