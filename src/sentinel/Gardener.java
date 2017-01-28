package sentinel;

import battlecode.common.*;

import static sentinel.Channels.*;
import static sentinel.Nav.*;
import static sentinel.RobotPlayer.isNearDeath;
import static sentinel.RobotPlayer.rc;
import static sentinel.Util.bulletCollisionImminent;
import static sentinel.Util.nearDeath;
import static sentinel.Util.shakeSurroundingTrees;

public class Gardener {

    static final float GARDENER_SPACE_RADIUS = 3.0f;

    // Tree building
    static boolean isTreePlanted;
    static int numOfTrees;
    static int numSoldiers;
    static int tries;

    // Keep number totals
    static int numScouts;
    static int numTanks;
    static int numLumberjacks;
    static int totalRobots;

    static void run() {

        try {

            // Gardener movement
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] teamInfo = rc.senseNearbyRobots(GARDENER_SPACE_RADIUS, rc.getTeam());
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();

            if (!isTreePlanted) {
                if (enemyInfo.length > 0) {
                    evadeRobotGroup(enemyInfo);
                    if (rc.hasRobotBuildRequirements(RobotType.SOLDIER)) {
                        tryToBuildUnit(RobotType.SOLDIER);
                    }
                } else if (bulletCollisionImminent(bulletInfo)) {
                    dodgeIncomingBullets(bulletInfo);
                } else if (teamInfo.length > 0) {
                    evadeRobotGroup(teamInfo);
                } else {
                    gardenerDefaultMove();
                }
            } else {
                if (enemyInfo.length > 0) {
                    if (rc.hasRobotBuildRequirements(RobotType.SOLDIER)) {
                        tryToBuildUnit(RobotType.SOLDIER);
                    }
                }
            }

            // Make sure the first soldier is built
            if (isTreePlanted) {
                if (numSoldiers < 1) {
                    if (rc.hasRobotBuildRequirements(RobotType.SOLDIER)) {
                        tryToBuildUnit(RobotType.SOLDIER);
                    }
                }
            }

            // Build, water, and update trees
            TreeInfo[] treeInfo = rc.senseNearbyTrees(-1, rc.getTeam());
            if (openTreeSpaces() > 1 && numOfTrees <= totalRobots) {
                //System.out.println("Trying to plant tree!");
                tryBuildTree();
            }

            /*
            TreeInfo[] treeInfo = rc.senseNearbyTrees(-1, rc.getTeam());
            if (numOfTrees <= maxNumTrees && numOfTrees <= totalRobots) {
                //System.out.println("Trying to plant tree!");
                tryBuildTree();
            }
            */

            if (treeInfo.length > 0) {
                waterTreeGroup(treeInfo);
            }

            updateTreeNum(treeInfo);

            // Build units
            int totalArchons = rc.readBroadcast(CHANNEL_ARCHON_COUNT);
            int totalScouts = rc.readBroadcast(CHANNEL_SCOUT_COUNT);
            int totalSoldiers = rc.readBroadcast(CHANNEL_SOLDIER_COUNT);
            int totalLumberjacks = rc.readBroadcast(CHANNEL_LUMBERJACK_COUNT);
            if (isTreePlanted) {
                if ((numScouts*6 < totalRobots && totalSoldiers >= 4*totalArchons) || (totalScouts == 0 && rc.getTeamBullets() >= 100)) {
                    if (rc.hasRobotBuildRequirements(RobotType.SCOUT)) {
                        tryToBuildUnit(RobotType.SCOUT);
                    }
                } else if (numLumberjacks*8 < totalRobots && totalSoldiers >= 4 || (totalLumberjacks == 0 && rc.getTeamBullets() >= 100)) {
                    if (rc.hasRobotBuildRequirements(RobotType.LUMBERJACK)) {
                        tryToBuildUnit(RobotType.LUMBERJACK);
                    }
                } else {
                    if (rc.hasRobotBuildRequirements(RobotType.SOLDIER)) {
                        tryToBuildUnit(RobotType.SOLDIER);
                    }
                }
            }

            // Shake trees to farm bullets
            shakeSurroundingTrees();

            // Re update if near death
            if (nearDeath() && !isNearDeath) {
                isNearDeath = true;
                rc.broadcast(CHANNEL_GARDENER_COUNT, rc.readBroadcast(CHANNEL_GARDENER_COUNT)-1);
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
        isTreePlanted = false;
        isNearDeath = false;
        numOfTrees = 0;
        numSoldiers = 0;
        numScouts = 0;
        numTanks = 0;
        numLumberjacks = 0;
        totalRobots = 0;
        tries = 0;

        // Increase robot type count
        try {
            rc.broadcast(CHANNEL_GARDENER_COUNT, rc.readBroadcast(CHANNEL_GARDENER_COUNT)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void addRobotAmt(RobotType robotType) {
        if (robotType.equals(RobotType.SOLDIER)) {
            numSoldiers += 1;
        } else if (robotType.equals(RobotType.SCOUT)) {
            numScouts += 1;
        } else if (robotType.equals(RobotType.TANK)) {
            numTanks += 1;
        } else if (robotType.equals(RobotType.LUMBERJACK)) {
            numLumberjacks += 1;
        }

        totalRobots += 1;
    }

    static void tryToBuildUnit(RobotType robotType) {

        try {

            final float radianInterval = (float)(Math.PI/3);

            if (rc.isBuildReady() && rc.hasRobotBuildRequirements(robotType) && rc.getBuildCooldownTurns() == 0) {

                // Check around robot
                float radians = 0;

                while (radians < Math.PI * 2) {
                    Direction buildDir = new Direction(radians);
                    if (rc.canBuildRobot(robotType, buildDir)) {
                        rc.setIndicatorDot(rc.getLocation().add(radians, 2.0f), 0, 0, 0);
                        rc.buildRobot(robotType, buildDir);
                        addRobotAmt(robotType);
                        //System.out.println("Successful build!");
                        return;
                    } else {
                        radians += radianInterval;
                    }
                }

                /*
                for (int i = 0; i < 5; i++) {
                    Direction buildDir = new Direction(radians);
                    if (rc.canBuildRobot(robotType, buildDir)) {
                        rc.buildRobot(robotType, buildDir);
                        addRobotAmt(robotType);
                        //System.out.println("Successful build!");
                        return;
                    }

                    radians += radianInterval;
                }
                */

                //System.out.println("Unable to build robot type " + robotType.toString() + ".");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void tryBuildTree() {

        final float radianInterval = (float)(Math.PI/3);

        try {

            // Make sure it can build (might be constructing other tree)
            if (rc.hasTreeBuildRequirements() && (rc.onTheMap(rc.getLocation(), GARDENER_SPACE_RADIUS) || tries > 10)) {

                // Search in intervals
                float radians = 0;
                while (radians <= Math.PI*2) {
                    Direction buildDir = new Direction(radians);
                    if (rc.canPlantTree(buildDir)) {
                        rc.plantTree(buildDir);
                        //System.out.println("Planted tree.");
                        isTreePlanted = true;
                        tries = 0;
                        return;
                    } else {
                        radians += radianInterval;
                    }
                }
            }

            tries++;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int openTreeSpaces() {

        final float radianInterval = (float)(Math.PI/3);

        try {

            // Search in intervals
            float radians = 0;
            int total = 0;
            for (int i = 0; i < 6; i++) {
                //if (rc.canPlantTree(buildDir)) {
                if (rc.senseNearbyTrees(rc.getLocation().add(radians, 2.0f), 1.0f, rc.getTeam().opponent()).length == 0 &&
                        rc.senseNearbyTrees(rc.getLocation().add(radians, 2.0f), 1.0f, rc.getTeam()).length == 0 &&
                        rc.senseNearbyTrees(rc.getLocation().add(radians, 2.0f), 1.0f, Team.NEUTRAL).length == 0 &&
                        rc.senseNearbyRobots(rc.getLocation().add(radians, 2.0f), 1.0f, rc.getTeam()).length == 0 &&
                        rc.onTheMap(rc.getLocation().add(radians, 2.0f), 1.0f) //&&
                        //!rc.isCircleOccupied(rc.getLocation().add(radians, 2.0f), 1.0f)
                        ) {
                    rc.setIndicatorDot(rc.getLocation().add(radians, 2.0f), 255, 255, 0);
                    //System.out.println("Can plant tree!");
                    total += 1;
                }
                radians += radianInterval;
            }

            return total;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /*
    static int findMaxNumTrees() {

        final float radianInterval = (float)(Math.PI/3);

        try {

            // Search in intervals
            float radians = 0;
            int total = 0;
            for (int i = 0; i < 6; i++) {
                //if (rc.canPlantTree(buildDir)) {
                if (!rc.isCircleOccupied(rc.getLocation().add(radians, 2.0f), 1.0f) && rc.onTheMap(rc.getLocation().add(radians, 2.0f), 1.0f)) {
                    rc.setIndicatorDot(rc.getLocation().add(radians, 2.0f), 0, 0, 255);
                    //System.out.println("Can plant tree!");
                    total += 1;
                }
                radians += radianInterval;
            }

            return total;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
    */

    static void updateTreeNum(TreeInfo[] treeInfo) {

        // Doesn't include neutral trees
        numOfTrees = treeInfo.length;
    }

    static void waterTreeGroup(TreeInfo[] treeInfo) {

        try {

            // Water tree with lowest hp
            int lowestIdx = 0;
            float lowestHp = treeInfo[0].getHealth();
            for (int i = 1; i < treeInfo.length; i++) {
                if (treeInfo[i].getHealth() < lowestHp) {
                    lowestHp = treeInfo[i].getHealth();
                    lowestIdx = i;
                }
            }

            if (rc.canWater(treeInfo[lowestIdx].getID())) {
                rc.water(treeInfo[lowestIdx].getID());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void gardenerDefaultMove() {

        try {

            // Avoid map boundaries or move randomly otherwise
            if (!avoidMapBoundaries()) {
                tryMove(randomDirection());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
