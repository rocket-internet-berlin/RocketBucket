
# RocketBucket

RocketBucket SDK is to let you split your app audiences into buckets base on arbitrary user_id

### Installation

add to gradle file the following line to include the library 
```sh
compile 'de.rocketinternet:android.bucket:0.1'
```
In your Application class, you need to initialize the instance by adding the following line:

```
 RocketBucket.initialize(this, "http://10.24.18.45:8080/split", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", callback, isDebugMode);
``` 

``"http://10.24.18.45:8080/split"`` the endpoint URL provided by server (see server instructions for more) 

``"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"`` is the API_KEY provided by server  (see server instructions for more) 

```callback``` optional callback to get notified when result is retrieved if you interested to log somthing or sending analytics about... etc.

```isDebugMode ```will decide to show debugging floating view to mannually test different buckets without updating server code and restart the application.

> Note: if you are using debug mode you have to initialize RocketBucket on Application class otherwise debugging handle view may not be shown on project activites 

That's it, you now ready to go and Happy Bucketing! , for more example see Sample Project

**pull requests are welcomed**
 
   