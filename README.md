## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

## Thought Process
I split the challenge into two components - the scheduler and the invoice processor.

### Choosing and Implementing a Scheduler - 4hrs
This is my first time using Kotlin and Gradle, my day-to-day development is mostly in Java with Spring, so I had to
familiarize myself with Kotlin and Gradle before starting.

As I do not want to re-invent the wheel I decided to find a scheduler library that I could use.

My go-to scheduler is the Spring @Scheduled since it minimizes the code and is pretty flexible. However, I found that
adding a Spring dependency to multi module Kotlin project is less than ideal just to have access to a scheduler.

My next thought was to go with something Kotlin specific, so I found Krontab but the documentation was limited and the 
project did not have much activity according to GitHub.

Lastly I decided to go for the Quartz scheduler which has good documentation, is widely used and can be easily added
as a dependency.

### Implementing the Invoice Processor
I determined the following requirements based on the description above and the contents of the code:

* Need to get the invoices from the Invoice Table
* Check if the invoice has been paid - don't want to charge it more than once
* Charge the invoice if it has not been paid
* This may throw at least 3 types of exceptions:
  * CustomerNotFoundException - Add the customer to the customer table and try again
  * CurrencyMismatchException - find the currency from the customer table and try again
  * NetworkException - sleep for a bit (configurable?) and try again
* Now since we are retrying, there needs to be some kind of retry counter that we should not exceed, so we don't end up with an endless loop
* If there is no exception thrown then there needs to be a check to see if the customer account was successfully charged
  * If true - update the invoice status with PAID
  * If false - nothing can be done for now? Maybe send an email to the customer in the future or set their account on hold