# FreeRCT Website

This is the new, under-development, not-yet-official website for [FreeRCT](https://github.com/FreeRCT/FreeRCT/).

It is written in Java 17 using the Spring Boot framework.

## Building

To build the project, you will need a Java compiler (version 17) and Maven:
```
sudo apt install openjdk-17-* maven
```

To compile and run the website locally, type:
```
git clone git@github.com:Noordfrees/freerct-website.git
cd freerct-website
./mvnw spring-boot:run
```
Then open your web browser and visit http://localhost:8080/.

## Configuration

You can override Spring Boot's default properties by adding a `config` file in the toplevel directory of the git checkout.
It's a simple ini-style file with key-value pairs, e.g.:
```
server.port = 9001
```
See [the docs](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html) for available properties.
