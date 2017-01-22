package exceptional;
import battlecode.common.*;


public strictfp class RobotPlayer {

    static RobotController rc;

    // Keep broadcasting channels
    final static int GARDENER_CHANNEL = 0;
    final static int SOLDIER_CHANNEL = 1;
    final static int TANK_CHANNEL = 2;
    final static int SCOUT_CHANNEL = 3;
    final static int LUMBERJACK_CHANNEL = 4;
    final static int ARCHON_CHANNEL = 5;
    final static int ARCHON_NUMBER_CHANNEL = 6;
    final static int ASSIST_TEAM_X = 7;
    final static int ASSIST_TEAM_Y = 8;
    final static int BUILD_CHANNEL = 100;

    // Constants
    final static float BUILD_LENGTH = (float)2.01001;
    final static float BOUNDARIES_LENGTH = (float)4.0;
    final static float QUADRANT_SIZE = (float)Math.PI/2;

    // Store temporary enemy info
    static int tempLocX = 0;
    static int tempLocY = 0;

    // Store robot squad info
    static int maxGardeners=0;
    static int numFightingRobots=0;
    static int numSquads=0;
    static int maxSquadSize=0;
    static int squadChannel=0;
    static int squadSize=0;
    static boolean isHeadLeader;
    static boolean isSquadLeader;

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

                // Go to endgame strategy if game is almost over
                if (rc.getRoundLimit() - rc.getRoundNum() < 200) {
                    implementEndgame();
                }

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
                if (squadSize < maxSquadSize || rc.getTeamBullets() > 110.0) {
                    if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {

                        // Build robot and send out ID to bind
                        rc.buildRobot(RobotType.SOLDIER, dir);
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
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    static void runSoldier() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        findRobotBind();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                robotMove();
                robotRangedAttack();

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
        findRobotBind();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                robotMove();
                robotRangedAttack();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runScout() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        findRobotBind();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                robotMove();
                robotRangedAttack();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        findRobotBind();

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

    static void implementEndgame() {

        try {

            // Get largest multiple of 10
            float total = 0;
            while (total < rc.getTeamBullets() - 20) {
                total += 10;
            }

            rc.donate(total);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void robotMove() {

        try {

            if (!rc.hasMoved()) {
                dodgeIncomingBullets();
            }

            if (!rc.hasMoved()) {

                if (isSquadLeader) {
                    squadLeaderMove();
                } else {
                    followSquadLeader();
                }
            }

            if (isSquadLeader && nearDeath()) {
                transferCommand();
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

    static void robotRangedAttack() {

        try {

            // Get all nearby robots
            RobotInfo[] robotAllies = rc.senseNearbyRobots(-1, rc.getTeam());
            RobotInfo[] robotEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

            // Find lowest health robot that isn't blocked by ally
            double lowestHealth = 1000.0;
            Direction bestDirection = null;
            for (RobotInfo enemyInfo: robotEnemies) {

                // See if the proposed bullet will hit an ally
                boolean willCollideWithAlly = false;
                for (RobotInfo allyInfo: robotAllies) {
                    Direction dirToEnemy = rc.getLocation().directionTo(enemyInfo.getLocation());
                    if (willCollideWithRobot(dirToEnemy, allyInfo.getLocation())) {
                        willCollideWithAlly = true;
                    }
                }

                // Set new lowest hp
                double enemyHp = enemyInfo.getHealth();
                if (!willCollideWithAlly && enemyHp < lowestHealth) {
                    lowestHealth = enemyHp;
                    bestDirection = rc.getLocation().directionTo(enemyInfo.getLocation());
                    //System.out.println("New best direction.");
                }
            }

            // Fire bullet if possible
            if (bestDirection != null) {
                if (rc.canFireSingleShot()) {
                    rc.fireSingleShot(bestDirection);
                    //System.out.println("Firing!");
                }
            } else {
                //System.out.println("Can't fire because of null location.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void squadLeaderMove() {

        try {

            // See if there are enemies around
            RobotInfo[] robotInfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            boolean enemyRobotsInSight = (robotInfo.length > 0);

            if (enemyRobotsInSight) {

                // Move towards first robot
                tryMove(rc.getLocation().directionTo(robotInfo[0].getLocation()), 5, 36);

                // Update for other squad members
                rc.broadcast(squadChannel+2, (int)robotInfo[0].getLocation().x);
                rc.broadcast(squadChannel+3, (int)robotInfo[0].getLocation().y);

                // See if a new target is needed
                if ((rc.readBroadcast(ASSIST_TEAM_X) == 0 && rc.readBroadcast(ASSIST_TEAM_Y) == 0) ||
                        (rc.readBroadcast(ASSIST_TEAM_X) == tempLocX && rc.readBroadcast(ASSIST_TEAM_Y) == tempLocY)) {

                    rc.broadcast(ASSIST_TEAM_X, (int)robotInfo[0].getLocation().x);
                    rc.broadcast(ASSIST_TEAM_Y, (int)robotInfo[0].getLocation().y);
                    tempLocX = (int)robotInfo[0].getLocation().x;
                    tempLocY = (int)robotInfo[0].getLocation().y;
                    System.out.println("Enemy @" + tempLocX + ", " + tempLocY);

                } else {
                    tempLocX = rc.readBroadcast(ASSIST_TEAM_X);
                    tempLocY = rc.readBroadcast(ASSIST_TEAM_Y);
                }

            } else {

                // No enemies found
                int x = rc.readBroadcast(ASSIST_TEAM_X);
                int y = rc.readBroadcast(ASSIST_TEAM_Y);
                MapLocation enemyBcLoc = new MapLocation(x, y);

                // Check if there are still robots at assist location, reset if there aren't
                if (x != 0 && y != 0 && rc.canSenseAllOfCircle(enemyBcLoc, (float)3.0)) {
                    if (rc.senseNearbyRobots(enemyBcLoc, (float)3.0, rc.getTeam().opponent()).length == 0) {
                        rc.broadcast(ASSIST_TEAM_X, 0);
                        rc.broadcast(ASSIST_TEAM_Y, 0);
                        System.out.println("Location @" + x + ", " + y + " has already been decimated.");
                        x = 0;
                        y = 0;
                    }
                }

                // Check if temp loc has stayed the same, reset if it is
                if (x != 0 && y != 0 && rc.readBroadcast(ASSIST_TEAM_X) == tempLocX && rc.readBroadcast(ASSIST_TEAM_Y) == tempLocY) {
                    rc.broadcast(ASSIST_TEAM_X, 0);
                    rc.broadcast(ASSIST_TEAM_Y, 0);
                    //System.out.println("Location @" + x + ", " + y + " has not been updated.");
                    x = 0;
                    y = 0;
                }

                if (x != 0 && y != 0) {

                    // Go to assist location
                    if (!tryMove(rc.getLocation().directionTo(enemyBcLoc), 5, 36)) {

                        // Move randomly
                        if (tryMove(randomDirection())) {

                            // Update for other squad members
                            rc.broadcast(squadChannel+2, (int)rc.getLocation().x);
                            rc.broadcast(squadChannel+3, (int)rc.getLocation().y);

                        } else {

                            // Can't move
                            rc.broadcast(squadChannel+2, 0);
                            rc.broadcast(squadChannel+3, 0);
                        }

                    } else {

                        // Update for other squad members
                        rc.broadcast(squadChannel+2, (int)rc.getLocation().x);
                        rc.broadcast(squadChannel+3, (int)rc.getLocation().y);
                    }

                } else {

                    // No assist location, move randomly
                    if (tryMove(randomDirection())) {

                        // Update for other squad members
                        rc.broadcast(squadChannel+2, (int)rc.getLocation().x);
                        rc.broadcast(squadChannel+3, (int)rc.getLocation().y);

                    } else {

                        // Can't move
                        rc.broadcast(squadChannel+2, 0);
                        rc.broadcast(squadChannel+3, 0);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void followSquadLeader() {

        try {

            // Get x and y from squad channel
            int x = rc.readBroadcast(squadChannel + 2);
            int y = rc.readBroadcast(squadChannel + 3);

            if (squadChannel != 0 ) {

                if (x != 0 && y != 0) {

                    // Move towards leader designated position
                    MapLocation loc = new MapLocation(x, y);
                    tryMove(rc.getLocation().directionTo(loc), 5, 36);

                } else if (x == 0 && y == 0) {

                    // Move away from leader designated position
                    MapLocation loc = new MapLocation(x, y);
                    tryMove(rc.getLocation().directionTo(loc).opposite(), 5, 36);
                }

                // Add as leader if no leader found
                if (rc.readBroadcast(squadChannel+1) == 0) {
                    rc.broadcast(squadChannel+1, rc.getID());
                    isSquadLeader = true;
                }

            } else {
                tryMove(randomDirection());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void transferCommand() {

        try {

            // Remove as squad leader
            isSquadLeader = false;
            rc.broadcast(squadChannel+1, 0);
            //System.out.println("Transferring command. ID: " + rc.getID() + " Origin: " + squadChannel + ", " + rc.readBroadcast(squadChannel) +
            //        " (" + rc.getLocation().x + ", " + rc.getLocation().y + ")");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
            System.out.println("Number of robots in squad " + rc.getID() + ": " + squadSize);

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

            //System.out.println("Channel to bind to not found.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void escapeFromEnemy(RobotInfo[] robotInfo) {

        // Move in opposite direction
        try {
            tryMove(rc.getLocation().directionTo(robotInfo[0].getLocation()).opposite(), 5, 36);
        } catch (Exception e) {
            System.out.println("Escape from enemy exception.");
        }
    }

    static void dodgeIncomingBullets() {

        try {

            // Get bullets that will collide
            BulletInfo[] incomingBullets = rc.senseNearbyBullets();

            for (BulletInfo info: incomingBullets) {

                if (willCollideWithMe(info)) {

                    // Find movement that won't collide
                    float strideRadius = rc.getType().strideRadius;
                    float x = rc.getLocation().x;
                    float y = rc.getLocation().y;

                    if (!willCollideWithMe(info, x, y+strideRadius) && rc.canMove(Direction.getNorth())) {
                        rc.move(Direction.getNorth());
                        return;
                    } else if (!willCollideWithMe(info, x+strideRadius, y) && rc.canMove(Direction.getEast())) {
                        rc.move(Direction.getEast());
                        return;
                    } else if (!willCollideWithMe(info, x, y-strideRadius) && rc.canMove(Direction.getSouth())) {
                        rc.move(Direction.getSouth());
                        return;
                    } else if (!willCollideWithMe(info, x-strideRadius, y) && rc.canMove(Direction.getWest())) {
                        rc.move(Direction.getWest());
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

    // Extra collision function that sees if ally will be hit
    static boolean willCollideWithRobot(Direction dir, MapLocation loc) {
        MapLocation myLocation = loc;

        // Get relevant bullet information
        Direction propagationDirection = dir;
        MapLocation bulletLocation = rc.getLocation();

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
}
