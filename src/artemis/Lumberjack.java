package artemis;

import battlecode.common.*;
import static artemis.Channels.*;
import static artemis.RobotPlayer.*;
import static artemis.Nav.*;
import static artemis.Util.*;
import static artemis.Combat.*;

public class Lumberjack {

    static void run() {

        try {

            // Execute every six rounds
            if (rc.getRoundNum() % 6 == 0) {
                updateRobotNum();
            }

            // Lumberjack move
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo[] nearbyTeamInfo = rc.senseNearbyRobots(1.5f, rc.getTeam());
            if (bulletCollisionImminent(bulletInfo)) {
                dodgeIncomingBullets(bulletInfo);
            } else if (nearbyTeamInfo.length > 0) {
                evadeRobotGroup(nearbyTeamInfo);
            } else if (priorityLocExists()) {
                moveToPriorityLoc();
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

        // Initialize variables
        isLocLeader = false;
        prevPriorityX = 0;
        prevPriorityY = 0;
    }

    static void updateRobotNum() {
        try {
            rc.broadcast(CHANNEL_LUMBERJACK_SUM, rc.readBroadcast(CHANNEL_LUMBERJACK_SUM)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
