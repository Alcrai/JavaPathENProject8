package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.concurrent.Computable;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.user.User;
import tourGuide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 2000000;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}
	
	public void calculateRewards(User user) {
		ExecutorService executor = Executors.newFixedThreadPool(1000);
		StopWatch stopRewardsInit = new StopWatch();
		stopRewardsInit.start();
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();
		stopRewardsInit.stop();
		//CompletableFuture<List<VisitedLocation>> locationFuture = CompletableFuture.supplyAsync(()->user.getVisitedLocations());
		//CompletableFuture<List<Attraction>> attractionFuture = CompletableFuture.supplyAsync(()->gpsUtil.getAttractions());

		/*		for(VisitedLocation visitedLocation : userLocations) {
					for (Attraction attraction : attractions) {
						if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
							if (nearAttraction(visitedLocation, attraction)) {
								CompletableFuture.supplyAsync(()->{
									int i=getRewardPoints(attraction,user);
								//	System.out.println(i);
									return i;}).thenAccept( points ->{
										user.addUserReward(new UserReward(visitedLocation, attraction, points));
										System.out.println(points);
								});
							}
						}
					}
				}*/
		StopWatch stop =new StopWatch();
		stop.start();
		userLocations.stream()
				.forEach(ul->{
					attractions.stream()
							.filter(at->nearAttraction(ul, at) && (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(at.attractionName)).count() == 0))
							.forEach(att-> CompletableFuture.supplyAsync(()->{return getRewardPoints(att,user);},executor).thenAccept(points->{
								user.addUserReward(new UserReward(ul, att, points));}));
				});
		executor.shutdown();
		stop.stop();
	/*	System.out.println("StopRewardsInit : "+ TimeUnit.MILLISECONDS.toMillis(stopRewardsInit.getTime()));
		System.out.println("StopRewards : "+ TimeUnit.MILLISECONDS.toMillis(stop.getTime()));*/
	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > (double)attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > (double)proximityBuffer ? false : true;
	}
	
	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
