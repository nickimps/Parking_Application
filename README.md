Department of Software Engineering - Lakehead University
Degree Project 
# Smart Parking Android Application - Link & Park
## Project Members
- Jimmy Tsang
- Nicholas Imperius
- Mason Tommasini

## Description
Many students and staff can attest to problematic parking at Lakehead University. To provide a simple, fast and effective solution to this problem, we have created a smart software application that makes finding a parking spot much easier. The software is implemented as a mobile application for smartphones that displays information such as free parking spaces, busy times, and the location of your parked car. When arriving at the university, it can be difficult to find an open parking spot and frustrating when you are running late. This application would provide a solution to this issue and provide additional benefits, such as saving the user time, reducing unnecessary stress, and helping to reduce the environmental impact of carbon emissions produced by motorized vehicles. Our initial testing and training of this project will be focused on the G campus parking lot at Lakehead University, but could be further expanded to other fields, such as public parking structures and mall parking lots. 

This application will be technologically quite different from other parking applications that exist currently. Instead of relying on sensory data or IP cameras to locate and detect empty parking locations, this application will use machine learning techniques to determine whether you have parked your car or not and then use GPS location and network information to notify other users that the parking space has been filled. Additionally, when you have left your parking spot, our model will be able to accurately detect that and mark the parking space as free. Different machine learning classification methods will need to be tested to ensure that we have chosen the most appropriate model. The machine learning model will predict based on your speed before and after a full stop; it should be able to recognize the difference between walking and driving, which allows it to determine the stopping location.

## Features
- Locate available parking spots closest to your destination on campus
- Find the position of your parked car
- Automatic tracking of position and camera movement to provide a hands-free experience
- Automatic detection of parking so that other users can see the spot has been occupied, no need to specify you have parked
- Storage of parking permits to help parking services

## User Interface
<div style="display:flex">
  <div style="flex:1;padding-right:10px;">
    <img src="/Screen_Captures/Login.png" width="20%">
    <img src="/Screen_Captures/EULA.png" width="20%">
    <img src="/Screen_Captures/register.png" width="20%">
  </div>
  <div style="flex:1;padding-right:10px;">
    <img src="/Screen_Captures/MainMaps.png" width="20%">
    <img src="/Screen_Captures/info_screen.png" width="20%">
    <img src="/Screen_Captures/adminActivity.png" width="20%">
  </div>
</div>

## Getting Started
1. Go to the .apk found within the repo
2. Download the app onto your android phone or tablet
3. Create an account or log in with an existing one
4. Allow application to access your notifications and location
5. Use the Google Maps screen to locate a preferred parking location
6. Park and enjoy!

## Requirements
- Android 7.1 or later
- Active internet connection
- GPS enabled (background use required)
- Notifications enabled

## Supporting Documents
Documents related to the development of this project are listed below:
- [Software Requirements Document](https://github.com/nickimps/Parking_Application/blob/master/Report%20Files/Software_Requirements_Document.pdf)
- [Software Project Management Plan](https://github.com/nickimps/Parking_Application/blob/master/Report%20Files/Software_Project_Management_Plan_Document.pdf)
- [Software Specifications Document](https://github.com/nickimps/Parking_Application/tree/master/Report%20Files/Software_Specifications_Document.pdf)
- [Software Design Document](https://github.com/nickimps/Parking_Application/blob/master/Screen_Captures/Design_Document.pdf)


