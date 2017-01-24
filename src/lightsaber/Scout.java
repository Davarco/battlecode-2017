package lightsaber;

import battlecode.common.*;

import java.awt.*;
import java.util.HashMap;

import static lightsaber.Channels.CHANNEL_SOLDIER_SUM;
import static lightsaber.Combat.defaultRangedAttack;
import static lightsaber.Nav.*;
import static lightsaber.RobotPlayer.*;
import static lightsaber.Util.*;

public class Scout {

    static void run() {

        try {

            // Add obstacles
            /*
            TreeInfo[] treeInfo = rc.senseNearbyTrees();
            RobotInfo[] robotInfo = rc.senseNearbyRobots();
            addObstacles(treeInfo);
            addObstacles(robotInfo);
            */

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
                moveTowardsLocation(initialArchonLocations[0]);
            }

            // Reset priority loc details
            resetPriorityStatus(enemyInfo);

            // Default ranged attack
            if (enemyInfo.length > 0) {
                defaultRangedAttack(enemyInfo);
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
        obstacleList = new HashMap<>();

        initialArchonLocations = rc.getInitialArchonLocations(rc.getTeam().opponent());
    }

    static void updateRobotNum() {
        try {
            rc.broadcast(CHANNEL_SOLDIER_SUM, rc.readBroadcast(CHANNEL_SOLDIER_SUM)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
