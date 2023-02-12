package tourGuide.tracker;

import gpsUtil.GpsUtil;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TrackerCompletableFuture extends Thread{
    private Logger logger = LoggerFactory.getLogger(Tracker.class);
    private final TourGuideService tourGuideService;
    private TreeMap<UUID, VisitedLocation> locationsUsers = new TreeMap<>();
    private GpsUtil gpsUtil;

    public TrackerCompletableFuture(TourGuideService tourGuideService,GpsUtil gpsUtil) {
        this.tourGuideService = tourGuideService;
        this.gpsUtil = gpsUtil;
    }


    @Override
    public void run() {
        Locale.setDefault(Locale.ENGLISH);
        StopWatch stoptc = new StopWatch();
        StopWatch stoptc1 = new StopWatch();
        stoptc.start();
        List<User> users = tourGuideService.getAllUsers();
        stoptc.stop();
        stoptc1.start();
        users.stream()
                .forEach(user -> locationsUsers.put(user.getUserId(), gpsUtil.getUserLocation(user.getUserId())));
        tourGuideService.setLocationsUsers(locationsUsers);
        stoptc1.stop();
       /* System.out.println("Stoptc : "+ TimeUnit.MILLISECONDS.toSeconds(stoptc.getTime()));
        System.out.println("Stoptc1 : "+ TimeUnit.MILLISECONDS.toSeconds(stoptc1.getTime()));*/

    }

    public TreeMap<UUID, VisitedLocation> getLocationsUsers() {
        return locationsUsers;
    }
}
