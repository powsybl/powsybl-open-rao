package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.*;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.*;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.usage_rule.OnConstraint;
import com.farao_community.farao.data.crac_impl.usage_rule.OnContingency;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.*;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SimpleCracDeserializer extends StdDeserializer<SimpleCrac> {
    private static final String NETWORK_ELEMENT = "networkElement";
    private static final String NETWORK_ELEMENTS = "networkElements";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String OPERATOR = "operator";
    private static final String CONTINGENCY = "contingency";
    private static final String STATE = "state";
    private static final String USAGE_METHOD = "usageMethod";
    private static final String SETPOINT = "setpoint";
    private static final String TYPE = "type";

    public SimpleCracDeserializer() {
        this(null);
    }

    public SimpleCracDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SimpleCrac deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        SimpleCrac simpleCrac = new SimpleCrac(node.get(ID).asText(), node.get(NAME).asText());
        for (Iterator<JsonNode> it = node.get(NETWORK_ELEMENTS).elements(); it.hasNext(); ) {
            JsonNode networkElement = it.next();
            simpleCrac.addNetworkElement(new NetworkElement(networkElement.get(ID).asText(), networkElement.get(NAME).asText()));
        }
        for (Iterator<JsonNode> it = node.get("instants").elements(); it.hasNext(); ) {
            JsonNode instant = it.next();
            simpleCrac.addInstant(new Instant(instant.get(ID).asText(), instant.get("seconds").asInt()));
        }
        for (Iterator<JsonNode> it = node.get("contingencies").elements(); it.hasNext(); ) {
            createAndAddContingency(simpleCrac, it.next());
        }
        for (Iterator<JsonNode> it = node.get("states").elements(); it.hasNext(); ) {
            createAndAddState(simpleCrac, it.next());
        }
        for (Iterator<JsonNode> it = node.get("cnecs").elements(); it.hasNext(); ) {
            createAndAddCnec(simpleCrac, it.next(), jsonParser);
        }
        for (Iterator<JsonNode> it = node.get("networkActions").elements(); it.hasNext(); ) {
            createAndAddNetworkAction(simpleCrac, it.next());

        }
        for (Iterator<JsonNode> it = node.get("rangeActions").elements(); it.hasNext(); ) {
            createAndAddRangeAction(simpleCrac, it.next(), jsonParser);
        }

        return simpleCrac;
    }

    private static void createAndAddContingency(SimpleCrac simpleCrac, JsonNode contingency) {
        Set<NetworkElement> networkElements = new HashSet<>();
        for (Iterator<JsonNode> itNE = contingency.get(NETWORK_ELEMENTS).elements(); itNE.hasNext(); ) {
            JsonNode networkElement = itNE.next();
            networkElements.add(simpleCrac.getNetworkElement(networkElement.asText()));
        }
        simpleCrac.addContingency(new ComplexContingency(contingency.get(ID).asText(), contingency.get(NAME).asText(), networkElements));
    }

    private static void createAndAddState(SimpleCrac simpleCrac, JsonNode state) {
        Optional<Contingency> contingency;
        if (state.get(CONTINGENCY).isNull()) {
            contingency = Optional.empty();
        } else {
            contingency = Optional.of(simpleCrac.getContingency(state.get(CONTINGENCY).asText()));
        }
        simpleCrac.addState(new SimpleState(contingency, simpleCrac.getInstant(state.get("instant").asText())));
    }

    private static void createAndAddCnec(SimpleCrac simpleCrac, JsonNode cnec, JsonParser jsonParser) throws JsonProcessingException {
        Set<AbstractThreshold> thresholds = new HashSet<>();
        for (Iterator<JsonNode> itElementary = cnec.get("thresholds").elements(); itElementary.hasNext(); ) {
            JsonNode range = itElementary.next();
            thresholds.add(jsonParser.getCodec().treeToValue(range, AbstractThreshold.class));
        }
        simpleCrac.addCnec(new SimpleCnec(
            cnec.get(ID).asText(),
            cnec.get(NAME).asText(),
            simpleCrac.getNetworkElement(cnec.get(NETWORK_ELEMENT).asText()),
            thresholds,
            simpleCrac.getState(cnec.get(STATE).asText())
        ));
    }

    private static List<UsageRule> getUsageRules(SimpleCrac simpleCrac, JsonNode abstractRemedialActionNode) {
        List<UsageRule> usageRules = new ArrayList<>();
        for (Iterator<JsonNode> itUsageRule = abstractRemedialActionNode.get("usageRules").elements(); itUsageRule.hasNext(); ) {
            JsonNode usageRule = itUsageRule.next();
            switch (usageRule.get(TYPE).asText()) {
                case "free-to-use":
                    usageRules.add(new FreeToUse(UsageMethod.valueOf(usageRule.get(USAGE_METHOD).asText()), simpleCrac.getState(usageRule.get(STATE).asText())));
                    break;
                case "on-constraint":
                    usageRules.add(new OnConstraint(
                        UsageMethod.valueOf(usageRule.get(USAGE_METHOD).asText()),
                        simpleCrac.getState(usageRule.get(STATE).asText()),
                        simpleCrac.getCnec(usageRule.get("cnec").asText())));
                    break;
                case "on-contingency":
                    usageRules.add(new OnContingency(
                        UsageMethod.valueOf(usageRule.get(USAGE_METHOD).asText()),
                        simpleCrac.getState(usageRule.get(STATE).asText()),
                        simpleCrac.getContingency(usageRule.get(CONTINGENCY).asText())));
                    break;
                default:
                    throw new FaraoException(format("Not implemented type %s", usageRule.get(TYPE).asText()));
            }
        }
        return usageRules;
    }

    private static void createAndAddRangeAction(SimpleCrac simpleCrac, JsonNode rangeAction, JsonParser jsonParser) throws JsonProcessingException {
        List<UsageRule> usageRules = getUsageRules(simpleCrac, rangeAction);
        List<Range> ranges = new ArrayList<>();
        for (Iterator<JsonNode> itElementary = rangeAction.get("ranges").elements(); itElementary.hasNext(); ) {
            JsonNode range = itElementary.next();
            ranges.add(jsonParser.getCodec().treeToValue(range, Range.class));
        }
        switch (rangeAction.get(TYPE).asText()) {
            case "aligned-range-action":
                Set<NetworkElement> networkElements = new HashSet<>();
                for (Iterator<JsonNode> itElementary = rangeAction.get(NETWORK_ELEMENTS).elements(); itElementary.hasNext(); ) {
                    JsonNode networkElement = itElementary.next();
                    networkElements.add(simpleCrac.getNetworkElement(networkElement.asText()));
                }
                simpleCrac.addRangeAction(new AlignedRangeAction(
                    rangeAction.get(ID).asText(),
                    rangeAction.get(NAME).asText(),
                    rangeAction.get(OPERATOR).asText(),
                    usageRules,
                    ranges,
                    networkElements
                ));
                break;
            case "pst-with-range":
                simpleCrac.addRangeAction(new PstWithRange(
                    rangeAction.get(ID).asText(),
                    rangeAction.get(NAME).asText(),
                    rangeAction.get(OPERATOR).asText(),
                    usageRules,
                    ranges,
                    simpleCrac.getNetworkElement(rangeAction.get(NETWORK_ELEMENT).asText())
                ));
                break;
            case "hvdc-range":
                simpleCrac.addRangeAction(new HvdcRange(
                    rangeAction.get(ID).asText(),
                    rangeAction.get(NAME).asText(),
                    rangeAction.get(OPERATOR).asText(),
                    usageRules,
                    ranges,
                    simpleCrac.getNetworkElement(rangeAction.get(NETWORK_ELEMENT).asText())
                ));
                break;
            case "injection-range":
                simpleCrac.addRangeAction(new InjectionRange(
                    rangeAction.get(ID).asText(),
                    rangeAction.get(NAME).asText(),
                    rangeAction.get(OPERATOR).asText(),
                    usageRules,
                    ranges,
                    simpleCrac.getNetworkElement(rangeAction.get(NETWORK_ELEMENT).asText())
                ));
                break;
            case "redispatching":
                simpleCrac.addRangeAction(new Redispatching(
                    rangeAction.get(ID).asText(),
                    rangeAction.get(NAME).asText(),
                    rangeAction.get(OPERATOR).asText(),
                    usageRules,
                    ranges,
                    rangeAction.get("minimumPower").asDouble(),
                    rangeAction.get("maximumPower").asDouble(),
                    rangeAction.get("targetPower").asDouble(),
                    rangeAction.get("startupCost").asDouble(),
                    rangeAction.get("marginalCost").asDouble(),
                    simpleCrac.getNetworkElement(rangeAction.get("generator").asText())
                ));
                break;
            case "countertrading":
            default:
                break;
        }
    }

    private static void createAndAddNetworkAction(SimpleCrac simpleCrac, JsonNode networkAction) {
        if (networkAction.get(TYPE).asText().equals("complex-network-action")) {
            List<UsageRule> usageRules = getUsageRules(simpleCrac, networkAction);
            Set<AbstractElementaryNetworkAction> abstractElementaryNetworkActions = new HashSet<>();
            for (Iterator<JsonNode> itElementary = networkAction.get("elementaryNetworkActions").elements(); itElementary.hasNext(); ) {
                JsonNode elementaryNetworkAction = itElementary.next();
                abstractElementaryNetworkActions.add(getAbstractElementaryNetworkAction(simpleCrac, elementaryNetworkAction));
            }
            simpleCrac.addNetworkAction(new ComplexNetworkAction(
                networkAction.get(ID).asText(),
                networkAction.get(NAME).asText(),
                networkAction.get(OPERATOR).asText(),
                usageRules,
                abstractElementaryNetworkActions
            ));
        } else {
            simpleCrac.addNetworkAction(getAbstractElementaryNetworkAction(simpleCrac, networkAction));
        }
    }

    private static AbstractElementaryNetworkAction getAbstractElementaryNetworkAction(SimpleCrac simpleCrac, JsonNode elementaryNetworkActionNode) {
        List<UsageRule> usageRules = getUsageRules(simpleCrac, elementaryNetworkActionNode);
        switch (elementaryNetworkActionNode.get(TYPE).asText()) {
            case "pst-setpoint":
                return new PstSetpoint(
                    elementaryNetworkActionNode.get(ID).asText(),
                    elementaryNetworkActionNode.get(NAME).asText(),
                    elementaryNetworkActionNode.get(OPERATOR).asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get(NETWORK_ELEMENT).asText()),
                    elementaryNetworkActionNode.get(SETPOINT).asDouble()
                );
            case "hvdc-setpoint":
                return new HvdcSetpoint(
                    elementaryNetworkActionNode.get(ID).asText(),
                    elementaryNetworkActionNode.get(NAME).asText(),
                    elementaryNetworkActionNode.get(OPERATOR).asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get(NETWORK_ELEMENT).asText()),
                    elementaryNetworkActionNode.get(SETPOINT).asDouble()
                );
            case "injection-setpoint":
                return new InjectionSetpoint(
                    elementaryNetworkActionNode.get(ID).asText(),
                    elementaryNetworkActionNode.get(NAME).asText(),
                    elementaryNetworkActionNode.get(OPERATOR).asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get(NETWORK_ELEMENT).asText()),
                    elementaryNetworkActionNode.get(SETPOINT).asDouble()
                );
            case "topology":
                return new Topology(
                    elementaryNetworkActionNode.get(ID).asText(),
                    elementaryNetworkActionNode.get(NAME).asText(),
                    elementaryNetworkActionNode.get(OPERATOR).asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get(NETWORK_ELEMENT).asText()),
                    ActionType.valueOf(elementaryNetworkActionNode.get("actionType").asText())
                );
            default:
                throw new FaraoException("Unknown type");
        }
    }
}
