/*
 * Copyright (C) 2019 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

package mekhq.campaign.personnel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import mekhq.campaign.Campaign;

/**
 * This class tracks morale for individual personnel,
 * as per the rules in Strategic Operations, page 38
 * @author NickAragua
 *
 */
public class MoraleTracker {
    public static final int MORALE_CYCLE_DAYS = 7;
    public static final int MORALE_NORMAL = 4;    
    
    public static final HashMap<Integer, MoraleEffect> moraleEffectsChart;
    
    private Map<UUID, Integer> currentMoraleLevels;
    
    static { 
        moraleEffectsChart = new HashMap<>();
        moraleEffectsChart.put(1, new MoraleEffect(1, 2, 0, 0));
        moraleEffectsChart.put(2, new MoraleEffect(1, 1, 0, 0));
        moraleEffectsChart.put(3, new MoraleEffect(0, 1, 0, 0));
        moraleEffectsChart.put(MORALE_NORMAL, new MoraleEffect(0, 0, 2, 0));
        moraleEffectsChart.put(5, new MoraleEffect(0, -1, 5, 4));
        moraleEffectsChart.put(6, new MoraleEffect(-1, -1, 5, 4));
        moraleEffectsChart.put(7, new MoraleEffect(-2, -2, 8, 7));
    }
    
    public MoraleTracker() {
        currentMoraleLevels = new HashMap<>();
    }
    
    /**
     * When a person participates in a battle, their morale may go up or down
     */
    public void processBattleForPerson(Campaign c, UUID personID, boolean victory) {
        // need the following info:
        // scenario won/lost:
        //      won: 1d6, if lower than person morale, morale-- (better)
        //      lost: 1d6, if higher than person morale, morale++ (worse)
        // list of deployed forces (?)
        //      examine force hierarchy, looping from highest level force to lowest
        //      highest level commander killed: morale++
        //      any below-highest level cmdr killed: 1d6, if higher than person morale, morale++ (worse)
        //      morale++ per 25% force unit losses
    }
    
    public void newDay(Campaign c) {
        // skip all this stuff if we're at the beginning 
        if(c.getCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            return;
        }
        
        // loop through all persons
        for(UUID personID : currentMoraleLevels.keySet()) {
            Person p;
            
            // fatigue morale adjustment if necessary
            // "4 cycles" morale adjustment (look in fatigue tracker for recovery start date)
            //      note that if morale is at normal, this is unnecessary
            //      null recovery start date qualifies as 4 cycles
            
            // now, 2d6 morale desertion check (modifiers based on the following)
            //      person's primary role skill level (or campaign overall skill level?)
            //      campaign's faction (clan/merc/house)
            //      person's assigned unit type (-1 if dispossessed)
            //      person's loyalty (how to determine overall equipment class? or use campaign eq class)
            //      desertion and/or mutiny in the last X cycles
            //      military police (?)
            // if 2d6 + mods < current morale level, possible desertion. Roll 2d6 vs desertion target #
            //      failure by 1 or 2 is just the guy leaves
            //      failure by 3+, the guy tries to take assigned unit as well
            //      keep track of units affected by desertion: 
            //          total crew - failure 1/2 deserted crew = loyal crew
            //          failure 3+ deserted crew = take equipment crew
            //          loyal crew < take equipment crew, unit is taken as well
            
            // now, 2d6 morale mutiny check (same modifiers)
            // if fail, possible mutiny check, same rules
            // if # mutinous units > 25% total force, generate a breakthrough scenario
            
            
        }
    }
    
    /**
     * Remove person from tracker, e.g. when they go inactive for whatever reason or
     * are removed from the campaign.
     * @param personID Person to remove
     */
    public void removePerson(UUID personID) {
        currentMoraleLevels.remove(personID);
    }
    
    /**
     * Get the fatigue effects for a person. If the person has no recorded fatigue level
     * we assume they are not fatigued.
     */
    public MoraleEffect getMoraleEffectForPerson(UUID personID) {
        int moraleLevel = getMoraleLevelForPerson(personID);
        return moraleEffectsChart.get(moraleLevel);
    }
    
    /**
     * Get the fatigue level for a person. If the person has no recorded fatigue level
     * we assume they have 0-level fatigue.
     */
    public int getMoraleLevelForPerson(UUID personID) {
        return currentMoraleLevels.containsKey(personID) ? currentMoraleLevels.get(personID) : MORALE_NORMAL;
    }
    
    public static class MoraleEffect {
        public int combatPenalty;
        public int nonCombatPenalty;
        public int desertionTarget;
        public int mutinyTarget;
        
        public MoraleEffect(int combatPenalty, int nonCombatPenalty, int desertionTarget, int mutinyTarget) {
            this.combatPenalty = combatPenalty;
            this.nonCombatPenalty = nonCombatPenalty;
            this.desertionTarget = desertionTarget;
            this.mutinyTarget = mutinyTarget;
        }
    }
}
