package mekhq.campaign.personnel;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mekhq.campaign.Campaign;

/**
 * This class tracks fatigue for individual personnel,
 * as per the rules in Strategic Operations, page [x]
 * @author NickAragua
 *
 */
public class FatigueTracker {
	private Map<UUID, Integer> currentFatigueLevels;
	private Map<UUID, Date> lastCombats;
	
	public FatigueTracker() {
		currentFatigueLevels = new HashMap<>();
		lastCombats = new HashMap<>();
	}
	
	/**
	 * When a person participates in a battle, their fatigue goes up by 1?
	 * and their last battle date is set to the given date.
	 * @param personID
	 * @param date
	 */
	public void processBattleForPerson(UUID personID, Date date) {
		lastCombats.put(personID, date);
		
		if(currentFatigueLevels.containsKey(personID)) {
			currentFatigueLevels.put(personID, currentFatigueLevels.get(personID) + 1);
		} else {
			currentFatigueLevels.put(personID, 1);
		}
	}
	
	public void newDay(Campaign c) {
		// loop through all the people with a fatigue entry
		// if there's a last combat entry and it's two or more cycles in the past
		// or if there's not a last combat entry
		// degrade fatigue by 1, and an extra 1 if there's a field kitchen
		// or maybe if we're completely off-contract and on a planet?
		for(UUID personID : currentFatigueLevels.keySet()) {
			if(!lastCombats.containsKey(personID) ||
					ChronoUnit.DAYS.between(lastCombats.get(personID).toInstant(), 
					c.getCalendar().getTime().toInstant()) > 14) {
				int fatigueDecrement = 1;
				
				//if campaign has field kitchen or we're off-contract and on a planet
				// calculate this only once, obviously
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
		lastCombats.remove(personID);
		currentFatigueLevels.remove(personID);
	}
	
	public Injury getFatigueEffects(UUID personID) {
		Injury injury = new Injury();
		
		
		return injury;
	}
}
