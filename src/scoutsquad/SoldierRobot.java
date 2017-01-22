package scoutsquad;

import battlecode.common.Clock;

import static scoutsquad.RobotCombat.robotRangedAttack;
import static scoutsquad.RobotMove.robotMove;
import static scoutsquad.RobotPlayer.rc;
import static scoutsquad.RobotSquad.findRobotBind;


public class SoldierRobot {

    // Process for every turn
    static void run() {

        // Move and attack
        robotMove();
        robotRangedAttack();

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }

    // Runs once and loops run
    static void loop() {

        // Get squad
        findRobotBind();

        while (true) {

            int startTurn = rc.getRoundNum();

            try {
                run();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Catch if over maximum number of bytecodes
            int endTurn = rc.getRoundNum();
            if (startTurn + 1 > endTurn) {
                System.out.println("Over maximum bytecodes! @start " + startTurn + " @end " + endTurn);
            }
        }
    }
}
