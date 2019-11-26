package mekhq.campaign.personnel;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.joda.time.Days;

import mekhq.campaign.Campaign;
import mekhq.campaign.mod.am.InjuryTypes;
import mekhq.campaign.mod.am.InjuryTypes.Fatigue;

/**
 * This class tracks fatigue for individual personnel,
 * as per the rules in Strategic Operations, page [x]
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
	public void processBattleForPerson(Campaign campaign, UUID personID, Date date) {
	    LocalDate recoveryStartDate = 
	            LocalDate.of(date.getYear(), date.getMonth(), date.getDate()).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
	    recoveryStartDates.put(personID, recoveryStartDate);
		
		if(currentFatigueLevels.containsKey(personID)) {
			currentFatigueLevels.put(personID, currentFatigueLevels.get(personID) + 1);
		} else {
			currentFatigueLevels.put(personID, 1);
		}
		
		updateFatigueEffectsForPerson(campaign, personID);
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
		    boolean campaignHasFieldKitchen = false;
		    if(campaignHasFieldKitchen) {
		        fatigueDecrement++;
		    }
		    
            currentFatigueLevels.put(personID, Math.max(currentFatigueLevels.get(personID) - fatigueDecrement, 0));
            
            updateFatigueEffectsForPerson(c, personID);
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
	 * Clear any existing fatigue 
	 * @param campaign
	 * @param personID
	 */
	public void updateFatigueEffectsForPerson(Campaign campaign, UUID personID) {
	    int fatigueLevel = currentFatigueLevels.get(personID);
				
		Person person = campaign.getPerson(personID);
        Injury existingFatigue = person.getInjuryByLocationAndType(BodyLocation.GENERIC, InjuryTypes.FATIGUE);
        if(existingFatigue == null) {
            existingFatigue = InjuryTypes.FATIGUE.newInjury(campaign, campaign.getPerson(personID), BodyLocation.GENERIC, fatigueLevel);
        } else {
            person.removeInjury(existingFatigue, false);
        } 
        
        person.addInjury(existingFatigue);
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
