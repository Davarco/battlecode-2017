package sentinel;

import battlecode.common.*;

import static sentinel.Channels.*;
import static sentinel.Nav.*;
import static sentinel.RobotPlayer.*;
import static sentinel.Util.*;

public class Gardener {

    static final float GARDENER_SPACE_RADIUS = 3.0f;

    // Tree building
    static boolean isTreePlanted;
    static int numOfTrees;
    static int numSoldiers;
    static int tries;
    static int totalTries;
    static Direction buildRobotDir;

    // Keep number totals
    static int numScouts;
    static int numTanks;
    static int numLumberjacks;
    static int totalRobots;

    static void run() {

        try {

            // Reset alternate every 6 turns
            if (rc.getRoundNum() % 6 == 0) {
                resetAltPriorityLoc();
            }

            // Gardener movement
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] teamInfo = rc.senseNearbyRobots(GARDENER_SPACE_RADIUS, rc.getTeam());
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();

            if (!isTreePlanted) {
                if (!enoughSpaceToBuild()) {
                    if (enemyInfo.length > 0) {
                        evadeRobotGroup(enemyInfo);
                    } else {
                        gardenerDefaultMove();
                    }
                }

                // Try to get at least 4 tree spaces
                if (enoughSpaceToBuild() || (totalTries > 30 && rc.readBroadcast(CHANNEL_GARDENER_COUNT) == 1) || totalTries > 100) {
                    tryBuildTree();
                }
                totalTries += 1;

            } else {
                if (enemyInfo.length > 0) {
                    setAltPriorityLoc(enemyInfo);
                }
            }

            // Get best robot build direction
            getBestBuildDirection();

            // Build, water, and update trees
            TreeInfo[] treeInfo = rc.senseNearbyTrees(-1, rc.getTeam());
            updateTreeNum(treeInfo);

            // Make sure the first lumberjack and soldier are built
            if (numSoldiers < 1 && isTreePlanted) {
                if (rc.hasRobotBuildRequirements(RobotType.SOLDIER)) {
                    tryToBuildUnit(RobotType.SOLDIER);
                }
            }

            if (numLumberjacks < 1 && numOfTrees > 2) {
                if (rc.hasRobotBuildRequirements(RobotType.LUMBERJACK)) {
                    tryToBuildUnit(RobotType.LUMBERJACK);
                }
            }

            if (openTreeSpaces() > 1 && isTreePlanted) {
                //System.out.println("Trying to plant tree!");
                tryBuildTree();
            }

            if (treeInfo.length > 0) {
                waterTreeGroup(treeInfo);
            }

            /*
            TreeInfo[] treeInfo = rc.senseNearbyTrees(-1, rc.getTeam());
            if (numOfTrees <= maxNumTrees && numOfTrees <= totalRobots) {
                //System.out.println("Trying to plant tree!");
                tryBuildTree();
            }
            */

            // Build units
            int totalArchons = rc.readBroadcast(CHANNEL_ARCHON_COUNT);
            int totalScouts = rc.readBroadcast(CHANNEL_SCOUT_COUNT);
            int totalSoldiers = rc.readBroadcast(CHANNEL_SOLDIER_COUNT);
            int totalLumberjacks = rc.readBroadcast(CHANNEL_LUMBERJACK_COUNT);
            if (isTreePlanted) {
                if (numLumberjacks*4 < totalRobots && totalSoldiers >= 3*totalArchons || (numLumberjacks == 0 && rc.getTeamBullets() >= 100)) {
                    if (rc.hasRobotBuildRequirements(RobotType.LUMBERJACK)) {
                        tryToBuildUnit(RobotType.LUMBERJACK);
                    }
                } else if ((numScouts*9 < totalRobots && totalSoldiers >= 6*totalArchons) /*|| (numScouts == 0 && rc.getTeamBullets() >= 100)*/) {
                    if (rc.hasRobotBuildRequirements(RobotType.SCOUT)) {
                        tryToBuildUnit(RobotType.SCOUT);
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
        initialArchonLocations = rc.getInitialArchonLocations(rc.getTeam().opponent());
        currentDirection = rc.getLocation().directionTo(initialArchonLocations[0]);
        numTries = 0;
        totalTries = 0;

        // Increase robot type count
        try {
            rc.broadcast(CHANNEL_GARDENER_COUNT, rc.readBroadcast(CHANNEL_GARDENER_COUNT)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void getBestBuildDirection() {

        try {

            final float radianInterval = (float)(Math.PI/3);
            float minDiff = (float)(Math.PI*2);
            Direction idealDir = rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);

            // Check around robot
            float radians = 0;
            while (radians < Math.PI*2) {
                if (isNotOccupiedNearby(radians)) {
                    Direction dir = new Direction(radians);
                    //System.out.println("Degrees between: " + idealDir.radiansBetween(dir));
                    //rc.setIndicatorDot(rc.getLocation().add(dir, 2.0f), 255, 255, 255);
                    if (Math.abs(idealDir.radiansBetween(dir)) < minDiff) {
                        minDiff = Math.abs(idealDir.radiansBetween(dir));
                        buildRobotDir = dir;
                        //System.out.println("Setting ideal direction to build robot @" + Math.toDegrees(radians));
                    }
                }

                radians += radianInterval;
            }

            //rc.setIndicatorDot(rc.getLocation().add(idealDir, 2.0f), 0, 102, 102);
            //rc.setIndicatorDot(rc.getLocation().add(buildRobotDir, 2.0f), 0, 0, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean enoughSpaceToBuild() {
        try {
            if (!rc.isCircleOccupiedExceptByThisRobot(rc.getLocation(), 2.0f) &&
                    rc.onTheMap(rc.getLocation(), 2.0f) &&
                    rc.senseNearbyTrees(5.0f).length == 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
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

            final float radianInterval = (float)(Math.PI/12);

            if (rc.isBuildReady() && rc.hasRobotBuildRequirements(robotType) && buildRobotDir != null && rc.getBuildCooldownTurns() == 0) {

                // Check if the ideal direction is possible
                if (rc.canBuildRobot(robotType, buildRobotDir)) {
                    //rc.setIndicatorDot(rc.getLocation().add(buildRobotDir, 2.0f), 0, 0, 0);
                    rc.buildRobot(robotType, buildRobotDir);
                    addRobotAmt(robotType);
                    return;
                }

                // Check around robot
                float radians = 0;

                while (radians < Math.PI * 2) {
                    Direction buildDir = new Direction(radians);
                    if (rc.canBuildRobot(robotType, buildDir)) {
                        //rc.setIndicatorDot(rc.getLocation().add(radians, 2.0f), 0, 0, 0);
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
            if (rc.hasTreeBuildRequirements() && buildRobotDir != null && (rc.onTheMap(rc.getLocation(), GARDENER_SPACE_RADIUS) || tries > 10)) {

                // Search in intervals
                float radians = 0;
                while (radians <= Math.PI*2) {
                    Direction buildDir = new Direction(radians);
                    if (rc.canPlantTree(buildDir) && Math.abs(buildDir.radiansBetween(buildRobotDir)) > 0.1) {
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
                if (isNotOccupiedNearby(radians)) {
                    rc.setIndicatorDot(rc.getLocation().add(radians, 2.0f), 255, 215, 0);
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

                // See if robot can still move in current dir
                if (rc.canMove(currentDirection) && numTries < 32) {
                    rc.move(currentDirection);
                    numTries += 1;
                } else {
                    MapLocation prevLoc = rc.getLocation();
                    tryMove(randomDirection(), 5, 36);

                    // Get post location and set
                    MapLocation postLoc = rc.getLocation();
                    currentDirection = prevLoc.directionTo(postLoc);
                    if (currentDirection == null) {
                        int i = (int) (Math.random() * initialArchonLocations.length);
                        currentDirection = rc.getLocation().directionTo(initialArchonLocations[i]);
                    }
                    numTries = 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
