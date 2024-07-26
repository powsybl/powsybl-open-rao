/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.Action;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkActionAdderImpl extends AbstractRemedialActionAdder<NetworkActionAdder> implements NetworkActionAdder {

    private Set<Action> elementaryActions;
    private Set<NetworkElement> networkElements;

    NetworkActionAdderImpl(CracImpl owner) {
        super(owner);
        this.elementaryActions = new HashSet<>();
        this.networkElements = new HashSet<>();
    }

    @Override
    protected String getTypeDescription() {
        return "NetworkAction";
    }

    @Override
    public TerminalsConnectionActionAdder newTerminalsConnectionAction() {
        return new TerminalsConnectionActionAdderImpl(this);
    }

    @Override
    public SwitchActionAdder newSwitchAction() {
        return new SwitchActionAdderImpl(this);
    }

    @Override
    public PhaseTapChangerTapPositionActionAdder newPhaseTapChangerTapPositionAction() {
        return new PhaseTapChangerTapPositionActionAdderImpl(this);
    }

    @Override
    public GeneratorActionAdder newGeneratorAction() {
        return new GeneratorActionAdderImpl(this);
    }

    @Override
    public LoadActionAdder newLoadAction() {
        return new LoadActionAdderImpl(this);
    }

    @Override
    public DanglingLineActionAdder newDanglingLineAction() {
        return new DanglingLineActionAdderImpl(this);
    }

    @Override
    public ShuntCompensatorPositionActionAdder newShuntCompensatorPositionAction() {
        return new ShuntCompensatorPositionActionAdderImpl(this);
    }

    @Override
    public SwitchPairAdder newSwitchPair() {
        return new SwitchPairAdderImpl(this);
    }

    @Override
    public NetworkAction add() {
        checkId();

        if (!Objects.isNull(getCrac().getRemedialAction(id))) {
            throw new OpenRaoException(String.format("A remedial action with id %s already exists", id));
        }
        if (usageRules.isEmpty()) {
            OpenRaoLoggerProvider.BUSINESS_WARNS.warn("NetworkAction {} does not contain any usage rule, by default it will never be available", id);
        }
        if (elementaryActions.isEmpty()) {
            throw new OpenRaoException(String.format("NetworkAction %s has to have at least one ElementaryAction.", id));
        }

        NetworkAction networkAction = new NetworkActionImpl(id, name, operator, usageRules, elementaryActions, speed, networkElements);
        getCrac().addNetworkAction(networkAction);
        return networkAction;
    }

    void addElementaryAction(Action elementaryAction, NetworkElement... networkElements) {
        this.elementaryActions.add(elementaryAction);
        Collections.addAll(this.networkElements, networkElements);
    }
}
