package sentinel;

import battlecode.common.*;

import static sentinel.Channels.CHANNEL_LUMBERJACK_SUM;
import static sentinel.Combat.defaultMeleeAttack;
import static sentinel.Combat.destroySurroundingTrees;
import static sentinel.Nav.*;
import static sentinel.RobotPlayer.*;
import static sentinel.Util.*;

public class Lumberjack {

    static void run() {

        try {

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
            } else if (teamInfo.length > 0) {
                evadeRobotGroup(teamInfo);
            } else if (enemyInfo.length > 0) {
                moveTowardsEnemy(enemyInfo);
                setPriorityLoc(enemyInfo);
                isLocLeader = true;
            } else {
                tryMove(randomDirection());
            }

            // Reset priority loc details
            resetPriorityStatus(enemyInfo);

            // Default melee attack
            defaultMeleeAttack();

            // Chop nearby trees
            destroySurroundingTrees(treeInfo);

            // Shake trees to farm bullets
            shakeSurroundingTrees();

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
        //obstacleList = new HashMap<>();
    }

    static void updateRobotNum() {
        try {
            rc.broadcast(CHANNEL_LUMBERJACK_SUM, rc.readBroadcast(CHANNEL_LUMBERJACK_SUM)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
