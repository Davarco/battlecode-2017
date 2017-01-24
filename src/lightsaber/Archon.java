package lightsaber;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.RobotInfo;

import static lightsaber.Channels.CHANNEL_TREE_COUNT;
import static lightsaber.Nav.*;
import static lightsaber.RobotPlayer.rc;
import static lightsaber.Util.bulletCollisionImminent;

public class Archon {

    static boolean isGardenerBuilt;
    static int numOfGardeners;
    static int numOfTrees;
    static int maxGardeners;

    static void run() {

        try {

            // Archon movement
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] teamInfo = rc.senseNearbyRobots(-1, rc.getTeam());
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();
            if (enemyInfo.length > 0) {
                evadeRobotGroup(enemyInfo);
            } else if (bulletCollisionImminent(bulletInfo)) {
                dodgeIncomingBullets(bulletInfo);
            } else if (teamInfo.length > 0) {
                evadeRobotGroup(teamInfo);
            } else {
                tryMove(randomDirection());
            }

            // Build gardener
            numOfTrees = rc.readBroadcast(CHANNEL_TREE_COUNT);
            if (!isGardenerBuilt || numOfGardeners < maxGardeners) {
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
                rc.setIndicatorDot(rc.getLocation(), 0, 0, 0);
            }

            Clock.yield();
        }
    }

    static void init() {

        // Initialize variables
        isGardenerBuilt = false;
        numOfGardeners = 0;
        numOfTrees = 0;
        maxGardeners = 1;
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
                    numOfGardeners += 1;
                    return;
                } else {
                    radians += radianInterval;
                }
            }

            //System.out.println("Couldn't build gardener.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
