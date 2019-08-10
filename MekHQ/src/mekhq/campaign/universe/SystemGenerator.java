package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.joda.time.DateTime;

import megamek.common.Compute;
import megamek.common.PlanetaryConditions;
import mekhq.campaign.universe.Planet.PlanetaryEvent;

public class SystemGenerator {
    private int ATMO_GAS_GIANT = -1;
    private int SPECIAL_COLONY = 11;
    private int SPECIAL_LOST_COLONY = 12;
    
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
    
    private TreeMap<Integer, TreeMap<Integer, Integer>> starLeaguePopulationMultipliers;
    private TreeMap<Integer, TreeMap<Integer, Integer>> starLeaguePopulationDice;
    private TreeMap<Integer, TreeMap<Integer, Integer>> postStarLeaguePopulationMultipliers;
    private TreeMap<Integer, TreeMap<Integer, Integer>> postStarLeaguePopulationDice;
    
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
    double outerLifeZone;
    double innerLifeZone;
    
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
        atmoTypes.put(2, PlanetaryConditions.ATMO_VACUUM);
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
        habitableAtmoCompositions.put(Integer.MIN_VALUE, "Toxic");
        habitableAtmoCompositions.put(2, "Tainted");
        habitableAtmoCompositions.put(7, "Breathable");
        
        habitableTemps = new TreeMap<>();
        habitableTemps.put(Integer.MIN_VALUE, 317);
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
        /*
        private TreeMap<Integer, TreeMap<Integer, Integer>> starLeaguePopulationDice;
        private TreeMap<Integer, TreeMap<Integer, Integer>> postStarLeaguePopulationMultipliers;
        private TreeMap<Integer, TreeMap<Integer, Integer>> postStarLeaguePopulationDice;*/
    }
    
    public Planet getCurrentPlanet() {
        return currentStar;
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
        
        commonSystemInit();
    }
    
    public void initializeSystem() {
        currentStar = new Planet();
        currentStar.setSpectralType(StarUtil.generateSpectralType(rng, true));
        currentStar.setMass(StarUtil.generateMass(rng, currentStar.getSpectralClass(), currentStar.getSubtype()));

        commonSystemInit();
    }
    
    private void commonSystemInit() {
        outerLifeZone = StarUtil.getMaxLifeZone(currentStar.getSpectralClass(), currentStar.getSubtype());
        innerLifeZone = StarUtil.getMinLifeZone(currentStar.getSpectralClass(), currentStar.getSubtype());
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
        
        if(p.getHabitability(dateTime) == 0) { 
            setUnbreathableAtmosphereGasContent(p);
            setUninhabitableTemperature(p);
        } else {
            setBreathableAtmosphereGasContent(p);
            setHabitableTemperature(p);
        }
        
        setWaterPercentage(p);
        setHighestLife(p);
        setSpecial(p);
        setColony(p);
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
    
    private void setBreathableAtmosphereGasContent(Planet p) {
        int atmoRoll = Compute.d6(2);
        if(p.getPlanetType() == PlanetType.GiantTerrestrial) {
            atmoRoll -= 2;
        }
        
        getEvent(p).atmosphere = habitableAtmoCompositions.floorEntry(atmoRoll).getValue();
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
    
    private void setSpecial(Planet p) {
        int specialPresenceRoll = Compute.d6(2);
        if(specialPresenceRoll < 8) {
            return;
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
        if(specialRoll == SPECIAL_COLONY || specialRoll == SPECIAL_LOST_COLONY) {
            getEvent(p).populationRating = 1;
        }
        
        p.setDescription(String.format("%s\n\n%s", p.getDescription(), specials.floorEntry(specialRoll).getValue()));
    }
    
    private void setColony(Planet p) {
        if(p.getPopulationRating(dateTime) > 0) {
            //set actual population
            // set socio-industrial ratings
        }
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
}
