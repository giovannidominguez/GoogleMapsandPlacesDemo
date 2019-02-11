On initial start up of the app. The app might throw a null
exception because it needs permission to get the current
location. Simply accept the permission and the app
will run. 
Current location is marked by a blue marker. Problems may arise with the gps of
the actual device and the spoofer which is why the 
marker may seem to be blinking.

In order to get the markers to associate with the POI,
the Places API was used. 

Getting the geofences and actual notifications to work were very difficult.
I used the following tutorial to assist me. http://www.coderzheaven.com/2016/06/20/geofencing-in-android-a-simple-example/
The notifications dont pop up but are there in the background and can be seen
when you pull down the notification bar and will update. I was unable to associate 
the POI with the notification. 

