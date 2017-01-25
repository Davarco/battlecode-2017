package sentinel;

import battlecode.common.*;

import static sentinel.Channels.CHANNEL_TREE_COUNT;
import static sentinel.Nav.*;
import static sentinel.RobotPlayer.rc;
import static sentinel.Util.bulletCollisionImminent;
import static sentinel.Util.shakeSurroundingTrees;

public class Archon {

    static boolean isGardenerBuilt;
    static int numOfGardeners;
    static int numOfArchons;
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

            // Donate bullets
            if (rc.getRoundNum() % 500 == 0) {
                rc.donate(rc.getTeamBullets());
            }

            // Build gardener
            if (rc.getRobotCount() > numOfGardeners*8*numOfArchons) {
                //System.out.println("Trying to build gardener, " + numOfGardeners + " gardeners, " + rc.getRobotCount() + " total.");
                tryBuildGardener();
            }

            // Shake trees to farm bullets
            shakeSurroundingTrees();

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
        numOfArchons = rc.getInitialArchonLocations(rc.getTeam()).length;
    }

    static void tryBuildGardener() {

        final float radianInterval = (float)(Math.PI/6);

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
