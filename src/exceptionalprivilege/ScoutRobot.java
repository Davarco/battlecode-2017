package exceptionalprivilege;

import static exceptionalprivilege.RobotPlayer.*;
import static exceptionalprivilege.RobotMove.*;
import static exceptionalprivilege.RobotCombat.*;
import static exceptionalprivilege.RobotSquad.*;
import battlecode.common.*;

public class ScoutRobot {

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
