package Exceptional;

import battlecode.common.*;

import static Exceptional.Channels.CHANNEL_SCOUT_COUNT;
import static Exceptional.Nav.*;
import static Exceptional.RobotPlayer.*;
import static Exceptional.Util.*;

public class Scout {

    static void run() {

        try {

            // Reset alternate every 6 turns
            if (rc.getRoundNum() % 6 == 0) {
                resetAltPriorityLoc();
            }

            // Add obstacles
            /*
            TreeInfo[] treeInfo = rc.senseNearbyTrees();
            RobotInfo[] robotInfo = rc.senseNearbyRobots();
            addObstacles(treeInfo);
            addObstacles(robotInfo);
            */

            /*

            // Scout move
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (bulletCollisionImminent(bulletInfo)) {
                dodgeIncomingBullets(bulletInfo);
            } else if (priorityLocExists()) {
                moveToPriorityLoc();
            } else if (enemyInfo.length > 0) {
                moveTowardsEnemy(enemyInfo);
                setPriorityLoc(enemyInfo);
            } else {
                tryMove(randomDirection());
            }

            // Reset priority loc details
            resetPriorityStatus(enemyInfo);

            */

            // Scout move
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            TreeInfo[] treeInfo = rc.senseNearbyTrees();
            MapLocation treeLoc = treesHaveBullets(treeInfo);
            if (bulletCollisionImminent(bulletInfo)) {
                dodgeIncomingBullets(bulletInfo);
            } else if (enemyInfo.length > 0) {

                // Avoid combat units, but attack sole unarmed robots
                if (enemyHostilesInRange(enemyInfo)) {
                    MapLocation prevLoc = rc.getLocation();
                    evadeRobotGroup(enemyInfo);
                    MapLocation postLoc = rc.getLocation();
                    currentDirection = prevLoc.directionTo(postLoc);
                    if (currentDirection == null) {
                        int i = (int)(Math.random()*initialArchonLocations.length);
                        currentDirection = rc.getLocation().directionTo(initialArchonLocations[i]);
                    }
                    setAltPriorityLoc(enemyInfo);
                } else {
                    moveTowardsEnemy(enemyInfo);
                    setAltPriorityLoc(enemyInfo);
                }
            } else if (treeLoc != null) {
                moveTowardsLocation(treeLoc);
            } else {

                // See if robot can still move in current dir
                if (rc.canMove(currentDirection)) {
                    rc.move(currentDirection);
                    numTries += 1;
                } else {
                    MapLocation prevLoc = rc.getLocation();
                    tryMove(randomDirection(), 5, 36);
                    MapLocation postLoc = rc.getLocation();
                    currentDirection = prevLoc.directionTo(postLoc);
                    if (currentDirection == null) {
                        int i = (int)(Math.random()*initialArchonLocations.length);
                        currentDirection = rc.getLocation().directionTo(initialArchonLocations[i]);
                    }
                    numTries = 0;
                }
            }

            // Default ranged attack
            /*
            if (enemyInfo.length > 0) {
                defaultRangedAttack(enemyInfo);
            }
            */

            // Shake trees to farm bullets
            shakeSurroundingTrees();

            // Re update if near death
            if (nearDeath() && !isNearDeath) {
                isNearDeath = true;
                rc.broadcast(CHANNEL_SCOUT_COUNT, rc.readBroadcast(CHANNEL_SCOUT_COUNT)-1);
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
        isLocLeader = false;
        isNearDeath = false;
        prevPriorityX = 0;
        prevPriorityY = 0;
        initialArchonLocations = rc.getInitialArchonLocations(rc.getTeam().opponent());
        currentDirection = rc.getLocation().directionTo(initialArchonLocations[0]);
        numTries = 0;

        // Increase robot type count
        try {
            rc.broadcast(CHANNEL_SCOUT_COUNT, rc.readBroadcast(CHANNEL_SCOUT_COUNT)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
