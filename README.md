# StupidJson
One more JSON serializer/deserializer for Java (Android).

# Why?
I wrote it overnight since I got sick with having 400K of zipped code (after optimization) attached to all of my packages for no apparent reason. Turns out, that simple JSON serializer now-days is a framework.

* No configuration or setup, super-simple to use
* Provide 90% of the features
* No external dependencies
* Single file ~700 lines of code; compiled to 7k

# Why not?
* While it is 50% faster in debug over [GSON](https://github.com/google/gson) or even [JACKSON](https://github.com/FasterXML/jackson) in fully optimized release, they will beat StupidJson by 30-70%. We are talking about millions of iteration in synthetic benchmark, still; if speed is your thing - you better go for those solutions.
* There are some features missing - depending how complicated your code is, you may need feature-full solution.

# How?
To JSON
```java
TestClass c = new TestClass();
String s = StupidJson.toJson(c);
```

From JSON
```java
TestClass c1 = StupidJson.fromJson(s, TestClass.class);
```

More examples can be found [HERE](https://github.com/alexportnov/StupidJson/blob/master/StupidJsonUsageExample/src/main/java/com/stupidjson/example/SimpleActivity.java)


You can use it either as library (see releases), or just copy-paste the source [HERE](https://github.com/alexportnov/StupidJson/blob/master/StupidJson/src/main/java/com/stupidjson/StupidJson.java)


# TODO
* Make the lib pure Java - there's no real reason for it not to be
* Remove dependency to org.json (I got lazy here)


**Fave fun!**
