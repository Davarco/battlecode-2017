package lightsaber;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import static lightsaber.RobotPlayer.rc;

public class Combat {

    static void defaultRangedAttack(RobotInfo[] robotInfo) {

        try {

            // Attack closest enemy and determine spread by distance
            MapLocation prefEnemyLoc = robotInfo[0].getLocation();
            Direction prefAttackDir = rc.getLocation().directionTo(prefEnemyLoc);
            float prefAttackDist = rc.getLocation().distanceTo(prefEnemyLoc);
            if (prefAttackDist < 1.0 && rc.canFirePentadShot()) {
                rc.firePentadShot(prefAttackDir);
            } else if (prefAttackDist < 5.0 && rc.canFireTriadShot()) {
                rc.fireTriadShot(prefAttackDir);
            } else if (rc.canFireSingleShot()) {
                rc.fireSingleShot(prefAttackDir);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void defaultMeleeAttack() {

        try {

            // Strike if there are enemies within radius
            RobotInfo[] enemyInfoWithinRadius = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, rc.getTeam().opponent());
            RobotInfo[] friendlyInfoWithinRadius = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, rc.getTeam());
            if (rc.canStrike() && enemyInfoWithinRadius.length > friendlyInfoWithinRadius.length) {
                rc.strike();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
