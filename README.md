# EmailProcessor
Main Project for Email Processing

mvn compile exec:java -Dexec.mainClass="com.mycompany.App"

schwabdev-java/
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── com
│   │           └── mycompany
│   │               └── schwabdev
│   │                   ├── TokenManager.java
│   │                   ├── SchwabClient.java
│   │                   ├── Streamer.java
│   │                   └── examples
│   │                       └── Main.java
│   └── test
│       └── java
│           └── com
│               └── mycompany
│                   └── schwabdev
│                       └── ExampleTest.java



Native GraalVM

sdk list java

 21.0.8       | graal   | installed  | 21.0.8-graal

export PATH=/usr/local/sdkman/candidates/java/21.0.8-graal/bin:$PATH


Removed from pom.xml
            <!-- GraalVM Native Image -->
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <version>0.10.2</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile-no-fork</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <imageName>schwabdev-java</imageName>
                    <mainClass>com.mycompany.schwabdev.examples.Main</mainClass>
                    <buildArgs>
                        <!-- Enable reflection for Jackson -->
                        --initialize-at-build-time=com.fasterxml.jackson
                        --report-unsupported-elements-at-runtime
                        --no-fallback
                    </buildArgs>
                </configuration>
            </plugin>
