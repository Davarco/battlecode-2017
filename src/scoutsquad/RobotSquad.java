package scoutsquad;

import battlecode.common.Clock;
import battlecode.common.RobotInfo;

import static scoutsquad.BroadcastChannels.BUILD_CHANNEL;
import static scoutsquad.RobotPlayer.*;


public class RobotSquad {

    static void bindRobot() {

        try {

            //System.out.println("Binding robot!");
            int idx = 0;
            if (squadChannel == 0) {

                // This binder robot has not created a leader yet
                while (rc.readBroadcast(BUILD_CHANNEL+idx) != 0) {
                    idx += 4;
                }

                rc.broadcast(BUILD_CHANNEL+idx, rc.getID());
                squadChannel = BUILD_CHANNEL+idx;
                //System.out.println("Broadcasting  @leader YES @channel " + squadChannel + " @id " + rc.getID());

            } else {

                // Has already created a leader
                rc.broadcast(squadChannel, rc.getID());
                //System.out.println("Broadcasting @leader NO @channel " + squadChannel + " @id " + rc.getID());
            }

            // Wait for robot to finish building
            for (int i = 0; i < 20; i++) {
                Clock.yield();
            }

            // Increase squad size
            squadSize++;
            //System.out.println("Number of robots in squad " + rc.getID() + ": " + squadSize);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void findRobotBind() {

        try {

            // Get possible makers
            RobotInfo[] robotInfo = rc.senseNearbyRobots(-1, rc.getTeam());
            for (RobotInfo info: robotInfo) {

                // Find right channel
                int idx = 0;
                while (rc.readBroadcast(BUILD_CHANNEL+idx) != 0) {

                    if (info.getID() == rc.readBroadcast(BUILD_CHANNEL+idx)) {

                        // Set as leader if needed
                        squadChannel = BUILD_CHANNEL+idx;
                        if (rc.readBroadcast(squadChannel+1) == 0) {
                            //System.out.println("New squad leader @maker " + info.getID() + " @channel " + squadChannel + "!");
                            isSquadLeader = true;
                            rc.broadcast(squadChannel+1, rc.getID());
                        } else {
                            //System.out.println("Not a squad leader.");
                        }

                        return;
                    }

                    idx += 4;
                }
            }

            System.out.println("Channel to bind to not found.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
