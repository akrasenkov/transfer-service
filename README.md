## Funds Transfer Service sample

**Version: 1.0**

Sample funds transfer service implementation. 
Features:
* saving payment accounts
* fetching payment accounts
* funds transfer between accounts

[RESTful API reference](../master/apidoc.yml)

### Libraries and frameworks used

* SparkJava 2.7.2 (Micro framework for Web apps)
* Google Guava 26.0 (Java Essentials)
* Google Guice 4.2.1 (Dependency Injection)
* Google Gson 2.8.5 (JSON serialization/deserialization)
* Lombok 1.18.2 (Boilerplate code reducing)

**For tests:**
* JUnit Jupiter 5.3.1 (Unit tests)
* Google Truth 0.42 (Fluent test assertions)
* Retrofit 2.4.0 (Type-safe HTTP client)

### Build and run

1. Clone repository to any directory
2. Execute `./gradlew clean shadowJar`
3. Executable jar is located at `./build/libs/transfer-service-1.0.jar`. 
4. To run application execute `java -jar transfer-service-1.0.jar 8081`. You can replace `8081` with any free port you prefer.

*TIP:* To run tests with gradle, execute `./gradlew clean test`