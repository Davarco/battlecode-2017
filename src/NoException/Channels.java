package sentinel;


public class Channels {

    // Keep running robot totals
    static final int CHANNEL_ARCHON_COUNT = 0;
    static final int CHANNEL_GARDENER_COUNT = 1;
    static final int CHANNEL_SOLDIER_COUNT = 2;
    static final int CHANNEL_TANK_COUNT = 3;
    static final int CHANNEL_SCOUT_COUNT = 4;
    static final int CHANNEL_LUMBERJACK_COUNT = 5;

    // Make sure gardeners build linearly
    static final int CHANNEL_GARDENER_STATUS = 6;
    static final int CHANNEL_GARDENER_COUNTDOWN = 7;

    // Strategic positioning
    static final int ALT_PRIORITY_X = 996;
    static final int ALT_PRIORITY_Y = 997;
    static final int PRIORITY_X = 998;
    static final int PRIORITY_Y = 999;
}
