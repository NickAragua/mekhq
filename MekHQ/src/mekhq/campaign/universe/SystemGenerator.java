package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import org.joda.time.DateTime;

import megamek.common.Compute;
import megamek.common.PlanetaryConditions;

public class SystemGenerator {
    private int ATMO_GAS_GIANT = -1;
    
    private List<Double> baseAUs = new ArrayList<>();
    private Map<PlanetType, Integer> baseDiameters;
    private Map<PlanetType, Integer> diameterIncrement;
    private Map<PlanetType, Integer> numDiameterDice;
    
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
    private Random rng;
    
    public SystemGenerator() {
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
    }
    
    public Planet getCurrentPlanet() {
        return currentStar;
    }
    
    public void initializeSystem(Planet p) {
        currentStar = new Planet();
        currentStar.copyDataFrom(p);
    }
    
    public void initializeSystem(int spectralClass, int spectralSubType, String spectralType) {
        currentStar = new Planet();
        currentStar.setSpectralClass(spectralClass);
        currentStar.setSubtype(spectralSubType);
        currentStar.setSpectralType(spectralType);
    }
    
    public void initializeSystem() {
        currentStar = new Planet();
        currentStar.setSpectralType(StarUtil.generateSpectralType(rng, true));
        currentStar.setMass(StarUtil.generateMass(rng, currentStar.getSpectralClass(), currentStar.getSubtype()));
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
        double outerLifeZone = StarUtil.getMaxLifeZone(currentStar.getSpectralClass(), currentStar.getSubtype());
        double innerLifeZone = StarUtil.getMinLifeZone(currentStar.getSpectralClass(), currentStar.getSubtype());
        
        for(int slot = 0; slot < stellarOrbitalSlots.size(); slot++) {
            fillOrbitalSlot(slot, outerLifeZone);
        }
    }
    
    private void fillOrbitalSlot(int index, double outerLifeZone) {
        Planet p = stellarOrbitalSlots.get(index);
        PlanetType planetType;
        int slotRoll = Compute.d6(2);
        
        try {        
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
        } catch(Exception e) {
            int alpha = 1;
        }
        
    }
    
    private void fillPlanetData(Planet p, boolean canHaveSatellites) {
        setDiameter(p);
        setDensity(p);
        setDayLength(p);
        
        if(p.getPlanetType() != PlanetType.AsteroidBelt) {
            setGravity(p);
            setEscapeVelocity(p);
        }
        
        populateMoons(p);
        setAtmosphere(p);
    }
    
    private void setDiameter(Planet p) {
        if(!baseDiameters.containsKey(p.getPlanetType())) {
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
            p.setPressure(atmoTypes.floorEntry(atmoRoll).getValue());
        } else if((p.getPlanetType() == PlanetType.GasGiant) ||
                (p.getPlanetType() == PlanetType.IceGiant) ||
                (p.getPlanetType() == PlanetType.GiantTerrestrial)) {
            p.setPressure(ATMO_GAS_GIANT);
        } else {
            p.setPressure(PlanetaryConditions.ATMO_VACUUM);
        }
    }
    
    public String getOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(String.format("Star, class %s, # slots %d", currentStar.getSpectralTypeNormalized(), stellarOrbitalSlots.size()));
        sb.append("<br/>\n");
        
        for(int slotIndex = 0; slotIndex < stellarOrbitalSlots.size(); slotIndex++) {
            Planet p = stellarOrbitalSlots.get(slotIndex);
            sb.append(String.format("Orbit %d:", slotIndex + 1));
            sb.append("<br/>\n");
            sb.append(getPlanetOutput(p));
            sb.append("<br/>\n\n");
        }
        
        sb.append("</html>");
        return sb.toString();
    }
    
    private String getPlanetOutput(Planet p) {
        StringBuilder sb = new StringBuilder();
        sb.append("<br/>\n");
        sb.append(p.getPlanetType().toString());
        sb.append("<br/>\n");
        
        if(p.getPlanetType() != PlanetType.AsteroidBelt &&
                p.getPlanetType() != PlanetType.Empty) {
            sb.append(String.format("Diameter %.4f<br/>\n", p.getRadius()));
            sb.append(String.format("Density: %.4f<br/>\n", p.getDensity()));
            sb.append(String.format("Day Length: %.4f<br/>\n", p.getDayLength()));
            sb.append(String.format("<b>Gravity: %.4f</b><br/>\n", p.getGravity()));
            sb.append(String.format("Escape Velocity: %.4f<br/>\n", p.getEscapeVelocity()));
            
            if(p.getPressure(new DateTime(3060, 1, 1, 1, 1)) == ATMO_GAS_GIANT) {
                sb.append(String.format("Atmospheric Pressure: Crushing\n"));
            } else {
                sb.append(String.format("Atmospheric Pressure: %s<br/>\n", p.getPressureName(new DateTime(3060, 1, 1, 1, 1))));
            }
            
            boolean hasSatellites = p.getSatellites() != null && p.getSatellites().size() > 0;
            if(hasSatellites) {
                sb.append(p.getSatelliteDescription());
                sb.append("<br/>\n");
            }
        }
        
        if(satellites.containsKey(p.getSystemPosition())) {
            for(Planet moon : satellites.get(p.getSystemPosition())) {
                if(p.getPlanetType() == PlanetType.AsteroidBelt) {
                    sb.append("\tMajor Asteroid: <br/>\n");
                } else {
                    sb.append("\tSatellite: <br/>\n");
                }
                sb.append(getPlanetOutput(moon));
            }
        }
        
        return sb.toString();
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
