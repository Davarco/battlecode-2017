package scoutsquad;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;


public strictfp class RobotPlayer {

    static RobotController rc;

    // Store temporary enemy info
    static int tempLocX = 0;
    static int tempLocY = 0;

    // Store robot info
    static boolean isHeadLeader;
    static boolean isSquadLeader;
    static boolean closeToDeath;

    // Store robot team info
    static int numFightingRobots=0;
    static int numSquads=0;
    static int maxSquadSize=0;

    // Store robot squad info
    static int squadChannel=0;
    static int squadSize=0;

    // Archon
    static int prevArchonNum;
    static int maxGardeners=0;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        RobotPlayer.rc = rc;

        switch (rc.getType()) {
            case ARCHON:
                ArchonRobot.loop();
                break;
            case GARDENER:
                GardenerRobot.loop();
                break;
            case SOLDIER:
                SoldierRobot.loop();
                break;
            case TANK:
                TankRobot.loop();
                break;
            case SCOUT:
                ScoutRobot.loop();
                break;
            case LUMBERJACK:
                LumberjackRobot.loop();
                break;
        }
    }
}
