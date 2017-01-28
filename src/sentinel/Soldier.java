package sentinel;

import battlecode.common.*;

import static sentinel.Channels.CHANNEL_SOLDIER_COUNT;
import static sentinel.Combat.defaultRangedAttack;
import static sentinel.Nav.*;
import static sentinel.RobotPlayer.*;
import static sentinel.Util.*;

public class Soldier {

    static void run() {

        try {

            // Reset alternate every 20 turns
            if (rc.getRoundNum() % 20 == 0) {
                resetAltPriorityLoc();
            }

            // Soldier move
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (bulletCollisionImminent(bulletInfo)) {
                dodgeIncomingBullets(bulletInfo);
            } else if (priorityLocExists()) {
                moveToPriorityLoc();
            } else if (altPriorityLocExists()) {
                moveToAltPriorityLoc();
            } else if (enemyInfo.length > 0) {
                moveTowardsEnemy(enemyInfo);
                setPriorityLoc(enemyInfo);
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

            // Default ranged attack
            if (enemyInfo.length > 0) {
                defaultRangedAttack(enemyInfo);
            }

            // Destroy trees in way if possible
            //destroyTreesInWay();

            // Shake trees to farm bullets
            shakeSurroundingTrees();

            // Re update if near death
            if (nearDeath() && !isNearDeath) {
                isNearDeath = true;
                rc.broadcast(CHANNEL_SOLDIER_COUNT, rc.readBroadcast(CHANNEL_SOLDIER_COUNT)-1);
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
        prevPriorityX = 0;
        prevPriorityY = 0;
        initialArchonLocations = rc.getInitialArchonLocations(rc.getTeam().opponent());
        currentDirection = rc.getLocation().directionTo(initialArchonLocations[0]);
        //obstacleList = new HashMap<>();

        // Increase robot type count
        try {
            rc.broadcast(CHANNEL_SOLDIER_COUNT, rc.readBroadcast(CHANNEL_SOLDIER_COUNT)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
