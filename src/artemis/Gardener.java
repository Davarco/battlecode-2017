package artemis;

import battlecode.common.*;
import java.util.*;
import static artemis.Channels.*;
import static artemis.RobotPlayer.*;
import static artemis.Nav.*;
import static artemis.Util.*;

public class Gardener {

    static final float GARDENER_SPACE_RADIUS = 4.0f;

    static boolean isTreePlanted;
    static int numOfTrees;
    static int maxNumTrees;
    static int numSoldiers;
    static int numScouts;
    static int numTanks;
    static int numLumberjacks;
    static int totalRobots;

    static void run() {

        try {

            // Execute every six rounds
            if (rc.getRoundNum() % 6 == 0) {
                updateRobotNum();
            }

            // Gardener movement
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] teamInfo = rc.senseNearbyRobots(GARDENER_SPACE_RADIUS, rc.getTeam());
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();

            if (!isTreePlanted) {
                if (enemyInfo.length > 0) {
                    evadeRobotGroup(enemyInfo);
                } else if (bulletInfo.length > 0) {
                    dodgeIncomingBullets(bulletInfo);
                } else if (teamInfo.length > 0) {
                    evadeRobotGroup(teamInfo);
                } else {
                    gardenerDefaultMove();
                }

                maxNumTrees = findMaxNumTrees() - 1;
                System.out.println("Number of max trees: " + maxNumTrees);
            }

            // Build, water, and update trees
            TreeInfo[] treeInfo = rc.senseNearbyTrees(-1, rc.getTeam());
            if (numOfTrees <= maxNumTrees) {
                //System.out.println("Trying to plant tree!");
                tryBuildTree();
            }

            if (treeInfo.length > 0) {
                waterTreeGroup(treeInfo);
            }

            updateTreeNum(treeInfo);

            // Build units
            if (isTreePlanted) {
                if (numScouts * 6 <= totalRobots) {
                    if (rc.hasRobotBuildRequirements(RobotType.SCOUT)) {
                        tryToBuildUnit(RobotType.SCOUT);
                    }
                } else if (numLumberjacks * 2 <= totalRobots) {
                    if (rc.hasRobotBuildRequirements(RobotType.LUMBERJACK)) {
                        tryToBuildUnit(RobotType.LUMBERJACK);
                    }
                } else {
                    if (rc.hasRobotBuildRequirements(RobotType.SOLDIER)) {
                        tryToBuildUnit(RobotType.SOLDIER);
                    }
                }
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

        isTreePlanted = false;
        numOfTrees = 0;
        maxNumTrees = 5;
        numSoldiers = 0;
        numScouts = 0;
        numTanks = 0;
        numLumberjacks = 0;
        totalRobots = 0;
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

            final float radianInterval = (float)(Math.PI/6);

            if (rc.isBuildReady() && rc.hasRobotBuildRequirements(robotType) && rc.getBuildCooldownTurns() == 0) {

                // Check around robot
                float radians = 0;
                while (radians < Math.PI * 2) {
                    Direction buildDir = new Direction(radians);
                    if (rc.canBuildRobot(robotType, buildDir)) {
                        rc.buildRobot(robotType, buildDir);
                        addRobotAmt(robotType);
                        //System.out.println("Successful build!");
                        return;
                    } else {
                        radians += radianInterval;
                    }
                }
            }

            //System.out.println("Unable to build robot type " + robotType.toString() + ".");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void updateRobotNum() {
        try {
            rc.broadcast(CHANNEL_GARDENER_SUM, rc.readBroadcast(CHANNEL_GARDENER_SUM)+1);
            rc.broadcast(CHANNEL_TREE_SUM, rc.readBroadcast(CHANNEL_TREE_SUM)+numOfTrees);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void tryBuildTree() {

        final float radianInterval = (float)(Math.PI/6);

        try {

            // Make sure it can build (might be constructing other tree)
            if (rc.hasTreeBuildRequirements() && rc.onTheMap(rc.getLocation(), GARDENER_SPACE_RADIUS)) {

                // Search in intervals
                float radians = 0;
                while (radians <= Math.PI*2) {
                    Direction buildDir = new Direction(radians);
                    if (rc.canPlantTree(buildDir)) {
                        rc.plantTree(buildDir);
                        //System.out.println("Planted tree.");
                        isTreePlanted = true;
                        return;
                    } else {
                        radians += radianInterval;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int findMaxNumTrees() {

        final float radianInterval = (float)(Math.PI/3);

        try {

            // Search in intervals
            float radians = 0;
            int total = 0;
            for (int i = 0; i < 5; i++) {
                Direction buildDir = new Direction(radians);
                if (rc.canPlantTree(buildDir)) {
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
