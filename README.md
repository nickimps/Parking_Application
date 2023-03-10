# Smart Parking Android Application
## Project Members
- Jimmy Tsang
- Nicholas Imperius
- Mason Tommasini

## Description
Many students and staff can attest to problematic parking at Lakehead University. To provide a simple, fast and effective solution to this problem, we are creating a software application that makes finding a parking spot much easier. The software is implemented as a mobile application for smartphones that displays information such as free parking spaces, busy times, and the location of your parked car. When arriving at the university, it can be difficult to find an open parking spot and frustrating when you are running late. This application would provide a solution to this issue and provide additional benefits, such as saving the user time, reducing unnecessary stress, and helping to reduce the environmental impact of carbon emissions produced by motorized vehicles. Our initial testing and training of this project will be focused on the G campus parking lot at Lakehead University, but could be further expanded to other fields, such as public parking structures and mall parking lots. 

This application will be technologically quite different from other parking applications that exist currently. Instead of relying on sensory data or IP cameras to locate and detect empty parking locations, this application will use machine learning techniques to determine whether you have parked your car or not and then use GPS location and network information to notify other users that the parking space has been filled. Additionally, when you have left your parking spot, our model will be able to accurately detect that and mark the parking space as free. Different machine learning classification methods will need to be tested to ensure that we have chosen the most appropriate model. The machine learning model will predict based on your speed before and after a full stop; it should be able to recognize the difference between walking and driving, which allows it to determine the stopping location.

## Features
- Locate available parking spots closest to your destination on campus
- Find the position of your parked car
- Automatic detection of parking so that other users can see the spot has been occupied, no need to specify you have parked
- Storage of parking permits to help parking services

## Current Bugs
- [ ] When user logs out, it brings them to login screen, if they click the back button on the phone it brings them back to infoActivity under null user
- [ ] The app rotates but only one direction, need to turn off rotatablility

## TODO
- [X] Have live location show up on the map
- [ ] Functional Parking Spaces
  - [X] Use small subset of parking lot to collect and save the GPS location of the parking spaces (This small sample could be used for the demo)
  - [X] Parking space UI for occupied and empty spots
  - [X] UI for your current location, do we leave as a blue circle or do we try and make it look like a car and have custom icons that the user can choose from?
  - [X] Functionality to recognize when a user is within a parking space or not
  - [ ] When you click on a parking space, it should bring that parking space to centre of screen and even zoom in/out if needed
- [ ] Implement Admin Screen
  - [X] Add field to select users within the database
  - [X] Add a button in the settings for admins to go to admin mode
  - [ ] Add function to be able to give admin privileges to a user
  - [X] Create admin screen to have more functionality
  - [ ] Admin mode should give capability to quickly see if device is in parking space or not and what is the ID of the parking space in the admin screen
  - [X] Ability to save a gps location to a file or something
  - [ ] Ability to select a parking space and make it occupied or empty on demand
- [ ] Geofencing ability, have it turn on precise location when within campus parking lot and go to approximate when it is not on campus parking lot
- [X] Update user interface, make sure the UI looks presentable
  - [X] LoginActivity
  - [X] RegisterActivity
  - [X] InfoActivity
  - [X] MapsActivity
  - [X] AdminActivity
- [ ] Change font, sizing and scaling for application to look the same on any size device
- [X] Add legend for types of parking spaces on the MapActivity
- [ ] Create API for the connection to the Python model when it eventually a go

## TODO If Time Left
- [ ] Dark mode
- [ ] Transitions (especially on login and register screen)
- [ ] Pause button that turns of location tracking until reactivated
- [ ] Recenter button to bring user back to good view of the parking lot map


## User Interface
<div style="display:flex">
  <div style="flex:1;padding-right:10px;">
    <img src="/Screen_Captures/Picture1.png" width="20%">
    <img src="/Screen_Captures/Picture2.png" width="20%">
    <img src="/Screen_Captures/Picture3.png" width="20%">
  </div>
  <div style="flex:1;padding-right:10px;">
    <img src="/Screen_Captures/Picture4.png" width="20%">
    <img src="/Screen_Captures/Picture5.png" width="20%">
    <img src="/Screen_Captures/Picture6.png" width="20%">
  </div>
</div>

## Getting Started
1. Download the app onto your android phone or tablet
2. Create an account or log in with an existing one
3. Use the Google Maps screen to locate a preferred parking location
4. Park and enjoy!

## Requirements
- Android 5.0 or later
- Active internet connection
- GPS enabled

## Supporting Documents
Documents related to the development of this project are listed below:
- [Software Requirements Document](https://github.com/nickimps/Parking_Application/blob/master/Report%20Files/Software_Requirements_Document.pdf)
- [Software Project Management Plan](https://github.com/nickimps/Parking_Application/blob/master/Report%20Files/Software_Project_Management_Plan_Document.pdf)
- [Software Specifications Document](https://github.com/nickimps/Parking_Application/tree/master/Report%20Files/Software_Specifications_Document.pdf)


