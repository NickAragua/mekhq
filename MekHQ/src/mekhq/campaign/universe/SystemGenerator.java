package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.joda.time.DateTime;

import megamek.common.Compute;
import megamek.common.EquipmentType;
import megamek.common.PlanetaryConditions;
import mekhq.campaign.universe.Faction.Tag;
import mekhq.campaign.universe.Planet.PlanetaryEvent;
import mekhq.campaign.universe.Planet.SocioIndustrialData;

public class SystemGenerator {
    private int ATMO_GAS_GIANT = -1;
    private int SPECIAL_STAR_LEAGUE_OUTPOST_ABANDONED = 9;
    private int SPECIAL_STAR_LEAGUE_OUTPOST_OCCUPIED = 10;
    private int SPECIAL_COLONY = 11;
    private int SPECIAL_LOST_COLONY = 12;
    
    private int LOST_COLONY_DICE_KEY = -2;
    private int OUTPOST_DICE_KEY = -1;
    
    private String ATMO_VALUE_TOXIC = "Toxic";
    private String ATMO_VALUE_TAINTED = "Tainted";
    
    private int VERY_HIGH_TEMP = 317;
    
    private List<Double> baseAUs = new ArrayList<>();
    private Map<PlanetType, Integer> baseDiameters;
    private Map<PlanetType, Integer> diameterIncrement;
    private Map<PlanetType, Integer> numDiameterDice;
    private Map<Integer, Map<Double, Integer>> starHabMods;
    
    private Map<Integer, Double> atmoTemperatureMultipliers;
    private TreeMap<Integer, Integer> waterPercentages;
    
    private TreeMap<Integer, String> unbreathableAtmoPrimaryGases;
    private TreeMap<Integer, String> unbreathableAtmoSecondaryGases;
    private TreeMap<Integer, String> unbreathableAtmoTraceGases;
    private TreeMap<Integer, String> unbreathableAtmoSpecialTraceGases;
    
    private TreeMap<Integer, String> habitableAtmoCompositions;
    private TreeMap<Integer, Integer> habitableTemps;
    private TreeMap<Integer, LifeForm> lifeForms;
    
    private TreeMap<Integer, String> specials;
    
    // first key is the distance from terra in light years
    // second key is dice roll result on 1d6
    private TreeMap<Integer, TreeMap<Integer, Integer>> starLeaguePopulationMultipliers;
    private TreeMap<Integer, TreeMap<Integer, Integer>> starLeaguePopulationDice;
    private TreeMap<Integer, TreeMap<Integer, Integer>> postStarLeaguePopulationMultipliers;
    private TreeMap<Integer, TreeMap<Integer, Integer>> postStarLeaguePopulationDice;
    
    private int recentColonyThreshold = 3;
    
    // nice and complex data structure.
    // first key is the 1d6 roll on CamOps moons table
    // second key is the planet type for which we're generating moons
    // third key is the moon type
    private TreeMap<Integer, Map<PlanetType, Map<PlanetType, Integer>>> moonDice;
    private TreeMap<Integer, Map<PlanetType, Map<PlanetType, Integer>>> moonDiceModifiers;
    private TreeMap<Integer, Map<PlanetType, Integer>> ringOdds;
    
    private TreeMap<Integer, Integer> atmoTypes;
    
    private TreeMap<Integer, PlanetType> innerSystemPlanetTypes;
    private TreeMap<Integer, PlanetType> outerSystemPlanetTypes;
    
    private Planet currentStar;
    private List<Planet> stellarOrbitalSlots;
    private Map<Integer, List<Planet>> satellites; 
    private List<Planet> majorRockyStellarBodies;
    private double outerLifeZone;
    private double innerLifeZone;
    private double distanceToTerra;
    
    private DateTime dateTime;
    private Random rng;
    
    
    public SystemGenerator() {
        dateTime = new DateTime(3060, 1, 1, 1, 1);
        rng = new Random(DateTime.now().getMillis());
        
        // camops, page 102, orbital placement table
        baseAUs.add(.4);
        baseAUs.add(.7);
        baseAUs.add(1.0);
        baseAUs.add(1.6);
        baseAUs.add(2.8);
        baseAUs.add(5.2);
        baseAUs.add(10.0);
        baseAUs.add(19.6);
        baseAUs.add(38.8);
        baseAUs.add(77.2);
        baseAUs.add(154.0);
        baseAUs.add(307.6);
        baseAUs.add(614.8);
        baseAUs.add(1229.2);
        baseAUs.add(2458.0);
        
        innerSystemPlanetTypes = new TreeMap<>();
        innerSystemPlanetTypes.put(2, PlanetType.Empty);
        innerSystemPlanetTypes.put(4, PlanetType.AsteroidBelt);
        innerSystemPlanetTypes.put(5, PlanetType.DwarfTerrestrial);
        innerSystemPlanetTypes.put(6, PlanetType.Terrestrial);
        innerSystemPlanetTypes.put(8, PlanetType.GiantTerrestrial);
        innerSystemPlanetTypes.put(9, PlanetType.GasGiant);
        innerSystemPlanetTypes.put(11, PlanetType.IceGiant);
        
        outerSystemPlanetTypes = new TreeMap<>();
        outerSystemPlanetTypes.put(2, PlanetType.Empty);
        outerSystemPlanetTypes.put(4, PlanetType.AsteroidBelt);
        outerSystemPlanetTypes.put(5, PlanetType.DwarfTerrestrial);
        outerSystemPlanetTypes.put(6, PlanetType.GasGiant);
        outerSystemPlanetTypes.put(8, PlanetType.Terrestrial);
        outerSystemPlanetTypes.put(10, PlanetType.GiantTerrestrial);
        outerSystemPlanetTypes.put(11, PlanetType.IceGiant);
        
        baseDiameters = new HashMap<>();
        baseDiameters.put(PlanetType.SmallAsteroid, 0);
        baseDiameters.put(PlanetType.MediumAsteroid, 0);
        baseDiameters.put(PlanetType.DwarfTerrestrial, 400);
        baseDiameters.put(PlanetType.Terrestrial, 2500);
        baseDiameters.put(PlanetType.GiantTerrestrial, 12500);
        baseDiameters.put(PlanetType.GasGiant, 50000);
        baseDiameters.put(PlanetType.IceGiant, 25000);
        
        diameterIncrement = new HashMap<>();
        diameterIncrement.put(PlanetType.SmallAsteroid, 10);
        diameterIncrement.put(PlanetType.MediumAsteroid, 100);
        diameterIncrement.put(PlanetType.DwarfTerrestrial, 100);
        diameterIncrement.put(PlanetType.Terrestrial, 1000);
        diameterIncrement.put(PlanetType.GiantTerrestrial, 1000);
        diameterIncrement.put(PlanetType.GasGiant, 10000);
        diameterIncrement.put(PlanetType.IceGiant, 5000);
        
        numDiameterDice = new HashMap<>();
        numDiameterDice.put(PlanetType.SmallAsteroid, 2);
        numDiameterDice.put(PlanetType.MediumAsteroid, 1);
        numDiameterDice.put(PlanetType.DwarfTerrestrial, 3);
        numDiameterDice.put(PlanetType.Terrestrial, 2);
        numDiameterDice.put(PlanetType.GiantTerrestrial, 1);
        numDiameterDice.put(PlanetType.GasGiant, 2);
        numDiameterDice.put(PlanetType.IceGiant, 1);
        
        atmoTypes = new TreeMap<>();
        atmoTypes.put(Integer.MIN_VALUE, PlanetaryConditions.ATMO_VACUUM);
        atmoTypes.put(4, PlanetaryConditions.ATMO_TRACE);
        atmoTypes.put(5, PlanetaryConditions.ATMO_THIN);
        atmoTypes.put(7, PlanetaryConditions.ATMO_STANDARD);
        atmoTypes.put(9, PlanetaryConditions.ATMO_HIGH);
        atmoTypes.put(11, PlanetaryConditions.ATMO_VHIGH);
        
        moonDice = new TreeMap<>();
        moonDice.put(1, new HashMap<>());
        moonDice.get(1).put(PlanetType.DwarfTerrestrial, new HashMap<>());
        moonDice.get(1).get(PlanetType.DwarfTerrestrial).put(PlanetType.SmallAsteroid, 1);
        moonDice.get(1).get(PlanetType.DwarfTerrestrial).put(PlanetType.MediumAsteroid, 1);
        moonDice.get(1).put(PlanetType.Terrestrial, new HashMap<>());
        moonDice.get(1).get(PlanetType.Terrestrial).put(PlanetType.DwarfTerrestrial, 1);
        moonDice.get(1).put(PlanetType.GiantTerrestrial, new HashMap<>());
        moonDice.get(1).get(PlanetType.GiantTerrestrial).put(PlanetType.SmallAsteroid, 1);
        moonDice.get(1).get(PlanetType.GiantTerrestrial).put(PlanetType.Terrestrial, 1);
        moonDice.get(1).put(PlanetType.GasGiant, new HashMap<>());
        moonDice.get(1).get(PlanetType.GasGiant).put(PlanetType.MediumAsteroid, 1);
        moonDice.get(1).get(PlanetType.GasGiant).put(PlanetType.DwarfTerrestrial, 1);
        moonDice.get(1).get(PlanetType.GasGiant).put(PlanetType.Terrestrial, 1);
        moonDice.get(1).put(PlanetType.IceGiant, new HashMap<>());
        moonDice.get(1).get(PlanetType.IceGiant).put(PlanetType.SmallAsteroid, 2);
        moonDice.get(1).get(PlanetType.IceGiant).put(PlanetType.DwarfTerrestrial, 1);
        moonDice.get(1).get(PlanetType.IceGiant).put(PlanetType.Terrestrial, 1);
        moonDice.put(3, new HashMap<>());
        moonDice.get(3).put(PlanetType.DwarfTerrestrial, new HashMap<>());
        moonDice.get(3).get(PlanetType.DwarfTerrestrial).put(PlanetType.SmallAsteroid, 1);
        moonDice.get(3).put(PlanetType.Terrestrial, new HashMap<>());
        moonDice.get(3).get(PlanetType.Terrestrial).put(PlanetType.SmallAsteroid, 1);
        moonDice.get(3).get(PlanetType.Terrestrial).put(PlanetType.MediumAsteroid, 1);
        moonDice.get(3).put(PlanetType.GiantTerrestrial, new HashMap<>());
        moonDice.get(3).get(PlanetType.GiantTerrestrial).put(PlanetType.SmallAsteroid, 1);
        moonDice.get(3).get(PlanetType.GiantTerrestrial).put(PlanetType.MediumAsteroid, 1);
        moonDice.get(3).get(PlanetType.GiantTerrestrial).put(PlanetType.DwarfTerrestrial, 1);
        moonDice.get(3).put(PlanetType.GasGiant, new HashMap<>());
        moonDice.get(3).get(PlanetType.GasGiant).put(PlanetType.SmallAsteroid, 5);
        moonDice.get(3).get(PlanetType.GasGiant).put(PlanetType.MediumAsteroid, 1);
        moonDice.get(3).get(PlanetType.GasGiant).put(PlanetType.DwarfTerrestrial, 1);
        moonDice.get(3).put(PlanetType.IceGiant, new HashMap<>());
        moonDice.get(3).get(PlanetType.IceGiant).put(PlanetType.SmallAsteroid, 2);
        moonDice.get(3).get(PlanetType.IceGiant).put(PlanetType.MediumAsteroid, 1);
        moonDice.get(3).get(PlanetType.IceGiant).put(PlanetType.DwarfTerrestrial, 1);
        moonDice.put(5, new HashMap<>());
        moonDice.get(5).put(PlanetType.DwarfTerrestrial, new HashMap<>());
        moonDice.get(5).put(PlanetType.Terrestrial, new HashMap<>());
        moonDice.get(5).get(PlanetType.Terrestrial).put(PlanetType.SmallAsteroid, 2);
        moonDice.get(5).put(PlanetType.GiantTerrestrial, new HashMap<>());
        moonDice.get(5).get(PlanetType.GiantTerrestrial).put(PlanetType.SmallAsteroid, 2);
        moonDice.get(5).get(PlanetType.GiantTerrestrial).put(PlanetType.MediumAsteroid, 1);
        moonDice.get(5).put(PlanetType.GasGiant, new HashMap<>());
        moonDice.get(5).get(PlanetType.GasGiant).put(PlanetType.SmallAsteroid, 5);
        moonDice.get(5).get(PlanetType.GasGiant).put(PlanetType.MediumAsteroid, 1);
        moonDice.get(5).get(PlanetType.GasGiant).put(PlanetType.DwarfTerrestrial, 1);
        moonDice.get(5).put(PlanetType.IceGiant, new HashMap<>());
        moonDice.get(5).get(PlanetType.IceGiant).put(PlanetType.SmallAsteroid, 2);
        moonDice.get(5).get(PlanetType.IceGiant).put(PlanetType.MediumAsteroid, 1);
        moonDice.get(5).get(PlanetType.IceGiant).put(PlanetType.DwarfTerrestrial, 1);
        
        
        moonDiceModifiers = new TreeMap<>();
        moonDiceModifiers.put(1, new HashMap<>());
        moonDiceModifiers.get(1).put(PlanetType.DwarfTerrestrial, new HashMap<>());
        moonDiceModifiers.get(1).get(PlanetType.DwarfTerrestrial).put(PlanetType.SmallAsteroid, -3);
        moonDiceModifiers.get(1).get(PlanetType.DwarfTerrestrial).put(PlanetType.MediumAsteroid, -5);
        moonDiceModifiers.get(1).put(PlanetType.Terrestrial, new HashMap<>());
        moonDiceModifiers.get(1).get(PlanetType.Terrestrial).put(PlanetType.DwarfTerrestrial, -5);
        moonDiceModifiers.get(1).put(PlanetType.GiantTerrestrial, new HashMap<>());
        moonDiceModifiers.get(1).get(PlanetType.GiantTerrestrial).put(PlanetType.SmallAsteroid, -3);
        moonDiceModifiers.get(1).get(PlanetType.GiantTerrestrial).put(PlanetType.Terrestrial, -5);
        moonDiceModifiers.get(1).put(PlanetType.GasGiant, new HashMap<>());
        moonDiceModifiers.get(1).get(PlanetType.GasGiant).put(PlanetType.MediumAsteroid, -2);
        moonDiceModifiers.get(1).get(PlanetType.GasGiant).put(PlanetType.DwarfTerrestrial, -1);
        moonDiceModifiers.get(1).get(PlanetType.GasGiant).put(PlanetType.Terrestrial, -4);
        moonDiceModifiers.get(1).put(PlanetType.IceGiant, new HashMap<>());
        moonDiceModifiers.get(1).get(PlanetType.IceGiant).put(PlanetType.SmallAsteroid, 2);
        moonDiceModifiers.get(1).get(PlanetType.IceGiant).put(PlanetType.DwarfTerrestrial, -3);
        moonDiceModifiers.get(1).get(PlanetType.IceGiant).put(PlanetType.Terrestrial, -4);
        moonDiceModifiers.put(3, new HashMap<>());
        moonDiceModifiers.get(3).put(PlanetType.DwarfTerrestrial, new HashMap<>());
        moonDiceModifiers.get(3).get(PlanetType.DwarfTerrestrial).put(PlanetType.SmallAsteroid, -2);
        moonDiceModifiers.get(3).put(PlanetType.Terrestrial, new HashMap<>());
        moonDiceModifiers.get(3).get(PlanetType.Terrestrial).put(PlanetType.SmallAsteroid, -3);
        moonDiceModifiers.get(3).get(PlanetType.Terrestrial).put(PlanetType.MediumAsteroid, -3);
        moonDiceModifiers.get(3).put(PlanetType.GiantTerrestrial, new HashMap<>());
        moonDiceModifiers.get(3).get(PlanetType.GiantTerrestrial).put(PlanetType.SmallAsteroid, -2);
        moonDiceModifiers.get(3).get(PlanetType.GiantTerrestrial).put(PlanetType.MediumAsteroid, -3);
        moonDiceModifiers.get(3).get(PlanetType.GiantTerrestrial).put(PlanetType.DwarfTerrestrial, -4);
        moonDiceModifiers.get(3).put(PlanetType.GasGiant, new HashMap<>());
        moonDiceModifiers.get(3).get(PlanetType.GasGiant).put(PlanetType.MediumAsteroid, -2);
        moonDiceModifiers.get(3).get(PlanetType.GasGiant).put(PlanetType.DwarfTerrestrial, -3);
        moonDiceModifiers.get(3).put(PlanetType.IceGiant, new HashMap<>());
        moonDiceModifiers.get(3).get(PlanetType.IceGiant).put(PlanetType.MediumAsteroid, -2);
        moonDiceModifiers.get(3).get(PlanetType.IceGiant).put(PlanetType.DwarfTerrestrial, -3);
        moonDiceModifiers.put(5, new HashMap<>());
        moonDiceModifiers.get(5).put(PlanetType.DwarfTerrestrial, new HashMap<>());
        moonDiceModifiers.get(5).put(PlanetType.Terrestrial, new HashMap<>());
        moonDiceModifiers.get(5).get(PlanetType.Terrestrial).put(PlanetType.SmallAsteroid, -4);
        moonDiceModifiers.get(5).put(PlanetType.GiantTerrestrial, new HashMap<>());
        moonDiceModifiers.get(5).get(PlanetType.GiantTerrestrial).put(PlanetType.MediumAsteroid, -3);
        moonDiceModifiers.get(5).put(PlanetType.GasGiant, new HashMap<>());
        moonDiceModifiers.get(5).get(PlanetType.GasGiant).put(PlanetType.MediumAsteroid, -3);
        moonDiceModifiers.get(5).get(PlanetType.GasGiant).put(PlanetType.DwarfTerrestrial, -4);
        moonDiceModifiers.get(5).put(PlanetType.IceGiant, new HashMap<>());
        moonDiceModifiers.get(5).get(PlanetType.IceGiant).put(PlanetType.MediumAsteroid, -3);
        moonDiceModifiers.get(5).get(PlanetType.IceGiant).put(PlanetType.DwarfTerrestrial, -4);
        
        ringOdds = new TreeMap<>();
        ringOdds.put(1, new HashMap<>());
        ringOdds.get(1).put(PlanetType.GasGiant, 3);
        ringOdds.put(3, new HashMap<>());
        ringOdds.get(3).put(PlanetType.GasGiant, 4);
        ringOdds.get(3).put(PlanetType.IceGiant, 3);
        ringOdds.put(5, new HashMap<>());
        ringOdds.get(5).put(PlanetType.Terrestrial, 1);
        ringOdds.get(5).put(PlanetType.GiantTerrestrial, 2);
        ringOdds.get(5).put(PlanetType.GasGiant, 4);
        ringOdds.get(5).put(PlanetType.IceGiant, 3);
        
        atmoTemperatureMultipliers = new HashMap<>();
        atmoTemperatureMultipliers.put(PlanetaryConditions.ATMO_THIN, .95);
        atmoTemperatureMultipliers.put(PlanetaryConditions.ATMO_STANDARD, .9);
        atmoTemperatureMultipliers.put(PlanetaryConditions.ATMO_HIGH, .8);
        atmoTemperatureMultipliers.put(PlanetaryConditions.ATMO_VHIGH, .5);
        
        starHabMods = new HashMap<>();
        
        waterPercentages = new TreeMap<>();
        waterPercentages.put(Integer.MIN_VALUE, 0);
        waterPercentages.put(0, 5);
        waterPercentages.put(1, 10);
        waterPercentages.put(2, 20);
        waterPercentages.put(3, 30);
        waterPercentages.put(4, 40);
        waterPercentages.put(5, 40);
        waterPercentages.put(6, 50);
        waterPercentages.put(7, 50);
        waterPercentages.put(8, 60);
        waterPercentages.put(9, 70);
        waterPercentages.put(10, 80);
        waterPercentages.put(11, 90);
        waterPercentages.put(12, 100);
        
        unbreathableAtmoPrimaryGases = new TreeMap<>();
        unbreathableAtmoPrimaryGases.put(Integer.MIN_VALUE, "Methane");
        unbreathableAtmoPrimaryGases.put(4, "Ammonia");
        unbreathableAtmoPrimaryGases.put(6, "Nitrogen");
        unbreathableAtmoPrimaryGases.put(10, "Carbon Dioxide");
        unbreathableAtmoSecondaryGases = new TreeMap<>();
        unbreathableAtmoSecondaryGases.put(Integer.MIN_VALUE, "Methane");
        unbreathableAtmoSecondaryGases.put(3, "Ammonia");
        unbreathableAtmoSecondaryGases.put(7, "Carbon Dioxide");
        unbreathableAtmoSecondaryGases.put(10, "Nitrogen");
        unbreathableAtmoTraceGases = new TreeMap<>();
        unbreathableAtmoTraceGases.put(Integer.MIN_VALUE, "Chlorine");
        unbreathableAtmoTraceGases.put(3, "");
        unbreathableAtmoTraceGases.put(4, "Sulfur Dioxide");
        unbreathableAtmoTraceGases.put(5, "Carbon Dioxide");
        unbreathableAtmoTraceGases.put(6, "Argon");
        unbreathableAtmoTraceGases.put(7, "Methane");
        unbreathableAtmoTraceGases.put(8, "Water Vapor");
        unbreathableAtmoTraceGases.put(9, "Argon");
        unbreathableAtmoTraceGases.put(10, "Nitrous Oxide");
        unbreathableAtmoSpecialTraceGases = new TreeMap<>();
        unbreathableAtmoSpecialTraceGases.put(Integer.MIN_VALUE, "Helium");
        unbreathableAtmoSpecialTraceGases.put(3, "Complex Hydrocarbons");
        unbreathableAtmoSpecialTraceGases.put(4, "Nitric Acid");
        unbreathableAtmoSpecialTraceGases.put(5, "Phosphine");
        unbreathableAtmoSpecialTraceGases.put(6, "Hydrogen Peroxide");
        unbreathableAtmoSpecialTraceGases.put(7, "Hydrochloric Acid");
        unbreathableAtmoSpecialTraceGases.put(8, "Hydrogen Sulfide");
        unbreathableAtmoSpecialTraceGases.put(9, "Simple Hydrocarbons");
        unbreathableAtmoSpecialTraceGases.put(10, "Sulfuric Acid");
        unbreathableAtmoSpecialTraceGases.put(11, "Carbonyl Sulfide");
        unbreathableAtmoSpecialTraceGases.put(12, "Hydrofluoric Acid");
        
        habitableAtmoCompositions = new TreeMap<>();
        habitableAtmoCompositions.put(Integer.MIN_VALUE, ATMO_VALUE_TOXIC);
        habitableAtmoCompositions.put(2, ATMO_VALUE_TAINTED);
        habitableAtmoCompositions.put(7, "Breathable");
        
        habitableTemps = new TreeMap<>();
        habitableTemps.put(Integer.MIN_VALUE, VERY_HIGH_TEMP);
        habitableTemps.put(1, 307);
        habitableTemps.put(5, 297);
        habitableTemps.put(10, 287);
        
        lifeForms = new TreeMap<>();
        lifeForms.put(Integer.MIN_VALUE, LifeForm.MICROBE);
        lifeForms.put(1, LifeForm.PLANT);
        lifeForms.put(2, LifeForm.INSECT);
        lifeForms.put(3, LifeForm.FISH);
        lifeForms.put(5, LifeForm.AMPH);
        lifeForms.put(7, LifeForm.REPTILE);
        lifeForms.put(9, LifeForm.BIRD);
        lifeForms.put(11, LifeForm.MAMMAL);
        
        specials = new TreeMap<>();
        specials.put(Integer.MIN_VALUE, "");
        specials.put(3, "Recent Natural Disaster");
        specials.put(4, "Intense Volcanic Activity");
        specials.put(5, "Intense Seismic Activity");
        specials.put(6, "Disease");
        specials.put(7, "Incompatible Biochemistry");
        specials.put(8, "Hostile Life Forms");
        specials.put(9, "Abandoned Star League Facility");
        specials.put(10, "Occupied Star League Facility");
        specials.put(11, "Colony");
        specials.put(12, "Lost Colony");
        
        starLeaguePopulationMultipliers = new TreeMap<>();
        starLeaguePopulationMultipliers.put(0, new TreeMap<>());
        starLeaguePopulationMultipliers.get(0).put(1, 50000000);
        starLeaguePopulationMultipliers.get(0).put(6, 500000000);
        starLeaguePopulationMultipliers.put(500, new TreeMap<>());
        starLeaguePopulationMultipliers.get(500).put(1, 10000000);
        starLeaguePopulationMultipliers.get(500).put(6, 100000000);
        starLeaguePopulationMultipliers.put(601, new TreeMap<>());
        starLeaguePopulationMultipliers.get(601).put(1, 25000000);
        starLeaguePopulationMultipliers.get(601).put(6, 250000000);
        starLeaguePopulationMultipliers.put(751, new TreeMap<>());
        starLeaguePopulationMultipliers.get(751).put(1, 500000);
        starLeaguePopulationMultipliers.get(751).put(6, 5000000);
        starLeaguePopulationMultipliers.put(1001, new TreeMap<>());
        starLeaguePopulationMultipliers.get(1001).put(1, 100000);
        starLeaguePopulationMultipliers.get(1001).put(6, 1000000);
        starLeaguePopulationMultipliers.put(1251, new TreeMap<>());
        starLeaguePopulationMultipliers.get(1251).put(1, 10000);
        starLeaguePopulationMultipliers.get(1251).put(6, 200000);
        starLeaguePopulationMultipliers.put(2000, new TreeMap<>());
        starLeaguePopulationMultipliers.get(2000).put(1, 2500);
        starLeaguePopulationMultipliers.get(2000).put(6, 50000);
        // outpost
        starLeaguePopulationMultipliers.put(OUTPOST_DICE_KEY, new TreeMap<>());
        starLeaguePopulationMultipliers.get(OUTPOST_DICE_KEY).put(1, 2500);
        starLeaguePopulationMultipliers.get(OUTPOST_DICE_KEY).put(6, 50000);
        // lost colony
        starLeaguePopulationMultipliers.put(LOST_COLONY_DICE_KEY, new TreeMap<>());
        starLeaguePopulationMultipliers.get(LOST_COLONY_DICE_KEY).put(1, 10000);
        starLeaguePopulationMultipliers.get(LOST_COLONY_DICE_KEY).put(6, 100000);
        
        starLeaguePopulationDice = new TreeMap<>();
        starLeaguePopulationDice.put(0, new TreeMap<>());
        starLeaguePopulationDice.get(0).put(1, 4);
        starLeaguePopulationDice.get(0).put(6, 4);
        starLeaguePopulationDice.put(500, new TreeMap<>());
        starLeaguePopulationDice.get(500).put(1, 4);
        starLeaguePopulationDice.get(500).put(6, 4);
        starLeaguePopulationDice.put(601, new TreeMap<>());
        starLeaguePopulationDice.get(601).put(1, 4);
        starLeaguePopulationDice.get(601).put(6, 4);
        starLeaguePopulationDice.put(751, new TreeMap<>());
        starLeaguePopulationDice.get(751).put(1, 4);
        starLeaguePopulationDice.get(751).put(6, 4);
        starLeaguePopulationDice.put(1001, new TreeMap<>());
        starLeaguePopulationDice.get(1001).put(1, 4);
        starLeaguePopulationDice.get(1001).put(6, 4);
        starLeaguePopulationDice.put(1251, new TreeMap<>());
        starLeaguePopulationDice.get(1251).put(1, 4);
        starLeaguePopulationDice.get(1251).put(6, 4);
        starLeaguePopulationDice.put(2000, new TreeMap<>());
        starLeaguePopulationDice.get(2000).put(1, 4);
        starLeaguePopulationDice.get(2000).put(6, 4);
        // outpost
        starLeaguePopulationDice.put(OUTPOST_DICE_KEY, new TreeMap<>());
        starLeaguePopulationDice.get(OUTPOST_DICE_KEY).put(1, 4);
        starLeaguePopulationDice.get(OUTPOST_DICE_KEY).put(6, 4);
        // lost colony
        starLeaguePopulationDice.put(LOST_COLONY_DICE_KEY, new TreeMap<>());
        starLeaguePopulationDice.get(LOST_COLONY_DICE_KEY).put(1, 2);
        starLeaguePopulationDice.get(LOST_COLONY_DICE_KEY).put(6, 2);
        
        postStarLeaguePopulationMultipliers = new TreeMap<>();
        postStarLeaguePopulationMultipliers.put(0, new TreeMap<>());
        postStarLeaguePopulationMultipliers.get(0).put(1, 10000);
        postStarLeaguePopulationMultipliers.get(0).put(6, 100000);
        postStarLeaguePopulationMultipliers.put(500, new TreeMap<>());
        postStarLeaguePopulationMultipliers.get(500).put(1, 2000000);
        postStarLeaguePopulationMultipliers.get(500).put(6, 20000000);
        postStarLeaguePopulationMultipliers.put(601, new TreeMap<>());
        postStarLeaguePopulationMultipliers.get(601).put(1, 50000);
        postStarLeaguePopulationMultipliers.get(601).put(6, 1000000);
        postStarLeaguePopulationMultipliers.put(751, new TreeMap<>());
        postStarLeaguePopulationMultipliers.get(751).put(1, 20000);
        postStarLeaguePopulationMultipliers.get(751).put(6, 200000);
        postStarLeaguePopulationMultipliers.put(1001, new TreeMap<>());
        postStarLeaguePopulationMultipliers.get(1001).put(1, 5000);
        postStarLeaguePopulationMultipliers.get(1001).put(6, 50000);
        postStarLeaguePopulationMultipliers.put(1251, new TreeMap<>());
        postStarLeaguePopulationMultipliers.get(1251).put(1, 500);
        postStarLeaguePopulationMultipliers.get(1251).put(6, 10000);
        postStarLeaguePopulationMultipliers.put(2000, new TreeMap<>());
        postStarLeaguePopulationMultipliers.get(2000).put(1, 100);
        postStarLeaguePopulationMultipliers.get(2000).put(6, 2500);
        // outpost
        postStarLeaguePopulationMultipliers.put(OUTPOST_DICE_KEY, new TreeMap<>());
        postStarLeaguePopulationMultipliers.get(OUTPOST_DICE_KEY).put(1, 50);
        postStarLeaguePopulationMultipliers.get(OUTPOST_DICE_KEY).put(6, 1000);
        // lost colony
        postStarLeaguePopulationMultipliers.put(LOST_COLONY_DICE_KEY, new TreeMap<>());
        postStarLeaguePopulationMultipliers.get(LOST_COLONY_DICE_KEY).put(1, 0);
        postStarLeaguePopulationMultipliers.get(LOST_COLONY_DICE_KEY).put(6, 0);
        
        postStarLeaguePopulationDice = new TreeMap<>();
        postStarLeaguePopulationDice.put(0, new TreeMap<>());
        postStarLeaguePopulationDice.get(0).put(1, 2);
        postStarLeaguePopulationDice.get(0).put(6, 2);
        postStarLeaguePopulationDice.put(500, new TreeMap<>());
        postStarLeaguePopulationDice.get(500).put(1, 2);
        postStarLeaguePopulationDice.get(500).put(6, 2);
        postStarLeaguePopulationDice.put(601, new TreeMap<>());
        postStarLeaguePopulationDice.get(601).put(1, 2);
        postStarLeaguePopulationDice.get(601).put(6, 2);
        postStarLeaguePopulationDice.put(751, new TreeMap<>());
        postStarLeaguePopulationDice.get(751).put(1, 2);
        postStarLeaguePopulationDice.get(751).put(6, 2);
        postStarLeaguePopulationDice.put(1001, new TreeMap<>());
        postStarLeaguePopulationDice.get(1001).put(1, 2);
        postStarLeaguePopulationDice.get(1001).put(6, 2);
        postStarLeaguePopulationDice.put(1251, new TreeMap<>());
        postStarLeaguePopulationDice.get(1251).put(1, 2);
        postStarLeaguePopulationDice.get(1251).put(6, 2);
        postStarLeaguePopulationDice.put(2000, new TreeMap<>());
        postStarLeaguePopulationDice.get(2000).put(1, 2);
        postStarLeaguePopulationDice.get(2000).put(6, 2);
        // outpost
        postStarLeaguePopulationDice.put(OUTPOST_DICE_KEY, new TreeMap<>());
        postStarLeaguePopulationDice.get(OUTPOST_DICE_KEY).put(1, 2);
        postStarLeaguePopulationDice.get(OUTPOST_DICE_KEY).put(6, 2);
        // lost colony
        postStarLeaguePopulationDice.put(LOST_COLONY_DICE_KEY, new TreeMap<>());
        postStarLeaguePopulationDice.get(LOST_COLONY_DICE_KEY).put(1, 2);
        postStarLeaguePopulationDice.get(LOST_COLONY_DICE_KEY).put(6, 2);
    }
    
    public Planet getCurrentPlanet() {
        return currentStar;
    }
    
    public void initializeSystem(double x, double y) {
        initializeSystem();
        currentStar.setX(x);
        currentStar.setY(y);
        
        commonSystemInit();
    }
    
    public void initializeSystem(Planet p) {
        currentStar = new Planet();
        currentStar.copyDataFrom(p);
        
        commonSystemInit();
    }
    
    public void initializeSystem(int spectralClass, int spectralSubType, String spectralType) {
        currentStar = new Planet();
        currentStar.setSpectralClass(spectralClass);
        currentStar.setSubtype(spectralSubType);
        currentStar.setSpectralType(spectralType);
        
        currentStar.setX(Compute.randomInt(2500));
        currentStar.setY(Compute.randomInt(2500));
        
        commonSystemInit();
    }
    
    public void initializeSystem() {
        currentStar = new Planet();
        currentStar.setSpectralType(StarUtil.generateSpectralType(rng, true));
        currentStar.setMass(StarUtil.generateMass(rng, currentStar.getSpectralClass(), currentStar.getSubtype()));
        
        currentStar.setX(Compute.randomInt(2500));
        currentStar.setY(Compute.randomInt(2500));

        commonSystemInit();
    }
    
    private void commonSystemInit() {
        outerLifeZone = StarUtil.getMaxLifeZone(currentStar.getSpectralClass(), currentStar.getSubtype());
        innerLifeZone = StarUtil.getMinLifeZone(currentStar.getSpectralClass(), currentStar.getSubtype());
        majorRockyStellarBodies = new ArrayList<>();
        distanceToTerra = currentStar.getDistanceTo(0, 0);
    }
    
    public void initializeOrbitalSlots() {
        int planetCountRoll = Compute.d6(2) + 3;
        stellarOrbitalSlots = new ArrayList<>();
        satellites = new HashMap<>();
        
        for(int planetIndex = 0; planetIndex < planetCountRoll; planetIndex++) {
            Planet p = new Planet();
            p.setSystemPosition(planetIndex + 1);
            p.setOrbitSemimajorAxis(baseAUs.get(planetIndex) * currentStar.getMass());
            stellarOrbitalSlots.add(p);
        }
    }
    
    public void fillOrbitalSlots() {
        
        
        for(int slot = 0; slot < stellarOrbitalSlots.size(); slot++) {
            fillOrbitalSlot(slot, outerLifeZone);
        }
    }
    
    private void fillOrbitalSlot(int index, double outerLifeZone) {
        Planet p = stellarOrbitalSlots.get(index);
        PlanetType planetType;
        int slotRoll = Compute.d6(2);
        
        if(p.getOrbitSemimajorAxisKm() > outerLifeZone) {
            planetType = outerSystemPlanetTypes.floorEntry(slotRoll).getValue();
        } else {
            planetType = innerSystemPlanetTypes.floorEntry(slotRoll).getValue();
        }
        
        p.setPlanetType(planetType);
        p.setSystemPosition(index + 1);
        
        if(planetType != PlanetType.Empty) {
            fillPlanetData(p, true);
        }
        
        if(planetType == PlanetType.DwarfTerrestrial ||
                planetType == PlanetType.Terrestrial ||
                planetType == PlanetType.GiantTerrestrial) {
            majorRockyStellarBodies.add(p);
        }
    }
    
    private void fillPlanetData(Planet p, boolean canHaveSatellites) {
        setDiameter(p);
        setDensity(p);
        setDayLength(p);
        
        setGravity(p);
        setEscapeVelocity(p);
        
        populateMoons(p);
        setAtmosphere(p);
        setHabitabilityIndex(p);
        
        int atmoRoll = -1;
        if(p.getHabitability(dateTime) == 0) { 
            setUnbreathableAtmosphereGasContent(p);
            setUninhabitableTemperature(p);
        } else {
            atmoRoll = setBreathableAtmosphereGasContent(p);
            setHabitableTemperature(p);
        }
        
        setWaterPercentage(p);
        setHighestLife(p);
        int special = setSpecial(p);
        
        if(special >= SPECIAL_STAR_LEAGUE_OUTPOST_ABANDONED) {
            ColonyState colonyState = setColony(p, special);
            setUSILR(p, colonyState == ColonyState.Recent);
            setGovernment(p);
            setHPG(p);
            setRechargeStation(p);
            
            if(colonyState == ColonyState.Abandoned) {
                getEvent(p).population = (long) -1;
            }
        }
    }
    
    public void forceColony() {
        int bestPlanetCandidateScore = Integer.MIN_VALUE;
        Planet bestPlanet = null;
        
        for(Planet p : majorRockyStellarBodies) {
            int candidateScore = 0;
            // things we look for when building a colony:
            // gravity between .9 and 1.1: +1
            // atmospheric pressure between thin and very high: +1
            // habitability = 1: +1
            // also, if there is already a colony/abandoned colony in this system, we are done
            
            if(p.getPopulation(dateTime) != null && p.getPopulation(dateTime) != 0) {
                return;
            }
            
            if(p.getGravity() < 1.1 && p.getGravity() > .9) {
                candidateScore += 1;
            } else {
                candidateScore -= 1;
            }
            
            if(p.getPressure(dateTime) >= PlanetaryConditions.ATMO_THIN) {
                candidateScore += 1;
            } else if(p.getPressure(dateTime) == ATMO_GAS_GIANT) {
                continue;
            } else {
                candidateScore -= 1;
            }
            
            if(p.getHabitability(dateTime) > 0) {
                candidateScore += 1;
            } else {
                candidateScore -= 1;
            }
            
            if(candidateScore > bestPlanetCandidateScore) {
                bestPlanetCandidateScore = candidateScore;
                bestPlanet = p;
            }
        }
        
        int adjustedSpecialRoll = 0;
        while(adjustedSpecialRoll < 9) {
            adjustedSpecialRoll = Compute.d6(2);
        }
        
        ColonyState colonyState = setColony(bestPlanet, adjustedSpecialRoll);
        setUSILR(bestPlanet, colonyState == ColonyState.Recent);
        setGovernment(bestPlanet);
        setHPG(bestPlanet);
        setRechargeStation(bestPlanet);
        
        if(colonyState == ColonyState.Abandoned) {
            getEvent(bestPlanet).population = (long) -1;
        }
    }
    
    private void setDiameter(Planet p) {
        if(!baseDiameters.containsKey(p.getPlanetType())) {
            p.setRadius(0);
            return;
        }
        
        int diceRoll = Compute.d6(numDiameterDice.get(p.getPlanetType()));
        int diameter = baseDiameters.get(p.getPlanetType()) + diceRoll * diameterIncrement.get(p.getPlanetType());
        
        // per CamOps page 106
        if(p.getPlanetType() == PlanetType.SmallAsteroid && diceRoll <= 10) {
            diameter /= 10.0;
        }
        
        p.setRadius(diameter / 2.0);
    }
    
    private void setDensity(Planet p) {
        // yeah, I know, giant case statement, but density formulas are weird and can't really be reduced to
        // similar equations.
        switch(p.getPlanetType()) {
        case AsteroidBelt:
            p.setDensity(0);
            break;
        case SmallAsteroid:
        case MediumAsteroid:
            p.setDensity(Math.pow(Compute.d6(), 1.15));
            break;
        case DwarfTerrestrial:
            p.setDensity(Compute.d6());
            break;
        case Terrestrial:
            p.setDensity(2.5 + Math.pow(Compute.d6(), .75));
            break;
        case GiantTerrestrial:
            p.setDensity(2 + Compute.d6());
            break;
        case GasGiant:
            p.setDensity(.5 + Compute.d6(2) / 10);
            break;
        case IceGiant:
            p.setDensity(1.0 + Compute.d6(2) / 10);
            break;
        }
    }
    
    private void setDayLength(Planet p) {
        switch(p.getPlanetType()) {
        case SmallAsteroid:
        case MediumAsteroid:
            p.setDayLength(Compute.d6(2));
            break;
        case DwarfTerrestrial:
        case Terrestrial:
            p.setDayLength(Compute.d6(3) + 12);
            break;
        case GiantTerrestrial:
        case GasGiant:
        case IceGiant:
            p.setDayLength(Compute.d6(4));
            break;
        }
    }
    
    private void setGravity(Planet p) {
        double diameter = p.getRadius() * 2;
        double gravity = (diameter / 12742) * (p.getDensity() / 5.5153);
        p.setGravity(gravity);
    }
    
    private void setEscapeVelocity(Planet p) {
        double diameter = p.getRadius() * 2;
        double escapeVelocity = 11186 * (diameter / 12742) * Math.sqrt(p.getDensity() / 5.5153);
        p.setEscapeVelocity(escapeVelocity);
    }
    
    private void populateMoons(Planet p) {
        int smallMoons = 0;
        int mediumMoons = 0;
        int largeMoons = 0;
        int giantMoons = 0;
        p.clearSatellites();
        
        // do moons later
        switch(p.getPlanetType()) {
        case AsteroidBelt:
            int asteroidRoll = Compute.d6();
            double asteroidMultiplier = (p.getSystemPosition() / 2.8) * Math.pow(asteroidRoll / 3, 2.0);
            // per CamOps, there are 1.2M x multiplier "small asteroids" in a belt, but I'm not generating millions of small asteroids
            // per CamOps, there are 200 x multiplier "medium asteroids" in a belt, but I'm not generating hundreds of them, either
            p.getSatellites().add(String.format("Approx. %d small asteroids", (int) (1200000 * asteroidMultiplier)));
            p.getSatellites().add(String.format("Approx. %d medium asteroids", (int) (200 * asteroidMultiplier)));
            largeMoons = (int) Math.ceil(asteroidMultiplier * 4);
            break;
        default:
            smallMoons = rollMoons(p, PlanetType.SmallAsteroid);
            mediumMoons = rollMoons(p, PlanetType.MediumAsteroid);
            p.getSatellites().add(String.format("%d small moons", smallMoons));
            p.getSatellites().add(String.format("%d medium moons", mediumMoons));
            
            largeMoons = rollMoons(p, PlanetType.DwarfTerrestrial);
            giantMoons = rollMoons(p, PlanetType.Terrestrial);
        }
        
        for(int x = 0; x < largeMoons; x++) {
            addMoonToPlanet(p, PlanetType.DwarfTerrestrial);
        }
        
        for(int x = 0; x < giantMoons; x++) {
            addMoonToPlanet(p, PlanetType.Terrestrial);
        }
    }
    
    /**
     * Helper function that rolls up moons for a planet
     * @param p Planet for which to roll moons
     * @param moonType The moon type we're rolling
     * @return number of moons of that type
     */
    private int rollMoons(Planet p, PlanetType moonType) {
        int moonChartRoll = Compute.d6();
        Map<PlanetType, Integer> planetTable = moonDice.floorEntry(moonChartRoll).getValue().get(p.getPlanetType());
        Map<PlanetType, Integer> planetModifierTable = moonDiceModifiers.floorEntry(moonChartRoll).getValue().get(p.getPlanetType());
        
        if(planetTable.containsKey(moonType)) {
            int moonRoll = Compute.d6(planetTable.get(moonType));
            int moonModifier = planetModifierTable.containsKey(moonType) ? 
                    planetModifierTable.get(moonType) : 0;
            return Math.max(moonRoll + moonModifier, 0);
        } else {
            return 0;
        }
    }
    
    private void addMoonToPlanet(Planet p, PlanetType moonType) {
        Planet moon = new Planet();
        moon.setOrbitSemimajorAxis(p.getOrbitSemimajorAxis());
        moon.setPlanetType(moonType);
        fillPlanetData(moon, false);
        if(!satellites.containsKey(p.getSystemPosition())) {
            satellites.put(p.getSystemPosition(), new ArrayList<>());
        }
        
        satellites.get(p.getSystemPosition()).add(moon);
        p.getSatellites().add(moonType.toString()); //placeholder
    }
    
    
    private void setAtmosphere(Planet p) {
        // todo: dwarf terrestrial moons of gas giants
        int gtAtmoRoll = Compute.d6();
        
        if((p.getPlanetType() == PlanetType.Terrestrial) ||
                (p.getPlanetType() == PlanetType.GiantTerrestrial) && (gtAtmoRoll == 5)) {
            int atmoRoll = Compute.d6(2);
            
            if(p.getOrbitSemimajorAxisKm() < innerLifeZone) {
                atmoRoll -= 2;
            }
            
            double atmoRollMultiplier = p.getEscapeVelocity() / 11186.0;
            atmoRoll = (int) Math.ceil(atmoRoll * atmoRollMultiplier);
            
            getEvent(p).pressure = atmoTypes.floorEntry(atmoRoll).getValue();
        } else if((p.getPlanetType() == PlanetType.GasGiant) ||
                (p.getPlanetType() == PlanetType.IceGiant) ||
                (p.getPlanetType() == PlanetType.GiantTerrestrial)) {
            getEvent(p).pressure = ATMO_GAS_GIANT;
        } else {
            getEvent(p).pressure = PlanetaryConditions.ATMO_VACUUM;
        }
    }
    
    private void setUninhabitableTemperature(Planet p) {
        double luminosity = StarUtil.getAvgLuminosity(currentStar.getSpectralClass(), currentStar.getSubtype());
        double starDistMultiplier = atmoTemperatureMultipliers.containsKey(p.getPressure(dateTime)) ?
                atmoTemperatureMultipliers.get(p.getPressure(dateTime)) : 1.0;
        double starDistInAU = p.getOrbitSemimajorAxisKm() / StarUtil.AU * starDistMultiplier;                    
        double avgTemp = 277 * Math.pow(luminosity, .25) * Math.sqrt(1 / starDistInAU);
        getEvent(p).temperature = (int) Math.ceil(avgTemp);
    }
    
    private void setHabitabilityIndex(Planet p) {
        boolean isHabitable = false;
        
        if(p.getPressure(dateTime) == PlanetaryConditions.ATMO_THIN ||
            p.getPressure(dateTime) == PlanetaryConditions.ATMO_STANDARD||
            p.getPressure(dateTime) == PlanetaryConditions.ATMO_HIGH) {
            int habMod = getStarHabitabilityMod();   
            if(p.getPressure(dateTime) == PlanetaryConditions.ATMO_THIN ||
                    p.getPressure(dateTime) == PlanetaryConditions.ATMO_HIGH) {
                habMod -= 1;
            }
            
            if(p.getPlanetType() == PlanetType.GiantTerrestrial) {
                habMod -= 2;
            }
            
            int habRoll = Compute.d6(2) + habMod;
            isHabitable = habRoll >= 9;
        }
        
        getEvent(p).habitability = isHabitable ? 1 : 0;
    }
    
    private void setWaterPercentage(Planet p) {
        double escapeVelocityMultiplier = p.getEscapeVelocity() / 11186.0;
        
        if(p.getGravity() < .5 ||
                p.getPressure(dateTime) < PlanetaryConditions.ATMO_THIN ||
                p.getPressure(dateTime) == ATMO_GAS_GIANT ||
                p.getTemperature(dateTime) > 323) {
           getEvent(p).percentWater = 0;
        } else {
            double surfaceWaterRoll = Compute.d6(2);
            surfaceWaterRoll *= calculateLifeZoneMultiplier(p);
            surfaceWaterRoll *= escapeVelocityMultiplier;
            surfaceWaterRoll += p.getPlanetType() == PlanetType.GiantTerrestrial ? 3 : 0;
            surfaceWaterRoll = Math.ceil(surfaceWaterRoll);
            getEvent(p).percentWater = waterPercentages.floorEntry((int) surfaceWaterRoll).getValue();
        }
    }
    
    private void setUnbreathableAtmosphereGasContent(Planet p) {
        // if it doesn't have an atmosphere, it doesn't have an atmosphere
        if(p.getPressure(dateTime) < PlanetaryConditions.ATMO_TRACE) {
            return;
        }
        
        int gasModifier = 0;
        if(p.getOrbitSemimajorAxisKm() < innerLifeZone) {
            gasModifier += 2;
        } else if(p.getOrbitSemimajorAxisKm() > outerLifeZone) {
            gasModifier -= 2;
        }
        
        if(p.getEscapeVelocity() > 12000) {
            gasModifier--;
        } else if(p.getEscapeVelocity() < 7000) {
            gasModifier++;
        }
        
        int secondaryGasRoll = Compute.d6(2) + gasModifier;
        String secondaryGas = unbreathableAtmoSecondaryGases.floorEntry(secondaryGasRoll).getValue();
        int secondaryGasPercentage = Compute.d6(5);
        
        int traceGasRoll = Compute.d6(2) + gasModifier;
        Integer secondaryTraceGasRoll = null;
        if(traceGasRoll == 12) {
            secondaryTraceGasRoll = Compute.d6(2) + gasModifier;
            traceGasRoll = Math.max(11, Compute.d6(2) + gasModifier);
        }
        
        String traceGas = unbreathableAtmoTraceGases.floorEntry(traceGasRoll).getValue();
        String secondaryTraceGas = secondaryTraceGasRoll != null ? 
                unbreathableAtmoSpecialTraceGases.floorEntry(secondaryTraceGasRoll).getValue() : "";
        
        int traceGasPercentage = Compute.d6() / 2;
        int secondaryTraceGasPercentage = secondaryTraceGas.isEmpty() ? 0 : Compute.d6() / 2;
        
        int primaryGasRoll = Compute.d6(2) + gasModifier;
        String primaryGas = unbreathableAtmoPrimaryGases.floorEntry(primaryGasRoll).getValue();
        int primaryGasPercentage = 100 - secondaryTraceGasPercentage - traceGasPercentage - secondaryGasPercentage;
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Primary: %s (%d%%)\n", primaryGas, primaryGasPercentage));
        sb.append(String.format("Secondary: %s (%d%%)\n", secondaryGas, secondaryGasPercentage));
        
        if(traceGas != "None") {
            sb.append(String.format("Trace: %s (%d%%)\n", traceGas, traceGasPercentage));
        }
        
        if(!secondaryTraceGas.isEmpty()) {
            sb.append(String.format("Trace: %s (%d%%)\n", secondaryTraceGas, secondaryTraceGasPercentage));
        }
        
        getEvent(p).atmosphere = sb.toString();
    }
    
    private int setBreathableAtmosphereGasContent(Planet p) {
        int atmoRoll = Compute.d6(2);
        if(p.getPlanetType() == PlanetType.GiantTerrestrial) {
            atmoRoll -= 2;
        }
        
        getEvent(p).atmosphere = habitableAtmoCompositions.floorEntry(atmoRoll).getValue();
        
        return atmoRoll;
    }
    
    private void setHabitableTemperature(Planet p) {
        double tempRoll = Compute.d6(2);
        tempRoll *= calculateLifeZoneMultiplier(p);
        tempRoll = Math.ceil(tempRoll);
        if(p.getPressure(dateTime) <= PlanetaryConditions.ATMO_THIN) {
            tempRoll -= 1;
        } else if(p.getPressure(dateTime) <= PlanetaryConditions.ATMO_HIGH) {
            tempRoll += 1;
        }
        
        getEvent(p).temperature = habitableTemps.floorEntry((int) tempRoll).getValue();
    }
    
    private void setHighestLife(Planet p) {
        if(p.getHabitability(dateTime) == 0 ||
                p.getPressure(dateTime) < PlanetaryConditions.ATMO_THIN) {
            getEvent(p).lifeForm = LifeForm.NONE;
            return;
        }
        
        int lifeRoll = Compute.d6(2);
        lifeRoll += getStarHabitabilityMod();
        // hab modifier
        
        if(getEvent(p).habitability == 0) {
            lifeRoll -= 4;
        }
        
        getEvent(p).lifeForm = lifeForms.floorEntry(lifeRoll).getValue();
    }
    
    private int setSpecial(Planet p) {
        int specialPresenceRoll = Compute.d6(2);
        if(specialPresenceRoll < 8) {
            return 0;
        }
        
        int specialModifier = 0;
        
        if(p.getPlanetType() == PlanetType.DwarfTerrestrial) {
            specialModifier = 2;
        } else if(p.getPlanetType() == PlanetType.GiantTerrestrial && p.getPressure(dateTime) != ATMO_GAS_GIANT) {
            specialModifier = 3;
        } else if(p.getPressure(dateTime) == ATMO_GAS_GIANT) {
            specialModifier = 4;
        }
        
        int specialRoll = Compute.d6(2) - specialModifier;
        p.setDescription(String.format("%s\n\n%s", p.getDescription(), specials.floorEntry(specialRoll).getValue()));
        
        return specialRoll;
    }
    
    private ColonyState setColony(Planet p, int specialRoll) {
        int populationDice = 0;
        int populationMultiplier = 0;
        boolean recentColony = Compute.d6() > recentColonyThreshold;
        ColonyState colonyState = ColonyState.None;
        int populationRoll = Compute.d6();
        
        // colony has a 50/50 chance of being occupied
        if(specialRoll == SPECIAL_COLONY) {
            if(Compute.d6() < 4) {
                colonyState = ColonyState.Abandoned;
            }
            
            populationDice = recentColony ?
                    starLeaguePopulationDice.floorEntry((int) distanceToTerra).getValue().floorEntry(populationRoll).getValue() :
                    postStarLeaguePopulationDice.floorEntry((int) distanceToTerra).getValue().floorEntry(populationRoll).getValue();
                    
                    populationMultiplier = recentColony ?
                    starLeaguePopulationMultipliers.floorEntry((int) distanceToTerra).getValue().floorEntry(populationRoll).getValue() :
                    postStarLeaguePopulationMultipliers.floorEntry((int) distanceToTerra).getValue().floorEntry(populationRoll).getValue();
        } else if(specialRoll == SPECIAL_LOST_COLONY) {
            populationDice = recentColony ?
                    starLeaguePopulationDice.floorEntry(LOST_COLONY_DICE_KEY).getValue().floorEntry(populationRoll).getValue() :
                    postStarLeaguePopulationDice.floorEntry(LOST_COLONY_DICE_KEY).getValue().floorEntry(populationRoll).getValue();
                    
            populationDice = recentColony ?
                    starLeaguePopulationMultipliers.floorEntry(LOST_COLONY_DICE_KEY).getValue().floorEntry(populationRoll).getValue() :
                    postStarLeaguePopulationMultipliers.floorEntry(LOST_COLONY_DICE_KEY).getValue().floorEntry(populationRoll).getValue();
        } else if(specialRoll == SPECIAL_STAR_LEAGUE_OUTPOST_OCCUPIED ||
                specialRoll == SPECIAL_STAR_LEAGUE_OUTPOST_ABANDONED) {
            populationDice = recentColony ?
                    starLeaguePopulationDice.floorEntry(OUTPOST_DICE_KEY).getValue().floorEntry(populationRoll).getValue() :
                    postStarLeaguePopulationDice.floorEntry(OUTPOST_DICE_KEY).getValue().floorEntry(populationRoll).getValue();
                    
            populationDice = recentColony ?
                    starLeaguePopulationMultipliers.floorEntry(OUTPOST_DICE_KEY).getValue().floorEntry(populationRoll).getValue() :
                    postStarLeaguePopulationMultipliers.floorEntry(OUTPOST_DICE_KEY).getValue().floorEntry(populationRoll).getValue();
        }
        
        if(populationMultiplier == 0) {
            colonyState = ColonyState.Abandoned;
        }
        
        long population = populationMultiplier * Compute.d6(populationDice);
        double postPopulationMultiplier = 1.0;
        
        // adjust population based on criteria
        if(p.getPressure(dateTime) <= PlanetaryConditions.ATMO_TRACE ||
                p.getPressure(dateTime) >= PlanetaryConditions.ATMO_VHIGH ||
                p.getAtmosphere(dateTime).equals(ATMO_VALUE_TOXIC)) {
            postPopulationMultiplier *= .05;
        } 
        
        if(p.getAtmosphere(dateTime).equals(ATMO_VALUE_TAINTED)) {
            postPopulationMultiplier *= .8;
        }
        
        // very high avg temperature
        if(p.getTemperature(dateTime) > VERY_HIGH_TEMP) {
            postPopulationMultiplier *= .8;
        }
        
        if(p.getGravity() < .8 || (p.getGravity() > 1.2 && p.getGravity() <= 1.5)) {
            postPopulationMultiplier *= .8;
        } else if(p.getGravity() > 1.5) {
            postPopulationMultiplier *= .5;
        }
        
        if(p.getPercentWater(dateTime) < 40) {
            postPopulationMultiplier *= .8;
        }
        
        population *= postPopulationMultiplier;
        getEvent(p).population = population;
        
        if(colonyState == ColonyState.None) {
            colonyState = recentColony ? ColonyState.Recent : ColonyState.Established;
        }
        
        return colonyState;
    }
    
    private void setUSILR(Planet p, boolean recentColony) {
        getEvent(p).socioIndustrial = new SocioIndustrialData();
        setTech(p, recentColony);
        setIndustrialDevelopment(p);
        setIndustrialOutput(p);
        setAgricultural(p);
        setRawMaterial(p, recentColony);
    }
    
    private void setTech(Planet p, boolean recentColony) {
        int techLevel = EquipmentType.RATING_C;
        
        if(!recentColony) {
            techLevel--;
        }
        
        // one biiiillion dollars!
        if(p.getPopulation(dateTime) > 1000000000) {
            techLevel++;
        } else if(p.getPopulation(dateTime) < 100000000) {
            techLevel--;
        }
        
        if(p.getPopulation(dateTime) < 1000000) {
            techLevel--;
        }
        
        for(String factionCode : p.getFactions(dateTime)) {
            Faction faction = Faction.getFaction(factionCode);
            if(faction.isClan()) {
                techLevel--;
                break;
            } else if(faction.is(Tag.MINOR) || faction.is(Tag.SMALL)) {
                techLevel--;
                break;
            }
        }
        
        getEvent(p).socioIndustrial.tech = techLevel;
    }
    
    private void setIndustrialDevelopment(Planet p) {
        int indLevel = EquipmentType.RATING_C;
        long oneBillion = 1000000000;
        
        if(p.getPopulation(dateTime) > oneBillion) {
            indLevel--;
        }
        
        if(p.getPopulation(dateTime) > (oneBillion * 4)) {
            indLevel--;
        }
        
        if(p.getSocioIndustrial(dateTime).tech < EquipmentType.RATING_B) {
            indLevel--;
        }
        
        if(p.getPopulation(dateTime) < 100000000) {
            indLevel++;
        }
        
        if(p.getPopulation(dateTime) < 1000000) {
            indLevel++;
        }
        
        if(p.getSocioIndustrial(dateTime).tech > EquipmentType.RATING_F) {
            indLevel++;
        }
        
        getEvent(p).socioIndustrial.industry = indLevel;
    }
    
    private void setIndustrialOutput(Planet p) {
        int indLevel = EquipmentType.RATING_C;
        long oneBillion = 1000000000;
        
        if(p.getPopulation(dateTime) > oneBillion) {
            indLevel--;
        }
                
        if(p.getSocioIndustrial(dateTime).tech <= EquipmentType.RATING_A) {
            indLevel--;
        }
        
        if(p.getSocioIndustrial(dateTime).industry < EquipmentType.RATING_B) {
            indLevel--;
        }
        
        if(p.getSocioIndustrial(dateTime).tech >= EquipmentType.RATING_D) {
            indLevel++;
        }
        
        if(p.getSocioIndustrial(dateTime).tech == EquipmentType.RATING_FSTAR) {
            indLevel++;
        }
        
        if(p.getSocioIndustrial(dateTime).industry > EquipmentType.RATING_D) {
            indLevel++;
        }
        
        getEvent(p).socioIndustrial.output = indLevel;
    }
    
    private void setAgricultural(Planet p) {
        int agriLevel = EquipmentType.RATING_C;
        long oneBillion = 1000000000;
        
        if(p.getSocioIndustrial(dateTime).tech < EquipmentType.RATING_B) {
            agriLevel--;
        }
        
        if(p.getSocioIndustrial(dateTime).tech < EquipmentType.RATING_C) {
            agriLevel--;
        }
        
        if(p.getSocioIndustrial(dateTime).industry < EquipmentType.RATING_C) {
            agriLevel--;
        }
        
        if(p.getSocioIndustrial(dateTime).tech >= EquipmentType.RATING_F) {
            agriLevel++;
        }
        
        if(p.getPopulation(dateTime) > oneBillion) {
            agriLevel++;
        }
        
        if(p.getPopulation(dateTime) > (oneBillion * 5)) {
            agriLevel++;
        }
        
        if(p.getPercentWater(dateTime) < 50) {
            agriLevel++;
        }
        
        if(p.getAtmosphere(dateTime).equals(ATMO_VALUE_TAINTED)) {
            agriLevel++;
        }
        
        if(p.getAtmosphere(dateTime).equals(ATMO_VALUE_TOXIC)) {
            agriLevel += 2;
        }
        
        getEvent(p).socioIndustrial.agriculture = agriLevel;
    }
    
    private void setRawMaterial(Planet p, boolean recentColony) {
        int rawMatDepRating = EquipmentType.RATING_C;
        long oneBillion = 1000000000;
        
        if(p.getSocioIndustrial(dateTime).tech < EquipmentType.RATING_A) {
            rawMatDepRating--;
        }
        
        if(p.getSocioIndustrial(dateTime).tech <= EquipmentType.RATING_C) {
            rawMatDepRating--;
        }
        
        if(p.getDensity() > 5.5) {
            rawMatDepRating--;
        }
        
        if(p.getPopulation(dateTime) > (oneBillion * 3)) {
            rawMatDepRating++;
        }
        
        if(p.getSocioIndustrial(dateTime).industry <= EquipmentType.RATING_B) {
            rawMatDepRating++;
        }
        
        if(!recentColony) {
            rawMatDepRating++;
        }
        
        if(p.getDensity() < 4.0) {
            rawMatDepRating--;
        }
        
        getEvent(p).socioIndustrial.rawMaterials = rawMatDepRating;
    }
    
    private void setGovernment(Planet p) {
        // skip this for now, irrelevant for what I'm working on
    }
    
    private void setHPG(Planet p) {
        getEvent(p).hpg = 0;
    }
    
    private void setRechargeStation(Planet p) {
        getEvent(p).nadirCharge = false;
        getEvent(p).zenithCharge = false;
    }
    
    private int getStarHabitabilityMod() {
        return starHabMods.containsKey(currentStar.getSpectralClass()) &&
                starHabMods.get(currentStar.getSpectralClass()).containsKey(currentStar.getSubtype()) ?
                        starHabMods.get(currentStar.getSpectralClass()).get(currentStar.getSubtype()) : 0;
    }
    
    private double calculateLifeZoneMultiplier(Planet p) {
        double starDistInAU = p.getOrbitSemimajorAxisKm() / StarUtil.AU;
        double lifeZoneInnerEdge = innerLifeZone / StarUtil.AU;
        double lifeZoneOuterEdge = outerLifeZone / StarUtil.AU;
        
        // if gas giant moon, extend outer edge by 20%
        double lifeZoneMultiplier = (starDistInAU - lifeZoneInnerEdge) / (lifeZoneOuterEdge - lifeZoneInnerEdge);
        return lifeZoneMultiplier;
    }
    
    public String getOutput(boolean html) {
        StringBuilder sb = new StringBuilder();
        sb.append(html ? "<html>" : "");
        appendLine(sb, String.format("Star, class %s, # slots %d", currentStar.getSpectralTypeNormalized(), stellarOrbitalSlots.size()), html);
                
        for(int slotIndex = 0; slotIndex < stellarOrbitalSlots.size(); slotIndex++) {
            Planet p = stellarOrbitalSlots.get(slotIndex);
            appendLine(sb, String.format("Orbit %d:", slotIndex + 1), html);
            appendLine(sb, getPlanetOutput(p, html), html);
        }
        
        sb.append(html ? "</html>" : "");
        return sb.toString();
    }
    
    private String getPlanetOutput(Planet p, boolean html) {
        return getPlanetOutput(p, html, 0);
    }
        
    private String getPlanetOutput(Planet p, boolean html, int tabCount) {
        StringBuilder sb = new StringBuilder();
        appendLine(sb, p.getPlanetType().toString(), html);
        
        if(p.getPlanetType() != PlanetType.AsteroidBelt &&
                p.getPlanetType() != PlanetType.Empty) {
            String lifeZoneIndicator = "inner system life zone";
            if(p.getOrbitSemimajorAxisKm() > outerLifeZone) {
                lifeZoneIndicator = "outer system";
            } else if(p.getOrbitSemimajorAxisKm() < innerLifeZone) {
                lifeZoneIndicator = "inner system";
            }
            
            appendLine(sb, String.format("Distance (AU): %.2f (%s)", p.getOrbitSemimajorAxisKm() / StarUtil.AU, lifeZoneIndicator), html, tabCount);
            appendLine(sb, String.format("Diameter: %.2f", p.getRadius()), html, tabCount);
            appendLine(sb, String.format("Density: %.2f", p.getDensity()), html, tabCount);
            appendLine(sb, String.format("Day Length: %.2f", p.getDayLength()), html, tabCount);
            appendLine(sb, String.format("Gravity: %.2f", p.getGravity()), html, tabCount);
            appendLine(sb, String.format("Escape Velocity: %.2f", p.getEscapeVelocity()), html, tabCount);
            
            if(p.getPressure(dateTime) == ATMO_GAS_GIANT) {
                appendLine(sb, String.format("Atmospheric Pressure: Crushing"), html, tabCount);
            } else {
                appendLine(sb, String.format("Atmospheric Pressure: %s", p.getPressureName(dateTime)), html, tabCount);
                appendLine(sb, String.format("Avg Surface Temp: %d K", p.getTemperature(dateTime)), html, tabCount);
                appendLine(sb, String.format("Habitable: %s", p.getHabitability(dateTime) > 0 ? "Yes" : "No"), html, tabCount);
                appendLine(sb, String.format("Surface Water: %d%%", p.getPercentWater(dateTime)), html, tabCount);
                
                if(p.getPressure(dateTime) > PlanetaryConditions.ATMO_TRACE &&
                        p.getAtmosphere(dateTime) != null) {
                    appendLine(sb, String.format("Atmospheric Contents: %s", p.getAtmosphere(dateTime)), html, tabCount);
                }
                
                if(p.getLifeForm(dateTime) != LifeForm.NONE) {
                    appendLine(sb, String.format("Highest Native Life Form: %s", p.getLifeFormName(dateTime)), html, tabCount);
                }
            }
            
            if(p.getPopulation(dateTime) != null) {
                if(p.getPopulation(dateTime) != 0) {
                    appendLine(sb, String.format("Population: %d", p.getPopulation(dateTime)), html, tabCount);
                    appendLine(sb, String.format("Tech Level: %s", p.getSocioIndustrial(dateTime).getHTMLDescription()), html, tabCount);
                    appendLine(sb, String.format("Government: %s", p.getGovernment(dateTime)), html, tabCount);
                    appendLine(sb, String.format("HPG: %d", p.getHPGClass(dateTime)), html, tabCount);
                    appendLine(sb, String.format("Recharge Station(s): %d", p.getRechargeStationsText(dateTime)), html, tabCount);
                } else if(p.getPopulation(dateTime) == -1) {
                    appendLine(sb, "Abandoned", html, tabCount);
                }
            }
        }
        
        boolean hasSatellites = p.getSatellites() != null && p.getSatellites().size() > 0;
        if(hasSatellites) {
            appendLine(sb, p.getSatelliteDescription(), html, tabCount);
        }
        
        if(satellites.containsKey(p.getSystemPosition())) {
            for(Planet moon : satellites.get(p.getSystemPosition())) {
                if(p.getPlanetType() == PlanetType.AsteroidBelt) {
                    appendLine(sb, "Major Asteroid: ", html, tabCount);
                } else {
                    appendLine(sb, "Satellite: ", html, tabCount);
                }
                appendLine(sb, getPlanetOutput(moon, html, 1), html, 1);
            }
        }
        
        return sb.toString();
    }
    
    private void appendLine(StringBuilder sb, String text, boolean html) {
        appendLine(sb, text, html, 0);
    }
    
    private void appendLine(StringBuilder sb, String text, boolean html, int tabCount) {
        for(int x = 0; x < tabCount; x++) {
            sb.append("\t");
        }
        
        sb.append(text);
        sb.append(html ? "<br/>" : "\n");
    }
    
    private PlanetaryEvent getEvent(Planet p) {
        return p.getOrCreateEvent(dateTime);
    }
    
    /**
     * Possible ways to fill an orbital slot
     * @author NickAragua
     */
    public enum PlanetType {
        Empty,
        AsteroidBelt,
        SmallAsteroid,
        MediumAsteroid,
        DwarfTerrestrial,
        Terrestrial,
        GiantTerrestrial,
        GasGiant,
        IceGiant
    }
    
    public enum ColonyState {
        Abandoned,
        Recent,
        Established,
        None
    }
}
