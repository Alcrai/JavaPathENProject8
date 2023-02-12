package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.dto.GetNearByAttractionsDTO;
import tourGuide.helper.InternalTestHelper;
import tourGuide.tracker.Tracker;
import tourGuide.tracker.TrackerCompletableFuture;
import tourGuide.user.User;
import tourGuide.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	//public final Tracker tracker;
	public final TrackerCompletableFuture trackerCompletableFuture;
	private TreeMap<UUID, VisitedLocation> locationsUsers = new TreeMap<>();
	boolean testMode = true;
	
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		//tracker = new Tracker(this);
	//	addShutDownHook();
		trackerCompletableFuture = new TrackerCompletableFuture(this,gpsUtil);
		trackerCompletableFuture.run();
	}

	public void setLocationsUsers(TreeMap<UUID, VisitedLocation> locationsUsers) {
		this.locationsUsers = locationsUsers;
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}
	
	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ?
			user.getLastVisitedLocation() :
			trackUserLocation(user);
		return visitedLocation;
	}
	
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}
	
	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}
	
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(), 
				user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}


	public VisitedLocation trackUserLocation(User user) {
		ExecutorService executor = Executors.newFixedThreadPool(1000);
		final VisitedLocation[] location = {null};
		CompletableFuture.runAsync(()->{
			location[0] = locationsUsers.get(user.getUserId());
			user.addToVisitedLocations(location[0]);
				}).thenRunAsync(()->{rewardsService.calculateRewards(user);},executor);
		executor.shutdown();
			return location[0];
	}

	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
		List<Attraction> nearbyAttractions = new ArrayList<>();
		for(Attraction attraction : gpsUtil.getAttractions()) {
			if(rewardsService.isWithinAttractionProximity(attraction, visitedLocation.location)) {
				nearbyAttractions.add(attraction);
			}
		}
		return nearbyAttractions;
	}

	public List<GetNearByAttractionsDTO> getNearByAttractionsFiveCloser(VisitedLocation visitedLocation, User user) {
		List<Attraction> attractionsCloser = getNearByAttractions(visitedLocation);
		TreeMap<Double,Attraction> attractionByDistance = new TreeMap<>();
		attractionsCloser.forEach(ac->{
			Location locAttraction = new Location(ac.latitude,ac.longitude);
			attractionByDistance.put(rewardsService.getDistance(visitedLocation.location,locAttraction),ac);
		});
		Set keys = attractionByDistance.entrySet();
		Iterator it = keys.iterator();
		List<GetNearByAttractionsDTO> results = new ArrayList<>() ;
		int i=5;
		while(it.hasNext()){
			Map.Entry element = (Map.Entry)it.next();
			Attraction attraction = (Attraction) element.getValue();
			GetNearByAttractionsDTO getNearByAttractionsDTO = new GetNearByAttractionsDTO(
					attraction.attractionName,
					new Location(attraction.latitude,attraction.longitude),
					visitedLocation.location,
					rewardsService.getDistance(new Location(attraction.latitude,attraction.longitude),visitedLocation.location),
					rewardsService.getRewardPoints(attraction,user));
			results.add(getNearByAttractionsDTO);
			i--;
			if (i<=0){
				break;
			}

		}
		return results;
	}
	
	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() { 
		      public void run() {
		  //      tracker.stopTracking();
		      } 
		    }); 
	}
	
	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	public final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);
			
			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}
	
	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}
	
	private double generateRandomLongitude() {
		double leftLimit = -180;
	    double rightLimit = 180;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
	    double rightLimit = 85.05112878;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
	    return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}


}
