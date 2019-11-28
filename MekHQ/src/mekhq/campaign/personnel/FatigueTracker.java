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
 * This class tracks fatigue for individual personnel,
 * as per the rules in Strategic Operations, page 41
 * @author NickAragua
 *
 */
public class FatigueTracker {
    public static final int FATIGUE_CYCLE_DAYS = 7;
    
    public static final TreeMap<Integer, FatigueEffect> fatigueModifierChart;
    
	private Map<UUID, Integer> currentFatigueLevels;
	private Map<UUID, LocalDate> recoveryStartDates;
	
	static { 
	    fatigueModifierChart = new TreeMap<>();
	    fatigueModifierChart.put(0, new FatigueEffect(0, 1, false));
	    fatigueModifierChart.put(1, new FatigueEffect(0, 0, false));
	    fatigueModifierChart.put(5, new FatigueEffect(-1, 0, true));
	    fatigueModifierChart.put(9, new FatigueEffect(-2, -1, true));
	    fatigueModifierChart.put(13, new FatigueEffect(-3, -2, true));
	    fatigueModifierChart.put(17, new FatigueEffect(-4, -3, true));
	}
	
	public FatigueTracker() {
		currentFatigueLevels = new HashMap<>();
		recoveryStartDates = new HashMap<>();
	}
	
	/**
	 * When a person participates in a battle, their fatigue goes up by 1?
	 * and their last battle date is set to the given date.
	 * @param personID
	 * @param date
	 */
	public void processBattleForPerson(Campaign c, UUID personID) {
	    LocalDate recoveryStartDate = 
	            LocalDate.of(c.getCalendar().get(Calendar.YEAR),
	                    c.getCalendar().get(Calendar.MONTH), 
	                    c.getCalendar().get(Calendar.DAY_OF_MONTH)).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
	    recoveryStartDates.put(personID, recoveryStartDate);
		
		if(currentFatigueLevels.containsKey(personID)) {
			currentFatigueLevels.put(personID, currentFatigueLevels.get(personID) + 1);
		} else {
			currentFatigueLevels.put(personID, 1);
		}
	}
	
	public void newDay(Campaign c) {
	    // skip all this stuff if we're at the beginning 
	    if(c.getCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
	        return;
	    }
	    
	    LocalDate currentDate = LocalDate.of(c.getCalendar().get(Calendar.YEAR),
	            c.getCalendar().get(Calendar.MONTH), c.getCalendar().get(Calendar.DAY_OF_MONTH));
	    
		// loop through all the people with a fatigue entry
		// if there's a last combat entry and it's two or more cycles in the past
		// or if there's not a last combat entry
		// degrade fatigue by 1, and an extra 1 if there's a field kitchen
		// or maybe if we're completely off-contract and on a planet?
		for(UUID personID : currentFatigueLevels.keySet()) {
		    int fatigueDecrement = 0;
		    
		    // we won't bother updating fatigue info for non-fatigued personnel
		    if(!currentFatigueLevels.containsKey(personID) || 
		            (currentFatigueLevels.get(personID) <= 0)) {
		        continue;
		    }
		    
		    if(recoveryStartDates.containsKey(personID)) {
		        long dateDiff = ChronoUnit.DAYS.between(recoveryStartDates.get(personID), currentDate);
		        
		        if(dateDiff > 7) {
		            fatigueDecrement++;
		        }
		        
		        // if the date diff - 7 is evenly divisible by 14 (every two weeks after the first week)
		        if((dateDiff - 7) % 14 == 0) {
		            fatigueDecrement++;				
		        } 
			}
		    
		    //if campaign has field kitchen or we're off-contract and on a planet
		    boolean campaignHasSufficientFieldKitchens = false;
		    if(campaignHasSufficientFieldKitchens) {
		        fatigueDecrement++;
		    }
		    
		    // if there's no fatigue update to make after all this, don't bother doing it
		    if(fatigueDecrement > 0) {
                currentFatigueLevels.put(personID, Math.max(currentFatigueLevels.get(personID) - fatigueDecrement, 0));
		    }
		}
	}
	
	/**
	 * Remove person from tracker, e.g. when they go inactive for whatever reason or
	 * are removed from the campaign.
	 * @param personID Person to remove
	 */
	public void removePerson(UUID personID) {
	    recoveryStartDates.remove(personID);
		currentFatigueLevels.remove(personID);
	}
	
	/**
	 * Get the fatigue effects for a person. If the person has no recorded fatigue level
	 * we assume they are not fatigued.
	 */
	public FatigueEffect getFatigueEffectForPerson(UUID personID) {
	    int fatigueLevel = getFatigueLevelForPerson(personID);
	    return fatigueModifierChart.floorEntry(fatigueLevel).getValue();
	}
	
	/**
	 * Get the fatigue level for a person. If the person has no recorded fatigue level
     * we assume they have 0-level fatigue.
	 */
	public int getFatigueLevelForPerson(UUID personID) {
	    return currentFatigueLevels.containsKey(personID) ? currentFatigueLevels.get(personID) : 0;
	}
	
	public static class FatigueEffect {
	    public int combatPenalty;
	    public int nonCombatPenalty;
	    public boolean triggersMoraleCheck;
	    
	    public FatigueEffect(int combatPenalty, int nonCombatPenalty, boolean triggersMoraleCheck) {
	        this.combatPenalty = combatPenalty;
	        this.nonCombatPenalty = nonCombatPenalty;
	        this.triggersMoraleCheck = triggersMoraleCheck;
	    }
	}
}
