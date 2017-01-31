package sentinel;

import battlecode.common.*;

import static sentinel.Channels.CHANNEL_ARCHON_COUNT;
import static sentinel.Channels.CHANNEL_GARDENER_COUNT;
import static sentinel.Nav.*;
import static sentinel.RobotPlayer.*;
import static sentinel.Util.*;

public class Archon {

    static boolean isGardenerBuilt;
    static int numOfGardeners;
    static int numOfArchons;
    static int maxGardeners;

    static void run() {

        try {

            // Reset alternate every 6 turns
            if (rc.getRoundNum() % 6 == 0) {
                resetAltPriorityLoc();
            }

            // Archon movement
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] teamInfo = rc.senseNearbyRobots(-1, rc.getTeam());
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();
            if (enemyInfo.length > 0) {
                evadeRobotGroup(enemyInfo);
            } else if (bulletCollisionImminent(bulletInfo)) {
                dodgeIncomingBullets(bulletInfo);
            } else {

                // See if robot can still move in current dir
                if (rc.canMove(currentDirection) && numTries <= 8) {
                    rc.move(currentDirection);
                    numTries += 1;
                } else {
                    MapLocation prevLoc = rc.getLocation();
                    tryMove(randomDirection(), 5, 36);
                    MapLocation postLoc = rc.getLocation();
                    currentDirection = prevLoc.directionTo(postLoc);
                    if (currentDirection == null) {
                        int i = (int) (Math.random() * initialArchonLocations.length);
                        currentDirection = rc.getLocation().directionTo(initialArchonLocations[i]);
                    }
                    numTries = 0;
                }
            }

            // Build gardener
            if (!isBuildingGardener() && rc.readBroadcast(CHANNEL_GARDENER_COUNT)*5 < rc.getRobotCount()) {
                tryBuildGardener();
            }

            if (isBuildingGardener()) {
                if (getGardenerCountdown() == 0) {
                    System.out.println("Gardeners can build again!");
                    setBuildingGardener(false);
                } else {
                    decreaseGardenerCountdown();
                }
            }

            // Donate bullets
            if ((rc.readBroadcast(CHANNEL_GARDENER_COUNT) > 3 || rc.getRoundNum() > 1000) && (rc.getRoundNum() % 6 == 0 || rc.getTeamBullets() > 200)) {
                if (rc.getTeamBullets() > rc.getVictoryPointCost()) {
                    rc.donate(rc.getVictoryPointCost());
                }
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

        initialArchonLocations = rc.getInitialArchonLocations(rc.getTeam().opponent());
        currentDirection = rc.getLocation().directionTo(initialArchonLocations[0]).opposite();
        numTries = 0;

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
                    //System.out.println("Building gardener!");
                    setBuildingGardener(true);
                    setGardenerCountdown(numOfArchons*40);
                    //System.out.println("Gardener countdown@" + getGardenerCountdown());
                    return;
                } else {
                    radians += radianInterval;
                }
            }

            //System.out.println("Couldn't build gardener.");
            setGardenerCountdown(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
