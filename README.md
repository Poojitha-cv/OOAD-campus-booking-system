# Campus Resource Booking & Conflict Resolution System

A Spring Boot web application for managing campus resource bookings such as **labs, rooms, and auditoriums**. The system provides resource availability search, booking management, conflict detection and resolution, waitlist management, recurring bookings, notifications, ratings, and administrative controls.

The application was designed and implemented as a complete project by **Poojitha CV**, including the backend services, MVC controllers, database integration, frontend views, conflict management, waitlist functionality, admin features, and implementation of GRASP principles and design patterns.

---

## Tech Stack

- **Java 21**
- **Spring Boot 3.5**
- **Spring MVC**
- **Spring Data JPA**
- **Thymeleaf**
- **MySQL**
- **Lombok**
- **Maven**

---

## MVC Architecture

The application follows the **Model-View-Controller (MVC)** architectural pattern using Spring MVC.

| Layer | Implementation |
|---|---|
| **Model** | Booking, Resource, User, WaitlistEntry, Notification, Rating and other JPA entities |
| **View** | Thymeleaf HTML templates such as `search.html`, `booking-form.html`, `conflict.html`, `admin-dashboard.html`, and other application views |
| **Controller** | BookingController, AdminController, WaitlistController, UserController, ResourceController, RatingController, NotificationController |

The MVC structure separates presentation, request handling, and business logic, making the application easier to maintain and extend.

---

# Design Principles (GRASP)

The project applies several **GRASP (General Responsibility Assignment Software Patterns)** principles to improve maintainability, cohesion, and coupling between components.

## Information Expert → BookingService

`BookingService` is responsible for orchestrating the booking process because it works with the information required to create and validate bookings.

It coordinates:

- BookingRepository
- ResourceRepository
- UserRepository
- ConflictDetector

The service handles booking creation, conflict checking, and related booking operations. This follows the **Information Expert** principle by assigning responsibility to the class that has the information required to perform the operation.

---

## Low Coupling → WaitlistService

`WaitlistService` is designed to minimize dependencies between different parts of the application.

It primarily interacts with repositories and the `BookingEventObserver` abstraction rather than directly depending on concrete controllers or unrelated service implementations.

This allows waitlist management, booking cancellation, promotion, and notification handling to evolve independently, reducing the impact of changes across the system.

---

## High Cohesion → ReportService

`ReportService` has a focused responsibility: generating administrative report data.

It handles information such as:

- Total bookings
- Conflict counts
- Most-booked resources
- Bookings per resource

The reporting logic is separated from booking creation, conflict detection, and notification handling. `AdminController` delegates reporting operations to `ReportService` rather than implementing data-gathering logic directly.

This keeps the responsibilities of each class focused and follows the **High Cohesion** principle.

---

# Design Patterns

The project implements multiple object-oriented design patterns to solve specific design problems within the application.

---

## 1. Factory Method Pattern → ResourceFactory / ResourceCreator

**Package:** `factory/`

The Factory Method pattern is used for creating different types of campus resources.

`ResourceCreator` defines the resource creation abstraction, while concrete creator classes implement resource creation for different resource types:

- `LabResourceCreator`
- `RoomResourceCreator`
- `AuditoriumResourceCreator`

`ResourceFactory` selects the appropriate creator based on the requested resource type and delegates the creation process.

This separates object creation logic from the rest of the application and makes it easier to introduce new resource types without significantly modifying existing booking logic.

### Main Classes

```text
ResourceCreator
ResourceFactory
LabResourceCreator
RoomResourceCreator
AuditoriumResourceCreator
```

---

## 2. Observer Pattern → BookingEventObserver / NotificationObserver

**Package:** `observer/`

The Observer pattern is used to handle booking-related events and notifications.

`BookingEventObserver` defines the event notification abstraction for events such as:

- Booking confirmed
- Booking cancelled
- Booking promoted
- Booking conflict detected

`NotificationObserver` acts as a concrete observer and persists notification records in the database.

Booking-related services can trigger events without being tightly coupled to the implementation details of notification delivery.

### Main Classes

```text
BookingEventObserver
NotificationObserver
Notification
NotificationRepository
```

This design allows additional observers or event-handling mechanisms to be introduced in the future without significantly changing the core booking logic.

---

## 3. Strategy Pattern → ConflictResolutionStrategy

**Package:** `strategy/`

The Strategy pattern is used to support multiple conflict-resolution approaches.

`ConflictResolutionStrategy` defines a common interface for selecting or resolving booking conflicts.

The project implements three strategies:

### FCFS Resolution

`FcfsResolutionStrategy`

Selects the earliest eligible waitlist entry based on **First Come First Served (FCFS)** ordering.

### Priority Resolution

`PriorityResolutionStrategy`

Resolves conflicts based on user role priority, such as:

```text
Faculty > Admin > Student
```

### Alternate Room Resolution

`AlternateRoomResolutionStrategy`

Attempts to identify an available resource of the same type and reassign the booking when possible.

The strategy can be selected at runtime, allowing the conflict-resolution algorithm to change without modifying the core booking and waitlist logic.

### Main Classes

```text
ConflictResolutionStrategy
FcfsResolutionStrategy
PriorityResolutionStrategy
AlternateRoomResolutionStrategy
```

---

## 4. Decorator Pattern → RecurringBookingDecorator

**Package:** `decorator/`

The Decorator pattern is used to extend the booking service with recurring booking functionality.

`RecurringBookingDecorator` wraps the existing `BookingService` and adds the ability to create recurring bookings without modifying the underlying booking service implementation.

For a recurring booking, the decorator:

1. Receives the booking details.
2. Creates the initial booking.
3. Shifts the booking dates by seven days.
4. Repeats the booking process for the requested number of weeks.
5. Uses the existing booking service for each individual booking.

Because the original `BookingService` remains unchanged, existing conflict detection and notification behavior can continue to operate for each booking.

### Main Classes

```text
RecurringBookingDecorator
BookingService
```

---

# Key Features

## User Features

- User registration and login
- Session management
- Role-based access
- Search for campus resources
- View resource availability
- Calendar-based booking interface
- Create bookings
- View booking confirmation
- Cancel bookings
- Join waitlists
- Receive booking notifications
- Rate resources
- View resource ratings

---

## Booking and Conflict Management

- Resource availability checking
- Booking overlap detection
- Conflict identification
- Conflict outcome handling
- Winner and loser booking outcomes
- Multiple conflict-resolution strategies
- FCFS conflict resolution
- Priority-based conflict resolution
- Alternate-room resolution
- Waitlist management
- Automatic waitlist promotion

---

## Recurring Bookings

The system supports recurring bookings using the **Decorator design pattern**.

Administrators can create bookings that repeat weekly for a specified number of weeks.

Each generated booking is processed through the existing booking workflow, allowing normal validation and conflict detection to be applied.

---

## Notification System

The application implements an **Observer-based notification system**.

Notifications are generated for important booking events, including:

- Booking confirmation
- Booking cancellation
- Waitlist promotion
- Booking conflict

Notifications are persisted in the database and can be accessed through the notification interface.

---

## Rating and Feedback

Users can submit ratings for resources after completing bookings.

The system provides:

- Resource rating submission
- Rating persistence
- Resource rating retrieval
- Average rating information

---

## Admin Features

The administrative dashboard provides functionality for:

- Viewing booking statistics
- Managing campus resources
- Adding resources
- Editing resources
- Deleting resources
- Viewing reports
- Resolving conflicts manually
- Selecting conflict winners
- Managing recurring bookings
- Viewing booking and resource information

---

# Project Structure

```text
src/main/java/com/campus/booking/
│
├── controller/       # MVC Controllers
│
├── service/          # Business logic
│   ├── BookingService
│   ├── WaitlistService
│   ├── ReportService
│   └── Other Services
│
├── model/            # JPA Entities
│
├── repository/       # Spring Data JPA Repositories
│
├── factory/          # Factory Method Pattern
│
├── observer/         # Observer Pattern
│
├── strategy/         # Strategy Pattern
│
├── decorator/        # Decorator Pattern
│
└── config/           # Application configuration and data loading
```

```text
src/main/resources/
│
├── templates/        # Thymeleaf HTML Views
│
└── application.properties
```

---

# Design Overview

The overall application architecture can be summarized as:

```text
                         ┌─────────────────────┐
                         │       Users         │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │   Spring MVC        │
                         │   Controllers       │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │     Services        │
                         │ BookingService      │
                         │ WaitlistService     │
                         │ ReportService       │
                         └──────────┬──────────┘
                                    │
                 ┌──────────────────┼──────────────────┐
                 │                  │                  │
                 ▼                  ▼                  ▼
          ┌────────────┐    ┌──────────────┐   ┌──────────────┐
          │  Factory   │    │   Strategy   │   │  Decorator   │
          │  Pattern   │    │   Pattern    │   │   Pattern    │
          └────────────┘    └──────────────┘   └──────────────┘
                 │                  │                  │
                 └──────────────────┼──────────────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │     Repositories    │
                         │   Spring Data JPA   │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │       MySQL         │
                         └─────────────────────┘

                         Observer Pattern
                                │
                                ▼
                         ┌─────────────────────┐
                         │   Notifications     │
                         └─────────────────────┘
```

---

# Implementation Highlights

The project demonstrates practical application of:

- Object-Oriented Design
- SOLID-oriented design practices
- GRASP principles
- MVC architecture
- Factory Method Pattern
- Observer Pattern
- Strategy Pattern
- Decorator Pattern
- Service Layer Architecture
- Repository Pattern through Spring Data JPA
- Role-based access control
- Database-driven application development

The design separates responsibilities across controllers, services, repositories, and domain models while using design patterns where they provide clear architectural value.

---

# Setup & Run

## Prerequisites

Make sure the following are installed:

- Java 21
- Maven
- MySQL

---

## 1. Create the Database

Open MySQL and execute:

```sql
CREATE DATABASE campus_booking;
```

---

## 2. Configure Database Credentials

Update:

```text
src/main/resources/application.properties
```

with your MySQL credentials.

Example:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/campus_booking
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.globally_quoted_identifiers=true
```

---

## 3. Run the Application

From the project root directory, run:

```bash
mvn spring-boot:run
```

---

## 4. Access the Application

Open:

```text
http://localhost:8080
```

The default administrator account is seeded by `DataLoader` during the initial application startup.

---

# Author

**Poojitha CV**

This project was designed and implemented as a complete **Campus Resource Booking and Conflict Resolution System**, demonstrating practical application of Spring Boot, MVC architecture, database integration, GRASP principles, and object-oriented design patterns.
