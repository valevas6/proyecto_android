package com.example.ride_bit.Common;

import com.example.ride_bit.Remote.IGoogleAPI;
import com.example.ride_bit.Remote.RetrofitClient;

public class Common {


    public static final String driver_tb1 = "Drivers";
    public static final String user_driver_tb1 = "DriversInfo";
    public static final String user_rider_tb1 = "RiderInfo";
    public static final String pickup_tb1 = "PickupRequest";

    public static final String baseURL = "https://maps.googleapis.com";
    public static IGoogleAPI getGoogleAPI(){
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }

}
