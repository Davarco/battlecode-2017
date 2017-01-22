package artemis;

import battlecode.common.*;
import static artemis.Channels.*;
import static artemis.RobotPlayer.*;
import static artemis.Nav.*;
import static artemis.Util.*;

public class Gardener {

    static final float GARDENER_SPACE_RADIUS = 4.0f;

    static boolean isTreePlanted;
    static int numOfTrees;
    static int maxNumTrees;


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
            if (enemyInfo.length > 0) {
                evadeRobotGroup(enemyInfo);
            } else if (bulletInfo.length > 0) {
                dodgeIncomingBullets(bulletInfo);
            } else if (teamInfo.length > 0) {
                evadeRobotGroup(teamInfo);
            } else if (!isTreePlanted) {
                gardenerDefaultMove();
            }

            // Build, water, and update trees
            TreeInfo[] treeInfo = rc.senseNearbyTrees(-1, rc.getTeam());
            if (numOfTrees < maxNumTrees) {
                tryBuildTree();
            }

            if (numOfTrees > 0) {
                waterTreeGroup(treeInfo);
            }

            updateTreeNum(treeInfo);


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
    }

    static void updateRobotNum() {
        try {
            rc.broadcast(CHANNEL_GARDENER_SUM, rc.readBroadcast(CHANNEL_GARDENER_SUM)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void tryBuildTree() {

        final float radianInterval = (float)(Math.PI/12);

        try {

            // Make sure it can build (might be constructing other tree)
            if (rc.hasTreeBuildRequirements() && rc.isBuildReady() && rc.onTheMap(rc.getLocation(), GARDENER_SPACE_RADIUS)) {

                // Search in intervals
                float radians = 0;
                while (radians <= Math.PI * 2) {
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

                System.out.println("Couldn't plant tree.");
                int numSurroundingTrees = rc.senseNearbyTrees(GARDENER_SPACE_RADIUS, rc.getTeam()).length;
                if (numSurroundingTrees != 0) {
                    maxNumTrees = numSurroundingTrees;
                    System.out.println("New max trees is " + maxNumTrees + ".");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
