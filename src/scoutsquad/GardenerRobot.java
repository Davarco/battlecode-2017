package scoutsquad;

import battlecode.common.*;

import static scoutsquad.BroadcastChannels.ARCHON_NUMBER_CHANNEL;
import static scoutsquad.BroadcastChannels.GARDENER_CHANNEL;
import static scoutsquad.RobotEndgame.implementEndgame;
import static scoutsquad.RobotMove.*;
import static scoutsquad.RobotPlayer.*;
import static scoutsquad.RobotSquad.bindRobot;


public class GardenerRobot {

    // Process for each turn
    static void run() {

        try {

            // Go to endgame strategy if game is almost over
            if (rc.getRoundLimit() - rc.getRoundNum() < 200) {
                implementEndgame();
            }

            // Update number of fighting robots
            numFightingRobots = rc.getRobotCount() - rc.readBroadcast(ARCHON_NUMBER_CHANNEL) - rc.readBroadcast(GARDENER_CHANNEL);
            numSquads = rc.readBroadcast(GARDENER_CHANNEL);
            if (numSquads != 0) {
                maxSquadSize = Math.round(numFightingRobots/numSquads + 1);
            } else {
                maxSquadSize = 4;
            }

            // Escape enemy robots
            Team enemy = rc.getTeam().opponent();
            RobotInfo[] enemyInfo = rc.senseNearbyRobots(-1, enemy);
            if (enemyInfo.length > 0) {
                //System.out.println("Escaping from enemy robot!");
                escapeFromEnemy(enemyInfo);
            } else {
                tryMove(randomDirection());
            }

            // Generate a random direction
            Direction dir = Direction.getEast();

            // Randomly attempt to build a soldier or lumberjack in this direction
            if (squadSize < maxSquadSize || rc.getTeamBullets() > 90.0) {
                if (rc.canBuildRobot(RobotType.SCOUT, dir)) {

                    // Build robot and send out ID to bind
                    rc.buildRobot(RobotType.SCOUT, dir);
                    bindRobot();

                } /* else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .01 && rc.isBuildReady()) {

                        // Build robot and send out ID to bind
                        rc.buildRobot(RobotType.LUMBERJACK, dir);
                        bindRobot();
                    } */

            } else {
                //System.out.println("Can't build due to size.");
            }

            // Update number of gardeners if near death
            if (nearDeath() && !closeToDeath) {
                closeToDeath = true;
                rc.broadcast(GARDENER_CHANNEL, rc.readBroadcast(GARDENER_CHANNEL)-1);
                //System.out.println("This gardener is dying.");
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

            // Update number of gardeners
            boolean closeToDeath = false;
            int prevNumberGardeners = rc.readBroadcast(GARDENER_CHANNEL);
            rc.broadcast(GARDENER_CHANNEL, prevNumberGardeners+1);
            //System.out.println("Running gardener! Num: " + rc.readBroadcast(GARDENER_CHANNEL));

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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
