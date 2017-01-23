package artemis;

import battlecode.common.*;
import static artemis.Channels.*;
import static artemis.RobotPlayer.*;
import static artemis.Nav.*;
import static artemis.Util.*;
import static artemis.Combat.*;

public class Tank {

    static boolean isLocLeader;

    static void run() {

        try {

            // Execute every six rounds
            if (rc.getRoundNum() % 6 == 0) {
                updateRobotNum();
            }

            // Tank move
            BulletInfo[] bulletInfo = rc.senseNearbyBullets();
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (bulletInfo.length > 0) {
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

            // Attack enemies within range
            defaultRangedAttack(enemyInfo);

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
    }

    static void updateRobotNum() {
        try {
            rc.broadcast(CHANNEL_TANK_SUM, rc.readBroadcast(CHANNEL_TANK_SUM)+1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
