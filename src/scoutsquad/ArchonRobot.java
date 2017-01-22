package scoutsquad;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

import static scoutsquad.BroadcastChannels.*;
import static scoutsquad.RobotEndgame.implementEndgame;
import static scoutsquad.RobotMove.*;
import static scoutsquad.RobotPlayer.*;


public class ArchonRobot {

    // Process for each turn
    static void run() {

        try {

            // Go to endgame strategy if game is almost over
            if (rc.getRoundLimit() - rc.getRoundNum() < 200) {
                implementEndgame();
            }

            // See if there have been any other head archons
            if (rc.readBroadcast(ARCHON_CHANNEL) == 0 || isHeadLeader) {
                isHeadLeader = true;

                // Reset the number of robots
                rc.broadcast(ARCHON_CHANNEL, (int) (Math.random() * 1000));

            } else if (prevArchonNum == rc.readBroadcast(ARCHON_CHANNEL)) {
                isHeadLeader = true;
                //System.out.println("Became new head leader! @" + prevArchonNum);
            } else {
                prevArchonNum = rc.readBroadcast(ARCHON_CHANNEL);
                //System.out.println("Not head leader... @" + prevArchonNum);
            }

            // Escape enemy robots
            Team enemy = rc.getTeam().opponent();
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, enemy);
            if (enemyInfo.length > 0) {
                //System.out.println("Escaping from enemy robot!");
                escapeFromEnemy(enemyInfo);
            }

            // Update number of max gardeners
            switch (rc.readBroadcast(ARCHON_NUMBER_CHANNEL)) {
                case 1:
                    maxGardeners = 3;
                    break;
                case 2:
                    maxGardeners = 4;
                    break;
                case 3:
                    maxGardeners = 5;
                    break;
            }

            // Dodge bullets if necessary
            if (!rc.hasMoved()) {
                dodgeIncomingBullets();
            }

            // Try to build gardener by checking around archon
            int prevNumGardeners = rc.readBroadcast(GARDENER_CHANNEL);
            //System.out.println(prevNumGardeners);
            //System.out.println("The previous amount of gardeners was " + prevNumGardeners + ".");
            if (prevNumGardeners < maxGardeners) {
                Direction buildGardnerDirection = randomDirection();
                int tries = 0;
                while (!rc.canHireGardener(buildGardnerDirection) && tries < 4) {
                    buildGardnerDirection = randomDirection();
                    tries++;
                }
                try {
                    if (rc.canHireGardener(buildGardnerDirection)) {
                        //System.out.println("Building new gardener!");
                        rc.hireGardener(buildGardnerDirection);
                    }
                } catch (Exception e) {
                    //System.out.println("Can't build gardener now...");
                }
            }

            // Update number of archons if near death
            if (nearDeath() && !closeToDeath) {
                closeToDeath = true;
                rc.broadcast(ARCHON_NUMBER_CHANNEL, rc.readBroadcast(ARCHON_NUMBER_CHANNEL) - 1);
            }

            // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
            Clock.yield();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Runs once and loops run
    static void loop() {

        try {

            // Only one head leader
            prevArchonNum = 0;
            isHeadLeader = false;
            closeToDeath = false;

            // Update number of archons
            int prevNumberArchons = rc.readBroadcast(ARCHON_NUMBER_CHANNEL);
            rc.broadcast(ARCHON_NUMBER_CHANNEL, prevNumberArchons + 1);

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
                    System.out.println("Over maximum bytecodes! Start @" + startTurn + " End @" + endTurn);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
