package superplayer;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

public strictfp class RobotPlayer {

    static RobotController rc;
    enum GeneralDirection {
        NE, NW, SE, SW
    }

    // Keep broadcasting channels
    final static int GARDENER_CHANNEL = 0;
    final static int SOLDIER_CHANNEL = 1;
    final static int TANK_CHANNEL = 2;
    final static int SCOUT_CHANNEL = 3;
    final static int LUMBERJACK_CHANNEL = 4;
    final static int ARCHON_CHANNEL = 5;
    final static int ARCHON_NUMBER_CHANNEL = 6;

    final static int TEMP_ENEMY_X = 7;
    final static int TEMP_ENEMY_Y = 8;

    final static int BEGINNING_SUBLEADER_CHANNEL = 10;

    // Previous locations the robot has escaped to
    static GeneralDirection previousEscapeDirection;

    // Constants
    final static float BUILD_LENGTH = (float)2.01001;
    final static float BOUNDARIES_LENGTH = (float)4.0;
    final static float QUADRANT_SIZE = (float)Math.PI/2;

    // TODO: Make this more dynamic?
    static int maxGardeners = 3;

    // Store temporary enemy info
    static int tempLocX = 0;
    static int tempLocY = 0;

    // Store robot squad info
    //static List<RobotInfo> squadDataList = new ArrayList<>();
    static int squadLeader;
    static int squadLeaderChannel;
    static boolean isHeadLeader;
    static boolean isSubLeader;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case TANK:
                runTank();
                break;
            case SCOUT:
                runScout();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
        }
    }

    static void runArchon() throws GameActionException {

        // Only one head leader
        int prevArchonNum = 0;
        isHeadLeader = false;

        // Update number of archons
        boolean closeToDeath = false;
        int prevNumberArchons = rc.readBroadcast(ARCHON_NUMBER_CHANNEL);
        rc.broadcast(ARCHON_NUMBER_CHANNEL, prevNumberArchons+1);

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                /*
                PRIORITY:
                1. Get away from enemy robots
                2. Build gardeners
                3. Shake tree for bullets?
                 */

                // See if there have been any other head archons
                if (rc.readBroadcast(ARCHON_CHANNEL) == 0 || isHeadLeader) {
                    isHeadLeader = true;
                    //System.out.println("Is head leader!");

                    // Reset the number of robots
                    // TODO: Probably won't work with multiple archons, need to have a leader
                    //System.out.println("Resetting data from archon...");
                    rc.broadcast(ARCHON_CHANNEL, (int)(Math.random()*1000));
                    //rc.broadcast(GARDENER_CHANNEL, 0);
                    //rc.broadcast(SOLDIER_CHANNEL, 0);
                    //rc.broadcast(TANK_CHANNEL, 0);
                    //rc.broadcast(SCOUT_CHANNEL, 0);
                    //rc.broadcast(LUMBERJACK_CHANNEL, 0);
                    //System.out.println("Finished!");

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
                        maxGardeners = 6;
                        break;
                    case 3:
                        maxGardeners = 8;
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
                    rc.broadcast(ARCHON_NUMBER_CHANNEL, rc.readBroadcast(ARCHON_NUMBER_CHANNEL)-1);
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

	static void runGardener() throws GameActionException {

        // Update number of gardeners
        boolean closeToDeath = false;
        int prevNumberGardeners = rc.readBroadcast(GARDENER_CHANNEL);
        rc.broadcast(GARDENER_CHANNEL, prevNumberGardeners+1);
        //System.out.println("Running gardener! Num: " + rc.readBroadcast(GARDENER_CHANNEL));

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                robotMove();

                // Generate a random direction
                Direction dir = Direction.getEast();

                // Randomly attempt to build a soldier or lumberjack in this direction
                if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .01) {

                    // Build robot and send out ID to bind
                    rc.buildRobot(RobotType.SOLDIER, dir);
                    int gardenerId = rc.getID();
                    broadcastGardenerId(gardenerId);

                    //Clock.yield();
                    Clock.yield();
                    getBuiltLeaderId(dir);

                } else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .01 && rc.isBuildReady()) {

                    // Build robot and send out ID to bind
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                    int gardenerId = rc.getID();
                    broadcastGardenerId(gardenerId);

                    //Clock.yield();
                    Clock.yield();
                    getBuiltLeaderId(dir);
                }

                // Broadcast new squad leader
                if (squadLeaderChannel != 0) {
                    rc.broadcast(squadLeaderChannel, squadLeader);
                }

                // Update number of gardeners if near death
                if (nearDeath() && !closeToDeath) {
                    closeToDeath = true;
                    rc.broadcast(GARDENER_CHANNEL, rc.readBroadcast(GARDENER_CHANNEL)-1);
                    System.out.println("This gardener is dying.");
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    static void runSoldier() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        setAsNewLeader();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                robotMove();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

    static void runTank() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        setAsNewLeader();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                robotMove();

                // Dodge bullets if necessary
                dodgeIncomingBullets();
                if (!rc.hasMoved()) {
                    followSquadLeader();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runScout() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        setAsNewLeader();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                robotMove();

                // Dodge bullets if necessary
                dodgeIncomingBullets();
                if (!rc.hasMoved()) {
                    followSquadLeader();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        setAsNewLeader();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                robotMove();

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if (robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if (robots.length > 0 && !rc.hasMoved()) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        tryMove(toEnemy);
                    } else if (!rc.hasMoved()) {
                        // Move Randomly
                        tryMove(randomDirection());
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }

    static void robotMove() {

        try {

            // All robots follow the same priority list
            dodgeIncomingBullets();
            if (!rc.hasMoved()) {
                if (isSubLeader) {
                    RobotInfo[] enemyLoc = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
                    if (enemyLoc.length != 0) {

                        // Go attack closest enemy
                        tryMove(rc.getLocation().directionTo(enemyLoc[0].getLocation()), 10, 5);

                        // See if a new target is needed
                        if ((rc.readBroadcast(TEMP_ENEMY_X) == 0 && rc.readBroadcast(TEMP_ENEMY_Y) == 0) ||
                                (rc.readBroadcast(TEMP_ENEMY_X) == tempLocX && rc.readBroadcast(TEMP_ENEMY_Y) == tempLocY)) {
                            rc.broadcast(TEMP_ENEMY_X, (int)enemyLoc[0].getLocation().x);
                            rc.broadcast(TEMP_ENEMY_Y, (int)enemyLoc[0].getLocation().y);
                            tempLocX = (int)enemyLoc[0].getLocation().x;
                            tempLocY = (int)enemyLoc[0].getLocation().y;
                            //System.out.println("Enemy @" + tempLocX + ", " + tempLocY);
                        } else {
                            tempLocX = rc.readBroadcast(TEMP_ENEMY_X);
                            tempLocY = rc.readBroadcast(TEMP_ENEMY_Y);
                        }

                    } else {

                        // No enemies found
                        int x = rc.readBroadcast(TEMP_ENEMY_X);
                        int y = rc.readBroadcast(TEMP_ENEMY_Y);
                        MapLocation enemyBcLoc = new MapLocation(x, y);

                        // Check if there are still robots at location
                        if (x != 0 && y != 0 && rc.canSensePartOfCircle(enemyBcLoc, rc.getType().sensorRadius)) {
                            if (rc.senseNearbyRobots(enemyBcLoc, -1, rc.getTeam().opponent()).length == 0) {
                                rc.broadcast(TEMP_ENEMY_X, 0);
                                rc.broadcast(TEMP_ENEMY_Y, 0);
                                //System.out.println("Location @" + x + ", " + y + " has already been decimated.");
                                x = 0;
                                y = 0;
                            }
                        }

                        if (x != 0 && y != 0) {
                            if (!tryMove(rc.getLocation().directionTo(enemyBcLoc), 5, 36, 1)) {
                                tryMove(randomDirection());
                                //System.out.println("Trying to get to enemy location @" + x + ", " + y);
                            }
                        } else {
                            tryMove(randomDirection());
                        }
                    }
                } else {
                    //System.out.println("Following squad leader!");
                    followSquadLeader();
                }
            }

            if (!rc.hasMoved()) {
                tryMove(randomDirection());
            }

            // Shake surrounding trees
            TreeInfo[] treeInfo = rc.senseNearbyTrees();
            for (TreeInfo info: treeInfo) {
                if (rc.canShake(info.getLocation())) {
                    rc.shake(info.getLocation());
                    //System.out.println("Shaking tree!");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean nearDeath() {
        return (rc.getHealth()*8 < rc.getType().maxHealth);
    }

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    static float randomDirectionValue() {
        return (float)Math.random() * 2 * (float)Math.PI;
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide, float v) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir, v)) {
            rc.move(dir, v);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck), v)) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck), v);
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck), v)) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck), v);
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }

    // Extra collision function that takes in new location as input as well
    static boolean willCollideWithMe(BulletInfo bullet, float x, float y) {
        MapLocation myLocation = new MapLocation(x, y);

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }

    static GeneralDirection getGeneralDirection(float numRadians) {
        if (numRadians < QUADRANT_SIZE) {
            return GeneralDirection.NE;
        } else if (numRadians < QUADRANT_SIZE*2) {
            return GeneralDirection.NW;
        } else if (numRadians < QUADRANT_SIZE*3) {
            return GeneralDirection.SW;
        } else {
            return GeneralDirection.SE;
        }
    }

    static void escapeFromEnemy(RobotInfo[] robotInfo) {

        try {
            if (robotInfo.length > 0) {
                tryMove(rc.getLocation().directionTo(robotInfo[0].getLocation()).opposite(), 5, 36);
            }
        } catch (Exception e) {
            System.out.println("Escape from enemy exception.");
        }
    }

    static void getBuiltLeaderId(Direction dir) {

        /*
        // Get new robot location based on direction
        MapLocation newRobotLoc = new MapLocation(rc.getLocation().x+BUILD_LENGTH, rc.getLocation().y);

        // Sense where the robot is
        Team friendly = rc.getTeam();
        RobotInfo[] newRobotMemberInfo = rc.senseNearbyRobots(newRobotLoc, (float)0.5, friendly);
        RobotInfo info = newRobotMemberInfo[0];

        // Get its id to be the leader
        squadLeader = info.getID();
        //System.out.println("The squad leader's ID is " + squadLeader);
        System.out.println("Leader is type " + info.getType() + ", ID: " + info.getID());
        return info;

        */

        // All of the above is outdated by channel reading
        try {
            squadLeader = rc.readBroadcast(squadLeaderChannel);
            System.out.println("The squad leader's ID is " + squadLeader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void broadcastGardenerId(int leaderId) {
        try {

            // Search for an empty broadcasting channel
            int idx = 0;
            while (rc.readBroadcast(BEGINNING_SUBLEADER_CHANNEL+idx) != 0) {
                idx++;
            }
            rc.broadcast(BEGINNING_SUBLEADER_CHANNEL+idx, leaderId);

            // Wait for robot
            waitForRobotToBuild();

            // Remove broadcast
            // rc.broadcast(BEGINNING_SUBLEADER_CHANNEL+idx, 0);

            // Get new leader id
            squadLeader = rc.readBroadcast(BEGINNING_SUBLEADER_CHANNEL+idx);
            squadLeaderChannel = BEGINNING_SUBLEADER_CHANNEL+idx;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean setAsNewLeader() {
        try {

            // Get potential list of leaders
            Team friendly = rc.getTeam();
            RobotInfo[] potentialLeaderInfoList = rc.senseNearbyRobots(-1, friendly);

            // Get the leader's id
            for (RobotInfo info: potentialLeaderInfoList) {

                int idx = 0;
                while (rc.readBroadcast(BEGINNING_SUBLEADER_CHANNEL+idx) != 0) {

                    /*
                     * TODO: This could possibly bug if there was another subleader in the vicinity.
                     *       However, this is pretty unlikely, as the subleader would have to be broadcasting too.
                     */
                    if (rc.readBroadcast(BEGINNING_SUBLEADER_CHANNEL+idx) == info.getID()) {
                        //squadLeader = info.getID();
                        System.out.println("Found progenitor. ID: " + info.getID() + " Own ID: " + rc.getID());
                        rc.broadcast(BEGINNING_SUBLEADER_CHANNEL+idx, rc.getID());
                        squadLeader = rc.getID();
                        squadLeaderChannel = BEGINNING_SUBLEADER_CHANNEL+idx;
                        isSubLeader = true;
                        return true;
                    }

                    idx++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Should never get here
        System.out.println("Error, the unit did not find a gardener to assign with. Round " + rc.getRoundNum() +
                            ". x=" + rc.getLocation().x + " y=" + rc.getLocation().y);
        return false;
    }

    static void waitForRobotToBuild() {
        for (int i = 0; i < 21; i++) {
            if (rc.senseNearbyBullets().length == 0) {
                Clock.yield();
            } else {
                break;
            }
        }
    }

    static void dodgeIncomingBullets() {

        try {

            // Get bullets that will collide
            BulletInfo[] incomingBullets = rc.senseNearbyBullets();
            for (BulletInfo info : incomingBullets) {
                if (willCollideWithMe(info)) {

                    // Find movement that won't collide
                    float strideRadius = rc.getType().strideRadius;
                    float x = rc.getLocation().x;
                    float y = rc.getLocation().y;

                    if (!willCollideWithMe(info, x, y+strideRadius) && rc.canMove(Direction.getNorth())) {
                        rc.move(Direction.getNorth());
                        //System.out.println("Dodged bullet!");
                        return;
                    } else if (!willCollideWithMe(info, x+strideRadius, y) && rc.canMove(Direction.getEast())) {
                        rc.move(Direction.getEast());
                        //System.out.println("Dodged bullet!");
                        return;
                    } else if (!willCollideWithMe(info, x, y-strideRadius) && rc.canMove(Direction.getSouth())) {
                        rc.move(Direction.getSouth());
                        //System.out.println("Dodged bullet!");
                        return;
                    } else if (!willCollideWithMe(info, x-strideRadius, y) && rc.canMove(Direction.getWest())) {
                        rc.move(Direction.getWest());
                        //System.out.println("Dodged bullet!");
                        return;
                    } else {
                        //System.out.println("Failed to dodge bullet!");
                        Direction dir = randomDirection();
                        while (!rc.canMove(dir)) {
                            dir = randomDirection();
                        }

                        rc.move(dir);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Dodge failed!");
            e.printStackTrace();
        }
    }

    static boolean followSquadLeader() {

        try {

            Team friendly = rc.getTeam();
            RobotInfo[] potentialLeaderInfoList = rc.senseNearbyRobots(-1, friendly);

            // Get the leader's id
            for (RobotInfo info: potentialLeaderInfoList) {
                if (info.getID() == squadLeader) {
                    tryMove(rc.getLocation().directionTo(info.getLocation()), 5, 36, rc.getLocation().distanceTo(info.getLocation()));
                    return true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // No leader found
        //System.out.println("No leader to follow!");
        return false;
    }
}
