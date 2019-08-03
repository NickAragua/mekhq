package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import org.joda.time.DateTime;

import megamek.common.Compute;

public class SystemGenerator {
    private List<Double> baseAUs = new ArrayList<>();
    private TreeMap<Integer, PlanetType> innerSystemPlanetTypes;
    private TreeMap<Integer, PlanetType> outerSystemPlanetTypes;
    
    private Planet currentStar;
    private List<Planet> stellarOrbitalSlots;
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
        innerSystemPlanetTypes.put(4, PlanetType.Asteroid);
        innerSystemPlanetTypes.put(5, PlanetType.DwarfTerrestrial);
        innerSystemPlanetTypes.put(6, PlanetType.Terrestrial);
        innerSystemPlanetTypes.put(8, PlanetType.GiantTerrestrial);
        innerSystemPlanetTypes.put(9, PlanetType.GasGiant);
        innerSystemPlanetTypes.put(11, PlanetType.IceGiant);
        
        outerSystemPlanetTypes = new TreeMap<>();
        outerSystemPlanetTypes.put(2, PlanetType.Empty);
        outerSystemPlanetTypes.put(4, PlanetType.Asteroid);
        outerSystemPlanetTypes.put(5, PlanetType.DwarfTerrestrial);
        outerSystemPlanetTypes.put(6, PlanetType.GasGiant);
        outerSystemPlanetTypes.put(8, PlanetType.Terrestrial);
        outerSystemPlanetTypes.put(10, PlanetType.GiantTerrestrial);
        outerSystemPlanetTypes.put(11, PlanetType.IceGiant);
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
    }
    
    public void fillOrbitalSlot(int index, double outerLifeZone) {
        Planet p = stellarOrbitalSlots.get(index);
        
        PlanetType planetType;
        int slotRoll = Compute.d6(2);
        if(p.getOrbitSemimajorAxisKm() > outerLifeZone) {
            planetType = outerSystemPlanetTypes.get(slotRoll);
        } else {
            planetType = innerSystemPlanetTypes.get(slotRoll);
        }
        
    }
    
    public enum PlanetType {
        Empty,
        Asteroid,
        DwarfTerrestrial,
        Terrestrial,
        GiantTerrestrial,
        GasGiant,
        IceGiant
    }
}
