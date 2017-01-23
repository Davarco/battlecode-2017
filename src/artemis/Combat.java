package artemis;

import battlecode.common.*;
import static artemis.Channels.*;
import static artemis.RobotPlayer.*;

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
}
