/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.fbconstraint;

import com.powsybl.openrao.data.craccreation.creator.fbconstraint.xsd.FlowBasedConstraintDocument;
import com.powsybl.openrao.data.nativecracapi.NativeCrac;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class FbConstraint implements NativeCrac {

    private final FlowBasedConstraintDocument document;
    private final int flowBasedDocumentVersion;

    public FbConstraint(FlowBasedConstraintDocument document, int flowBasedDocumentVersion) {
        this.document = document;
        this.flowBasedDocumentVersion = flowBasedDocumentVersion;
    }

    @Override
    public String getFormat() {
        return "FlowBasedConstraintDocument";
    }

    public FlowBasedConstraintDocument getDocument() {
        return document;
    }

    public int getFlowBasedDocumentVersion() {
        return flowBasedDocumentVersion;
    }
}
