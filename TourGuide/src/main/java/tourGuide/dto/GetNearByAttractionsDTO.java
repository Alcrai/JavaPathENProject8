package tourGuide.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GetNearByAttractionsDTO {
    private String nameAttraction;
    private Location attractionLocation;
    private Location userLocation;
    private Double distance;
    private int rewardsPoint;

    public GetNearByAttractionsDTO(String nameAttraction, Location attractionLocation, Location userLocation, Double distance, int rewardsPoint) {
        this.nameAttraction = nameAttraction;
        this.attractionLocation = attractionLocation;
        this.userLocation = userLocation;
        this.distance = distance;
        this.rewardsPoint = rewardsPoint;
    }

    public String getNameAttraction() {
        return nameAttraction;
    }

    public void setNameAttraction(String nameAttraction) {
        this.nameAttraction = nameAttraction;
    }

    public Location getAttractionLocation() {
        return attractionLocation;
    }

    public void setAttractionLocation(Location attractionLocation) {
        this.attractionLocation = attractionLocation;
    }

    public Location getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public int getRewardsPoint() {
        return rewardsPoint;
    }

    public void setRewardsPoint(int rewardsPoint) {
        this.rewardsPoint = rewardsPoint;
    }

    @Override
    public String toString() {
        return "GetNearByAttractionsDTO{" +
                "nameAttraction='" + nameAttraction + '\'' +
                ", attractionLocation=" + attractionLocation +
                ", userLocation=" + userLocation +
                ", distance=" + distance +
                ", rewardsPoint=" + rewardsPoint +
                '}';
    }
}
