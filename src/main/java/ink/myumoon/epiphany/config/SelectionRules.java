package ink.myumoon.epiphany.config;

/** Server settings needed to present module choices and their slots on a client. */
public record SelectionRules(int moduleCost, int maxModules, int maxEpiphanySlots) {
    public static SelectionRules fromConfig() {
        return new SelectionRules(CommonConfig.MODULE_SELECT_COST.get(),
                CommonConfig.MAX_SELECTED_MODULES.get(), CommonConfig.MAX_EPIPHANY_SLOTS.get());
    }
}
