/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.f711;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintCreationContext;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.FlowBasedConstraintDocument;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class F711Utils {

    private F711Utils() {
        // should not be instantiated
    }

    public static void write(TemporalData<RaoResult> raoResults, TemporalData<FbConstraintCreationContext> cracCreationContexts, String cracPath, String outputPath) {
        DailyF711GeneratorInputs provider = new DailyF711GeneratorInputs(raoResults, cracCreationContexts, cracPath);
        FlowBasedConstraintDocument fbc = DailyF711Generator.generate(provider);
        // yyyyMMdd-FID2-711-v1-10V1001C--00264T-to-10V1001C--00085T.xml
        JaxbUtil.writeInFile(FlowBasedConstraintDocument.class, fbc, String.format(outputPath));
    }
}
