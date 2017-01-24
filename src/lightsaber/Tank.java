package lightsaber;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.RobotInfo;

import java.util.HashMap;

import static lightsaber.Channels.CHANNEL_TANK_SUM;
import static lightsaber.Combat.defaultRangedAttack;
import static lightsaber.Nav.*;
import static lightsaber.RobotPlayer.*;
import static lightsaber.Util.*;

public class Tank {

    static void run() {

        try {

            // Tank move
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

            // Update the locations to go to, or reset to 0 if they don't exist
            if (isLocLeader) {
                if (!updatePriorityLocStatus(enemyInfo)) {
                    isLocLeader = false;
                }
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
    }

    static void updateRobotNum() {
        try {
            rc.broadcast(CHANNEL_TANK_SUM, rc.readBroadcast(CHANNEL_TANK_SUM)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}