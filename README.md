# MRMS

One important issue in the development of recommender systems is a thorough and significant evaluation of the proposed algorithm. However, some aspects of recommending music are inherently subjective, such as serendipity, and thus the evaluation of such algorithms can only be done in vivo, i.e. with real users not in an artificial environment.

Therefore we developed a system that takes care of all regular tasks that are involved in conducting such evaluations. MRMS provides the typical off-the-shelf evaluation algorithms, offers an online evaluation system with automatic reporting, and by integrating online streaming services (eg. Spotify) also a legal possibility to evaluate the quality of recommended songs in real time. In addition evaluations can be configured and managed online.

Recommender algorithms can be added to the system internally, or plugged in comfortably by using our API.

# Get Started
### Step 0. Configuration
   You will need Java 6+, a servlet container (eg. Tomcat) and a database (eg. MySQL) to run the system.
   
### Step 1. Configuration

   Open the file  _app.properties_ in the src directory and provide the missing information. Then Move the file  _recommenders.json_  to the directory specified in the _perm_directory_ property. At last adapt the _log4j.xml_ configuration according to your needs.

### Step 2. Database schema

   The application requires access to a certain table structure in your specified database. The DDL is included in the file _DB`_`INIT.sql_. Simply import it to the database given in _app.properties_ from before. This will in addition create an initial administrator account (username: admin, password: admin).
   
   
# Abous Us

MRMS is a [research project](http://dbis.informatik.uni-freiburg.de/forschung/projekte/MusicRecommender) at the department of computer science, University of Freiburg. [MuSe](https://muse.informatik.uni-freiburg.de), a reference implementation of MRMS is available online.

