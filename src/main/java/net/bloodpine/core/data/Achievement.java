package net.bloodpine.core.data;

/**
 * Represents an achievement that players can unlock
 */
public enum Achievement {
    FIRST_BLOOD("First Blood", "Get your first kill", 1),
    KILLING_SPREE("Killing Spree", "Reach a 5 kill streak", 5),
    UNSTOPPABLE("Unstoppable", "Reach a 10 kill streak", 10),
    GODLIKE("Godlike", "Reach a 15 kill streak", 15),
    TOKEN_COLLECTOR("Token Collector", "Earn 25 tokens", 25),
    TOKEN_MASTER("Token Master", "Earn 50 tokens", 50),
    SURVIVOR("Survivor", "Reach 20 hearts", 15),
    WARRIOR("Warrior", "Win 10 PvP fights", 10),
    CHAMPION("Champion", "Win 50 PvP fights", 50),
    LEGEND("Legend", "Win 100 PvP fights", 100),
    BOUNTY_HUNTER("Bounty Hunter", "Claim your first bounty", 10),
    MARKED_SURVIVOR("Marked Survivor", "Survive while marked for 10 minutes", 20),
    REBIRTH_INITIATE("Rebirth Initiate", "Complete your first rebirth", 15),
    STAT_SPECIALIST("Stat Specialist", "Max out a stat", 10),
    BALANCED_BUILD("Balanced Build", "Allocate tokens to all stats", 15),
    GENEROUS("Generous", "Send tokens to 5 different players", 10);
    
    private final String name;
    private final String description;
    private final int tokenReward;
    
    Achievement(String name, String description, int tokenReward) {
        this.name = name;
        this.description = description;
        this.tokenReward = tokenReward;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getTokenReward() {
        return tokenReward;
    }
    
    public String getKey() {
        return name().toLowerCase();
    }
}
