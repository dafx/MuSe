--------------------------------------------------------
--  DDL for Table ACTIVITIES
--------------------------------------------------------

  CREATE TABLE "MUSE"."ACTIVITIES" 
   (	"CONSUMER_NAME" VARCHAR2(200), 
	"DURATION" NUMBER(*,2), 
	"SITE" VARCHAR2(100), 
	"TIMESTAMP" TIMESTAMP (6)
   ) ;
--------------------------------------------------------
--  DDL for Table CHARTS_CITY
--------------------------------------------------------

  CREATE TABLE "MUSE"."CHARTS_CITY" 
   (	"TRACKPOSITION" NUMBER, 
	"CHARTSCITY" VARCHAR2(200), 
	"TRACK_ID" NUMBER, 
	"CHARTSCOUNTRY" VARCHAR2(200)
   ) ;
--------------------------------------------------------
--  DDL for Table CHARTS_NEIGHBOR
--------------------------------------------------------

  CREATE TABLE "MUSE"."CHARTS_NEIGHBOR" 
   (	"TRACKPOSITION" NUMBER, 
	"TRACKPLAYCOUNT" NUMBER, 
	"CHARTSUSER" VARCHAR2(200), 
	"TRACK_ID" NUMBER, 
	"NEIGHBOR" VARCHAR2(200)
   ) ;
--------------------------------------------------------
--  DDL for Table CHARTS_REGION
--------------------------------------------------------

  CREATE TABLE "MUSE"."CHARTS_REGION" 
   (	"TRACKPOSITION" NUMBER, 
	"CHARTSREGION" VARCHAR2(200), 
	"TRACK_ID" NUMBER
   ) ;
--------------------------------------------------------
--  DDL for Table CHARTS_TAG
--------------------------------------------------------

  CREATE TABLE "MUSE"."CHARTS_TAG" 
   (	"TRACKPOSITION" NUMBER, 
	"CHARTSUSER" VARCHAR2(200), 
	"CHARTSTAG" VARCHAR2(200), 
	"TRACK_ID" NUMBER
   ) ;
--------------------------------------------------------
--  DDL for Table CHARTS_YEAR
--------------------------------------------------------

  CREATE TABLE "MUSE"."CHARTS_YEAR" 
   (	"TRACKPOSITION" NUMBER, 
	"CHARTSYEAR" NUMBER, 
	"TRACK_ID" NUMBER
   ) ;
--------------------------------------------------------
--  DDL for Table CONSUMER
--------------------------------------------------------

  CREATE TABLE "MUSE"."CONSUMER" 
   (	"NAME" VARCHAR2(100), 
	"PASSWORD" VARCHAR2(100), 
	"BIRTHYEAR" NUMBER, 
	"SEX" VARCHAR2(50), 
	"LFMACCOUNT" VARCHAR2(100), 
	"LIST" NUMBER DEFAULT 0, 
	"ROLE" VARCHAR2(200), 
	"EMAIL" VARCHAR2(200), 
	"REGISTRATION_DATE" DATE, 
	"NEWCOMER" CHAR(1 CHAR), 
	"EVAL_PARTICIPANT" CHAR(1 CHAR)
   ) ;

Insert into CONSUMER (NAME,PASSWORD,BIRTHYEAR,SEX,LFMACCOUNT,LIST,ROLE,EMAIL,REGISTRATION_DATE,NEWCOMER,EVAL_PARTICIPANT) values ('admin','$2a$10$g6JeJxomEayChXIvexM.F.ApC0aXhLRjZjPrX8GVeV0CLd/dD02Zu','1990','Male','admin','0','admin','admi@mrms.de',to_date('14.10.13','DD.MM.RR'),'Y',null);
--------------------------------------------------------
--  DDL for Table CONSUMER_LANGUAGE
--------------------------------------------------------

  CREATE TABLE "MUSE"."CONSUMER_LANGUAGE" 
   (	"CONSUMER_NAME" VARCHAR2(100), 
	"LANGUAGE" VARCHAR2(100)
   ) ;
--------------------------------------------------------
--  DDL for Table CONSUMER_OPTIONS
--------------------------------------------------------

  CREATE TABLE "MUSE"."CONSUMER_OPTIONS" 
   (	"CONSUMER_NAME" VARCHAR2(200), 
	"BEHAVIOR" VARCHAR2(20), 
	"RECOMMENDERS" VARCHAR2(200)
   ) ;
--------------------------------------------------------
--  DDL for Table EVALUATION
--------------------------------------------------------

  CREATE TABLE "MUSE"."EVALUATION" 
   (	"ID" NUMBER, 
	"CREATOR" VARCHAR2(200), 
	"START_DATE" DATE, 
	"END_DATE" DATE, 
	"COMPOSITION" VARCHAR2(200), 
	"CREATION_DATE" DATE, 
	"NAME" VARCHAR2(30 CHAR)
   ) ;
--------------------------------------------------------
--  DDL for Table EVALUATION_GROUPS
--------------------------------------------------------

  CREATE TABLE "MUSE"."EVALUATION_GROUPS" 
   (	"EVAL_ID" NUMBER, 
	"GROUP_NUM" NUMBER, 
	"SETTINGS_BEHAVIOR" VARCHAR2(200), 
	"SETTINGS_RECOMMENDERS" VARCHAR2(200)
   ) ;
--------------------------------------------------------
--  DDL for Table EVALUATION_PARTICIPANTS
--------------------------------------------------------

  CREATE TABLE "MUSE"."EVALUATION_PARTICIPANTS" 
   (	"EVAL_ID" NUMBER, 
	"GROUP_ID" NUMBER, 
	"PARTICIPANT" VARCHAR2(200), 
	"JOIN_DATE" DATE, 
	"QUIT_DATE" DATE
   ) ;
--------------------------------------------------------
--  DDL for Table LANGUAGE_COUNTRY
--------------------------------------------------------

  CREATE TABLE "MUSE"."LANGUAGE_COUNTRY" 
   (	"LANGUAGE" VARCHAR2(100), 
	"COUNTRY" VARCHAR2(100)
   ) ;
--------------------------------------------------------
--  DDL for Table LOGIN_ACTIVITIES
--------------------------------------------------------

  CREATE TABLE "MUSE"."LOGIN_ACTIVITIES" 
   (	"CONSUMER_NAME" VARCHAR2(200), 
	"DAY" DATE, 
	"TIME" TIMESTAMP (6)
   ) ;
--------------------------------------------------------
--  DDL for Table OPTION_ACTIVITIES
--------------------------------------------------------

  CREATE TABLE "MUSE"."OPTION_ACTIVITIES" 
   (	"CONSUMER_NAME" VARCHAR2(200), 
	"TIMESTAMP" DATE, 
	"BEHAVIOR" VARCHAR2(200), 
	"RECOMMENDERS" VARCHAR2(200)
   ) ;
--------------------------------------------------------
--  DDL for Table PASSWORD_RECOVERY
--------------------------------------------------------

  CREATE TABLE "MUSE"."PASSWORD_RECOVERY" 
   (	"KEY" VARCHAR2(200), 
	"CONSUMER_NAME" VARCHAR2(200), 
	"TIMESTAMP" DATE
   ) ;
--------------------------------------------------------
--  DDL for Table RECOMMENDATION
--------------------------------------------------------

  CREATE TABLE "MUSE"."RECOMMENDATION" 
   (	"ID" NUMBER, 
	"TIME" TIMESTAMP (6), 
	"CONSUMER" VARCHAR2(200), 
	"RATING" NUMBER, 
	"LIST" NUMBER, 
	"RECOMMENDER_ID" NUMBER, 
	"EXPLANATION" VARCHAR2(300), 
	"SCORE" NUMBER(*,2), 
	"TRACK_ID" NUMBER, 
	"EVAL_ID" NUMBER
   ) ;
--------------------------------------------------------
--  DDL for Table RECOMMENDATION_LIST
--------------------------------------------------------

  CREATE TABLE "MUSE"."RECOMMENDATION_LIST" 
   (	"CONSUMER" VARCHAR2(200), 
	"LIST_ID" NUMBER, 
	"RATING" NUMBER, 
	"CREATION_DATE" TIMESTAMP (6)
   ) ;
--------------------------------------------------------
--  DDL for Table TRACK_SIMILARITIES
--------------------------------------------------------

  CREATE TABLE "MUSE"."TRACK_SIMILARITIES" 
   (	"TRACK_ID_1" NUMBER, 
	"TRACK_ID_2" NUMBER, 
	"SIMILARITY" NUMBER
   ) ;
--------------------------------------------------------
--  DDL for Table TRACK_TAGS
--------------------------------------------------------

  CREATE TABLE "MUSE"."TRACK_TAGS" 
   (	"TRACK_ID" NUMBER, 
	"TAG" VARCHAR2(200), 
	"COUNT" NUMBER
   ) ;
--------------------------------------------------------
--  DDL for Table TRACKS
--------------------------------------------------------

  CREATE TABLE "MUSE"."TRACKS" 
   (	"ID" NUMBER, 
	"NAME" VARCHAR2(200), 
	"ARTIST" VARCHAR2(200), 
	"MBID" VARCHAR2(36 CHAR)
   ) ;
--------------------------------------------------------
--  DDL for Table USER_SIMILARITIES
--------------------------------------------------------

  CREATE TABLE "MUSE"."USER_SIMILARITIES" 
   (	"USER_1" VARCHAR2(300), 
	"USER_2" VARCHAR2(300), 
	"SIMILARITY" NUMBER
   ) ;
--------------------------------------------------------
--  DDL for Table USER_TRACK_SCORE
--------------------------------------------------------

  CREATE TABLE "MUSE"."USER_TRACK_SCORE" 
   (	"USER_NAME" VARCHAR2(200), 
	"TRACK_ID" NUMBER, 
	"SCORE" NUMBER, 
	"EXPLANATION" VARCHAR2(300)
   ) ;
--------------------------------------------------------
--  DDL for Table USER_USER_SCORE
--------------------------------------------------------

  CREATE TABLE "MUSE"."USER_USER_SCORE" 
   (	"USER_NAME" VARCHAR2(200), 
	"TRACK_ID" VARCHAR2(36), 
	"SCORE" NUMBER, 
	"EXPLANATION" VARCHAR2(300)
   ) ;
--------------------------------------------------------
--  DDL for Index CONSUMER_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."CONSUMER_PK" ON "MUSE"."CONSUMER" ("NAME") 
  ;
--------------------------------------------------------
--  DDL for Index EVALUATION_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."EVALUATION_PK" ON "MUSE"."EVALUATION" ("ID") 
  ;
--------------------------------------------------------
--  DDL for Index EVALUATION_GROUPS_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."EVALUATION_GROUPS_PK" ON "MUSE"."EVALUATION_GROUPS" ("EVAL_ID", "GROUP_NUM") 
  ;
--------------------------------------------------------
--  DDL for Index PASSWORD_RECOVERY_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."PASSWORD_RECOVERY_PK" ON "MUSE"."PASSWORD_RECOVERY" ("KEY") 
  ;
--------------------------------------------------------
--  DDL for Index RECOMMENDATION_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."RECOMMENDATION_PK" ON "MUSE"."RECOMMENDATION" ("ID") 
  ;
--------------------------------------------------------
--  DDL for Index RECOMMENDATION_LIST_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."RECOMMENDATION_LIST_PK" ON "MUSE"."RECOMMENDATION_LIST" ("CONSUMER", "LIST_ID") 
  ;
--------------------------------------------------------
--  DDL for Index TRACKS_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."TRACKS_PK" ON "MUSE"."TRACKS" ("ID") 
  ;
--------------------------------------------------------
--  Constraints for Table CONSUMER
--------------------------------------------------------

  ALTER TABLE "MUSE"."CONSUMER" ADD CONSTRAINT "CONSUMER_PK" PRIMARY KEY ("NAME")
  USING INDEX  ENABLE;
  ALTER TABLE "MUSE"."CONSUMER" MODIFY ("NAME" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table EVALUATION
--------------------------------------------------------

  ALTER TABLE "MUSE"."EVALUATION" ADD CONSTRAINT "EVALUATION_PK" PRIMARY KEY ("ID")
  USING INDEX  ENABLE;
  ALTER TABLE "MUSE"."EVALUATION" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table EVALUATION_GROUPS
--------------------------------------------------------

  ALTER TABLE "MUSE"."EVALUATION_GROUPS" ADD CONSTRAINT "EVALUATION_GROUPS_PK" PRIMARY KEY ("EVAL_ID", "GROUP_NUM")
  USING INDEX  ENABLE;
  ALTER TABLE "MUSE"."EVALUATION_GROUPS" MODIFY ("EVAL_ID" NOT NULL ENABLE);
  ALTER TABLE "MUSE"."EVALUATION_GROUPS" MODIFY ("GROUP_NUM" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table EVALUATION_PARTICIPANTS
--------------------------------------------------------

  ALTER TABLE "MUSE"."EVALUATION_PARTICIPANTS" MODIFY ("EVAL_ID" NOT NULL ENABLE);
  ALTER TABLE "MUSE"."EVALUATION_PARTICIPANTS" MODIFY ("GROUP_ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table PASSWORD_RECOVERY
--------------------------------------------------------

  ALTER TABLE "MUSE"."PASSWORD_RECOVERY" ADD CONSTRAINT "PASSWORD_RECOVERY_PK" PRIMARY KEY ("KEY")
  USING INDEX  ENABLE;
  ALTER TABLE "MUSE"."PASSWORD_RECOVERY" MODIFY ("KEY" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table RECOMMENDATION
--------------------------------------------------------

  ALTER TABLE "MUSE"."RECOMMENDATION" ADD CONSTRAINT "RECOMMENDATION_PK" PRIMARY KEY ("ID")
  USING INDEX  ENABLE;
  ALTER TABLE "MUSE"."RECOMMENDATION" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table RECOMMENDATION_LIST
--------------------------------------------------------

  ALTER TABLE "MUSE"."RECOMMENDATION_LIST" ADD CONSTRAINT "RECOMMENDATION_LIST_PK" PRIMARY KEY ("CONSUMER", "LIST_ID")
  USING INDEX  ENABLE;
  ALTER TABLE "MUSE"."RECOMMENDATION_LIST" MODIFY ("CONSUMER" NOT NULL ENABLE);
  ALTER TABLE "MUSE"."RECOMMENDATION_LIST" MODIFY ("LIST_ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table TRACKS
--------------------------------------------------------

  ALTER TABLE "MUSE"."TRACKS" MODIFY ("ID" NOT NULL ENABLE);
  ALTER TABLE "MUSE"."TRACKS" ADD CONSTRAINT "TRACKS_PK" PRIMARY KEY ("ID")
  USING INDEX  ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table CHARTS_CITY
--------------------------------------------------------

  ALTER TABLE "MUSE"."CHARTS_CITY" ADD CONSTRAINT "CHARTS_CITY_TRACKS_FK1" FOREIGN KEY ("TRACK_ID")
	  REFERENCES "MUSE"."TRACKS" ("ID") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table CHARTS_NEIGHBOR
--------------------------------------------------------

  ALTER TABLE "MUSE"."CHARTS_NEIGHBOR" ADD CONSTRAINT "CHARTS_NEIGHBOR_TRACKS_FK1" FOREIGN KEY ("TRACK_ID")
	  REFERENCES "MUSE"."TRACKS" ("ID") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table CHARTS_REGION
--------------------------------------------------------

  ALTER TABLE "MUSE"."CHARTS_REGION" ADD CONSTRAINT "CHARTS_REGION_TRACKS_FK1" FOREIGN KEY ("TRACK_ID")
	  REFERENCES "MUSE"."TRACKS" ("ID") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table CHARTS_TAG
--------------------------------------------------------

  ALTER TABLE "MUSE"."CHARTS_TAG" ADD CONSTRAINT "CHARTS_TAG_TRACKS_FK1" FOREIGN KEY ("TRACK_ID")
	  REFERENCES "MUSE"."TRACKS" ("ID") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table CHARTS_YEAR
--------------------------------------------------------

  ALTER TABLE "MUSE"."CHARTS_YEAR" ADD CONSTRAINT "CHARTS_YEAR_TRACKS_FK1" FOREIGN KEY ("TRACK_ID")
	  REFERENCES "MUSE"."TRACKS" ("ID") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table CONSUMER_LANGUAGE
--------------------------------------------------------

  ALTER TABLE "MUSE"."CONSUMER_LANGUAGE" ADD CONSTRAINT "CONSUMER_NAME" FOREIGN KEY ("CONSUMER_NAME")
	  REFERENCES "MUSE"."CONSUMER" ("NAME") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table CONSUMER_OPTIONS
--------------------------------------------------------

  ALTER TABLE "MUSE"."CONSUMER_OPTIONS" ADD CONSTRAINT "CONSUMER_OPTIONS_CONSUMER_FK1" FOREIGN KEY ("CONSUMER_NAME")
	  REFERENCES "MUSE"."CONSUMER" ("NAME") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table EVALUATION_GROUPS
--------------------------------------------------------

  ALTER TABLE "MUSE"."EVALUATION_GROUPS" ADD CONSTRAINT "EVALUATION_GROUPS_EVALUAT_FK1" FOREIGN KEY ("EVAL_ID")
	  REFERENCES "MUSE"."EVALUATION" ("ID") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table EVALUATION_PARTICIPANTS
--------------------------------------------------------

  ALTER TABLE "MUSE"."EVALUATION_PARTICIPANTS" ADD CONSTRAINT "EVALUATION_GROUPS_PARTICI_FK1" FOREIGN KEY ("EVAL_ID", "GROUP_ID")
	  REFERENCES "MUSE"."EVALUATION_GROUPS" ("EVAL_ID", "GROUP_NUM") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table RECOMMENDATION
--------------------------------------------------------

  ALTER TABLE "MUSE"."RECOMMENDATION" ADD CONSTRAINT "RECOMMENDATION_EVALUATION_FK1" FOREIGN KEY ("EVAL_ID")
	  REFERENCES "MUSE"."EVALUATION" ("ID") ON DELETE SET NULL ENABLE;
  ALTER TABLE "MUSE"."RECOMMENDATION" ADD CONSTRAINT "RECOMMENDATION_TRACKS_FK1" FOREIGN KEY ("TRACK_ID")
	  REFERENCES "MUSE"."TRACKS" ("ID") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table TRACK_SIMILARITIES
--------------------------------------------------------

  ALTER TABLE "MUSE"."TRACK_SIMILARITIES" ADD CONSTRAINT "TRACK_SIMILARITIES_TRACKS_FK1" FOREIGN KEY ("TRACK_ID_1")
	  REFERENCES "MUSE"."TRACKS" ("ID") ON DELETE CASCADE ENABLE;
  ALTER TABLE "MUSE"."TRACK_SIMILARITIES" ADD CONSTRAINT "TRACK_SIMILARITIES_TRACKS_FK2" FOREIGN KEY ("TRACK_ID_2")
	  REFERENCES "MUSE"."TRACKS" ("ID") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table TRACK_TAGS
--------------------------------------------------------

  ALTER TABLE "MUSE"."TRACK_TAGS" ADD CONSTRAINT "TRACK_TAGS_TRACKS_FK1" FOREIGN KEY ("TRACK_ID")
	  REFERENCES "MUSE"."TRACKS" ("ID") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table USER_SIMILARITIES
--------------------------------------------------------

  ALTER TABLE "MUSE"."USER_SIMILARITIES" ADD CONSTRAINT "USER_SIMILARITIES_CONSUME_FK1" FOREIGN KEY ("USER_1")
	  REFERENCES "MUSE"."CONSUMER" ("NAME") ON DELETE CASCADE ENABLE;
  ALTER TABLE "MUSE"."USER_SIMILARITIES" ADD CONSTRAINT "USER_SIMILARITIES_CONSUME_FK2" FOREIGN KEY ("USER_2")
	  REFERENCES "MUSE"."CONSUMER" ("NAME") ON DELETE CASCADE ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table USER_TRACK_SCORE
--------------------------------------------------------

  ALTER TABLE "MUSE"."USER_TRACK_SCORE" ADD CONSTRAINT "USER_TRACK_SCORE_TRACKS_FK1" FOREIGN KEY ("TRACK_ID")
	  REFERENCES "MUSE"."TRACKS" ("ID") ON DELETE CASCADE ENABLE;

--------------------------------------------------------
--  DDL for Index CONSUMER_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."CONSUMER_PK" ON "MUSE"."CONSUMER" ("NAME") 
  ;
--------------------------------------------------------
--  DDL for Index EVALUATION_GROUPS_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."EVALUATION_GROUPS_PK" ON "MUSE"."EVALUATION_GROUPS" ("EVAL_ID", "GROUP_NUM") 
  ;
--------------------------------------------------------
--  DDL for Index EVALUATION_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."EVALUATION_PK" ON "MUSE"."EVALUATION" ("ID") 
  ;
--------------------------------------------------------
--  DDL for Index PASSWORD_RECOVERY_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."PASSWORD_RECOVERY_PK" ON "MUSE"."PASSWORD_RECOVERY" ("KEY") 
  ;
--------------------------------------------------------
--  DDL for Index RECOMMENDATION_LIST_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."RECOMMENDATION_LIST_PK" ON "MUSE"."RECOMMENDATION_LIST" ("CONSUMER", "LIST_ID") 
  ;
--------------------------------------------------------
--  DDL for Index RECOMMENDATION_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."RECOMMENDATION_PK" ON "MUSE"."RECOMMENDATION" ("ID") 
  ;
--------------------------------------------------------
--  DDL for Index TRACKS_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "MUSE"."TRACKS_PK" ON "MUSE"."TRACKS" ("ID") 
  ;
