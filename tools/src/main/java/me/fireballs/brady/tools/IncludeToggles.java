package me.fireballs.brady.tools;

import kotlin.Lazy;
import kotlin.Unit;
import me.fireballs.brady.core.PluginExtensionsKt;
import me.fireballs.brady.corepgm.FeatureFlagEnum;
import me.fireballs.brady.corepgm.PGMExtensionsKt;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.koin.java.KoinJavaComponent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.variables.Variable;

import java.util.*;

public class IncludeToggles implements Listener {

    private final FeatureFlagEnum<OnePassRule> onePassRule = new FeatureFlagEnum<>("onePassRule", OnePassRule.MAP_DEFAULT, OnePassRule.class);
    private final FeatureFlagEnum<Instaheal> instaheal = new FeatureFlagEnum<>("instaheal", Instaheal.MAP_DEFAULT, Instaheal.class);

    public enum OnePassRule {
        MAP_DEFAULT(-1),
        OFF(0),
        ON(1);

        public final int xmlValue;

        OnePassRule(int xmlValue) {
            this.xmlValue = xmlValue;
        }
    }

    public enum Instaheal {
        MAP_DEFAULT(-1),
        OFF(0),
        ON(1),
        FLAG_CARRIER(2);

        public final int xmlValue;

        Instaheal(int xmlValue) {
            this.xmlValue = xmlValue;
        }
    }

    public IncludeToggles() {
        Lazy<Tools> plugin = KoinJavaComponent.inject(Tools.class);
        PluginExtensionsKt.registerEvents(plugin.getValue(), this);

        instaheal.getChangeHandlers().add(val -> {
            if (val != Instaheal.MAP_DEFAULT) {
                updateAllMatches("instaheal", val.xmlValue);
            }
            return Unit.INSTANCE;
        });

        onePassRule.getChangeHandlers().add(val -> {
            if (val != OnePassRule.MAP_DEFAULT) {
                updateAllMatches("one_pass_rule", val.xmlValue);
            }
            return Unit.INSTANCE;
        });
    }

    @EventHandler
    public void onCycle(MatchLoadEvent event) {
        if (onePassRule.getState() != OnePassRule.MAP_DEFAULT) {
            setVariable(event.getMatch(), "one_pass_rule", onePassRule.getState().xmlValue);
        }

        if (instaheal.getState() != Instaheal.MAP_DEFAULT) {
            setVariable(event.getMatch(), "instaheal", instaheal.getState().xmlValue);
        }
    }

    private void updateAllMatches(String variableId, int value) {
        Iterator<Match> it = PGM.get().getMatchManager().getMatches();
        while (it.hasNext()) {
            setVariable(it.next(), variableId, value);
        }
    }

    private static void setVariable(Match match, String variableId, int value) {
        Variable<?> variable = PGMExtensionsKt.getVariable(match, variableId);
        if (variable != null) {
            variable.setValue(match, value);
        }
    }
}