/*
 * Copyright (c) 2019 The Megamek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class HideAndSeekBuiltInScenario extends AtBScenario {
    private static final long serialVersionUID = 2914975739133286379L;

    @Override
    public int getScenarioType() {
        return HIDEANDSEEK;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Hide and Seek";
    }

    @Override
    public String getResourceKey() {
        return "hideAndSeek";
    }

    @Override
    public void setTerrain() {
        do {
            setTerrainType(terrainChart[Compute.d6(2) - 2]);
        } while ((getTerrainType() == TER_WETLANDS || getTerrainType() == TER_COASTAL
                || getTerrainType() == TER_FLATLANDS));
    }

    @Override
    public int getMapX() {
        return getBaseMapX() - 10;
    }

    @Override
    public int getMapY() {
        return getBaseMapY() - 10;
    }

    @Override
    public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
            ArrayList<Entity> enemyEntities) {
        int enemyStart;
        int playerHome;

        if (isAttacker()) {
            playerHome = startPos[Compute.randomInt(4)];
            setStart(playerHome);

            enemyStart = Board.START_CENTER;
            setEnemyHome(playerHome + 4);

            if (getEnemyHome() > 8) {
                setEnemyHome(getEnemyHome() - 8);
            }
        } else {
            setStart(Board.START_CENTER);
            enemyStart = startPos[Compute.randomInt(4)];
            setEnemyHome(enemyStart);
            playerHome = getEnemyHome() + 4;

            if (playerHome > 8) {
                playerHome -= 8;
            }
        }

        if (allyEntities.size() > 0) {
            addBotForce(getAllyBotForce(getContract(campaign), getStart(), playerHome, allyEntities));
        }

        if (isAttacker()) {
            addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), EntityWeightClass.WEIGHT_ASSAULT,
                    2, 0, campaign);
        } else {
            addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), EntityWeightClass.WEIGHT_HEAVY, 0,
                    0, campaign);
        }

        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities));
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 33);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                50, false);
        ScenarioObjective keepAttachedUnitsAlive = CommonObjectiveFactory.getKeepAttachedGroundUnitsAlive(contract,
                this);

        // attacker must destroy 50%
        if (!isAttacker()) {
            destroyHostiles.setPercentage(50);
            keepFriendliesAlive.setPercentage(33);
        }

        if (keepAttachedUnitsAlive != null) {
            getObjectives().add(keepAttachedUnitsAlive);
        }

        getObjectives().add(destroyHostiles);
        getObjectives().add(keepFriendliesAlive);
    }
}
