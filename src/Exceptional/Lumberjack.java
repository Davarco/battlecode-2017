package Exceptional;

import battlecode.common.*;

import static Exceptional.Channels.CHANNEL_LUMBERJACK_COUNT;
import static Exceptional.Combat.defaultMeleeAttack;
import static Exceptional.Combat.destroySurroundingTrees;
import static Exceptional.Nav.*;
import static Exceptional.RobotPlayer.*;
import static Exceptional.Util.*;


public class Lumberjack {

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

            // Lumberjack move
            TreeInfo[] treeInfo = combineArrayData(rc.senseNearbyTrees(-1, Team.NEUTRAL), rc.senseNearbyTrees(-1, rc.getTeam().opponent()));
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] teamInfo = rc.senseNearbyRobots(4.1f, rc.getTeam());
            if (treeInfo.length > 0) {
                if (!moveTowardsTree(treeInfo) && teamInfo.length > 0) {
                    evadeRobotGroup(teamInfo);
                }
            } else if (bulletCollisionImminent(bulletInfo)) {
                dodgeIncomingBullets(bulletInfo);
            } else if (priorityLocExists()) {
                moveToPriorityLoc();
            } else if (altPriorityLocExists()) {
                moveToAltPriorityLoc();
            } else if (teamInfo.length > 0) {
                evadeRobotGroup(teamInfo);
            } else if (enemyInfo.length > 0) {
                moveTowardsEnemy(enemyInfo);
                setPriorityLoc(enemyInfo);
                isLocLeader = true;
            } else {

                // See if robot can still move in current dir
                if (rc.canMove(currentDirection)) {
                    rc.move(currentDirection);
                } else {
                    MapLocation prevLoc = rc.getLocation();
                    tryMove(randomDirection(), 5, 36);
                    MapLocation postLoc = rc.getLocation();
                    currentDirection = prevLoc.directionTo(postLoc);
                    if (currentDirection == null) {
                        int i = (int)(Math.random()*initialArchonLocations.length);
                        currentDirection = rc.getLocation().directionTo(initialArchonLocations[i]);
                    }
                }
            }

            // Reset priority loc details
            resetPriorityStatus(enemyInfo);

            // Default melee attack
            defaultMeleeAttack();

            // Chop nearby trees
            destroySurroundingTrees(treeInfo);

            // Shake trees to farm bullets
            shakeSurroundingTrees();

            // Re update if near death
            if (nearDeath() && !isNearDeath) {
                isNearDeath = true;
                rc.broadcast(CHANNEL_LUMBERJACK_COUNT, rc.readBroadcast(CHANNEL_LUMBERJACK_COUNT)-1);
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
        //obstacleList = new HashMap<>();
        initialArchonLocations = rc.getInitialArchonLocations(rc.getTeam().opponent());
        currentDirection = rc.getLocation().directionTo(initialArchonLocations[0]);

        // Increase robot type count
        try {
            rc.broadcast(CHANNEL_LUMBERJACK_COUNT, rc.readBroadcast(CHANNEL_LUMBERJACK_COUNT)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
