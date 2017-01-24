package artemis;
import battlecode.common.*;


public strictfp class RobotPlayer {

    static RobotController rc;

    static boolean isLocLeader;
    static float prevPriorityX;
    static float prevPriorityY;

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
                Soldier.init();
                Soldier.loop();
                break;
            case TANK:
                Tank.init();
                Tank.loop();
                break;
            case SCOUT:
                Scout.init();
                Scout.loop();
                break;
            case LUMBERJACK:
                Lumberjack.init();
                Lumberjack.loop();
                break;
        }
    }
}
