Required software
1) JDK 1.8
2) Maven to build the project

Steps to run the application
1) Build the project using maven
	mvn clean install
2) Boot up the application (run the command from the location where the application is downloaded)
    mvn spring-boot:run
    
Rest Endpoints to test

1) Retrieve the number of seats available
http://localhost:8080/ticket-reservation/seatsAvailable
HTTP METHOD : GET

2) Find and hold a seat (allocates the number of seats to the customerEmail)
http://localhost:8080/ticket-reservation/find-and-hold?numSeats=5&customerEmail=ryan.rodrigues@outlook.com
HTTP METHOD : POST
HTTP BODY : empty
Response code : 201 
RESPONSE BODY : Json response with the seatHoldId , seats on hold and the url link to reserveseats.

3) Reserve a seat
http://localhost:8080/ticket-reservation/reserveSeats?seatHoldId=1&customerEmail=ryan.rodrigues@outlook.com
HTTP METHOD : PUT
HTTP BODY : empty
HTTP Response code : 200 successful response
Response Body : JSon Response with reservation ID and msg indicating successful reserved.
HTTP Response Code : 404 if seatHoldId and the email address do not match.


Additional Endpoints
Incase the application need to reset the DB , use the below REST Endpoint :
http://localhost:8080/ticket-reservation/resetReservation - PUT

Status details for each seat in the DB
http://localhost:8080/ticket-reservation/showSeatStatus

Assumptions 

1) The application on startup loads the HSQL DB with 30 seats (3 rows with 10 seats). Incase more seats need to added update the insert-data.sql with insert statements to SEAT_DET and SEAT_ALLOCATION table. 

2) Design of the application and the DB is based on ticket reservation for no more than 1 show and 1 location. 



Default Configuration

1) Session expiration time is 60 seconds (1 minute) as defined in the application.properties.
The number of retries to HOLD a seat is 3 after which the request will fail.
 


