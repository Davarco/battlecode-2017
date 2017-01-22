package artemis;

import battlecode.common.*;
import static artemis.RobotPlayer.*;
import static artemis.Channels.*;
import static artemis.Nav.*;
import static artemis.Util.*;

public class Archon {

    static boolean isGardenerBuilt;

    static void run() {

        try {

            // Execute every six rounds
            if (rc.getRoundNum() % 6 == 0) {
                resetRobotNum();
            }

            // Archon movement
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] teamInfo = rc.senseNearbyRobots(-1, rc.getTeam());
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();
            if (enemyInfo.length > 0) {
                evadeRobotGroup(enemyInfo);
            } else if (bulletInfo.length > 0) {
                dodgeIncomingBullets(bulletInfo);
            } else if (teamInfo.length > 0) {
                evadeRobotGroup(teamInfo);
            } else {
                tryMove(randomDirection());
            }

            // Build gardener
            if (!isGardenerBuilt) {
                tryBuildGardener();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void loop() {

        while (true) {

            int startTurn = rc.getRoundNum();

            try {
                run();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Catch if over maximum number of bytecodes
            int endTurn = rc.getRoundNum();
            if (startTurn != endTurn) {
                System.out.println("Over maximum bytecodes! Start @" + startTurn + " End @" + endTurn);
            }

            Clock.yield();
        }
    }

    static void init() {

        isGardenerBuilt = false;
    }

    static void tryBuildGardener() {

        final float radianInterval = (float)(Math.PI/12);

        try {

            // Search in intervals
            float radians = 0;
            while (radians <= Math.PI*2) {
                Direction buildDir = new Direction(radians);
                if (rc.canHireGardener(buildDir)) {
                    rc.hireGardener(buildDir);
                    isGardenerBuilt = true;
                    return;
                } else {
                    radians += radianInterval;
                }
            }

            System.out.println("Couldn't build gardener.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static void resetRobotNum() {

        try {

            rc.broadcast(CHANNEL_GARDENER_COUNT, rc.readBroadcast(CHANNEL_GARDENER_SUM));
            rc.broadcast(CHANNEL_GARDENER_SUM, 0);
            rc.broadcast(CHANNEL_SOLDIER_COUNT, rc.readBroadcast(CHANNEL_SOLDIER_SUM));
            rc.broadcast(CHANNEL_SOLDIER_SUM, 0);
            rc.broadcast(CHANNEL_TANK_COUNT, rc.readBroadcast(CHANNEL_TANK_SUM));
            rc.broadcast(CHANNEL_TANK_SUM, 0);
            rc.broadcast(CHANNEL_SCOUT_COUNT, rc.readBroadcast(CHANNEL_SCOUT_SUM));
            rc.broadcast(CHANNEL_SCOUT_SUM, 0);
            rc.broadcast(CHANNEL_LUMBERJACK_COUNT, rc.readBroadcast(CHANNEL_LUMBERJACK_SUM));
            rc.broadcast(CHANNEL_LUMBERJACK_SUM, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
