# Tasks schema

# --- !Ups

create table users (
  login                     varchar(255) not null primary key,
  email                     varchar(255) not null,
  firstname                 varchar(255) not null,
  lastname                  varchar(255) not null,
  password                  varchar(255) not null,
  skype                     varchar(255) not null,
  transverse                boolean not null,
  role                      varchar(255)
);

create table team (
  name                      varchar(255) not null primary key,
  scrum_master              varchar(255),
  steering                  boolean not null,
  foreign key(scrum_master) references users(login) on delete set null
);

create table team_member (
  team                      varchar(255) not null,
  user_login                varchar(255) not null,
  foreign key(team)         references team(name) on delete cascade,
  foreign key(user_login)   references users(login) on delete cascade
);

create sequence meeting_seq;

create table meeting (
  id                        bigint not null primary key,
  meeting_date              date not null,
  team                      varchar(255) not null,
  topic                     varchar(255),
  problem                   boolean,
  foreign key(team)         references team(name) on delete cascade
);

create table meeting_member (
  meeting_id                bigint not null,
  user_login                varchar(255) not null,
  foreign key(meeting_id)   references meeting(id) on delete cascade,
  foreign key(user_login)   references users(login) on delete cascade
);

create sequence meeting_item_seq;

create table meeting_item (
  id                        bigint not null primary key,
  meeting                   bigint not null,
  jiraIssue                 varchar(255),
  content                   varchar(10000),
  bug                       boolean not null,
  impediment                boolean not null,
  grouping                  varchar(255) not null,
  foreign key(meeting)      references meeting(id) on delete cascade
);

create table iteration (
  name                      varchar(255) not null primary key,
  date_start                date not null
);

create table guest (
  team                      varchar(255) not null,
  user_login                varchar(255),
  day_number                int not null,
  foreign key(team)         references team(name) on delete cascade,
  foreign key(user_login)   references users(login) on delete set null
);
# --- !Downs

drop sequence if exists meeting_item_seq;
drop table if exists meeting_item;
drop table if exists meeting_member;
drop table if exists meeting;
drop sequence if exists meeting_seq;
drop table if exists team_member;
drop table if exists guest;
drop table if exists team;
drop table if exists users;
drop table if exists iteration;
