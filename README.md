# MuSe

One important issue in the development of recommender systems is a thorough and significant evaluation of the proposed algorithm. However, some aspects of recommending music are inherently subjective, such as serendipity, and thus the evaluation of such algorithms can only be done in vivo, i.e. with real users not in an artificial environment.

Therefore we developed a system that takes care of all regular tasks that are involved in conducting such evaluations. MuSe provides the typical off-the-shelf evaluation algorithms, offers an online evaluation system with automatic reporting, and by integrating online streaming services (eg. Spotify) also a legal possibility to evaluate the quality of recommended songs in real time. In addition evaluations can be configured and managed online.

Recommender algorithms can be added to the system internally, or plugged in comfortably by using our API.

# Get Started
### Step 0. Requirements
   You will need Java 6+, a servlet container (eg. Tomcat) and a database to run the system. Download the current release [here](https://github.com/gausss/MuSe/releases).
   
### Step 1. Configuration

   There are 3 property files included. To configure [Log4j](http://logging.apache.org/log4j/) and [Quartz](http://www.quartz-scheduler.org/documentation) take a look at their docs.
   
   To configure the application open the file  _app.properties_ in the src directory and provide the missing information, inlcuding a permanent directory which can be **read and written** by the application. Finally move the file  _recommenders.json_ to the specified directory.

### Step 2. Database schema

   The application requires access to a certain table structure in your database. The DDL is included in the file _DB_INIT.sql_. Simply import it to the database given in _app.properties_ from before. This will in addition create an initial administrator account (username: admin, password: admin). Note that the init script and the queries of the application are written in the oracle flavor of SQL and problems may arise by using another RDBMS.
   
For further explanations take a look at our [wiki](https://github.com/gausss/MuSe/wiki).
   
   
# Abous Us

MuSe is a [research project](http://dbis.informatik.uni-freiburg.de/forschung/projekte/MusicRecommender) at the department of computer science, University of Freiburg. A reference implementation of MuSe is [available online](https://muse.informatik.uni-freiburg.de).

