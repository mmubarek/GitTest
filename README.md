# Overview
This project is developed to practice the software develpment process from implementation to deployment.

## Maven compile 
 mvn clean compile   
## Run  
 mvn test     
## Package Application 
 mvn package 
## Run Security Test 
 mvn spotbugs:check

---

# Lecture Demo: Incremental Testing
This repository is configured for the **Testing** lecture.

### 1. Run Basic & Incremental Tests
Run the functional unit tests and the new incremental performance/robustness tests:
```bash
cd simple-token-validator
mvn test
```
*Note: Look for `TokenServiceIncrementalTest` in the output.*

### 2. Static Security Scan (SAST)
Check for hardcoded secrets and common vulnerabilities:
```bash
mvn spotbugs:check
```
*Check `target/spotbugsXml.xml` or run `mvn spotbugs:gui` if you have a display.*

### 3. Dependency Check (SCA)
Check if our libraries have known vulnerabilities:
```bash
mvn dependency-check:check
```

### 4. Continuous Integration
Every push to GitHub triggers the `.github/workflows/ci.yml`, which runs all the above automatically.

