package com.example.gpsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.gpsapp.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.checkerframework.checker.units.qual.C;

import java.io.IOException;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // Default colours
    private static final int COLOR_LIGHT_GREEN_ARGB = 0xff81C784;
    private static final int COLOR_BLACK_ARGB = 0xff000000;
    private static final int POLYGON_STROKE_WIDTH_PX = 4;

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location userCurrentLocation;
    private ActivityMapsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences sharedPref = getSharedPreferences("ParkingSharedPref", MODE_PRIVATE);
        String username = sharedPref.getString("username", null);

        TextView name = findViewById(R.id.welcomeText);
        name.setText("Welcome " + username);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        Button button = findViewById(R.id.settingsButton);
        button.setOnClickListener(view -> startActivity(new Intent(MapsActivity.this, InfoActivity.class)));
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Polygon parkingLot = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.420126325030935, -89.26147925935744),
                        new LatLng(48.41973292490029, -89.2618279465245),
                        new LatLng(48.41862925218909, -89.25893920747806),
                        new LatLng(48.4217016751267, -89.25641524881235),
                        new LatLng(48.42187967772085, -89.25635624021314),
                        new LatLng(48.42209862004971, -89.25644207089933),
                        new LatLng(48.42227484123919, -89.25665664761475),
                        new LatLng(48.422493781880185, -89.25710189431085),
                        new LatLng(48.4228408809412, -89.2574183949661),
                        new LatLng(48.42305803914835, -89.25671833843202),
                        new LatLng(48.42410999734623, -89.25746399251712),
                        new LatLng(48.423930222617344, -89.2580165275593),
                        new LatLng(48.423467433261195, -89.25776976433659),
                        new LatLng(48.42334103866781, -89.2578987252553),
                        new LatLng(48.42295478336479, -89.25907621498116),
                        new LatLng(48.42245282508292, -89.25867388363974),
                        new LatLng(48.422196504038176, -89.25921032543057),
                        new LatLng(48.421870760858326, -89.258158899525),
                        new LatLng(48.420909539276124, -89.25895819780538),
                        new LatLng(48.42092555978428, -89.25910303708828),
                        new LatLng(48.419866415307744, -89.26004985685303),
                        new LatLng(48.41977563047, -89.26020810718065),
                        new LatLng(48.41973112803933, -89.26036367529933),
                        new LatLng(48.41977029018037, -89.26054606550744),
                        new LatLng(48.41990557734456, -89.26089743487894)));
        parkingLot.setStrokeWidth(5);

        Polygon R10 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.42177353427675, -89.25813385197395),
                        new LatLng(48.42152655872493, -89.25747300222767),
                        new LatLng(48.42069731082087, -89.25821504852982),
                        new LatLng(48.42093830303219, -89.25888492011565)));
        R10.setStrokeWidth(8);
        R10.setStrokeColor(0xff388E3C);

        Polygon G19 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.42068234232035, -89.25819474938874),
                        new LatLng(48.421514584119706, -89.2574504476267),
                        new LatLng(48.42129904078852, -89.2568234297787),
                        new LatLng(48.420451826894265, -89.25752262234303)));
        G19.setStrokeWidth(8);
        G19.setStrokeColor(0xff388E3C);

        Polygon G4 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.42259199303923, -89.2573537725602),
                        new LatLng(48.42247133608969, -89.25722458963087),
                        new LatLng(48.42226177333875, -89.25677324680372),
                        new LatLng(48.42212841477467, -89.25657389043131),
                        new LatLng(48.42198341339923, -89.25646384571372),
                        new LatLng(48.421725161749954, -89.2564813890745),
                        new LatLng(48.421384352234135, -89.25673816008302),
                        new LatLng(48.42186381341012, -89.25801085116451)));
        G4.setStrokeWidth(8);
        G4.setStrokeColor(0xff388E3C);



        // TL
        // BL
        // BR
        // TR
        Polygon space1 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.421698253359665, -89.25811850230407),
                        new LatLng(48.4216537526122, -89.25815538267703),
                        new LatLng(48.42163550729445, -89.25811179678172),
                        new LatLng(48.42168000805792, -89.25807156364756)));
        styleParkingSpace(space1);

        Polygon space2 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.42168000805792, -89.25807156364756),
                        new LatLng(48.42163550729445, -89.25811179678172),
                        new LatLng(48.42162215705782, -89.25807491640873),
                        new LatLng(48.421666657832965, -89.25803535382684)));
        styleParkingSpace(space2);

        Polygon space3 = googleMap.addPolygon(new PolygonOptions()
                .clickable(true)
                .add(
                        new LatLng(48.421666657832965, -89.25803535382684),
                        new LatLng(48.42162215705782, -89.25807491640873),
                        new LatLng(48.42160613676923, -89.25803937714025),
                        new LatLng(48.42165063755839, -89.25799981455835)));
        styleParkingSpace(space3);

        // Add a marker in Sydney and move the camera
        LatLng Lot = new LatLng(48.42101, -89.25828);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Lot, 18));
    }

    /**
     * Styles the parking space
     * @param polygon The polygon that represents the parking space
     */
    private void styleParkingSpace(Polygon polygon) {
        polygon.setStrokeWidth(POLYGON_STROKE_WIDTH_PX);
        polygon.setStrokeColor(COLOR_BLACK_ARGB);
        polygon.setFillColor(COLOR_LIGHT_GREEN_ARGB);
    }
}
