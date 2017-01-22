package artemis;
import battlecode.common.*;


public strictfp class RobotPlayer {

    static RobotController rc;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        RobotPlayer.rc = rc;

        switch (rc.getType()) {
            case ARCHON:
                Archon.init();
                Archon.loop();
                break;
            case GARDENER:
                Gardener.init();
                Gardener.loop();
                break;
            case SOLDIER:
                break;
            case TANK:
                break;
            case SCOUT:
                break;
            case LUMBERJACK:
                break;
        }
    }
}
