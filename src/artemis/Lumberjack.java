package artemis;

import battlecode.common.*;
import static artemis.Channels.*;
import static artemis.RobotPlayer.*;
import static artemis.Nav.*;
import static artemis.Util.*;

public class Lumberjack {

    static boolean isLocLeader;
    static float prevPriorityX;
    static float prevPriorityY;

    static void run() {

        try {

            // Execute every six rounds
            if (rc.getRoundNum() % 6 == 0) {
                updateRobotNum();
            }

            // Lumberjack move
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (bulletInfo.length > 0) {
                dodgeIncomingBullets(bulletInfo);
            } else if (priorityLocExists()) {
                moveToPriorityLoc();
            } else if (enemyInfo.length > 0) {
                moveTowardsEnemy(enemyInfo);
                setPriorityLoc(enemyInfo);
                isLocLeader = true;
            } else {
                tryMove(randomDirection());
            }

            // Update the locations to go to, or reset to 0 if they don't exist
            if (isLocLeader) {
                if (!updatePriorityLocStatus(enemyInfo)) {
                    isLocLeader = false;
                }
            } else {

                // Reset if same as previous round
                if (prevPriorityX == rc.readBroadcastFloat(PRIORITY_X) && prevPriorityY == rc.readBroadcastFloat(PRIORITY_Y)) {
                    rc.broadcast(PRIORITY_X, 0);
                    rc.broadcast(PRIORITY_Y, 0);
                }
            }

            prevPriorityX = rc.readBroadcastFloat(PRIORITY_X);
            prevPriorityY = rc.readBroadcastFloat(PRIORITY_Y);

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
