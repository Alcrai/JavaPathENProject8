package tourGuide;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tourGuide.user.UserReward;

public class TestPerformance {
	private int NB_USER=100;
	
	/*
	 * A note on performance improvements:
	 *     
	 *     The number of users generated for the high volume tests can be easily adjusted via this method:
	 *     
	 *     		InternalTestHelper.setInternalUserNumber(100000);
	 *     
	 *     
	 *     These tests can be modified to suit new solutions, just as long as the performance metrics
	 *     at the end of the tests remains consistent. 
	 * 
	 *     These are performance metrics that we are trying to hit:
	 *     
	 *     highVolumeTrackLocation: 100,000 users within 15 minutes:
	 *     		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     *
     *     highVolumeGetRewards: 100,000 users within 20 minutes:
	 *          assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */
	
	//@Ignore
	@Test
	public void highVolumeTrackLocation() {
		Locale.setDefault(Locale.ENGLISH);
		ExecutorService executor = Executors.newFixedThreadPool(1000);
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		// Users should be incremented up to 100,000, and test finishes within 15 minutes
		InternalTestHelper.setInternalUserNumber(NB_USER);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		StopWatch watchUserList = new StopWatch();
		watchUserList.start();
		List<User> allUsers = new ArrayList<>();
		allUsers = tourGuideService.getAllUsers();
		watchUserList.stop();
		System.out.println("WatchUserList: " + TimeUnit.MILLISECONDS.toSeconds(watchUserList.getTime()) + " seconds.");
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for(User user : allUsers) {
			tourGuideService.trackUserLocation(user);
		}
		stopWatch.stop();
		//tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeTrackLocation: Time Elapsed: " + TimeUnit.MILLISECONDS.toMillis(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
	

	@Test
	public void highVolumeGetRewards() {
		Locale.setDefault(Locale.ENGLISH);
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		StopWatch stopWatch = new StopWatch();

		// Users should be incremented up to 100,000, and test finishes within 20 minutes
		InternalTestHelper.setInternalUserNumber(NB_USER);

		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		//tourGuideService.tracker.stopTracking();
	    Attraction attraction = gpsUtil.getAttractions().get(0);

		List<User> allUsers = new ArrayList<>();
		allUsers = tourGuideService.getAllUsers();
		stopWatch.start();
		allUsers.forEach(u ->{ u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date()));
						rewardsService.calculateRewards(u);
		});
		allUsers.forEach(user->{
			while(user.getUserRewards().size()==0){
				try {
					sleep(1);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
	  	stopWatch.stop();
		for(User user : allUsers) {
			assertTrue(user.getUserRewards().size() > 0);
		}

		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
	
}
