package com.farao_community.farao.data.crac_creation_util.ucte;

import com.farao_community.farao.data.crac_creation_util.PstHelper;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class UctePstHelper extends AbstractUcteConnectableHelper implements PstHelper {

    private int lowTapPosition;
    private int highTapPosition;
    private int initialTapPosition;
    private Map<Integer, Double> tapToAngleConversionMap;

    /**
     * Constructor, based on a separate fields.
     *
     * @param fromNode,            UCTE-id of the origin extremity of the branch
     * @param toNode,              UCTE-id of the destination extremity of the branch
     * @param suffix,              suffix of the branch, either an order code or an elementName
     * @param ucteNetworkAnalyzer, UcteNetworkAnalyzer object built upon the network
     */
    public UctePstHelper(String fromNode, String toNode, String suffix, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        super(fromNode, toNode, suffix);
        if (isValid) {
            interpretWithNetworkAnalyzer(ucteNetworkAnalyzer);
        }
    }

    /**
     * Constructor, based on a separate fields. Either the order code, or the element name must be
     * non-null. If the two are defined, the suffix which will be used by default is the order code.
     *
     * @param fromNode,             UCTE-id of the origin extremity of the branch
     * @param toNode,               UCTE-id of the destination extremity of the branch
     * @param orderCode,            order code of the branch
     * @param elementName,          element name of the branch
     * @param ucteNetworkAnalyzer,  UcteNetworkAnalyzer object built upon the network
     */
    public UctePstHelper(String fromNode, String toNode, String orderCode, String elementName, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        super(fromNode, toNode, orderCode, elementName);
        if (isValid) {
            interpretWithNetworkAnalyzer(ucteNetworkAnalyzer);
        }
    }

    /**
     * Constructor, based on a concatenated id.
     *
     * @param ucteBranchId,         concatenated UCTE branch id, of the form "FROMNODE TO__NODE SUFFIX"
     * @param ucteNetworkAnalyzer,  UcteNetworkAnalyzer object built upon the network
     */
    public UctePstHelper(String ucteBranchId, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        super(ucteBranchId);
        if (isValid) {
            interpretWithNetworkAnalyzer(ucteNetworkAnalyzer);
        }
    }

    @Override
    public int getLowTapPosition() {
        return lowTapPosition;
    }

    @Override
    public int getHighTapPosition() {
        return highTapPosition;
    }

    @Override
    public int getInitialTap() {
        return initialTapPosition;
    }

    @Override
    public Map<Integer, Double> getTapToAngleConversionMap() {
        return tapToAngleConversionMap;
    }

    private void interpretWithNetworkAnalyzer(UcteNetworkAnalyzer ucteNetworkAnalyzer) {

        UcteMatchingResult ucteMatchingResult = ucteNetworkAnalyzer.findPstElement(from, to, suffix);

        if (ucteMatchingResult.getStatus() == UcteMatchingResult.MatchStatus.NOT_FOUND) {
            invalidate(format("PST was not found in the Network (from: %s, to: %s, suffix: %s)", from, to, suffix));
            return;
        }

        if (ucteMatchingResult.getStatus() == UcteMatchingResult.MatchStatus.SEVERAL_MATCH) {
            invalidate(format("several PSTs were found in the Network which match the description(from: %s, to: %s, suffix: %s)", from, to, suffix));
            return;
        }

        Identifiable<?> transformer = ucteMatchingResult.getIidmIdentifiable();
        this.connectableIdInNetwork = transformer.getId();

        PhaseTapChanger phaseTapChanger = ((TwoWindingsTransformer) transformer).getPhaseTapChanger();

        this.lowTapPosition = phaseTapChanger.getLowTapPosition();
        this.highTapPosition = phaseTapChanger.getHighTapPosition();
        this.initialTapPosition = phaseTapChanger.getTapPosition();

        buildTapToAngleConversionMap(phaseTapChanger);

    }

    private void buildTapToAngleConversionMap(PhaseTapChanger phaseTapChanger) {
        tapToAngleConversionMap = new HashMap<>();
        phaseTapChanger.getAllSteps().forEach((tap, step) -> tapToAngleConversionMap.put(tap, step.getAlpha()));
    }

}
