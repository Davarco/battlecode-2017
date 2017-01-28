package sentinel;

import battlecode.common.*;

import static sentinel.Channels.CHANNEL_ARCHON_COUNT;
import static sentinel.Channels.CHANNEL_GARDENER_COUNT;
import static sentinel.Nav.*;
import static sentinel.RobotPlayer.isNearDeath;
import static sentinel.RobotPlayer.rc;
import static sentinel.Util.bulletCollisionImminent;
import static sentinel.Util.nearDeath;
import static sentinel.Util.shakeSurroundingTrees;

public class Archon {

    static boolean isGardenerBuilt;
    static int numOfGardeners;
    static int numOfArchons;
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
            if (!isGardenerBuilt || numOfGardeners < maxGardeners || rc.getRobotCount() > numOfGardeners*8*numOfArchons || rc.readBroadcast(CHANNEL_GARDENER_COUNT) == 0) {
                tryBuildGardener();
            }

            // Donate bullets
            if (rc.getRoundNum() % 500 == 0) {
                rc.donate(rc.getTeamBullets());
            }

            // Shake trees to farm bullets
            shakeSurroundingTrees();

            // Re update if near death
            if (nearDeath() && !isNearDeath) {
                isNearDeath = true;
                rc.broadcast(CHANNEL_ARCHON_COUNT, rc.readBroadcast(CHANNEL_ARCHON_COUNT)-1);
            }

            // Implement endgame
            if (rc.getRoundNum() == rc.getRoundLimit()-1) {
                rc.donate(rc.getTeamBullets());
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
        maxGardeners = 1;
        numOfArchons = rc.getInitialArchonLocations(rc.getTeam()).length;

        // Increase robot type count
        try {
            rc.broadcast(CHANNEL_ARCHON_COUNT, rc.readBroadcast(CHANNEL_ARCHON_COUNT)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
