*Note that the app will not compile "as-is" because I have not committed my Flickr API keys to this repo.*

### What is this

This is my test assignment for [komoot](https://www.komoot.de/). The task was as follows: 
> **Develop an Android app that enables you to track your walk with images.**
> The user opens the app and presses the start button. After that the user puts their phone into their pocket and starts walking. The app requests a photo from the public flickr photo search api for his location every 100 meters to add to the stream. New pictures are added on top. Whenever the user takes a look at their phone, they see the most recent picture and can scroll through a stream of pictures which shows where the user has been. It should work for at least a two-hour walk.

### Implementation details

After the user starts a walk, a foreground service is launched. It tracks the user's location and retrieves photos along the way and it won't pause until explicitly stopped — even when the activity is destroyed. (In Android terms, the Service is **foreground, started and bound**). This is done for the service to live for as long as possible. In order to test the app, I went for a three-hour walk with my Pixel 4a; the service survived for that long. (Unfortunately, I forgot to save screenshots.)

After the user presses the stop button, the walk is erased.
