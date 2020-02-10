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

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SimpleCracDeserializer extends StdDeserializer<SimpleCrac> {

    public SimpleCracDeserializer() {
        this(null);
    }

    public SimpleCracDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SimpleCrac deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        SimpleCrac simpleCrac = new SimpleCrac(node.get("id").asText(), node.get("name").asText());
        for (Iterator<JsonNode> it = node.get("networkElements").elements(); it.hasNext(); ) {
            JsonNode networkElement = it.next();
            simpleCrac.addNetworkElement(new NetworkElement(networkElement.get("id").asText(), networkElement.get("name").asText()));
        }
        for (Iterator<JsonNode> it = node.get("instants").elements(); it.hasNext(); ) {
            JsonNode instant = it.next();
            simpleCrac.addInstant(new Instant(instant.get("id").asText(), instant.get("seconds").asInt()));
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
        for (Iterator<JsonNode> itNE = contingency.get("networkElements").elements(); itNE.hasNext(); ) {
            JsonNode networkElement = itNE.next();
            networkElements.add(simpleCrac.getNetworkElement(networkElement.asText()));
        }
        simpleCrac.addContingency(new ComplexContingency(contingency.get("id").asText(), contingency.get("name").asText(), networkElements));
    }

    private static void createAndAddState(SimpleCrac simpleCrac, JsonNode state) {
        Optional<Contingency> contingency;
        if (state.get("contingency").isNull()) {
            contingency = Optional.empty();
        } else {
            contingency = Optional.of(simpleCrac.getContingency(state.get("contingency").asText()));
        }
        simpleCrac.addState(new SimpleState(contingency, simpleCrac.getInstant(state.get("instant").asText())));
    }

    private static void createAndAddCnec(SimpleCrac simpleCrac, JsonNode cnec, JsonParser jsonParser) throws JsonProcessingException {
        simpleCrac.addCnec(new SimpleCnec(
            cnec.get("id").asText(),
            cnec.get("name").asText(),
            simpleCrac.getNetworkElement(cnec.get("networkElement").asText()),
            jsonParser.getCodec().treeToValue(cnec.get("threshold"), AbstractThreshold.class),
            simpleCrac.getState(cnec.get("state").asText())
        ));
    }

    private static List<UsageRule> getUsageRules(SimpleCrac simpleCrac, JsonNode abstractRemedialActionNode) {
        List<UsageRule> usageRules = new ArrayList<>();
        for (Iterator<JsonNode> itUsageRule = abstractRemedialActionNode.get("usageRules").elements(); itUsageRule.hasNext(); ) {
            JsonNode usageRule = itUsageRule.next();
            switch (usageRule.get("type").asText()) {
                case "free-to-use":
                    usageRules.add(new FreeToUse(UsageMethod.valueOf(usageRule.get("usageMethod").asText()), simpleCrac.getState(usageRule.get("state").asText())));
                    break;
                case "on-constraint":
                    usageRules.add(new OnConstraint(
                        UsageMethod.valueOf(usageRule.get("usageMethod").asText()),
                        simpleCrac.getState(usageRule.get("state").asText()),
                        simpleCrac.getCnec(usageRule.get("cnec").asText())));
                    break;
                case "on-contingency":
                    usageRules.add(new OnContingency(
                        UsageMethod.valueOf(usageRule.get("usageMethod").asText()),
                        simpleCrac.getState(usageRule.get("state").asText()),
                        simpleCrac.getContingency(usageRule.get("contingency").asText())));
                    break;
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
        switch (rangeAction.get("type").asText()) {
            case "aligned-range-action":
                Set<NetworkElement> networkElements = new HashSet<>();
                for (Iterator<JsonNode> itElementary = rangeAction.get("networkElements").elements(); itElementary.hasNext(); ) {
                    JsonNode networkElement = itElementary.next();
                    networkElements.add(simpleCrac.getNetworkElement(networkElement.asText()));
                }
                simpleCrac.addRangeAction(new AlignedRangeAction(
                    rangeAction.get("id").asText(),
                    rangeAction.get("name").asText(),
                    rangeAction.get("operator").asText(),
                    usageRules,
                    ranges,
                    networkElements
                ));
                break;
            case "pst-range":
                simpleCrac.addRangeAction(new PstRange(
                    rangeAction.get("id").asText(),
                    rangeAction.get("name").asText(),
                    rangeAction.get("operator").asText(),
                    usageRules,
                    ranges,
                    simpleCrac.getNetworkElement(rangeAction.get("networkElement").asText())
                ));
                break;
            case "hvdc-range":
                simpleCrac.addRangeAction(new HvdcRange(
                    rangeAction.get("id").asText(),
                    rangeAction.get("name").asText(),
                    rangeAction.get("operator").asText(),
                    usageRules,
                    ranges,
                    simpleCrac.getNetworkElement(rangeAction.get("networkElement").asText())
                ));
                break;
            case "injection-range":
                simpleCrac.addRangeAction(new InjectionRange(
                    rangeAction.get("id").asText(),
                    rangeAction.get("name").asText(),
                    rangeAction.get("operator").asText(),
                    usageRules,
                    ranges,
                    simpleCrac.getNetworkElement(rangeAction.get("networkElement").asText())
                ));
                break;
            case "redispatching":
                simpleCrac.addRangeAction(new Redispatching(
                    rangeAction.get("id").asText(),
                    rangeAction.get("name").asText(),
                    rangeAction.get("operator").asText(),
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
        switch (networkAction.get("type").asText()) {
            case "complex-network-action":
                List<UsageRule> usageRules = getUsageRules(simpleCrac, networkAction);
                Set<AbstractElementaryNetworkAction> abstractElementaryNetworkActions = new HashSet<>();
                for (Iterator<JsonNode> itElementary = networkAction.get("elementaryNetworkActions").elements(); itElementary.hasNext(); ) {
                    JsonNode elementaryNetworkAction = itElementary.next();
                    abstractElementaryNetworkActions.add(getAbstractElementaryNetworkAction(simpleCrac, elementaryNetworkAction));
                }
                simpleCrac.addNetworkAction(new ComplexNetworkAction(
                    networkAction.get("id").asText(),
                    networkAction.get("name").asText(),
                    networkAction.get("operator").asText(),
                    usageRules,
                    abstractElementaryNetworkActions
                ));
                break;
            default:
                simpleCrac.addNetworkAction(getAbstractElementaryNetworkAction(simpleCrac, networkAction));
        }
    }

    private static AbstractElementaryNetworkAction getAbstractElementaryNetworkAction(SimpleCrac simpleCrac, JsonNode elementaryNetworkActionNode) {
        List<UsageRule> usageRules = getUsageRules(simpleCrac, elementaryNetworkActionNode);
        switch (elementaryNetworkActionNode.get("type").asText()) {
            case "pst-setpoint":
                return new PstSetpoint(
                    elementaryNetworkActionNode.get("id").asText(),
                    elementaryNetworkActionNode.get("name").asText(),
                    elementaryNetworkActionNode.get("operator").asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get("networkElement").asText()),
                    elementaryNetworkActionNode.get("setpoint").asDouble()
                );
            case "hvdc-setpoint":
                return new HvdcSetpoint(
                    elementaryNetworkActionNode.get("id").asText(),
                    elementaryNetworkActionNode.get("name").asText(),
                    elementaryNetworkActionNode.get("operator").asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get("networkElement").asText()),
                    elementaryNetworkActionNode.get("setpoint").asDouble()
                );
            case "injection-setpoint":
                return new InjectionSetpoint(
                    elementaryNetworkActionNode.get("id").asText(),
                    elementaryNetworkActionNode.get("name").asText(),
                    elementaryNetworkActionNode.get("operator").asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get("networkElement").asText()),
                    elementaryNetworkActionNode.get("setpoint").asDouble()
                );
            case "topology":
                return new Topology(
                    elementaryNetworkActionNode.get("id").asText(),
                    elementaryNetworkActionNode.get("name").asText(),
                    elementaryNetworkActionNode.get("operator").asText(),
                    usageRules,
                    simpleCrac.getNetworkElement(elementaryNetworkActionNode.get("networkElement").asText()),
                    ActionType.valueOf(elementaryNetworkActionNode.get("actionType").asText())
                );
            default:
                throw new FaraoException("Unknown type");
        }
    }
}
