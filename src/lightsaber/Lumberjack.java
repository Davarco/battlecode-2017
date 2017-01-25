package lightsaber;

import battlecode.common.*;
import java.util.HashMap;

import static lightsaber.Channels.CHANNEL_LUMBERJACK_SUM;
import static lightsaber.Combat.defaultMeleeAttack;
import static lightsaber.Combat.destroySurroundingTrees;
import static lightsaber.Nav.*;
import static lightsaber.RobotPlayer.*;
import static lightsaber.Util.*;

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
            if (bulletCollisionImminent(bulletInfo)) {
                dodgeIncomingBullets(bulletInfo);
            } else if (priorityLocExists()) {
                moveToPriorityLoc();
            } else if (enemyInfo.length > 0) {
                moveTowardsEnemy(enemyInfo);
                setPriorityLoc(enemyInfo);
                isLocLeader = true;
            } else if (treeInfo.length > 0) {
                moveTowardsTree(treeInfo);
            } else {
                tryMove(randomDirection());
            }

            // Reset priority loc details
            resetPriorityStatus(enemyInfo);

            // Default melee attack
            defaultMeleeAttack();

            // Chop nearby trees
            destroySurroundingTrees(treeInfo);

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
        obstacleList = new HashMap<>();
    }

    static void updateRobotNum() {
        try {
            rc.broadcast(CHANNEL_LUMBERJACK_SUM, rc.readBroadcast(CHANNEL_LUMBERJACK_SUM)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
