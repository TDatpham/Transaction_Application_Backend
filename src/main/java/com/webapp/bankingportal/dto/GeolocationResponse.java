package com.webapp.bankingportal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeolocationResponse {
    
    @JsonProperty("query")
    private String query;  // IP address
    
    @JsonProperty("status")
    private String status; // "success" or "fail"
    
    @JsonProperty("message")
    private String message; // Error message if status is "fail"
    
    @JsonProperty("country")
    private String country;
    
    @JsonProperty("countryCode")
    private String countryCode;
    
    @JsonProperty("region")
    private String region;
    
    @JsonProperty("regionName")
    private String regionName;
    
    @JsonProperty("city")
    private String city;
    
    @JsonProperty("zip")
    private String zip;
    
    @JsonProperty("lat")
    private Double lat;
    
    @JsonProperty("lon")
    private Double lon;
    
    @JsonProperty("timezone")
    private String timezone;
    
    @JsonProperty("isp")
    private String isp;
    
    @JsonProperty("org")
    private String org;
    
    @JsonProperty("as")
    private String as;
    
    // Constructors
    public GeolocationResponse() {
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getRegionName() {
        return regionName;
    }
    
    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getZip() {
        return zip;
    }
    
    public void setZip(String zip) {
        this.zip = zip;
    }
    
    public Double getLat() {
        return lat;
    }
    
    public void setLat(Double lat) {
        this.lat = lat;
    }
    
    public Double getLon() {
        return lon;
    }
    
    public void setLon(Double lon) {
        this.lon = lon;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public String getIsp() {
        return isp;
    }
    
    public void setIsp(String isp) {
        this.isp = isp;
    }
    
    public String getOrg() {
        return org;
    }
    
    public void setOrg(String org) {
        this.org = org;
    }
    
    public String getAs() {
        return as;
    }
    
    public void setAs(String as) {
        this.as = as;
    }
    
    // Helper methods
    public boolean isSuccess() {
        return "success".equals(status);
    }
    
    public String getFormattedLocation() {
        StringBuilder sb = new StringBuilder();
        
        if (city != null && !city.isEmpty()) {
            sb.append(city);
        }
        
        if (regionName != null && !regionName.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(regionName);
        }
        
        if (country != null && !country.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country);
        }
        
        return sb.length() > 0 ? sb.toString() : "Unknown Location";
    }
    
    @Override
    public String toString() {
        return "GeolocationResponse{" +
                "query='" + query + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", country='" + country + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", regionName='" + regionName + '\'' +
                ", city='" + city + '\'' +
                ", timezone='" + timezone + '\'' +
                ", isp='" + isp + '\'' +
                '}';
    }
}