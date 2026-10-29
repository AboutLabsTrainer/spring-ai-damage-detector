# Transistor Damage Detection

- A Spring Boot AI tool that is able to identify transistors that do not meet quality requirements, e.g.:
    - misplaced on the board
    - damaged on the board
    - cut lead on the board
    - bent lead on the board

- Good cases should pass, bad cases should be documented with the image and a textual description

- Aimed at QS of the transistor production line to document scrap production

## Initial Assumptions

- For classification we use JEV / Typesafe
- Follow Spring Boot Best Practices
- The project should run in Docker
- Further extensions should be possible (database, microservice)