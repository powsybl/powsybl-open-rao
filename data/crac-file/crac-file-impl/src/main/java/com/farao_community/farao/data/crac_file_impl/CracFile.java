/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_file_impl;

import java.util.List;

/**
 * Business object of the CRAC file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracFile extends AbstractIdentifiable {
    private List<Cnec> cnecs;
    private List<RemedialAction> remedialActions;

    CracFile(String id, String name, List<Cnec> cnecs, List<RemedialAction> remedialActions) {
        super(id, name);
        this.cnecs = cnecs;
        this.remedialActions = remedialActions;
    }

    public List<Cnec> getCnecs() {
        return cnecs;
    }

    public void setCnecs(List<Cnec> cnecs) {
        this.cnecs = cnecs;
    }

    public List<RemedialAction> getRemedialActions() {
        return remedialActions;
    }

    public void setRemedialActions(List<RemedialAction> remedialActions) {
        this.remedialActions = remedialActions;
    }

    public void addCnec(Cnec cnec) {
        cnecs.add(cnec);
    }

    public void addRemedialAction(RemedialAction remedialAction) {
        remedialActions.add(remedialAction);
    }

    @Override
    protected String getTypeDescription() {
        return "Crac file";
    }
}
