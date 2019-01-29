# StupidJson
One more JSON serializer/deserializer for Java.

# Why?
I wrote it overnight since I got sick with having 400K of zipped code (after optimization) attached to all of my packages for no apparent reason. Turns out, that simple JSON serializer now-days is a framework.
There has to be a better simpler way, no?

* No configuration, super-simple to use
* Provide 90% of the features
* No external dependencies
* Single file << 1000 lines of code; compiled to 7k

# Why not?
While it is 50% faster in debug over [GSON](https://github.com/google/gson) or even [JACKSON](https://github.com/FasterXML/jackson) in fully optimized release, they will beat StupidJson by 30-70%. We are talking about millions of iteration in synthetic benchmark, still; if speed is your thing - you better go for those solutions.

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

You can use it either as library (see releases), or just copy-paste the source [HERE](https://github.com/alexportnov/StupidJson/blob/master/StupidJson/src/main/java/com/stupidjson/StupidJson.java)

**Fave fun!**
