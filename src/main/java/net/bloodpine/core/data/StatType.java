package net.bloodpine.core.data;

public enum StatType {
    DAMAGE("Damage", "damage"),
    DEFENSE("Defense", "defense"),
    CRYSTAL("Crystal Mastery", "crystal"),
    TOTEM("Totem Efficiency", "totem"),
    VITALITY("Vitality", "vitality");
    
    private final String displayName;
    private final String configKey;
    
    StatType(String displayName, String configKey) {
        this.displayName = displayName;
        this.configKey = configKey;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getConfigKey() {
        return configKey;
    }
    
    public static StatType fromString(String str) {
        for (StatType type : values()) {
            if (type.name().equalsIgnoreCase(str) || 
                type.displayName.equalsIgnoreCase(str) ||
                type.configKey.equalsIgnoreCase(str)) {
                return type;
            }
        }
        return null;
    }
}
