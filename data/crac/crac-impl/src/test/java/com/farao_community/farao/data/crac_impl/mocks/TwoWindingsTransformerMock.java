/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.mocks;

import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionAdder;
import com.powsybl.iidm.network.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class TwoWindingsTransformerMock implements TwoWindingsTransformer {
    private int lowTapPosition;
    private int highTapPosition;
    private int currentTapPosition;

    public TwoWindingsTransformerMock(int lowTapPosition, int highTapPosition, int currentTapPosition) {
        this.lowTapPosition = lowTapPosition;
        this.highTapPosition = highTapPosition;
        this.currentTapPosition = currentTapPosition;
    }

    @Override
    public Substation getSubstation() {
        return null;
    }

    @Override
    public double getR() {
        return 0;
    }

    @Override
    public TwoWindingsTransformer setR(double v) {
        return null;
    }

    @Override
    public double getX() {
        return 0;
    }

    @Override
    public TwoWindingsTransformer setX(double v) {
        return null;
    }

    @Override
    public double getG() {
        return 0;
    }

    @Override
    public TwoWindingsTransformer setG(double v) {
        return null;
    }

    @Override
    public double getB() {
        return 0;
    }

    @Override
    public TwoWindingsTransformer setB(double v) {
        return null;
    }

    @Override
    public double getRatedU1() {
        return 0;
    }

    @Override
    public TwoWindingsTransformer setRatedU1(double v) {
        return null;
    }

    @Override
    public double getRatedU2() {
        return 0;
    }

    @Override
    public TwoWindingsTransformer setRatedU2(double v) {
        return null;
    }

    @Override
    public Terminal getTerminal1() {
        return null;
    }

    @Override
    public Terminal getTerminal2() {
        return null;
    }

    @Override
    public Terminal getTerminal(Side side) {
        return null;
    }

    @Override
    public Terminal getTerminal(String s) {
        return null;
    }

    @Override
    public Side getSide(Terminal terminal) {
        return null;
    }

    @Override
    public CurrentLimits getCurrentLimits(Side side) {
        return null;
    }

    @Override
    public CurrentLimits getCurrentLimits1() {
        return null;
    }

    @Override
    public CurrentLimitsAdder newCurrentLimits1() {
        return null;
    }

    @Override
    public ActivePowerLimitsAdder newActivePowerLimits1() {
        return null;
    }

    @Override
    public ApparentPowerLimitsAdder newApparentPowerLimits1() {
        return null;
    }

    @Override
    public CurrentLimits getCurrentLimits2() {
        return null;
    }

    @Override
    public CurrentLimitsAdder newCurrentLimits2() {
        return null;
    }

    @Override
    public ActivePowerLimitsAdder newActivePowerLimits2() {
        return null;
    }

    @Override
    public ApparentPowerLimitsAdder newApparentPowerLimits2() {
        return null;
    }

    @Override
    public boolean isOverloaded() {
        return false;
    }

    @Override
    public boolean isOverloaded(float v) {
        return false;
    }

    @Override
    public int getOverloadDuration() {
        return 0;
    }

    @Override
    public boolean checkPermanentLimit(Side side, float v) {
        return false;
    }

    @Override
    public boolean checkPermanentLimit(Side side) {
        return false;
    }

    @Override
    public boolean checkPermanentLimit1(float v) {
        return false;
    }

    @Override
    public boolean checkPermanentLimit1() {
        return false;
    }

    @Override
    public boolean checkPermanentLimit2(float v) {
        return false;
    }

    @Override
    public boolean checkPermanentLimit2() {
        return false;
    }

    @Override
    public Overload checkTemporaryLimits(Side side, float v) {
        return null;
    }

    @Override
    public Overload checkTemporaryLimits(Side side) {
        return null;
    }

    @Override
    public Overload checkTemporaryLimits1(float v) {
        return null;
    }

    @Override
    public Overload checkTemporaryLimits1() {
        return null;
    }

    @Override
    public Overload checkTemporaryLimits2(float v) {
        return null;
    }

    @Override
    public Overload checkTemporaryLimits2() {
        return null;
    }

    @Override
    public ConnectableType getType() {
        return null;
    }

    @Override
    public List<? extends Terminal> getTerminals() {
        return null;
    }

    @Override
    public void remove() {

    }

    @Override
    public Network getNetwork() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean hasProperty() {
        return false;
    }

    @Override
    public boolean hasProperty(String s) {
        return false;
    }

    @Override
    public String getProperty(String s) {
        return null;
    }

    @Override
    public String getProperty(String s, String s1) {
        return null;
    }

    @Override
    public String setProperty(String s, String s1) {
        return null;
    }

    @Override
    public Set<String> getPropertyNames() {
        return null;
    }

    @Override
    public <E extends Extension<TwoWindingsTransformer>> void addExtension(Class<? super E> aClass, E e) {

    }

    @Override
    public <E extends Extension<TwoWindingsTransformer>> E getExtension(Class<? super E> aClass) {
        return null;
    }

    @Override
    public <E extends Extension<TwoWindingsTransformer>> E getExtensionByName(String s) {
        return null;
    }

    @Override
    public <E extends Extension<TwoWindingsTransformer>> boolean removeExtension(Class<E> aClass) {
        return false;
    }

    @Override
    public <E extends Extension<TwoWindingsTransformer>> Collection<E> getExtensions() {
        return null;
    }

    @Override
    public String getImplementationName() {
        return null;
    }

    @Override
    public <E extends Extension<TwoWindingsTransformer>, B extends ExtensionAdder<TwoWindingsTransformer, E>> B newExtension(Class<B> aClass) {
        return null;
    }

    @Override
    public PhaseTapChangerAdder newPhaseTapChanger() {
        return null;
    }

    @Override
    public PhaseTapChanger getPhaseTapChanger() {
        return new PhaseTapChangerMock(lowTapPosition, highTapPosition, currentTapPosition);
    }

    @Override
    public RatioTapChangerAdder newRatioTapChanger() {
        return null;
    }

    @Override
    public RatioTapChanger getRatioTapChanger() {
        return null;
    }
}
