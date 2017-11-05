package com.sunrider.bikeparking.presenters;


import com.sunrider.bikeparking.interfaces.HomeView;
import com.sunrider.bikeparking.models.BikeUtilityLocation;
import com.sunrider.bikeparking.services.MapService;

public class HomePresenter extends BasePresenter {

    private final HomeView view;
    private final MapService<BikeUtilityLocation> mapService;

    public HomePresenter(final HomeView view, final MapService<BikeUtilityLocation> mapService) {
        this.view = view;
        this.mapService = mapService;
    }

    @Override
    public void init() {
        mapService.loadMap();
    }
}
